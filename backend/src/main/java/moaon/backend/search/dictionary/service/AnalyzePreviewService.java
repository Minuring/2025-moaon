package moaon.backend.search.dictionary.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import moaon.backend.search.ElasticsearchClientConfig;
import moaon.backend.search.dictionary.dto.AnalyzeResponse;
import moaon.backend.search.dictionary.dto.AnalyzedToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * alias가 가리키는 현재 인덱스의 분석기로 입력을 분석한다.
 * 사용자 사전은 인덱스를 만들거나 열 때 읽으므로, dictionary.txt를 바꿔도 재색인 전까지는 결과가 바뀌지 않는다.
 */
@Service
public class AnalyzePreviewService {

    private static final String ARTICLE_ALIAS = "articles";
    private static final String INDEX_ANALYZER = "article_nori_analyzer";
    private static final String SEARCH_ANALYZER = "article_nori_search_analyzer";
    private static final String SETTINGS_PATH = "elasticsearch/article-settings.json";

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectNode noDictionaryChain;

    public AnalyzePreviewService(
            @Qualifier(ElasticsearchClientConfig.BULK_CLIENT) ElasticsearchClient elasticsearchClient,
            ObjectMapper objectMapper
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.noDictionaryChain = buildNoDictionaryChain(objectMapper);
    }

    public AnalyzeResponse analyze(String text) {
        try {
            String index = elasticsearchClient.indices().getAlias(a -> a.name(ARTICLE_ALIAS))
                    .result().keySet().iterator().next();
            return new AnalyzeResponse(
                    index,
                    analyze(AnalyzeRequest.of(a -> a.index(ARTICLE_ALIAS).analyzer(INDEX_ANALYZER).text(text))),
                    analyze(AnalyzeRequest.of(a -> a.index(ARTICLE_ALIAS).analyzer(SEARCH_ANALYZER).text(text))),
                    analyze(noDictionaryRequest(text))
            );
        } catch (IOException e) {
            throw new UncheckedIOException("분석 요청 실패", e);
        }
    }

    private List<AnalyzedToken> analyze(AnalyzeRequest request) throws IOException {
        return elasticsearchClient.indices().analyze(request).tokens().stream()
                .map(AnalyzedToken::from)
                .toList();
    }

    private AnalyzeRequest noDictionaryRequest(String text) {
        ObjectNode body = noDictionaryChain.deepCopy();
        body.put("text", text);
        return AnalyzeRequest.of(a -> a.index(ARTICLE_ALIAS).withJson(new StringReader(body.toString())));
    }

    // 색인 분석기 설정에서 토크나이저의 user_dictionary만 뺀 분석 체인을 만든다.
    private static ObjectNode buildNoDictionaryChain(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(SETTINGS_PATH).getInputStream()) {
            JsonNode analysis = objectMapper.readTree(in).path("analysis");
            JsonNode analyzer = analysis.path("analyzer").path(INDEX_ANALYZER);
            ObjectNode tokenizer = analysis.path("tokenizer").path(analyzer.path("tokenizer").asText()).deepCopy();
            tokenizer.remove("user_dictionary");

            ObjectNode chain = objectMapper.createObjectNode();
            chain.set("tokenizer", tokenizer);
            chain.set("char_filter", analyzer.path("char_filter"));
            chain.set("filter", analyzer.path("filter"));
            return chain;
        } catch (IOException e) {
            throw new UncheckedIOException("분석 설정을 읽지 못했습니다: " + SETTINGS_PATH, e);
        }
    }
}

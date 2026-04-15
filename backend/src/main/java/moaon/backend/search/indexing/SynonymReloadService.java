package moaon.backend.search.indexing;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ReloadSearchAnalyzersResponse;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.dictionary.dto.ReloadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SynonymReloadService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.article-index:articles}")
    private String articleIndex;

    public ReloadResponse reload() {
        try {
            ReloadSearchAnalyzersResponse response =
                    elasticsearchClient.indices().reloadSearchAnalyzers(r -> r.index(articleIndex));

            String detail = response.reloadDetails().stream()
                    .map(d -> d.index() + " → " + d.reloadedAnalyzers())
                    .collect(Collectors.joining(", "));

            log.info("Synonym reload 성공: {}", detail);
            return new ReloadResponse(true, "ES synonym reload 완료: " + detail);
        } catch (Exception e) {
            log.error("Synonym reload 실패", e);
            return new ReloadResponse(false, "synonym reload 실패: " + e.getMessage());
        }
    }
}

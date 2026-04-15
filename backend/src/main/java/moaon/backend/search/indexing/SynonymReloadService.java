package moaon.backend.search.indexing;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
            elasticsearchClient.indices().reloadSearchAnalyzers(r -> r.index(articleIndex));
            log.info("Synonym reload 성공: index={}", articleIndex);
            return new ReloadResponse(true, "synonym reload 요청이 완료되었습니다.");
        } catch (Exception e) {
            log.error("Synonym reload 실패", e);
            return new ReloadResponse(false, "synonym reload 실패: " + e.getMessage());
        }
    }
}

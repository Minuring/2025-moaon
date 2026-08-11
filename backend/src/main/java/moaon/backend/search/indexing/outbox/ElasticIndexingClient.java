package moaon.backend.search.indexing.outbox;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ElasticsearchClient를 직접 의존하면 테스트에서 client._transport().jsonpMapper() 같은
 * 내부 체이닝까지 스텁해야 해서 목킹이 번거롭다. IndexEventWorker가 이 얇은 래퍼에만
 * 의존하게 해서, 테스트에서는 이 클래스 하나만 목킹하면 되게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ElasticIndexingClient {

    private static final String INDEX_NAME = "articles";
    private static final int HEALTH_CHECK_INTERVAL_SECONDS = 5;

    private final ElasticsearchClient esClient;
    @Getter
    private volatile boolean healthy = true;

    public void upsert(ArticleDocument doc) throws IOException {
        esClient.update(u -> u
                        .index(INDEX_NAME)
                        .id(String.valueOf(doc.getId()))
                        .doc(doc.convertToJson())
                        .docAsUpsert(true),
                ArticleDocument.class);
    }

    public void delete(Long entityId) throws IOException {
        esClient.delete(d -> d
                .index(INDEX_NAME)
                .id(String.valueOf(entityId))
        );
    }

    @Scheduled(fixedDelay = HEALTH_CHECK_INTERVAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void refreshHealthStatus() {
        boolean prev = healthy;
        try {
            healthy = HealthStatus.Red != esClient.cluster().health().status();
        } catch (IOException ignore) {
            healthy = false;
        }
        if (prev != healthy) {
            log.info("Elasticsearch healthy : {}로 변경됨", healthy);
        }
    }
}

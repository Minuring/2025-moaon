package moaon.backend.article.repository.es.event;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.domain.Article;
import moaon.backend.article.repository.es.ArticleDocument;
import moaon.backend.article.repository.db.ArticleDBRepository;
import moaon.backend.article.repository.es.event.IndexEvent.Action;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSyncScheduler {

    private static final int BATCH_SIZE = 100;

    private final IndexEventRepository indexEventRepository;
    private final ArticleDBRepository articleRepository;
    private final ElasticsearchClient esClient;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndProcessEvents() {
        List<IndexEvent> events = indexEventRepository.findDueToProcess(PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty() || !isEsClusterHealthy()) {
            return;
        }

        for (IndexEvent e : events) {
            try {
                processOne(e);
                indexEventRepository.markAsProcessed(e);
            } catch (Exception ex) {
                log.warn("Index sync failed. entityId={}, action={}, processedRevision={}, requiredRevision={}, msg={}",
                        e.getEntityId(), e.getAction(), e.getProcessedRevision(), e.getRequiredRevision(),
                        ex.getMessage());
            }
        }
    }

    private boolean isEsClusterHealthy() {
        try {
            return HealthStatus.Red != esClient.cluster().health().status();
        } catch (IOException ignore) {
            return false;
        }
    }

    private void processOne(IndexEvent e) throws IOException {
        if (e.getAction() == Action.DELETED) {
            delete(e);
            return;
        }

        Article article = articleRepository.findById(e.getEntityId()).orElse(null);
        if (article == null) {
            delete(e);
            return;
        }

        if (e.getAction() == Action.INDEXING) {
            upsert(e, article);
        }
    }

    private void delete(final IndexEvent e) throws IOException {
        esClient.delete(d -> d
                .index("articles")
                .id(String.valueOf(e.getEntityId()))
        );
    }

    private void upsert(final IndexEvent e, final Article article) throws IOException {
        ArticleDocument doc = new ArticleDocument(article);
        esClient.update(u -> u
                        .index("articles")
                        .id(String.valueOf(e.getEntityId()))
                        .doc(doc.convertToJson())
                        .docAsUpsert(true),
                ArticleDocument.class);
    }
}

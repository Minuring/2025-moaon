package moaon.backend.search.indexing.outbox;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.domain.Article;
import moaon.backend.article.repository.ArticleDBRepository;
import moaon.backend.search.query.ArticleDocument;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexEventWorker {

    private static final int BATCH_SIZE = 100;

    private final IndexEventRepository indexEventRepository;
    private final ArticleDBRepository articleRepository;
    private final ElasticsearchClient esClient;

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void doIndexIfRequired() {
        List<IndexEvent> events = indexEventRepository.findDueToProcess(PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty() || isEsClusterNotHealthy()) {
            return;
        }

        for (IndexEvent e : events) {
            syncEvent(e);
            indexEventRepository.markAsProcessed(e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doIndexOneAsync(Long entityId) {
        if (isEsClusterNotHealthy()) {
            return;
        }

        indexEventRepository.findByEntityId(entityId)
                .ifPresent(e -> {
                    syncEvent(e);
                    indexEventRepository.markAsProcessed(e);
                });
    }

    private void syncEvent(IndexEvent e) {
        try {
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
                upsert(article);
            }

        } catch (Exception ex) {
            log.warn("Index sync failed. entityId={}, action={}, processedRevision={}, requiredRevision={}, msg={}",
                    e.getEntityId(), e.getAction(), e.getProcessedRevision(), e.getRequiredRevision(),
                    ex.getMessage());
        }
    }

    private void delete(final IndexEvent e) throws IOException {
        esClient.delete(d -> d
                .index("articles")
                .id(String.valueOf(e.getEntityId()))
        );
    }

    private void upsert(final Article article) throws IOException {
        ArticleDocument doc = new ArticleDocument(article);
        esClient.update(u -> u
                        .index("articles")
                        .id(String.valueOf(article.getId()))
                        .doc(doc.convertToJson())
                        .docAsUpsert(true),
                ArticleDocument.class);
    }

    private boolean isEsClusterNotHealthy() {
        try {
            return HealthStatus.Red == esClient.cluster().health().status();
        } catch (IOException ignore) {
            return true;
        }
    }
}

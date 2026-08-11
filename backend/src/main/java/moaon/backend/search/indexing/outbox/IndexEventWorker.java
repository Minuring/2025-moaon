package moaon.backend.search.indexing.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moaon.backend.article.repository.ArticleRepository;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import moaon.backend.search.query.ArticleDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexEventWorker {

    private static final int BATCH_SIZE = 1000;

    private final IndexEventRepository indexEventRepository;
    private final ArticleRepository articleRepository;
    private final ElasticIndexingClient indexingClient;
    private final TransactionTemplate transactionTemplate;

    //TODO: 배치로 변경
    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void doIndexIfRequired() {
        List<IndexEvent> events = indexEventRepository.findDueToProcess(PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) {
            return;
        }
        if (!indexingClient.isHealthy()) {
            log.warn("색인 배치 스킵: ES 비정상 상태, 대상 {}건 보류", events.size());
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        for (IndexEvent e : events) {
            try {
                processEvent(e);
                successCount++;
            } catch (Exception ex) {
                log.warn("Index sync failed. entityId={}, msg={}", e.getEntityId(), ex.getMessage());
                failureCount++;
            }
        }
        log.info("색인 스케줄러 미반영분 처리 완료: 성공 {}건, 실패 {}건 (대상 {}건)", successCount, failureCount, events.size());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void doIndexOneAsync(IndexRequestedEvent event) {
        if (!indexingClient.isHealthy()) {
            return;
        }

        IndexEvent indexEvent = indexEventRepository.findByEntityId(event.entityId()).orElseThrow();
        if (indexEvent.isProcessed()) {
            return;
        }

        try {
            processEvent(indexEvent);
        } catch (IOException ex) {
            log.warn("Index sync failed. entityId={}, msg={}", indexEvent.getEntityId(), ex.getMessage());
        }
    }

    private void processEvent(IndexEvent e) throws IOException {
        if (e.getAction() == Action.DELETED) {
            indexingClient.delete(e.getEntityId());
        } else {
            ArticleDocument doc = transactionTemplate.execute(status ->
                    articleRepository.findById(e.getEntityId())
                            .map(ArticleDocument::new)
                            .orElse(null));
            if (doc == null) {
                indexingClient.delete(e.getEntityId());
            } else if (e.getAction() == Action.INDEXING) {
                indexingClient.upsert(doc);
            }
        }

        markAsProcessed(e);
    }

    private void markAsProcessed(IndexEvent e) {
        transactionTemplate.executeWithoutResult(status -> indexEventRepository.markAsProcessed(e));
    }
}

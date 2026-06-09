package moaon.backend.search.indexing.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import moaon.backend.search.indexing.outbox.IndexEvent;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import moaon.backend.search.indexing.outbox.IndexEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("IndexEventRepository JPQL 쿼리 테스트")
class IndexEventRepositoryTest {

    @Autowired
    private IndexEventRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findDueToProcess: processedRevision < requiredRevision인 이벤트를 조회한다")
    void findEventsNeedingProcessing() {
        // given
        IndexEvent event1 = IndexEvent.builder()
                .entityId(100L)
                .action(Action.INDEXING)
                .requiredRevision(3)
                .processedRevision(2)
                .build();

        IndexEvent event2 = IndexEvent.builder()
                .entityId(200L)
                .action(Action.DELETED)
                .requiredRevision(5)
                .processedRevision(5)
                .build();

        IndexEvent event3 = IndexEvent.builder()
                .entityId(300L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();

        // when
        List<IndexEvent> events = repository.findDueToProcess(PageRequest.of(0, 10));

        // then
        assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events).extracting(IndexEvent::getEntityId)
                .contains(100L, 300L);
    }

    @Test
    @DisplayName("findDueToProcess: Pageable로 결과 개수를 제한한다")
    void findEventsWithPagination() {
        // given - 기존 데이터가 있을 수 있으므로 테스트 데이터를 충분히 추가
        for (long i = 1000; i <= 1010; i++) {
            IndexEvent event = IndexEvent.builder()
                    .entityId(i)
                    .action(Action.INDEXING)
                    .requiredRevision(1)
                    .processedRevision(0)
                    .build();
            entityManager.persist(event);
        }
        entityManager.flush();

        // when
        List<IndexEvent> events = repository.findDueToProcess(PageRequest.of(0, 5));

        // then
        assertThat(events).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("findDueToProcess: 모든 이벤트가 처리 완료면 해당 이벤트를 반환하지 않는다")
    void findEventsReturnsEmptyWhenAllProcessed() {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(2000L)
                .action(Action.INDEXING)
                .requiredRevision(3)
                .processedRevision(3)
                .build();
        entityManager.persist(event);
        entityManager.flush();

        // when
        List<IndexEvent> events = repository.findDueToProcess(PageRequest.of(0, 100));

        // then
        assertThat(events).extracting(IndexEvent::getEntityId)
                .doesNotContain(2000L);
    }

    @Test
    @DisplayName("markAsProcessed: processedRevision을 requiredRevision으로 업데이트한다")
    void updateProcessedRevision() {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(3000L)
                .action(Action.INDEXING)
                .requiredRevision(5)
                .processedRevision(2)
                .build();
        entityManager.persist(event);
        entityManager.flush();
        entityManager.clear();

        // when
        repository.markAsProcessed(event);
        entityManager.flush();
        entityManager.clear();

        // then
        IndexEvent found = entityManager.find(IndexEvent.class, 3000L);
        assertThat(found.getProcessedRevision()).isEqualTo(5);
    }

    @Test
    @DisplayName("merge: 새로운 이벤트를 INSERT한다.")
    void insertNewEventWithMerge() {
        // given
        IndexEvent event = IndexEvent.builder()
                .entityId(4000L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        // when
        repository.merge(event);
        entityManager.flush();
        entityManager.clear();

        // then
        IndexEvent found = entityManager.find(IndexEvent.class, 4000L);
        assertThat(found).isNotNull();
        assertThat(found.getEntityId()).isEqualTo(4000L);
        assertThat(found.getAction()).isEqualTo(Action.INDEXING);
    }

    @Test
    @DisplayName("incrementRequiredRevisionSince: updatedAt >= since인 이벤트의 requiredRevision을 1 증가시킨다")
    void incrementRequiredRevisionForRecentEvents() {
        // given
        LocalDateTime since = LocalDateTime.of(2024, 6, 1, 0, 0, 0);

        IndexEvent recentEvent = IndexEvent.builder()
                .entityId(7000L)
                .action(Action.INDEXING)
                .requiredRevision(2)
                .processedRevision(2)
                .updatedAt(since.plusHours(1))
                .build();

        IndexEvent oldEvent = IndexEvent.builder()
                .entityId(7001L)
                .action(Action.INDEXING)
                .requiredRevision(2)
                .processedRevision(2)
                .updatedAt(since.minusSeconds(1))
                .build();

        entityManager.persist(recentEvent);
        entityManager.persist(oldEvent);
        entityManager.flush();
        entityManager.clear();

        // when
        int count = repository.incrementRequiredRevisionSince(since);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(count).isEqualTo(1);
        assertThat(entityManager.find(IndexEvent.class, 7000L).getRequiredRevision()).isEqualTo(3);
        assertThat(entityManager.find(IndexEvent.class, 7001L).getRequiredRevision()).isEqualTo(2);
    }

    @Test
    @DisplayName("incrementRequiredRevisionSince: updatedAt == since인 경계값 이벤트도 포함된다")
    void incrementRequiredRevisionIncludesBoundary() {
        // given
        LocalDateTime since = LocalDateTime.of(2024, 6, 1, 0, 0, 0);

        IndexEvent exactEvent = IndexEvent.builder()
                .entityId(8000L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(1)
                .updatedAt(since)
                .build();

        entityManager.persist(exactEvent);
        entityManager.flush();
        entityManager.clear();

        // when
        repository.incrementRequiredRevisionSince(since);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(entityManager.find(IndexEvent.class, 8000L).getRequiredRevision()).isEqualTo(2);
    }

    @Test
    @DisplayName("merge: 기존 이벤트가 있으면 action을 업데이트하고 requiredRevision을 1 증가시킨다")
    void updateExistingEventWithMerge() {
        // given
        IndexEvent existing = IndexEvent.builder()
                .entityId(5000L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(1)
                .build();
        entityManager.persist(existing);
        entityManager.flush();

        int initialRevision = existing.getRequiredRevision();

        entityManager.clear();

        IndexEvent newEvent = IndexEvent.builder()
                .entityId(5000L)
                .action(Action.DELETED)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        // when
        repository.merge(newEvent);
        entityManager.flush();
        entityManager.clear();

        // then
        IndexEvent found = entityManager.find(IndexEvent.class, 5000L);
        assertThat(found.getRequiredRevision()).isEqualTo(initialRevision + 1);
        assertThat(found.getAction()).isEqualTo(Action.DELETED);
    }
}

package moaon.backend.search.indexing.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import moaon.backend.article.domain.Article;
import moaon.backend.search.indexing.outbox.IndexEvent;
import moaon.backend.search.indexing.outbox.IndexEvent.Action;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IndexEvent 단위 테스트")
class IndexEventTest {

    @Test
    @DisplayName("entityId로 IndexEvent 생성 시 필드가 올바르게 초기화된다")
    void createIndexEventFromArticle() {
        // given
        Article article = Article.builder()
                .id(100L)
                .build();

        // when
        IndexEvent event = new IndexEvent(article.getId(), Action.INDEXING);

        // then
        assertThat(event.getEntityId()).isEqualTo(100L);
        assertThat(event.getAction()).isEqualTo(Action.INDEXING);
        assertThat(event.getRequiredRevision()).isZero();
        assertThat(event.getProcessedRevision()).isZero();
        assertThat(event.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Builder로 생성한 IndexEvent의 기본값이 올바르게 설정된다")
    void createIndexEventWithBuilder() {
        // when
        IndexEvent event = IndexEvent.builder()
                .entityId(200L)
                .action(Action.DELETED)
                .requiredRevision(3)
                .processedRevision(2)
                .build();

        // then
        assertThat(event.getEntityId()).isEqualTo(200L);
        assertThat(event.getAction()).isEqualTo(Action.DELETED);
        assertThat(event.getRequiredRevision()).isEqualTo(3);
        assertThat(event.getProcessedRevision()).isEqualTo(2);
        assertThat(event.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 entityId를 가진 IndexEvent는 동일하다")
    void equalsWithSameEntityId() {
        // given
        IndexEvent event1 = IndexEvent.builder()
                .entityId(100L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        IndexEvent event2 = IndexEvent.builder()
                .entityId(100L)
                .action(Action.DELETED)
                .requiredRevision(5)
                .processedRevision(3)
                .build();

        // when & then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    @DisplayName("다른 entityId를 가진 IndexEvent는 동일하지 않다")
    void notEqualsWithDifferentEntityId() {
        // given
        IndexEvent event1 = IndexEvent.builder()
                .entityId(100L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        IndexEvent event2 = IndexEvent.builder()
                .entityId(200L)
                .action(Action.INDEXING)
                .requiredRevision(1)
                .processedRevision(0)
                .build();

        // when & then
        assertThat(event1).isNotEqualTo(event2);
    }
}

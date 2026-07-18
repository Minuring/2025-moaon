package moaon.backend.article.draft.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import moaon.backend.article.draft.domain.ArticleDraft;
import moaon.backend.fixture.Fixture;
import moaon.backend.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("ArticleDraftRepository 테스트")
class ArticleDraftRepositoryTest {

    @Autowired
    private ArticleDraftRepository repository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("deleteByCreatedAtBefore: 기준 시각 이전에 생성된 draft만 삭제한다")
    @Test
    void deleteExpiredDrafts() {
        // given
        Member member = Fixture.anyMember();
        entityManager.persist(member);

        ArticleDraft oldDraft = new ArticleDraft(member, "https://old.example.com", "old title", "old content");
        ArticleDraft recentDraft = new ArticleDraft(member, "https://recent.example.com", "recent title", "recent content");
        entityManager.persist(oldDraft);
        entityManager.persist(recentDraft);
        entityManager.flush();

        entityManager.createQuery("update ArticleDraft d set d.createdAt = :time where d.id = :id")
                .setParameter("time", LocalDateTime.now().minusDays(2))
                .setParameter("id", oldDraft.getId())
                .executeUpdate();
        entityManager.clear();

        // when
        int deletedCount = repository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(1));

        // then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(repository.findById(oldDraft.getId())).isEmpty();
        assertThat(repository.findById(recentDraft.getId())).isPresent();
    }
}

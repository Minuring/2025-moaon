package moaon.backend.article.draft.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import moaon.backend.article.domain.Sector;
import moaon.backend.article.domain.Topic;
import moaon.backend.article.draft.ArticleDraft;
import moaon.backend.article.draft.ArticleDraftRepository;
import moaon.backend.fixture.Fixture;
import moaon.backend.member.Member;
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

    @DisplayName("deleteByCreatedAtBefore: 분석까지 완료되어 자식 테이블에 행이 있는 draft도 기준 시각 이전이면 함께 삭제한다")
    @Test
    void deleteExpiredDrafts() {
        // given
        Member member = Fixture.anyMember();
        entityManager.persist(member);

        ArticleDraft oldDraft = new ArticleDraft(member, "https://old.example.com", "old title", "old content");
        oldDraft.applyAnalysis("분석된 요약", Sector.BE, List.of(Topic.TECHNOLOGY_ADOPTION), List.of("java"));
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

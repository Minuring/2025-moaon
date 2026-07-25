package moaon.backend.article.draft.repository;

import java.time.LocalDateTime;
import moaon.backend.article.draft.domain.ArticleDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleDraftRepository extends JpaRepository<ArticleDraft, Long> {

    @Modifying
    @Query("delete from ArticleDraft d where d.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}

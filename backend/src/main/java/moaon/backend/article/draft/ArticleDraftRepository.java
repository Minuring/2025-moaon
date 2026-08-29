package moaon.backend.article.draft;

import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ArticleDraftRepository extends JpaRepository<ArticleDraft, Long> {

    default ArticleDraft getById(Long id) {
        return findById(id).orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_DRAFT_NOT_FOUND));
    }

    @Modifying
    @Query("delete from ArticleDraft d where d.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}

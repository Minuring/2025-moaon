package moaon.backend.article.repository;

import java.util.List;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.dto.ArticleDto;

public record ArticleSearchResult(
        List<ArticleDto> articles,
        long totalCount,
        boolean hasNext,
        ArticleCursor nextCursor
) {
}

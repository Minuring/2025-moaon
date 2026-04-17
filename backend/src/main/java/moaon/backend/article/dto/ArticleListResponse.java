package moaon.backend.article.dto;

import java.util.List;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.repository.ArticleSearchResult;

public record ArticleListResponse(
        List<ArticleDto> contents,
        int totalCount,
        boolean hasNext,
        String nextCursor
) {

    public static ArticleListResponse from(ArticleSearchResult searchResult) {
        return new ArticleListResponse(
                searchResult.articles(),
                (int) searchResult.totalCount(),
                searchResult.hasNext(),
                nextCursorToString(searchResult.nextCursor())
        );
    }

    private static String nextCursorToString(ArticleCursor nextCursor) {
        if (nextCursor == null) {
            return null;
        }
        return nextCursor.toString();
    }
}

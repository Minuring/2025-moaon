package moaon.backend.fixture;

import java.util.List;
import moaon.backend.article.domain.ArticleCursor;
import moaon.backend.article.domain.ArticleSortType;
import moaon.backend.article.dto.ArticleDto;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.search.query.SearchWithLog;
import moaon.backend.search.log.domain.SearchLogCapture;

public class FakeArticleSearchResult {

    public static ArticleSearchResult create(
            List<ArticleDto> articles,
            long totalCount,
            int limit,
            ArticleSortType sortType
    ) {
        boolean hasNext = articles.size() == limit;
        ArticleCursor cursor = hasNext ? generateCursor(articles, sortType) : null;
        return new ArticleSearchResult(articles, totalCount, hasNext, cursor);
    }

    public static SearchWithLog createWithLog(
            List<ArticleDto> articles,
            long totalCount,
            int limit,
            ArticleSortType sortType
    ) {
        ArticleSearchResult result = create(articles, totalCount, limit, sortType);
        SearchLogCapture logCapture = new SearchLogCapture(null, (int) totalCount, 0, List.of());
        return new SearchWithLog(result, logCapture);
    }

    public static ArticleSearchResult empty() {
        return new ArticleSearchResult(List.of(), 0, false, null);
    }

    private static ArticleCursor generateCursor(List<ArticleDto> articles, ArticleSortType sortType) {
        ArticleDto last = articles.getLast();
        if (sortType == ArticleSortType.CREATED_AT) {
            return new ArticleCursor(last.createdAt(), last.id());
        }
        if (sortType == ArticleSortType.CLICKS) {
            return new ArticleCursor(last.clicks(), last.id());
        }
        if (sortType == ArticleSortType.RELEVANCE) {
            return new ArticleCursor(0.0, last.id());
        }
        return null;
    }
}

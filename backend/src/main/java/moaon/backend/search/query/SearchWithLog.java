package moaon.backend.search.query;

import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.search.log.domain.SearchLogCapture;

public record SearchWithLog(ArticleSearchResult result, SearchLogCapture logCapture) {}

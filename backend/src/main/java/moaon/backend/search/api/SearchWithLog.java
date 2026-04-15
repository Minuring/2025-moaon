package moaon.backend.search.api;

import moaon.backend.search.query.log.SearchLogCapture;

public record SearchWithLog(ArticleSearchResult result, SearchLogCapture logCapture) {}

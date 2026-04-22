package moaon.backend.search.query;

import java.util.List;
import moaon.backend.article.repository.ArticleSearchResult;
import moaon.backend.search.log.domain.SearchHitLog;

public record SearchWithLog(ArticleSearchResult result, List<SearchHitLog> hitLogs, int queryTimeMs) {}

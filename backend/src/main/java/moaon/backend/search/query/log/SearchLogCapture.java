package moaon.backend.search.query.log;

import java.util.List;

public record SearchLogCapture(String query, int resultCount, int queryTimeMs, List<SearchHitLog> hits) {}

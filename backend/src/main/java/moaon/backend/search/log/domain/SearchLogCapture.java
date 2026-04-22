package moaon.backend.search.log.domain;

import java.util.List;

public record SearchLogCapture(
        String query,
        int resultCount,
        int queryTimeMs,
        List<SearchHitLog> hits,
        FieldMatchStats fieldMatchStats,
        boolean hasCursor,
        Integer synonymMatchCount
) {}

package moaon.backend.search.log.dto;

import moaon.backend.search.log.domain.FieldMatchStats;
import moaon.backend.search.log.domain.SearchLogDocument;

import java.time.LocalDateTime;

public record SearchLogSummary(
        String id,
        String query,
        LocalDateTime searchedAt,
        int resultCount,
        int queryTimeMs,
        boolean hasCursor,
        Integer synonymMatchCount,
        FieldMatchStats fieldMatchStats
) {
    public static SearchLogSummary from(SearchLogDocument doc) {
        return new SearchLogSummary(
                doc.getId(),
                doc.getQuery(),
                doc.getSearchedAt(),
                doc.getResultCount(),
                doc.getQueryTimeMs(),
                doc.isHasCursor(),
                doc.getSynonymMatchCount(),
                doc.getFieldMatchStats()
        );
    }
}

package moaon.backend.search.log.dto;

import moaon.backend.search.log.domain.SearchLogDocument;

import java.time.LocalDateTime;
import java.util.List;

public record SearchLogSummary(
        String id,
        String query,
        LocalDateTime searchedAt,
        int resultCount,
        int queryTimeMs,
        int badCaseScore,
        List<String> suspectFlags
) {
    public static SearchLogSummary from(SearchLogDocument doc) {
        return new SearchLogSummary(
                doc.getId(),
                doc.getQuery(),
                doc.getSearchedAt(),
                doc.getResultCount(),
                doc.getQueryTimeMs(),
                doc.getBadCaseScore(),
                doc.getSuspectFlags() != null ? doc.getSuspectFlags() : List.of()
        );
    }
}

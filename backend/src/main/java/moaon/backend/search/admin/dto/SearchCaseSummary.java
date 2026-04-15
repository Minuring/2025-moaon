package moaon.backend.search.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.domain.SearchCase;

public record SearchCaseSummary(
    String id,
    String query,
    LocalDateTime searchedAt,
    int resultCount,
    int queryTimeMs,
    int badCaseScore,
    List<String> suspectFlags,
    ReviewStatus status
) {
    public static SearchCaseSummary from(SearchCase sc) {
        return new SearchCaseSummary(
            sc.getId(),
            sc.getQuery(),
            sc.getSearchedAt(),
            sc.getResultCount(),
            sc.getQueryTimeMs(),
            sc.getBadCaseScore(),
            sc.getSuspectFlags(),
            sc.getStatus()
        );
    }
}

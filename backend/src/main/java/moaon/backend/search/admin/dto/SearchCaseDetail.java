package moaon.backend.search.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import moaon.backend.search.admin.domain.ReviewStatus;
import moaon.backend.search.admin.domain.SearchCase;
import moaon.backend.search.admin.domain.SearchCaseResult;

public record SearchCaseDetail(
    String id,
    String query,
    LocalDateTime searchedAt,
    int resultCount,
    int queryTimeMs,
    int badCaseScore,
    List<String> suspectFlags,
    ReviewStatus status,
    String memo,
    List<SearchHitLogSummary> hits
) {
    public static SearchCaseDetail from(SearchCase sc, List<SearchCaseResult> results) {
        List<SearchHitLogSummary> hits = results.stream()
            .map(SearchHitLogSummary::from)
            .toList();
        return new SearchCaseDetail(
            sc.getId(),
            sc.getQuery(),
            sc.getSearchedAt(),
            sc.getResultCount(),
            sc.getQueryTimeMs(),
            sc.getBadCaseScore(),
            sc.getSuspectFlags(),
            sc.getStatus(),
            sc.getMemo(),
            hits
        );
    }

    public record SearchHitLogSummary(
        int rank,
        Long docId,
        String title,
        float score,
        List<String> matchedFields
    ) {
        public static SearchHitLogSummary from(SearchCaseResult r) {
            return new SearchHitLogSummary(
                r.getRank(),
                r.getDocId(),
                r.getTitle(),
                r.getScore(),
                r.getMatchedFields()
            );
        }
    }
}

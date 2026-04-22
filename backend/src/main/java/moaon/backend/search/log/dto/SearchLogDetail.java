package moaon.backend.search.log.dto;

import moaon.backend.search.log.domain.FieldMatchStats;
import moaon.backend.search.log.domain.SearchLogDocument;

import java.time.LocalDateTime;
import java.util.List;

public record SearchLogDetail(
        String id,
        String query,
        LocalDateTime searchedAt,
        int resultCount,
        int queryTimeMs,
        boolean hasCursor,
        Integer synonymMatchCount,
        FieldMatchStats fieldMatchStats,
        List<SearchHitSummary> hits
) {
    public static SearchLogDetail from(SearchLogDocument doc) {
        List<SearchHitSummary> hits = doc.getSearchedDocs() == null ? List.of() :
                doc.getSearchedDocs().stream().map(SearchHitSummary::from).toList();
        return new SearchLogDetail(
                doc.getId(),
                doc.getQuery(),
                doc.getSearchedAt(),
                doc.getResultCount(),
                doc.getQueryTimeMs(),
                doc.isHasCursor(),
                doc.getSynonymMatchCount(),
                doc.getFieldMatchStats(),
                hits
        );
    }

    public record SearchHitSummary(
            int rank,
            Long docId,
            String title,
            float score,
            List<String> matchedFields
    ) {
        public static SearchHitSummary from(SearchLogDocument.SearchedDoc r) {
            return new SearchHitSummary(
                    r.getRank(), r.getDocId(), r.getTitle(), r.getScore(), r.getMatchedFields());
        }
    }
}

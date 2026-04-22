package moaon.backend.search.log.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String query;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime searchedAt;

    @Field(type = FieldType.Integer)
    private int resultCount;

    @Field(type = FieldType.Integer)
    private int queryTimeMs;

    @Field(type = FieldType.Boolean)
    private boolean hasCursor;

    @Field(type = FieldType.Integer)
    private Integer synonymMatchCount;

    @Field(type = FieldType.Object)
    private FieldMatchStats fieldMatchStats;

    @Field(type = FieldType.Object)
    private List<SearchedDoc> searchedDocs;

    public SearchLogDocument(String query, int resultCount, int queryTimeMs,
                              boolean hasCursor, Integer synonymMatchCount,
                              FieldMatchStats fieldMatchStats,
                              List<SearchedDoc> searchedDocs) {
        this.id = UUID.randomUUID().toString();
        this.query = query;
        this.searchedAt = LocalDateTime.now();
        this.resultCount = resultCount;
        this.queryTimeMs = queryTimeMs;
        this.hasCursor = hasCursor;
        this.synonymMatchCount = synonymMatchCount;
        this.fieldMatchStats = fieldMatchStats;
        this.searchedDocs = searchedDocs;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SearchedDoc {
        private int rank;
        private Long docId;
        private String title;
        private float score;
        private List<String> matchedFields;
    }
}

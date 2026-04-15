package moaon.backend.search.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import moaon.backend.global.converter.StringListConverter;

@Entity
@Table(name = "search_case")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchCase {

    @Id
    private String id;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    @Column(nullable = false)
    private int resultCount;

    @Column(nullable = false)
    private int queryTimeMs;

    @Column(nullable = false)
    private int badCaseScore;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> suspectFlags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(length = 1000)
    private String memo;

    public SearchCase(String query, int resultCount, int queryTimeMs,
                      int badCaseScore, List<String> suspectFlags) {
        this.id = UUID.randomUUID().toString();
        this.query = query;
        this.searchedAt = LocalDateTime.now();
        this.resultCount = resultCount;
        this.queryTimeMs = queryTimeMs;
        this.badCaseScore = badCaseScore;
        this.suspectFlags = suspectFlags;
        this.status = ReviewStatus.NEW;
    }

    public void review(ReviewStatus status, String memo) {
        this.status = status;
        this.memo = memo;
    }
}

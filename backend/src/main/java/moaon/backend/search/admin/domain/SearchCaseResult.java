package moaon.backend.search.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import moaon.backend.global.converter.SnippetsConverter;
import moaon.backend.global.converter.StringListConverter;

@Entity
@Table(name = "search_case_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchCaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String caseId;

    @Column(name = "`rank`", nullable = false)
    private int rank;

    @Column(nullable = false)
    private Long docId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private float score;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> matchedFields;

    @Convert(converter = SnippetsConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private Map<String, List<String>> snippets;

    public SearchCaseResult(String caseId, int rank, Long docId, String title,
                            float score, List<String> matchedFields, Map<String, List<String>> snippets) {
        this.caseId = caseId;
        this.rank = rank;
        this.docId = docId;
        this.title = title;
        this.score = score;
        this.matchedFields = matchedFields;
        this.snippets = snippets;
    }
}

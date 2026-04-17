package moaon.backend.search.dictionary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "synonym_dictionary_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id")
@ToString
public class SynonymDictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String rawExpression;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public SynonymDictionaryEntry(List<String> terms) {
        this.rawExpression = String.join(", ", terms);
        this.createdAt = LocalDateTime.now();
    }

    public void update(List<String> terms) {
        this.rawExpression = String.join(", ", terms);
    }

    public List<String> terms() {
        return Arrays.stream(rawExpression.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}

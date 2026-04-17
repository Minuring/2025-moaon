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
@Table(name = "nori_dictionary_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id")
@ToString
public class NoriDictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String surface;

    @Column(nullable = false)
    private String segments;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public NoriDictionaryEntry(String surface, List<String> segmentList) {
        this.surface = surface;
        this.segments = String.join(" ", segmentList);
        this.createdAt = LocalDateTime.now();
    }

    public void update(String surface, List<String> segmentList) {
        this.surface = surface;
        this.segments = String.join(" ", segmentList);
    }

    public List<String> segmentList() {
        if (segments.isBlank()) return List.of();
        return Arrays.asList(segments.split(" "));
    }

    public String toFileLine() {
        return segments.isBlank() ? surface : surface + " " + segments;
    }
}

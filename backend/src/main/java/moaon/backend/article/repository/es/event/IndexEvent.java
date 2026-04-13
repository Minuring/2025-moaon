package moaon.backend.article.repository.es.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import moaon.backend.article.domain.Article;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "entityId", callSuper = false)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class IndexEvent {

    @Id
    @Column(nullable = false, unique = true)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false)
    private int requiredRevision;

    @Column(nullable = false)
    private int processedRevision;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public IndexEvent(Long entityId, Action action) {
        this.entityId = entityId;
        this.action = action;
        this.requiredRevision = 0;
        this.processedRevision = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Action {
        INDEXING,
        DELETED
    }
}

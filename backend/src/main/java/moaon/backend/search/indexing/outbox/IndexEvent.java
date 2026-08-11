package moaon.backend.search.indexing.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    public boolean isProcessed() {
        return processedRevision == requiredRevision;
    }

    public enum Action {
        INDEXING,
        DELETED
    }
}

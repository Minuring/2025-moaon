package moaon.backend.article.repository.es.event;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface IndexEventRepository extends Repository<IndexEvent, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO index_event (entity_id, action, required_revision, processed_revision, updated_at)
            VALUES (:#{#e.entityId}, :#{#e.action.name()}, 1, 0, NOW())
            ON DUPLICATE KEY UPDATE
              action = VALUES(action),
              updated_at = NOW(),
              required_revision = required_revision + 1;
            """,
            nativeQuery = true
    )
    void merge(@Param("e") IndexEvent e);

    @Modifying
    @Query("update IndexEvent e set e.processedRevision = GREATEST(e.processedRevision, LEAST(:#{#e.requiredRevision}, e.requiredRevision)) where e.entityId = :#{#e.entityId}")
    void markAsProcessed(@Param("e") IndexEvent e);

    @Query("SELECT i FROM IndexEvent i WHERE i.processedRevision < i.requiredRevision ORDER BY i.updatedAt ASC")
    @Transactional(readOnly = true)
    List<IndexEvent> findDueToProcess(Pageable pageable);
}

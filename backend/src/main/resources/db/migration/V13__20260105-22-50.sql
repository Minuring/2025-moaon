DROP TABLE event_outbox;

CREATE TABLE index_event
(
    entity_id          BIGINT       NOT NULL,
    action             VARCHAR(20)  NOT NULL,           -- 'INDEXING' | 'DELETED'
    required_revision  BIGINT       NOT NULL DEFAULT 0, -- 쓰기에서만 증가(변경 발생 카운터)
    processed_revision BIGINT       NOT NULL DEFAULT 0, -- 워커가 성공 반영한 지점
    updated_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (entity_id),
    KEY idx_due (processed_revision, required_revision, updated_at),
    KEY idx_action_due (action, processed_revision, required_revision, updated_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

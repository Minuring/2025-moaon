-- 1. 본문 컬럼 분리
CREATE TABLE article_content_separated
(
    id      BIGINT NOT NULL,
    content TEXT   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_article_content_separated_article
        FOREIGN KEY (id) REFERENCES article (id)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO article_content_separated (id, content)
SELECT id, content
FROM article;

ALTER TABLE article DROP COLUMN content;

-- 2. 조회 최적화용 인덱스 제거

DROP INDEX idx_article_fulltext ON article;
DROP INDEX idx_article_sector_created_at_id ON article;
DROP INDEX idx_article_sector_clicks_id ON article;
DROP INDEX idx_article_created_at_id ON article;
DROP INDEX idx_article_clicks_id ON article;

DROP INDEX idx_topics_article_id ON article_topics;

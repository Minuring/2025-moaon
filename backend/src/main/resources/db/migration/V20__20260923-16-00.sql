-- 튜닝

-- article.project_id not null제약 추가 (불필요한 Using where 제거)
ALTER TABLE article DROP FOREIGN KEY FK_article_project;
ALTER TABLE article MODIFY COLUMN project_id BIGINT NOT NULL;
ALTER TABLE article ADD CONSTRAINT FK_article_project FOREIGN KEY (project_id) REFERENCES project(id);

-- 항상 포함되는 정렬 조건에 대한 인덱스 추가
ALTER TABLE article ADD INDEX idx_createdat_id (created_at desc, id desc);
ALTER TABLE article ADD INDEX idx_clicks_id (clicks desc, id desc);
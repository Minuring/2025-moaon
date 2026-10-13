-- 튜닝

-- count 쿼리 테이블 풀스캔으로 인한 속도 저하 해결을 위한 인덱스 추가
ALTER TABLE article ADD INDEX idx_sector (sector);

ALTER TABLE article_topics ADD INDEX idx_topics (topics);
ALTER TABLE article_tech_stack ADD INDEX idx_tech_stack_id_article_id (tech_stack_id, article_id);

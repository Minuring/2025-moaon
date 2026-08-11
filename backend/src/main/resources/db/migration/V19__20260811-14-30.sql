-- content가 article_content로 분리되며 사라진 전문검색 인덱스를 테이블별로 재구성
-- (DB 폴백 검색의 MATCH AGAINST 실행을 위해 필수)
ALTER TABLE article
    ADD FULLTEXT INDEX idx_article_fulltext (title, summary) WITH PARSER ngram;

ALTER TABLE article_content
    ADD FULLTEXT INDEX idx_article_content_fulltext (content) WITH PARSER ngram;

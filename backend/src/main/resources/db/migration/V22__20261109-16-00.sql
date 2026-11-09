-- DB 폴백 검색이 title, summary만 사용하므로 본문 전문검색 인덱스 제거
ALTER TABLE article_content DROP INDEX idx_article_content_fulltext;

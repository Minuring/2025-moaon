-- FullText Index
ALTER TABLE project
    ADD FULLTEXT INDEX idx_project_fulltext (title, summary, description) WITH PARSER ngram;

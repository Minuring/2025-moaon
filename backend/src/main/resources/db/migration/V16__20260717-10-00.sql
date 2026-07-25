CREATE TABLE article_draft (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    crawled_title VARCHAR(500) NOT NULL,
    crawled_content TEXT NOT NULL,
    analyzed_summary VARCHAR(255),
    analyzed_sector VARCHAR(50),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_article_draft_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB;

CREATE TABLE article_draft_topic (
    article_draft_id BIGINT NOT NULL,
    topic VARCHAR(50) NOT NULL,
    CONSTRAINT FK_article_draft_topic_draft
        FOREIGN KEY (article_draft_id) REFERENCES article_draft (id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE article_draft_tech_stack (
    article_draft_id BIGINT NOT NULL,
    tech_stack_name VARCHAR(255) NOT NULL,
    CONSTRAINT FK_article_draft_tech_stack_draft
        FOREIGN KEY (article_draft_id) REFERENCES article_draft (id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

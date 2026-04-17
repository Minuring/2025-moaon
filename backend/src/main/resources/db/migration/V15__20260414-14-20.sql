CREATE TABLE nori_dictionary_entry
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    surface    VARCHAR(255) NOT NULL,
    segments   VARCHAR(500) NOT NULL DEFAULT '',
    created_at DATETIME(6)  NOT NULL
);

CREATE TABLE synonym_dictionary_entry
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    raw_expression VARCHAR(1000) NOT NULL,
    created_at     DATETIME(6)  NOT NULL
);

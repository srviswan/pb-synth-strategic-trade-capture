CREATE TABLE bk_book (
  book_id VARCHAR(64) PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  entity_name VARCHAR(256),
  desk VARCHAR(64),
  normalised_key VARCHAR(128),
  version BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX idx_bk_book_code ON bk_book (code);

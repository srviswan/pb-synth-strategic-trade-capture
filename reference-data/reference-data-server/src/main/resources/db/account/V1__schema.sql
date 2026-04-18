CREATE TABLE acc_account (
  account_id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(256),
  classification VARCHAR(64),
  credit_tier VARCHAR(32),
  stp_eligible BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 1
);

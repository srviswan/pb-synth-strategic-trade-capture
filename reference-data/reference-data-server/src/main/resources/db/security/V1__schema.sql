CREATE TABLE sec_security (
  security_id VARCHAR(64) PRIMARY KEY,
  ric VARCHAR(32),
  isin VARCHAR(32),
  currency VARCHAR(8),
  asset_type VARCHAR(64),
  description VARCHAR(512),
  version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_sec_security_ric ON sec_security (ric);

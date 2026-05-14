CREATE TABLE api_keys (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_value   VARCHAR(128) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    role        VARCHAR(32)  NOT NULL,   -- ADMIN | OPS | BANK
    bank_code   VARCHAR(32)  NULL,       -- populated for BANK role
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL,
    last_used_at DATETIME    NULL,

    CONSTRAINT uk_api_keys_value UNIQUE (key_value),
    INDEX idx_api_keys_enabled (enabled)
);

-- ---------------------------------------------------------------
-- Demo / seed keys  (replace before going to production)
-- ---------------------------------------------------------------
INSERT INTO api_keys (key_value, name, role, bank_code, enabled, created_at) VALUES
('sk-admin-switching-2026',  'Admin Key',      'ADMIN', NULL,     TRUE, NOW()),
('sk-ops-switching-2026',    'Operations Key', 'OPS',   NULL,     TRUE, NOW()),
('sk-bank-a-switching-2026', 'Bank A Key',     'BANK',  'BANK_A', TRUE, NOW()),
('sk-bank-b-switching-2026', 'Bank B Key',     'BANK',  'BANK_B', TRUE, NOW());

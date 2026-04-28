CREATE TABLE connector_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    connector_name VARCHAR(128) NOT NULL,
    bank_code VARCHAR(32) NOT NULL,
    connector_type VARCHAR(32) NOT NULL,
    endpoint_url VARCHAR(512) NULL,
    timeout_ms INT NOT NULL DEFAULT 5000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    force_reject BOOLEAN NOT NULL DEFAULT FALSE,
    reject_reason_code VARCHAR(32) NULL,
    reject_reason_message VARCHAR(512) NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,

    CONSTRAINT uk_connector_configs_connector_name UNIQUE (connector_name),
    INDEX idx_connector_configs_bank_code (bank_code),
    INDEX idx_connector_configs_enabled (enabled)
);

INSERT INTO connector_configs (
    connector_name,
    bank_code,
    connector_type,
    endpoint_url,
    timeout_ms,
    enabled,
    force_reject,
    reject_reason_code,
    reject_reason_message,
    created_at,
    updated_at
) VALUES
('MOCK_BANK_A_CONNECTOR', 'BANK_A', 'MOCK', NULL, 5000, TRUE, FALSE, 'AC01', 'Mock Bank A rejected transfer', NOW(), NULL),
('MOCK_BANK_B_CONNECTOR', 'BANK_B', 'MOCK', NULL, 5000, TRUE, FALSE, 'AC01', 'Mock Bank B rejected transfer', NOW(), NULL),
('MOCK_BANK_C_CONNECTOR', 'BANK_C', 'MOCK', NULL, 5000, TRUE, FALSE, 'AC01', 'Mock Bank C rejected transfer', NOW(), NULL);
CREATE TABLE participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_code VARCHAR(32) NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    participant_type VARCHAR(32) NOT NULL,
    country VARCHAR(8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,

    CONSTRAINT uk_participants_bank_code UNIQUE (bank_code)
);

CREATE TABLE routing_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_code VARCHAR(128) NOT NULL,
    source_bank VARCHAR(32) NOT NULL,
    destination_bank VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    connector_name VARCHAR(128) NOT NULL,
    priority INT NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,

    CONSTRAINT uk_routing_rules_route_code UNIQUE (route_code),
    INDEX idx_routing_rules_lookup (
        source_bank,
        destination_bank,
        message_type,
        enabled,
        priority
    )
);

INSERT INTO participants (
    bank_code,
    bank_name,
    status,
    participant_type,
    country,
    currency,
    created_at,
    updated_at
) VALUES
('BANK_A', 'Demo Bank A', 'ACTIVE', 'BANK', 'TH', 'THB', NOW(), NULL),
('BANK_B', 'Demo Bank B', 'ACTIVE', 'BANK', 'TH', 'THB', NOW(), NULL),
('BANK_C', 'Demo Bank C', 'ACTIVE', 'BANK', 'TH', 'THB', NOW(), NULL);

INSERT INTO routing_rules (
    route_code,
    source_bank,
    destination_bank,
    message_type,
    connector_name,
    priority,
    enabled,
    created_at,
    updated_at
) VALUES
('ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY', 'BANK_A', 'BANK_B', 'PACS_008', 'MOCK_BANK_B_CONNECTOR', 1, TRUE, NOW(), NULL),
('ROUTE_BANK_B_TO_BANK_A_PACS008_PRIMARY', 'BANK_B', 'BANK_A', 'PACS_008', 'MOCK_BANK_A_CONNECTOR', 1, TRUE, NOW(), NULL),
('ROUTE_BANK_A_TO_BANK_C_PACS008_PRIMARY', 'BANK_A', 'BANK_C', 'PACS_008', 'MOCK_BANK_C_CONNECTOR', 1, TRUE, NOW(), NULL),
('ROUTE_BANK_C_TO_BANK_A_PACS008_PRIMARY', 'BANK_C', 'BANK_A', 'PACS_008', 'MOCK_BANK_A_CONNECTOR', 1, TRUE, NOW(), NULL);
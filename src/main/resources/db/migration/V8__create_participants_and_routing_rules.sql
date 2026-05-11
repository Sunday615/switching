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

DROP TABLE IF EXISTS routing_rules;

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
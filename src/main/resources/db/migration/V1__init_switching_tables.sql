CREATE TABLE participant_banks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bank_code VARCHAR(20) NOT NULL,
    bank_name VARCHAR(120) NOT NULL,
    short_name VARCHAR(40),
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_participant_banks_bank_code UNIQUE (bank_code)
) ENGINE=InnoDB;

CREATE TABLE routing_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_code VARCHAR(40) NOT NULL,
    destination_bank_code VARCHAR(20) NOT NULL,
    connector_name VARCHAR(80) NOT NULL,
    priority INT NOT NULL DEFAULT 1,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_routing_rules_route_code UNIQUE (route_code),
    CONSTRAINT fk_routing_rules_bank_code
        FOREIGN KEY (destination_bank_code) REFERENCES participant_banks(bank_code)
) ENGINE=InnoDB;

CREATE TABLE transfers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_ref VARCHAR(255) NOT NULL,
    client_transfer_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    source_bank_code VARCHAR(20) NOT NULL,
    source_account_no VARCHAR(40) NOT NULL,
    destination_bank_code VARCHAR(20) NOT NULL,
    destination_account_no VARCHAR(40) NOT NULL,
    destination_account_name VARCHAR(120),
    amount DECIMAL(38,2),
    currency VARCHAR(255),
    channel_id VARCHAR(40) NOT NULL,
    route_code VARCHAR(40),
    connector_name VARCHAR(80),
    external_reference VARCHAR(80),
    status VARCHAR(30) NOT NULL,
    error_code VARCHAR(40),
    error_message VARCHAR(255),
    reference VARCHAR(255),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_transfers_transfer_ref UNIQUE (transfer_ref),
    CONSTRAINT uq_transfers_channel_client_transfer UNIQUE (channel_id, client_transfer_id)
) ENGINE=InnoDB;

CREATE INDEX idx_transfers_status_created_at
    ON transfers(status, created_at);

CREATE INDEX idx_transfers_destination_bank_code
    ON transfers(destination_bank_code);

CREATE INDEX idx_transfers_external_reference
    ON transfers(external_reference);

CREATE TABLE transfer_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_ref VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason_code VARCHAR(40),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

CREATE INDEX idx_transfer_status_history_ref_created_at
    ON transfer_status_history(transfer_ref, created_at);

CREATE TABLE idempotency_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    transfer_ref VARCHAR(255),
    status VARCHAR(30),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expired_at DATETIME(3),
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
) ENGINE=InnoDB;

CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_ref VARCHAR(255) NOT NULL,
    message_type VARCHAR(100),
    payload LONGTEXT,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events(status, created_at);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(60),
    reference_type VARCHAR(40),
    reference_id VARCHAR(60),
    actor VARCHAR(60),
    payload LONGTEXT,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;
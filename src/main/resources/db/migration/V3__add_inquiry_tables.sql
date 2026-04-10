CREATE TABLE inquiries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_ref VARCHAR(255) NOT NULL,
    client_inquiry_id VARCHAR(255),
    source_bank VARCHAR(20) NOT NULL,
    destination_bank VARCHAR(20) NOT NULL,
    creditor_account VARCHAR(40) NOT NULL,
    destination_account_name VARCHAR(120),
    amount DECIMAL(38,2),
    currency VARCHAR(20),
    channel_id VARCHAR(40) NOT NULL,
    route_code VARCHAR(40),
    connector_name VARCHAR(80),
    account_found BOOLEAN NOT NULL DEFAULT FALSE,
    bank_available BOOLEAN NOT NULL DEFAULT FALSE,
    eligible_for_transfer BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    error_code VARCHAR(40),
    error_message VARCHAR(255),
    reference VARCHAR(255),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_inquiries_inquiry_ref UNIQUE (inquiry_ref)
) ENGINE=InnoDB;

CREATE INDEX idx_inquiries_status_created_at
    ON inquiries(status, created_at);

CREATE INDEX idx_inquiries_destination_bank
    ON inquiries(destination_bank);

CREATE TABLE inquiry_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_ref VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason_code VARCHAR(40),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

CREATE INDEX idx_inquiry_status_history_ref_created_at
    ON inquiry_status_history(inquiry_ref, created_at);

CREATE TABLE iso_inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inquiry_ref VARCHAR(80) NOT NULL,
    channel_id VARCHAR(50) NOT NULL,
    message_id VARCHAR(120) NOT NULL,
    instruction_id VARCHAR(120) NULL,
    end_to_end_id VARCHAR(120) NULL,

    source_bank_code VARCHAR(30) NOT NULL,
    destination_bank_code VARCHAR(30) NOT NULL,

    debtor_account_no VARCHAR(60) NULL,
    creditor_account_no VARCHAR(60) NOT NULL,

    amount DECIMAL(19, 2) NULL,
    currency VARCHAR(10) NULL,
    reference VARCHAR(255) NULL,

    status VARCHAR(30) NOT NULL,
    account_found BOOLEAN NOT NULL DEFAULT FALSE,
    bank_available BOOLEAN NOT NULL DEFAULT FALSE,
    eligible_for_transfer BOOLEAN NOT NULL DEFAULT FALSE,

    failure_code VARCHAR(30) NULL,
    failure_message TEXT NULL,

    expires_at DATETIME NULL,
    used_by_transfer_ref VARCHAR(80) NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_iso_inquiries_inquiry_ref (inquiry_ref),
    UNIQUE KEY uq_iso_inquiries_channel_message (channel_id, message_id),
    KEY idx_iso_inquiries_status (status),
    KEY idx_iso_inquiries_source_destination (source_bank_code, destination_bank_code),
    KEY idx_iso_inquiries_used_by_transfer_ref (used_by_transfer_ref)
);
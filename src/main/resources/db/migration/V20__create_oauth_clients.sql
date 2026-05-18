CREATE TABLE oauth_clients (
    client_id          VARCHAR(128) PRIMARY KEY,
    psp_id             VARCHAR(32)  NOT NULL,
    client_secret_hash VARCHAR(64)  NOT NULL,
    tier               VARCHAR(16)  NOT NULL,
    scopes             TEXT         NOT NULL,
    created_at         DATETIME     NOT NULL,
    expires_at         DATETIME     NULL,
    status             VARCHAR(32)  NOT NULL,

    CONSTRAINT fk_oauth_clients_psp
        FOREIGN KEY (psp_id) REFERENCES participants(bank_code),
    INDEX idx_oauth_clients_psp_status (psp_id, status),
    INDEX idx_oauth_clients_status_expiry (status, expires_at)
);

INSERT INTO oauth_clients (
    client_id,
    psp_id,
    client_secret_hash,
    tier,
    scopes,
    created_at,
    expires_at,
    status
) VALUES
('client-bank-a', 'BANK_A', SHA2('secret-bank-a-switching-2026', 256), 'TIER1', 'payments:write inquiries:write payments:read', NOW(), NULL, 'ACTIVE'),
('client-bank-b', 'BANK_B', SHA2('secret-bank-b-switching-2026', 256), 'TIER1', 'payments:write inquiries:write payments:read', NOW(), NULL, 'ACTIVE');

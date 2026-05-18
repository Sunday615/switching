-- V21: PSP mTLS client certificates
-- Stores SHA-256 fingerprints of X.509 client certificates issued to PSP
-- participants for mutual TLS authentication.
-- The fingerprint is computed from the DER-encoded certificate bytes.

CREATE TABLE psp_certificates (
    cert_id          VARCHAR(36)   PRIMARY KEY,
    psp_id           VARCHAR(32)   NOT NULL,
    cert_fingerprint VARCHAR(64)   NOT NULL,
    subject_dn       TEXT          NOT NULL,
    issued_at        DATETIME      NOT NULL,
    expires_at       DATETIME      NOT NULL,
    status           VARCHAR(16)   NOT NULL   COMMENT 'ACTIVE | REVOKED',
    created_at       DATETIME      NOT NULL,

    CONSTRAINT uq_psp_certificates_fingerprint UNIQUE (cert_fingerprint),
    CONSTRAINT fk_psp_certificates_psp
        FOREIGN KEY (psp_id) REFERENCES participants(bank_code),

    INDEX idx_psp_certificates_psp_status (psp_id, status),
    INDEX idx_psp_certificates_fingerprint (cert_fingerprint),
    INDEX idx_psp_certificates_status_expiry (status, expires_at)
);

ALTER TABLE idempotency_records
    ADD COLUMN channel_id VARCHAR(64) NOT NULL DEFAULT 'DEFAULT';

ALTER TABLE idempotency_records
    DROP INDEX uq_idempotency_key;

ALTER TABLE idempotency_records
    ADD CONSTRAINT uq_idempotency_channel_key UNIQUE (channel_id, idempotency_key);

ALTER TABLE idempotency_records
    ADD COLUMN updated_at DATETIME NULL;

UPDATE idempotency_records
SET updated_at = created_at
WHERE updated_at IS NULL
  AND created_at IS NOT NULL;

ALTER TABLE idempotency_records
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
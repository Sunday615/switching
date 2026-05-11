ALTER TABLE outbox_events
    ADD COLUMN last_error LONGTEXT NULL AFTER retry_count;

ALTER TABLE outbox_events
    ADD COLUMN processed_at DATETIME(3) NULL AFTER updated_at;

ALTER TABLE outbox_events
    ADD COLUMN next_retry_at DATETIME(3) NULL AFTER processed_at;

CREATE INDEX idx_outbox_events_status_next_retry_at
    ON outbox_events(status, next_retry_at);

CREATE INDEX idx_outbox_events_status_updated_at
    ON outbox_events(status, updated_at);
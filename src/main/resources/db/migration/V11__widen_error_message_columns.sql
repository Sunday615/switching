ALTER TABLE transfers
    MODIFY COLUMN error_message TEXT NULL;

ALTER TABLE outbox_events
    MODIFY COLUMN last_error TEXT NULL;
-- V18: Remove duplicate outbox_events indexes created by V16.
-- V10 already created idx_outbox_events_status_next_retry_at and
-- idx_outbox_events_status_updated_at on the same columns.
-- V16 added idx_outbox_status_retry and idx_outbox_status_updated on
-- identical columns, causing MySQL duplicate-index warnings.
-- Dropping the V16 names; the V10 originals remain and cover the same queries.

DROP INDEX idx_outbox_status_retry   ON outbox_events;
DROP INDEX idx_outbox_status_updated ON outbox_events;

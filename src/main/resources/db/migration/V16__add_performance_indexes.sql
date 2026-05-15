-- V16: Production performance indexes
-- All indexes verified against queries in:
--   OutboxProcessorService (status + next_retry_at / updated_at)
--   TransferListService (status + created_at)
--   OperationsTransferTraceService (transfer_ref + direction)
--   OperationsAuditLogQueryService (reference_id + created_at)
--   IdempotencyService (expired_at cleanup)

-- Transfers: list by status ordered by time (dashboard + ops queries)
CREATE INDEX idx_transfers_status_created
    ON transfers (status, created_at DESC);

-- Outbox: poller finds PENDING events to dispatch
CREATE INDEX idx_outbox_status_retry
    ON outbox_events (status, next_retry_at);

-- Outbox: recovery worker finds stuck PROCESSING events
CREATE INDEX idx_outbox_status_updated
    ON outbox_events (status, updated_at);

-- Audit logs: lookup by reference (transfer trace + ops audit view)
CREATE INDEX idx_audit_ref_created
    ON audit_logs (reference_id, created_at);

-- ISO messages: transfer trace fetches messages by transferRef
CREATE INDEX idx_iso_transfer_dir
    ON iso_messages (transfer_ref, direction);

-- Idempotency: TTL cleanup job filters by expiry
CREATE INDEX idx_idempotency_expired
    ON idempotency_records (expired_at);

-- V17: API key hardening
-- 1. Add key_prefix  — stores first 12 chars of the original key for display (never the full key)
-- 2. Add expires_at  — enables key expiry (NULL = no expiry)
-- 3. Hash key_value  — converts plaintext keys to SHA-256 hex (64 chars)
--    Uses MySQL SHA2(str, 256) to compute the hash in-place.
--    After this migration, key_value is ALWAYS a SHA-256 hex digest.
--    The plaintext key is never stored again after creation.

ALTER TABLE api_keys
    ADD COLUMN key_prefix VARCHAR(16) NULL COMMENT 'First 12 chars of original key for display',
    ADD COLUMN expires_at DATETIME    NULL COMMENT 'NULL = does not expire';

-- Capture prefix from existing plaintext keys before hashing
UPDATE api_keys
SET key_prefix = SUBSTRING(key_value, 1, 12)
WHERE key_prefix IS NULL;

-- Hash all existing key_value entries using SHA-256
-- (idempotent: SHA2 of a 64-char hex string produces a different hash,
--  so this must only run once — Flyway guarantees that)
UPDATE api_keys
SET key_value = SHA2(key_value, 256);

-- Widen key_value to 64 chars (SHA-256 hex is exactly 64 chars)
-- Current column is VARCHAR(128) so this is a narrowing change that still fits
ALTER TABLE api_keys MODIFY COLUMN key_value VARCHAR(64) NOT NULL;

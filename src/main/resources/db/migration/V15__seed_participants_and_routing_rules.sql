-- V15: Seed starter participants and routing rules for fresh install
-- Matches the connector_configs seeded in V9 (BANK_A, BANK_B, BANK_C)
-- Uses ON DUPLICATE KEY UPDATE so this is safe to run on an existing DB
-- with manually-created participants.

INSERT INTO participants (bank_code, bank_name, status, participant_type, country, currency, created_at, updated_at)
VALUES
    ('BANK_A', 'Bank A',  'ACTIVE', 'DIRECT',   'LA', 'LAK', NOW(), NOW()),
    ('BANK_B', 'Bank B',  'ACTIVE', 'DIRECT',   'LA', 'LAK', NOW(), NOW()),
    ('BANK_C', 'Bank C',  'ACTIVE', 'INDIRECT', 'LA', 'LAK', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = updated_at;

-- Bidirectional routing through the mock connectors seeded in V9
INSERT INTO routing_rules (route_code, source_bank, destination_bank, message_type, connector_name, priority, enabled, created_at, updated_at)
VALUES
    ('ROUTE_BANK_A_TO_BANK_B_PACS008', 'BANK_A', 'BANK_B', 'PACS_008', 'MOCK_BANK_B_CONNECTOR', 1, TRUE, NOW(), NOW()),
    ('ROUTE_BANK_A_TO_BANK_C_PACS008', 'BANK_A', 'BANK_C', 'PACS_008', 'MOCK_BANK_C_CONNECTOR', 1, TRUE, NOW(), NOW()),
    ('ROUTE_BANK_B_TO_BANK_A_PACS008', 'BANK_B', 'BANK_A', 'PACS_008', 'MOCK_BANK_A_CONNECTOR', 1, TRUE, NOW(), NOW()),
    ('ROUTE_BANK_C_TO_BANK_A_PACS008', 'BANK_C', 'BANK_A', 'PACS_008', 'MOCK_BANK_A_CONNECTOR', 1, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = updated_at;

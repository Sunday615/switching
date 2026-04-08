INSERT INTO participant_banks (bank_code, bank_name, short_name, active_flag, maintenance_flag)
VALUES
('BANK_A', 'Source Test Bank', 'BKA', TRUE, FALSE),
('BANK_B', 'Receiver Test Bank', 'BKB', TRUE, FALSE);

INSERT INTO routing_rules (route_code, destination_bank_code, connector_name, priority, active_flag)
VALUES
('ROUTE_BANK_B_PRIMARY', 'BANK_B', 'MOCK_CONNECTOR', 1, TRUE);
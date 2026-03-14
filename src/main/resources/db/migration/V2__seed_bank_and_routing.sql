INSERT INTO participant_banks (bank_code, bank_name, short_name, active_flag, maintenance_flag)
VALUES
('STB001', 'Source Test Bank', 'STB', TRUE, FALSE),
('RCV002', 'Receiver Test Bank', 'RCB', TRUE, FALSE);

INSERT INTO routing_rules (route_code, destination_bank_code, connector_name, priority, active_flag)
VALUES
('ROUTE_RCV002_PRIMARY', 'RCV002', 'MOCK_CONNECTOR', 1, TRUE);
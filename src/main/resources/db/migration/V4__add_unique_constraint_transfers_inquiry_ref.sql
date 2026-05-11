ALTER TABLE transfers
    ADD COLUMN inquiry_ref VARCHAR(64) NULL;

ALTER TABLE transfers
    ADD CONSTRAINT uk_transfers_inquiry_ref UNIQUE (inquiry_ref);
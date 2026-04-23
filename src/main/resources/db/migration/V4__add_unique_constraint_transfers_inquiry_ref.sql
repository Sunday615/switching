ALTER TABLE transfers
    ADD CONSTRAINT uq_transfers_inquiry_ref UNIQUE (inquiry_ref);

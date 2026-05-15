-- V19: Migrate participant_type values from old enum (BANK, SWITCHING, SERVICE_PROVIDER)
-- to new enum (DIRECT, INDIRECT).
-- The Java ParticipantType enum was corrected to use DIRECT/INDIRECT to match
-- the ISO switching network terminology (direct vs indirect participants).
-- V15 seeds already use DIRECT/INDIRECT; this fixes rows seeded before V15.

UPDATE participants
SET participant_type = 'DIRECT'
WHERE participant_type IN ('BANK', 'SWITCHING');

UPDATE participants
SET participant_type = 'INDIRECT'
WHERE participant_type = 'SERVICE_PROVIDER';

-- V16: Lead -> Customer conversion. Mirrors Customer.convertedFromLeadId
-- (customers migration) - same reasoning: NOT a foreign key, because Lead and
-- Customer are independent aggregates and archiving/deleting either one must
-- never cascade into the other. Nullable; null means "not converted yet".
--
-- IF NOT EXISTS: this project has had several migrations land out of the
-- order they were authored in (see the V8 scrap-comment mismatch), so this
-- guards against the column already existing under a different version
-- number by the time this actually runs.

ALTER TABLE leads ADD COLUMN IF NOT EXISTS converted_to_customer_id UUID;
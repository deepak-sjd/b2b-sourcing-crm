-- V16: Supplier Enquiry fixes - link to Requirement (PRD 8/12.2: "keep the
-- link between the customer requirement and the supplier reply"), per-reply
-- email delivery tracking, and DB-level guards that match what the service
-- layer enforces (defense in depth, not a replacement for it).

ALTER TABLE supplier_enquiries ADD COLUMN requirement_id UUID REFERENCES requirements(id);
ALTER TABLE supplier_enquiries ADD COLUMN response_due_date DATE;
CREATE INDEX idx_supplier_enquiries_requirement_id ON supplier_enquiries(requirement_id);

-- Denormalized onto the reply row (not just reachable via enquiry.requirement_id)
-- so "at most one chosen reply per requirement" - which must hold across every
-- enquiry ever raised for that requirement, not just within one enquiry - can be
-- expressed as a single partial unique index instead of application-only logic.
ALTER TABLE supplier_enquiry_replies ADD COLUMN requirement_id UUID REFERENCES requirements(id);
ALTER TABLE supplier_enquiry_replies ADD COLUMN email_error VARCHAR(500);

ALTER TABLE supplier_enquiry_replies
    ADD CONSTRAINT uq_enquiry_supplier UNIQUE (enquiry_id, supplier_id);

CREATE UNIQUE INDEX uq_one_chosen_reply_per_requirement
    ON supplier_enquiry_replies (requirement_id)
    WHERE chosen = true;

-- Sequence-backed enquiry codes (ENQ-000001, ...) - count()-based codes race
-- under concurrent creates and repeat once a row is ever deleted/archived.
CREATE SEQUENCE IF NOT EXISTS supplier_enquiry_code_seq START WITH 1 INCREMENT BY 1;

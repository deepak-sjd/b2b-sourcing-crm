-- V71: Re-applies the parts of the old "V22_quotation_lead_integration.sql" that
-- never ran (that file had a single underscore, so Flyway ignored it).
-- Idempotent on purpose: safe on databases that already have some of these
-- columns. supplier_enquiries.requirement_id is NOT here - V23 already added it.

ALTER TABLE quotations
    ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE quotations
    ADD COLUMN IF NOT EXISTS lead_id UUID REFERENCES leads(id),
    ADD COLUMN IF NOT EXISTS requirement_id UUID REFERENCES requirements(id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_quotations_customer_or_lead'
    ) THEN
        ALTER TABLE quotations
            ADD CONSTRAINT chk_quotations_customer_or_lead
            CHECK (customer_id IS NOT NULL OR lead_id IS NOT NULL);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_quotations_lead ON quotations (lead_id);
CREATE INDEX IF NOT EXISTS idx_quotations_requirement ON quotations (requirement_id);

ALTER TABLE quotation_line_items
    ADD COLUMN IF NOT EXISTS enquiry_reply_id UUID REFERENCES supplier_enquiry_replies(id);

CREATE INDEX IF NOT EXISTS idx_quotation_line_items_enquiry_reply
    ON quotation_line_items (enquiry_reply_id);
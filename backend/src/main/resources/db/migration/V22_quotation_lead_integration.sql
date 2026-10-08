-- V16: Let a quotation be prepared against an active Lead, not just a Customer
-- (PRD's funnel has a Quotation stage BEFORE Won, but a Customer only exists
-- after a lead is won — see Quotation.java's updated class comment). Also
-- links a quotation line to the specific chosen supplier-enquiry-reply its
-- price came from, and links a supplier enquiry back to the requirement it's
-- sourcing for, so quotations/prefill can find "the chosen price for this
-- requirement" without guessing from free-text item descriptions.

ALTER TABLE quotations
    ALTER COLUMN customer_id DROP NOT NULL,
    ADD COLUMN lead_id UUID REFERENCES leads(id),
    ADD COLUMN requirement_id UUID REFERENCES requirements(id),
    ADD CONSTRAINT chk_quotations_customer_or_lead CHECK (customer_id IS NOT NULL OR lead_id IS NOT NULL);

CREATE INDEX idx_quotations_lead ON quotations (lead_id);
CREATE INDEX idx_quotations_requirement ON quotations (requirement_id);

ALTER TABLE quotation_line_items
    ADD COLUMN enquiry_reply_id UUID REFERENCES supplier_enquiry_replies(id);

CREATE INDEX idx_quotation_line_items_enquiry_reply ON quotation_line_items (enquiry_reply_id);

ALTER TABLE supplier_enquiries
    ADD COLUMN requirement_id UUID REFERENCES requirements(id);

CREATE INDEX idx_supplier_enquiries_requirement ON supplier_enquiries (requirement_id);

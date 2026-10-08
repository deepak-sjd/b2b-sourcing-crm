-- V55: Generalize Document from Lead-only to any record (PRD §20, §12.2).
-- lead_id becomes optional; five new optional parent columns added. CHECK
-- enforces exactly one of the six is ever set. Soft delete (deleted_at/by)
-- replaces hard delete - PRD: "valuable records are never deleted."

ALTER TABLE documents
    ALTER COLUMN lead_id DROP NOT NULL,
    ADD COLUMN customer_id UUID REFERENCES customers(id),
    ADD COLUMN supplier_id UUID REFERENCES suppliers(id),
    ADD COLUMN requirement_id UUID REFERENCES requirements(id),
    ADD COLUMN quotation_id UUID REFERENCES quotations(id),
    ADD COLUMN enquiry_reply_id UUID REFERENCES supplier_enquiry_replies(id),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by_id UUID REFERENCES users(id);

ALTER TABLE documents
    ADD CONSTRAINT chk_documents_single_parent CHECK (
        (CASE WHEN lead_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN customer_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN supplier_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN requirement_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN quotation_id IS NOT NULL THEN 1 ELSE 0 END)
      + (CASE WHEN enquiry_reply_id IS NOT NULL THEN 1 ELSE 0 END)
      = 1
    );

CREATE INDEX idx_documents_customer_id ON documents(customer_id);
CREATE INDEX idx_documents_supplier_id ON documents(supplier_id);
CREATE INDEX idx_documents_requirement_id ON documents(requirement_id);
CREATE INDEX idx_documents_quotation_id ON documents(quotation_id);
CREATE INDEX idx_documents_enquiry_reply_id ON documents(enquiry_reply_id);
CREATE INDEX idx_documents_deleted_at ON documents(deleted_at);
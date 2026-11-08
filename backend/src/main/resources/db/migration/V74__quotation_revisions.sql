-- V74: quotation revisions + database-enforced content lock after DRAFT/UNDER_REVIEW.
ALTER TABLE quotations ADD COLUMN revision_number INTEGER NOT NULL DEFAULT 1;
ALTER TABLE quotations ADD COLUMN root_id UUID REFERENCES quotations (id);
ALTER TABLE quotations ADD COLUMN superseded BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_quotations_root ON quotations (root_id);
CREATE UNIQUE INDEX uq_quotations_root_revision ON quotations (root_id, revision_number) WHERE root_id IS NOT NULL;

-- Once a quotation has left DRAFT/UNDER_REVIEW its commercial content is a record of what the
-- customer was offered. Status, superseded and the customer/lead pointers can still change
-- (lead conversion re-points quotations at the new customer).
CREATE OR REPLACE FUNCTION quotations_lock_content() RETURNS trigger AS $$
BEGIN
    IF OLD.status NOT IN ('DRAFT', 'UNDER_REVIEW') AND (
        NEW.quotation_date, NEW.valid_until, NEW.currency, NEW.tax_percent,
        NEW.terms_and_conditions, NEW.remarks,
        NEW.subtotal, NEW.tax_amount, NEW.grand_total, NEW.total_margin_amount
    ) IS DISTINCT FROM (
        OLD.quotation_date, OLD.valid_until, OLD.currency, OLD.tax_percent,
        OLD.terms_and_conditions, OLD.remarks,
        OLD.subtotal, OLD.tax_amount, OLD.grand_total, OLD.total_margin_amount
    ) THEN
        RAISE EXCEPTION 'Quotation % is % - its content is locked, create a revision instead',
            OLD.quotation_number, OLD.status USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_quotations_lock_content
    BEFORE UPDATE ON quotations
    FOR EACH ROW EXECUTE FUNCTION quotations_lock_content();

CREATE OR REPLACE FUNCTION quotation_line_items_lock() RETURNS trigger AS $$
DECLARE
    qid UUID;
    parent_status TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN qid := OLD.quotation_id; ELSE qid := NEW.quotation_id; END IF;
    SELECT status INTO parent_status FROM quotations WHERE id = qid;
    IF parent_status IS NOT NULL AND parent_status NOT IN ('DRAFT', 'UNDER_REVIEW') THEN
        RAISE EXCEPTION 'Quotation is % - its line items are locked, create a revision instead', parent_status
            USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_quotation_line_items_lock
    BEFORE INSERT OR UPDATE OR DELETE ON quotation_line_items
    FOR EACH ROW EXECUTE FUNCTION quotation_line_items_lock();
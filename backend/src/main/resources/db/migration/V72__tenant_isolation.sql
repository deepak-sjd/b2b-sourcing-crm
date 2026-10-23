-- V72: tenant isolation, part 1A.
-- Adds organization_id to every tenant-owned table, backfills existing rows with the
-- default organization, and makes business codes unique per tenant instead of globally.
-- (users, teams already have organization_id; refresh/reset tokens stay global.)
DO $$
DECLARE
    default_org UUID;
    t TEXT;
BEGIN
    SELECT id INTO default_org FROM organizations WHERE slug = 'default';
    IF default_org IS NULL THEN
        RAISE EXCEPTION 'default organization missing - V6 should have created it';
    END IF;

    FOREACH t IN ARRAY ARRAY[
        'leads', 'customers', 'customer_contacts', 'contacts', 'activities',
        'requirements', 'tasks', 'quotations', 'quotation_line_items', 'documents',
        'notes', 'audit_logs', 'suppliers', 'supplier_enquiries', 'supplier_enquiry_replies'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS organization_id UUID', t);
        EXECUTE format('UPDATE %I SET organization_id = %L WHERE organization_id IS NULL', t, default_org);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN organization_id SET NOT NULL', t);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (organization_id) REFERENCES organizations (id)',
                       t, 'fk_' || t || '_organization');
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (organization_id)', 'idx_' || t || '_organization_id', t);
    END LOOP;
END $$;

-- Business codes: unique per tenant, not globally.
DROP INDEX IF EXISTS idx_suppliers_supplier_code;
CREATE UNIQUE INDEX idx_suppliers_org_supplier_code ON suppliers (organization_id, supplier_code);

DROP INDEX IF EXISTS idx_quotations_number;
CREATE UNIQUE INDEX idx_quotations_org_number ON quotations (organization_id, quotation_number);

ALTER TABLE supplier_enquiries DROP CONSTRAINT IF EXISTS supplier_enquiries_enquiry_code_key;
CREATE UNIQUE INDEX idx_supplier_enquiries_org_code ON supplier_enquiries (organization_id, enquiry_code);
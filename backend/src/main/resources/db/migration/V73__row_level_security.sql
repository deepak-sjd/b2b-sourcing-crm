-- V73: Postgres row-level security as a second safeguard behind the application filter.
-- A row is visible/writable only when organization_id equals the tenant that the
-- connection announced via set_config('app.tenant_id', ...). No tenant set => no rows.
-- The table owner (the migration user) is NOT forced, so migrations are unaffected;
-- the application connects as the non-owner role crm_app (see docs/db/create_app_role.sql).
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'teams', 'leads', 'customers', 'customer_contacts', 'contacts', 'activities',
        'requirements', 'tasks', 'quotations', 'quotation_line_items', 'documents',
        'notes', 'audit_logs', 'suppliers', 'supplier_enquiries', 'supplier_enquiry_replies'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            || 'USING (organization_id = NULLIF(current_setting(''app.tenant_id'', true), '''')::uuid) '
            || 'WITH CHECK (organization_id = NULLIF(current_setting(''app.tenant_id'', true), '''')::uuid)',
            t);
    END LOOP;
END $$;
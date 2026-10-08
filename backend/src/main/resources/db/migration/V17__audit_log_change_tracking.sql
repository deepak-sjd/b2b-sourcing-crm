-- V50: old/new values on audit_logs (PRD §17). Nullable — every row written
-- via the pre-existing AuditService.log(...) stays valid untouched.
ALTER TABLE audit_logs
    ADD COLUMN old_values JSONB,
    ADD COLUMN new_values JSONB,
    ADD COLUMN changed_fields VARCHAR(500),
    ADD COLUMN ip_address VARCHAR(45);

CREATE INDEX idx_audit_logs_entity_created_at ON audit_logs (entity_type, entity_id, created_at DESC);
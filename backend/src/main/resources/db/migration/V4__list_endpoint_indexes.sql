-- Indexes to support:
--   1) The new global GET /tasks and GET /calls list endpoints (filter + sort)
--   2) The new global GET /audit-logs endpoint (sorted by created_at desc)
--   3) by-lead lookups (findByLeadId) on tasks/calls/notes, which previously
--      relied only on the FK constraint - Postgres does NOT auto-index FK
--      columns, so these were full scans once a table grew large.

CREATE INDEX idx_tasks_lead_id ON tasks (lead_id);
CREATE INDEX idx_tasks_assigned_to_id ON tasks (assigned_to_id);

CREATE INDEX idx_calls_lead_id ON calls (lead_id);
CREATE INDEX idx_calls_created_at ON calls (created_at);
CREATE INDEX idx_calls_status ON calls (status);
CREATE INDEX idx_calls_direction ON calls (direction);

CREATE INDEX idx_notes_lead_id ON notes (lead_id);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

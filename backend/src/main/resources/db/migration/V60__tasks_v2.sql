-- V60 (D4-01): Task module v2 (PRD section 11).
-- A task may hang off a lead OR a customer (at least one), with optional links to the
-- requirement / quotation / enquiry / activity it came from. Cancel is a soft state (R1).
ALTER TABLE tasks
    ALTER COLUMN lead_id DROP NOT NULL,
    ADD COLUMN task_type VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN customer_id UUID REFERENCES customers(id),
    ADD COLUMN requirement_id UUID REFERENCES requirements(id),
    ADD COLUMN quotation_id UUID REFERENCES quotations(id),
    ADD COLUMN enquiry_id UUID REFERENCES supplier_enquiries(id),
    ADD COLUMN activity_id UUID REFERENCES activities(id),
    ADD COLUMN created_by_id UUID REFERENCES users(id),
    ADD COLUMN completed_by_id UUID REFERENCES users(id),
    ADD COLUMN reminder_at TIMESTAMP,
    ADD COLUMN remarks VARCHAR(2000),
    ADD COLUMN cancel_reason VARCHAR(500);

ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_lead_or_customer CHECK (lead_id IS NOT NULL OR customer_id IS NOT NULL);

-- Overdue sweep and My Day buckets only ever look at open tasks.
CREATE INDEX idx_tasks_open_due ON tasks (due_date) WHERE status IN ('PENDING', 'IN_PROGRESS');
CREATE INDEX idx_tasks_assignee_status_due ON tasks (assigned_to_id, status, due_date);
CREATE INDEX idx_tasks_customer_id ON tasks (customer_id);
CREATE INDEX idx_tasks_requirement_id ON tasks (requirement_id);
CREATE INDEX idx_tasks_quotation_id ON tasks (quotation_id);
CREATE INDEX idx_tasks_enquiry_id ON tasks (enquiry_id);
CREATE INDEX idx_tasks_activity_id ON tasks (activity_id);
CREATE INDEX idx_tasks_reminder_at ON tasks (reminder_at) WHERE reminder_at IS NOT NULL;

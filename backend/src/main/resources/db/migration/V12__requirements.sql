-- V12: Customer requirements (PRD section 8). One lead/customer can have many
-- requirements. A requirement is the anchor that Supplier Enquiries (next
-- module) and Quotations link back to, so it is never hard-deleted: a
-- requirement that is dropped is moved to CANCELLED and stays in history.

CREATE TABLE requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,

    product_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    quantity NUMERIC(14,2),
    unit VARCHAR(30),
    expected_timeline VARCHAR(120),
    budget NUMERIC(14,2),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    required_delivery_date DATE,
    additional_specifications VARCHAR(2000),
    remarks VARCHAR(2000),

    -- PRD workflow decision "Supplier needed? (item not available)".
    needs_supplier BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

    assigned_to_id UUID REFERENCES users(id),
    created_by_id UUID REFERENCES users(id),

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_requirements_lead_id ON requirements (lead_id);
CREATE INDEX idx_requirements_status ON requirements (status);
CREATE INDEX idx_requirements_assigned_to_id ON requirements (assigned_to_id);
CREATE INDEX idx_requirements_delivery_date ON requirements (required_delivery_date);

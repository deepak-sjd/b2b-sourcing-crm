-- V7: Customer module — company records with billing/shipping addresses and
-- multiple contacts. Independent of leads (see Customer.java javadoc for why
-- converted_from_lead_id is a bare UUID, not a foreign key).

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(255) NOT NULL,
    gst_no VARCHAR(20),
    nature_of_business VARCHAR(30),
    account_manager_id UUID REFERENCES users(id),

    billing_line1 VARCHAR(255),
    billing_line2 VARCHAR(255),
    billing_area VARCHAR(120),
    billing_country VARCHAR(120),
    billing_state VARCHAR(120),
    billing_city VARCHAR(120),
    billing_pin_code VARCHAR(12),
    billing_phone VARCHAR(30),
    billing_email VARCHAR(255),
    billing_gst_no VARCHAR(20),

    shipping_line1 VARCHAR(255),
    shipping_line2 VARCHAR(255),
    shipping_area VARCHAR(120),
    shipping_country VARCHAR(120),
    shipping_state VARCHAR(120),
    shipping_city VARCHAR(120),
    shipping_pin_code VARCHAR(12),
    shipping_phone VARCHAR(30),
    shipping_email VARCHAR(255),
    shipping_gst_no VARCHAR(20),

    shipping_same_as_billing BOOLEAN NOT NULL DEFAULT true,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    active BOOLEAN NOT NULL DEFAULT true,
    converted_from_lead_id UUID,
    ai_health_summary VARCHAR(2000),
    ai_health_generated_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_account_manager ON customers (account_manager_id);
CREATE INDEX idx_customers_approval_status ON customers (approval_status);
CREATE INDEX idx_customers_nature_of_business ON customers (nature_of_business);
-- Backs CustomerDuplicateService's case-insensitive name match.
CREATE INDEX idx_customers_company_name_lower ON customers (lower(company_name));

CREATE TABLE customer_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    contact_type VARCHAR(60) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    contact_no VARCHAR(30),
    designation VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_customer_contacts_customer ON customer_contacts (customer_id);

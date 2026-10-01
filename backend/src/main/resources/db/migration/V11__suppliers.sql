-- V7: Supplier / vendor master data (B2B procurement side), independent of
-- the Lead pipeline. supplier_code is assigned server-side after insert
-- (see SupplierService.generateCode), so it's nullable at the DB level and
-- enforced unique+not-null in practice by application code + the unique index.

CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_code VARCHAR(20) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',

    gst_number VARCHAR(20),
    gst_verified BOOLEAN NOT NULL DEFAULT false,
    pan_number VARCHAR(20),

    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    state VARCHAR(100),
    city VARCHAR(100),
    pincode VARCHAR(20),

    bank_name VARCHAR(255),
    bank_address VARCHAR(255),
    account_holder_name VARCHAR(255),
    account_number VARCHAR(50),
    ifsc_code VARCHAR(20),

    contact_name VARCHAR(150),
    contact_designation VARCHAR(150),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(30),
    payment_terms VARCHAR(20),

    created_by_id UUID REFERENCES users(id),
    trust_score INTEGER NOT NULL DEFAULT 0,
    score_note VARCHAR(500),
    archived BOOLEAN NOT NULL DEFAULT false,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_suppliers_supplier_code ON suppliers (supplier_code);
CREATE INDEX idx_suppliers_status ON suppliers (status);
CREATE INDEX idx_suppliers_category ON suppliers (category);
CREATE INDEX idx_suppliers_gst_number ON suppliers (gst_number);
CREATE INDEX idx_suppliers_company_name ON suppliers (lower(company_name));

CREATE TABLE supplier_products (
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL
);
CREATE INDEX idx_supplier_products_supplier_id ON supplier_products (supplier_id);

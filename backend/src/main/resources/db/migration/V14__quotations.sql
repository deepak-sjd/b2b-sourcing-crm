-- V12: Quotation Management (PRD §13) - multi-line-item customer quotations,
-- each line optionally priced against a Supplier (PRD §12.3's "price trail").
-- quotation_number is assigned server-side after insert (see
-- QuotationService.generateNumber), nullable at the DB level and enforced
-- unique+not-null in practice by application code + the unique index, same
-- approach as suppliers.supplier_code in V11.

CREATE TABLE quotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_number VARCHAR(30) NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    customer_contact_id UUID REFERENCES customer_contacts(id),
    prepared_by_id UUID REFERENCES users(id),

    quotation_date DATE NOT NULL DEFAULT current_date,
    valid_until DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    tax_percent NUMERIC(5,2),

    terms_and_conditions VARCHAR(2000),
    remarks VARCHAR(1000),

    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_margin_amount NUMERIC(14,2) NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_quotations_number ON quotations (quotation_number);
CREATE INDEX idx_quotations_customer ON quotations (customer_id);
CREATE INDEX idx_quotations_status ON quotations (status);

CREATE TABLE quotation_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_id UUID NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,

    product_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    unit VARCHAR(30) NOT NULL DEFAULT 'Nos',
    quantity NUMERIC(14,3) NOT NULL,

    supplier_id UUID REFERENCES suppliers(id),
    supplier_price NUMERIC(14,2),
    selling_price NUMERIC(14,2) NOT NULL,

    line_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    margin_amount NUMERIC(14,2),
    margin_percent NUMERIC(5,2),

    remarks VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_quotation_line_items_quotation ON quotation_line_items (quotation_id);
CREATE INDEX idx_quotation_line_items_supplier ON quotation_line_items (supplier_id);

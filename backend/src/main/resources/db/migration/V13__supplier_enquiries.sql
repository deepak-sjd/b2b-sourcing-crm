-- V12: Supplier Enquiry & Pricing — the PRD's headline "main feature".
--
-- supplier_enquiries: one row per "we need to source this item" request,
-- raised against a Lead (the active deal the sourcing is for).
--
-- supplier_enquiry_replies: one row per supplier the enquiry was sent to.
-- Created (status PENDING) at the same time as the enquiry itself, for every
-- supplier selected — so "who did we ask and have they replied yet" is
-- answerable immediately, not only once someone bothers to log a reply.
-- Exactly one reply per enquiry can be chosen=true: the price that becomes
-- the quotation's reference (PRD 12.3).

CREATE TABLE supplier_enquiries (
    id              UUID PRIMARY KEY,
    enquiry_code    VARCHAR(20)  NOT NULL UNIQUE,
    lead_id         UUID         NOT NULL REFERENCES leads(id),
    item_description VARCHAR(500) NOT NULL,
    quantity        VARCHAR(100),
    notes           VARCHAR(2000),
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_by_id   UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_supplier_enquiries_lead_id ON supplier_enquiries(lead_id);
CREATE INDEX idx_supplier_enquiries_status ON supplier_enquiries(status);

CREATE TABLE supplier_enquiry_replies (
    id                  UUID PRIMARY KEY,
    enquiry_id          UUID        NOT NULL REFERENCES supplier_enquiries(id),
    supplier_id         UUID        NOT NULL REFERENCES suppliers(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    price               NUMERIC(14,2),
    currency            VARCHAR(10) NOT NULL DEFAULT 'INR',
    valid_until         DATE,
    delivery_info       VARCHAR(500),
    supplier_reference  VARCHAR(100),
    terms               VARCHAR(1000),
    remarks             VARCHAR(1000),
    chosen              BOOLEAN     NOT NULL DEFAULT FALSE,
    email_sent_at       TIMESTAMPTZ,
    replied_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_supplier_enquiry_replies_enquiry_id ON supplier_enquiry_replies(enquiry_id);
CREATE INDEX idx_supplier_enquiry_replies_supplier_id ON supplier_enquiry_replies(supplier_id);

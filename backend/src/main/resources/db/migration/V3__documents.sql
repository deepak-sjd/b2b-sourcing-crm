-- V3: Document upload/attachment support for leads.

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    lead_id UUID NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    uploaded_by UUID REFERENCES users (id),
    document_type VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    original_file_name VARCHAR(255) NOT NULL,
    -- Server-generated key (never the client's filename) used to locate the file in
    -- storage (local disk path or S3 object key). See FileStorageService.
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_documents_lead_id ON documents (lead_id);
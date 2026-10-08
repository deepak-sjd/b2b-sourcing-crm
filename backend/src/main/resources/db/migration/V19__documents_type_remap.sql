-- V56: PRD-aligned DocumentType values. Old data remapped, not lost.
-- Column stays VARCHAR (enum stored as STRING) - no schema change needed,
-- just the stored values.
UPDATE documents SET document_type = 'CUSTOMER_DOCUMENT' WHERE document_type IN ('ID_PROOF', 'MARKSHEET');
UPDATE documents SET document_type = 'OTHER' WHERE document_type IN ('PAYMENT_RECEIPT', 'AGREEMENT');
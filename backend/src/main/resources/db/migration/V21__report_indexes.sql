-- V30: supporting indexes for the new report aggregate queries (ReportService).
-- idx_leads_scrapped and idx_leads_lost_reason already exist (V15) - not repeated here.

CREATE INDEX idx_leads_source_lower ON leads (lower(source));
CREATE INDEX idx_activities_scheduled_at ON activities (scheduled_at);
CREATE INDEX idx_quotations_quotation_date ON quotations (quotation_date);
CREATE INDEX idx_supplier_enquiry_replies_status ON supplier_enquiry_replies (status);
CREATE INDEX idx_customers_active ON customers (active);
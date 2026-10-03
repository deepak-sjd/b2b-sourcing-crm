-- V12: Lead Won/Lost reasons (PRD §14.1/§14.2) and the Scrap lifecycle (§14.3).
--
-- `archived` on leads (added in V1) was a bare soft-delete flag with no
-- reason, no timestamp, and nothing exposing it to the frontend - this
-- promotes it into the PRD's full Scrap concept and renames it to match that
-- language throughout the codebase. Note: `contacts` (V7) and `suppliers`
-- (V11) have the exact same bare `archived` pattern and are NOT touched here -
-- same gap, different chat's scope.

ALTER TABLE leads RENAME COLUMN archived TO scrapped;

ALTER TABLE leads
    ADD COLUMN scrap_reason VARCHAR(30),
    ADD COLUMN scrap_remarks VARCHAR(1000),
    ADD COLUMN scrapped_at TIMESTAMP,

    ADD COLUMN closed_at TIMESTAMP,
    ADD COLUMN final_value NUMERIC(14, 2),
    ADD COLUMN closure_remarks VARCHAR(1000),

    ADD COLUMN lost_reason VARCHAR(40),
    ADD COLUMN lost_comment VARCHAR(1000);

-- Backs the Scrap Records page (filter to scrapped = true) and the dashboard's
-- lost-business-by-reason breakdown (PRD §15).
CREATE INDEX idx_leads_scrapped ON leads (scrapped);
CREATE INDEX idx_leads_lost_reason ON leads (lost_reason);

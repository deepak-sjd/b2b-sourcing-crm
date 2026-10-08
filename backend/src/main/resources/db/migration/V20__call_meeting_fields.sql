-- V35: call outcomes + meeting lifecycle fields (PRD §7, §9).
-- Additive only; existing rows/enums keep working. Requirement/follow-up/notes
-- concepts already covered by requirements/notes/follow_up_at - not duplicated.

ALTER TABLE activities
    ADD COLUMN call_outcome VARCHAR(30),
    ADD COLUMN customer_response VARCHAR(2000),
    ADD COLUMN customer_interest VARCHAR(10),
    ADD COLUMN next_action VARCHAR(500),
    ADD COLUMN meeting_status VARCHAR(20),
    ADD COLUMN meeting_type VARCHAR(20),
    ADD COLUMN agenda VARCHAR(2000);

CREATE INDEX idx_activities_call_outcome ON activities(call_outcome);
CREATE INDEX idx_activities_meeting_status ON activities(meeting_status);
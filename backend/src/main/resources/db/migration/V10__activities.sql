-- V7: Generalize Calls into Activities. A call is one specific kind of
-- customer-facing touchpoint; meetings, site visits, emails, and WhatsApp
-- messages are the same record shape with a different activityType, so we
-- broaden the existing table rather than adding a parallel one (same
-- domain-agnostic direction as V5).

ALTER TABLE calls RENAME TO activities;
ALTER TABLE activities RENAME COLUMN caller_id TO logged_by_id;

ALTER TABLE activities
    ADD COLUMN activity_type VARCHAR(20) NOT NULL DEFAULT 'CALL',
    ADD COLUMN contact_person VARCHAR(255),
    ADD COLUMN location VARCHAR(255),
    ADD COLUMN requirements VARCHAR(2000),
    ADD COLUMN follow_up_at TIMESTAMP,
    ADD COLUMN scheduled_at TIMESTAMP,
    ADD COLUMN tagged_resource_id UUID REFERENCES users(id);

-- Every existing row was a call that already happened.
UPDATE activities SET scheduled_at = created_at WHERE scheduled_at IS NULL;
ALTER TABLE activities ALTER COLUMN scheduled_at SET NOT NULL;

-- direction (inbound/outbound) doesn't apply to MEETING/SITE_VISIT/OTHER.
ALTER TABLE activities ALTER COLUMN direction DROP NOT NULL;

ALTER INDEX idx_calls_lead_id RENAME TO idx_activities_lead_id;
ALTER INDEX idx_calls_created_at RENAME TO idx_activities_created_at;
ALTER INDEX idx_calls_status RENAME TO idx_activities_status;
ALTER INDEX idx_calls_direction RENAME TO idx_activities_direction;

CREATE INDEX idx_activities_type ON activities(activity_type);
CREATE INDEX idx_activities_follow_up_at ON activities(follow_up_at);
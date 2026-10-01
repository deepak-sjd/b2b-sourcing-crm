-- V7: Contacts module — people at customer accounts, distinct from Leads
-- (a pipeline of prospects). A Contact tracks an auto-computed engagement
-- score/state so the UI can surface "who's going cold" without any manual
-- upkeep (see ContactEngagementService).

CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    designation VARCHAR(150),
    company VARCHAR(200),
    phone VARCHAR(30),
    email VARCHAR(255),
    city VARCHAR(120),
    source VARCHAR(120),
    owner_id UUID REFERENCES users(id),
    created_by_id UUID REFERENCES users(id),
    last_activity_at TIMESTAMP,
    next_follow_up_at TIMESTAMP,
    email_bounced BOOLEAN NOT NULL DEFAULT false,
    engagement_score INT NOT NULL DEFAULT 20,
    engagement VARCHAR(20) NOT NULL DEFAULT 'NEW',
    ai_insight VARCHAR(500),
    archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_contacts_owner ON contacts (owner_id);
CREATE INDEX idx_contacts_engagement ON contacts (engagement);
CREATE INDEX idx_contacts_email ON contacts (lower(email));
CREATE INDEX idx_contacts_phone ON contacts (phone);

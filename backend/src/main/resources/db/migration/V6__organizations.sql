-- V6: Introduce Organization as the tenant boundary, ahead of deciding the
-- exact multi-tenant hosting model. Every existing team/user is backfilled
-- onto one "Default Organization" row, so this is a no-op for current
-- single-tenant behavior - nothing about how the app currently works changes
-- yet, this only makes the schema ready for that decision later.

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_organizations_slug ON organizations (slug);

INSERT INTO organizations (name, slug) VALUES ('Default Organization', 'default');

ALTER TABLE teams ADD COLUMN organization_id UUID REFERENCES organizations(id);
UPDATE teams SET organization_id = (SELECT id FROM organizations WHERE slug = 'default');
ALTER TABLE teams ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX idx_teams_organization_id ON teams (organization_id);

ALTER TABLE users ADD COLUMN organization_id UUID REFERENCES organizations(id);
UPDATE users SET organization_id = (SELECT id FROM organizations WHERE slug = 'default');
ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX idx_users_organization_id ON users (organization_id);

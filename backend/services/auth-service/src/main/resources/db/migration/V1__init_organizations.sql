-- Organizations (billing entity, dashboard access)
CREATE TABLE organizations (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(100) NOT NULL,
  slug        VARCHAR(50) NOT NULL UNIQUE,
  status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
);

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_status ON organizations(status);

-- Organizations members (developers who manage project via dashboard)
CREATE TABLE organization_members (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  email           VARCHAR(255) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  name            VARCHAR(100),
  avatar_url      VARCHAR(500),
  role            VARCHAR(20) NOT NULL,
  email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
  status          VARTCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_org_member_email UNIQUE(organization_id, email)
);

CREATE INDEX idx_org_members_org ON organization_members(organization_id);
CREATE INDEX idx_org_members_email ON organization_members(email);

-- Invitations for new org members
CREATE TABLE organization_invitations (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  email           VARCHAR(255) NOT NULL,
  role            VARCHAR(20) NOT NULL,
  token           VARCHAR(255) NOT NULL UNIQUE,
  invited_by      UUID REFERENCES organization_members(id) ON DELETE SET NULL,
  expires_at      TIMESTAMP NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_invitations_org ON organization_invitations(organization_id);
CREATE INDEX idx_org_invitations_token ON organization_invitations(token);

--MFA for org members
CREATE TABLE org_member_mfa (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id       UUID NOT NULL UNIQUE REFERENCES organization_members(id) ON DELETE CASCADE,
  secret          VARCHAR(255) NOT NULL,
  recovery_codes  TEXT[],
  enabled         BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Session for org members (dashboard sessions)
CREATE TABLE org_member_sessions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id       UUID NOT NULL UNIQUE REFERENCES organization_members(id) ON DELETE CASCADE,
  device_info     JSONB,
  ip_address      INET,
  expires_at      TIMESTAMP NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_member_sessions_member ON org_member_sessions(member_id);
CREATE INDEX idx_org_member_sessions_expires ON org_member_sessions(expires_at);

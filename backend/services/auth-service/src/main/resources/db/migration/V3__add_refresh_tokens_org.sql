-- Refresh tokens for organization members (dashboard access)
CREATE TABLE org_member_refresh_tokens (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id       UUID NOT NULL REFERENCES organization_members(id) ON DELETE CASCADE,
  token_hash      VARCHAR(255) NOT NULL,
  expires_at      TIMESTAMP NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  revoked         BOOLEAN NOT NULL DEFAULT FALSE,
  replaced_by     VARCHAR(255), -- For rotation
  
  -- Audit metadata
  user_agent      TEXT,
  ip_address      INET,
  
  CONSTRAINT uq_org_member_token_hash UNIQUE(token_hash)
);

CREATE INDEX idx_org_member_refresh_member ON org_member_refresh_tokens(member_id);
CREATE INDEX idx_org_member_refresh_expires ON org_member_refresh_tokens(expires_at);

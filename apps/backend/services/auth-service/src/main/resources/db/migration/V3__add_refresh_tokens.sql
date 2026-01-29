-- Refresh tokens for org members (supports rotation and revocation)
CREATE TABLE org_member_refresh_tokens (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id       UUID NOT NULL REFERENCES organization_members(id) ON DELETE CASCADE,
  token_hash      VARCHAR(128) NOT NULL UNIQUE,
  expires_at      TIMESTAMPTZ NOT NULL,
  revoked         BOOLEAN NOT NULL DEFAULT FALSE,
  replaced_by_id  UUID REFERENCES org_member_refresh_tokens(id) ON DELETE SET NULL,
  ip_address      VARCHAR(45),
  user_agent      VARCHAR(500),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_member ON org_member_refresh_tokens(member_id);
CREATE INDEX idx_refresh_tokens_expires ON org_member_refresh_tokens(expires_at);

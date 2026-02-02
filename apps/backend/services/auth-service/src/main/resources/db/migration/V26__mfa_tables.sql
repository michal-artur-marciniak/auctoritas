-- MFA tables for end users and org members
-- Includes: user_mfa, mfa_recovery_codes (hashed), mfa_challenges (short-lived tokens)

-- End user MFA settings
CREATE TABLE user_mfa (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  project_id        UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  encrypted_secret  VARCHAR(255) NOT NULL,
  enabled           BOOLEAN NOT NULL DEFAULT FALSE,
  verified_at       TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_user_mfa_user_project UNIQUE(user_id, project_id)
);

CREATE INDEX idx_user_mfa_user ON user_mfa(user_id);
CREATE INDEX idx_user_mfa_project ON user_mfa(project_id);

-- Recovery codes (hashed) for both end users and org members
-- Only one of user_id or member_id should be non-null (enforced by CHECK constraint)
CREATE TABLE mfa_recovery_codes (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID REFERENCES end_users(id) ON DELETE CASCADE,
  member_id         UUID REFERENCES organization_members(id) ON DELETE CASCADE,
  code_hash         VARCHAR(255) NOT NULL,
  used_at           TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_recovery_code_owner CHECK (
    (user_id IS NOT NULL AND member_id IS NULL) OR
    (user_id IS NULL AND member_id IS NOT NULL)
  )
);

CREATE INDEX idx_recovery_codes_user ON mfa_recovery_codes(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_recovery_codes_member ON mfa_recovery_codes(member_id) WHERE member_id IS NOT NULL;
CREATE INDEX idx_recovery_codes_hash ON mfa_recovery_codes(code_hash);

-- MFA challenges (short-lived tokens for login flow)
CREATE TABLE mfa_challenges (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  token             VARCHAR(255) NOT NULL UNIQUE,
  user_id           UUID,
  member_id         UUID,
  project_id        UUID REFERENCES projects(id) ON DELETE CASCADE,
  organization_id   UUID REFERENCES organizations(id) ON DELETE CASCADE,
  expires_at        TIMESTAMPTZ NOT NULL,
  used              BOOLEAN NOT NULL DEFAULT FALSE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_challenge_target CHECK (
    (user_id IS NOT NULL AND member_id IS NULL) OR
    (user_id IS NULL AND member_id IS NOT NULL)
  ),
  CONSTRAINT chk_challenge_context CHECK (
    (project_id IS NOT NULL AND organization_id IS NULL) OR
    (project_id IS NULL AND organization_id IS NOT NULL)
  ),
  CONSTRAINT fk_mfa_challenges_user_id FOREIGN KEY (user_id)
    REFERENCES end_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_mfa_challenges_member_id FOREIGN KEY (member_id)
    REFERENCES organization_members(id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_challenges_token ON mfa_challenges(token);
CREATE INDEX idx_mfa_challenges_expires ON mfa_challenges(expires_at);
CREATE INDEX idx_mfa_challenges_user ON mfa_challenges(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_mfa_challenges_member ON mfa_challenges(member_id) WHERE member_id IS NOT NULL;

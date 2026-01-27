-- Email verification tokens (end users)
CREATE TABLE email_verification_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id     UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL UNIQUE,
  code_hash   VARCHAR(128) NOT NULL,
  expires_at  TIMESTAMPTZ NOT NULL,
  used_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_tokens_project_user
  ON email_verification_tokens(project_id, user_id);
CREATE INDEX idx_email_verification_tokens_user
  ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_expires
  ON email_verification_tokens(expires_at);

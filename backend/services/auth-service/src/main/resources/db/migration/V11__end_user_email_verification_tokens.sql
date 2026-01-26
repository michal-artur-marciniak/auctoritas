-- End user email verification tokens
CREATE TABLE end_user_email_verification_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL UNIQUE,
  code_hash   VARCHAR(128) NOT NULL,
  expires_at  TIMESTAMPTZ NOT NULL,
  used_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_end_user_email_verification_tokens_user
  ON end_user_email_verification_tokens(user_id);
CREATE INDEX idx_end_user_email_verification_tokens_expires
  ON end_user_email_verification_tokens(expires_at);

-- End user password reset tokens
CREATE TABLE end_user_password_reset_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  used_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_end_user_password_reset_tokens_user ON end_user_password_reset_tokens(user_id);
CREATE INDEX idx_end_user_password_reset_tokens_expires ON end_user_password_reset_tokens(expires_at);

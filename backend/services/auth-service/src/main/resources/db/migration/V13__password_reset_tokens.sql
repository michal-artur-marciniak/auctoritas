-- Password reset tokens (end users)
CREATE TABLE password_reset_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  user_id     UUID NOT NULL REFERENCES end_users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ NOT NULL,
  used_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ip_address  VARCHAR(45),
  user_agent  VARCHAR(500)
);

CREATE INDEX idx_password_reset_tokens_project_user ON password_reset_tokens(project_id, user_id);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires ON password_reset_tokens(expires_at);

-- Backfill legacy table if present in this repo history
DO $$
BEGIN
  IF to_regclass('public.end_user_password_reset_tokens') IS NOT NULL THEN
    INSERT INTO password_reset_tokens (id, project_id, user_id, token_hash, expires_at, used_at, created_at)
    SELECT t.id, u.project_id, t.user_id, t.token_hash, t.expires_at, t.used_at, t.created_at
    FROM end_user_password_reset_tokens t
    JOIN end_users u ON u.id = t.user_id;

    DROP TABLE end_user_password_reset_tokens;
  END IF;
END $$;

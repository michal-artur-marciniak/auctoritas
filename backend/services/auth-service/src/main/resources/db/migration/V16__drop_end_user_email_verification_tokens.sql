-- Drop legacy end-user email verification tokens table
-- Superseded by `email_verification_tokens` (project-scoped)
DROP TABLE IF EXISTS end_user_email_verification_tokens;

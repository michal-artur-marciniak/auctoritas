ALTER TABLE end_users
  ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN failed_login_window_start TIMESTAMPTZ,
  ADD COLUMN lockout_until TIMESTAMPTZ;

ALTER TABLE project_settings
  ADD COLUMN failed_login_max_attempts INT NOT NULL DEFAULT 5,
  ADD COLUMN failed_login_window_seconds INT NOT NULL DEFAULT 900;

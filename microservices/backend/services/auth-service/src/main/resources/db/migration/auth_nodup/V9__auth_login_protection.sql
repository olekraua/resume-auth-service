ALTER TABLE public.auth_user
    ADD COLUMN IF NOT EXISTS failed_login_attempts integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_failed_login_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS login_locked_until timestamp with time zone;

CREATE INDEX IF NOT EXISTS idx_auth_user_login_locked_until
    ON public.auth_user (login_locked_until);

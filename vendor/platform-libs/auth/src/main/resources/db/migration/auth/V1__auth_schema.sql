CREATE TABLE IF NOT EXISTS profile_restore (
    id bigserial PRIMARY KEY,
    token varchar(64) NOT NULL,
    created timestamptz NOT NULL DEFAULT now(),
    profile_id bigint NOT NULL,
    CONSTRAINT uk_profile_restore_token UNIQUE (token),
    CONSTRAINT uk_profile_restore_profile UNIQUE (profile_id)
);

CREATE TABLE IF NOT EXISTS remember_me_token (
    series varchar(64) PRIMARY KEY,
    token varchar(64) NOT NULL,
    last_used timestamptz NOT NULL,
    profile_id bigint NOT NULL,
    username varchar(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_remember_me_profile ON remember_me_token(profile_id);
CREATE INDEX IF NOT EXISTS idx_remember_me_username ON remember_me_token(username);

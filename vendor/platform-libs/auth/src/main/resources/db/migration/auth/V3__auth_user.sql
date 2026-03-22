-- Split auth data from profile into auth_user

CREATE TABLE auth_user (
    id bigint NOT NULL,
    uid character varying(64) NOT NULL,
    password_hash character varying(255) NOT NULL,
    first_name character varying(64) NOT NULL,
    last_name character varying(64) NOT NULL,
    created timestamp without time zone DEFAULT now() NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    CONSTRAINT auth_user_pkey PRIMARY KEY (id),
    CONSTRAINT auth_user_uid_key UNIQUE (uid),
    CONSTRAINT chk_auth_user_uid_format CHECK ((uid)::text ~ '^[a-z0-9_-]{3,64}$'::text),
    CONSTRAINT chk_auth_user_uid_lowercase CHECK ((uid)::text = lower((uid)::text))
);

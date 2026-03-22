CREATE TABLE IF NOT EXISTS persistent_logins (
    username character varying(64) NOT NULL,
    series character varying(64) NOT NULL,
    token character varying(64) NOT NULL,
    last_used timestamp without time zone NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'persistent_logins_pkey'
          AND conrelid = 'persistent_logins'::regclass
    ) THEN
        ALTER TABLE ONLY persistent_logins
            ADD CONSTRAINT persistent_logins_pkey PRIMARY KEY (username, series);
    END IF;
END$$;



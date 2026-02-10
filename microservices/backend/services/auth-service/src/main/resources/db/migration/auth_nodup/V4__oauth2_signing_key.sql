CREATE TABLE public.oauth2_signing_key (
    id smallint NOT NULL,
    jwk_json text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY public.oauth2_signing_key
    ADD CONSTRAINT oauth2_signing_key_pkey PRIMARY KEY (id);

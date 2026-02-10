CREATE TABLE public.oauth2_signing_jwk (
    key_id character varying(128) NOT NULL,
    jwk_json text NOT NULL,
    is_current boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY public.oauth2_signing_jwk
    ADD CONSTRAINT oauth2_signing_jwk_pkey PRIMARY KEY (key_id);

CREATE UNIQUE INDEX uq_oauth2_signing_jwk_current
    ON public.oauth2_signing_jwk (is_current)
    WHERE (is_current = true);

INSERT INTO public.oauth2_signing_jwk (key_id, jwk_json, is_current, created_at)
SELECT
    COALESCE(NULLIF((jwk_json::jsonb ->> 'kid'), ''), 'legacy-signing-key-' || id::text),
    jwk_json,
    true,
    created_at
FROM public.oauth2_signing_key
ON CONFLICT (key_id) DO NOTHING;

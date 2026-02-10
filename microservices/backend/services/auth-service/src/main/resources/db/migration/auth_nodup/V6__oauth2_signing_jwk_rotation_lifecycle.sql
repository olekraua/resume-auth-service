ALTER TABLE public.oauth2_signing_jwk
    ADD COLUMN publish_until timestamp without time zone;

CREATE INDEX idx_oauth2_signing_jwk_publish_until
    ON public.oauth2_signing_jwk (publish_until)
    WHERE is_current = false;

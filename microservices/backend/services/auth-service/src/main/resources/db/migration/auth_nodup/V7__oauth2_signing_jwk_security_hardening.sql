CREATE TABLE public.oauth2_signing_jwk_audit (
    id bigserial NOT NULL,
    event_type character varying(32) NOT NULL,
    key_id character varying(128),
    is_current boolean,
    publish_until timestamp without time zone,
    actor character varying(128) DEFAULT CURRENT_USER NOT NULL,
    app_name text DEFAULT current_setting('application_name', true),
    txid bigint DEFAULT txid_current() NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY public.oauth2_signing_jwk_audit
    ADD CONSTRAINT oauth2_signing_jwk_audit_pkey PRIMARY KEY (id);

CREATE INDEX idx_oauth2_signing_jwk_audit_created_at
    ON public.oauth2_signing_jwk_audit (created_at DESC);

CREATE OR REPLACE FUNCTION public.audit_oauth2_signing_jwk_changes()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO public.oauth2_signing_jwk_audit (event_type, key_id, is_current, publish_until)
        VALUES ('INSERT', NEW.key_id, NEW.is_current, NEW.publish_until);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO public.oauth2_signing_jwk_audit (event_type, key_id, is_current, publish_until)
        VALUES ('UPDATE', NEW.key_id, NEW.is_current, NEW.publish_until);
        RETURN NEW;
    ELSE
        INSERT INTO public.oauth2_signing_jwk_audit (event_type, key_id, is_current, publish_until)
        VALUES ('DELETE', OLD.key_id, OLD.is_current, OLD.publish_until);
        RETURN OLD;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS tr_oauth2_signing_jwk_audit ON public.oauth2_signing_jwk;

CREATE TRIGGER tr_oauth2_signing_jwk_audit
AFTER INSERT OR UPDATE OR DELETE ON public.oauth2_signing_jwk
FOR EACH ROW EXECUTE FUNCTION public.audit_oauth2_signing_jwk_changes();

ALTER TABLE public.oauth2_signing_jwk
    ADD CONSTRAINT ck_oauth2_signing_jwk_current_key_encrypted
    CHECK (is_current = false OR jwk_json LIKE 'ENC:v1:%')
    NOT VALID;

REVOKE ALL ON TABLE public.oauth2_signing_jwk FROM PUBLIC;
REVOKE ALL ON TABLE public.oauth2_signing_jwk_audit FROM PUBLIC;
REVOKE ALL ON SEQUENCE public.oauth2_signing_jwk_audit_id_seq FROM PUBLIC;

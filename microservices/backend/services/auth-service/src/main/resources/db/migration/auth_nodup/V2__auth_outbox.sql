-- Outbox table for auth notification events

CREATE SEQUENCE public.auth_outbox_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.auth_outbox (
    id bigint DEFAULT nextval('public.auth_outbox_seq'::regclass) NOT NULL,
    event_type character varying(32) NOT NULL,
    payload text NOT NULL,
    status character varying(16) NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    available_at timestamp with time zone DEFAULT now() NOT NULL,
    sent_at timestamp with time zone,
    last_error text
);

ALTER TABLE ONLY public.auth_outbox
    ADD CONSTRAINT auth_outbox_pkey PRIMARY KEY (id);

CREATE INDEX auth_outbox_status_available_idx
    ON public.auth_outbox (status, available_at, id);

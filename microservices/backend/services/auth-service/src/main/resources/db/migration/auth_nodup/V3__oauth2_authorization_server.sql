-- Spring Authorization Server schema (PostgreSQL)

CREATE TABLE public.oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp without time zone DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL
);

ALTER TABLE ONLY public.oauth2_registered_client
    ADD CONSTRAINT oauth2_registered_client_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.oauth2_registered_client
    ADD CONSTRAINT oauth2_registered_client_client_id_key UNIQUE (client_id);

CREATE TABLE public.oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes bytea DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value bytea DEFAULT NULL,
    authorization_code_issued_at timestamp without time zone DEFAULT NULL,
    authorization_code_expires_at timestamp without time zone DEFAULT NULL,
    authorization_code_metadata bytea DEFAULT NULL,
    access_token_value bytea DEFAULT NULL,
    access_token_issued_at timestamp without time zone DEFAULT NULL,
    access_token_expires_at timestamp without time zone DEFAULT NULL,
    access_token_metadata bytea DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value bytea DEFAULT NULL,
    oidc_id_token_issued_at timestamp without time zone DEFAULT NULL,
    oidc_id_token_expires_at timestamp without time zone DEFAULT NULL,
    oidc_id_token_metadata bytea DEFAULT NULL,
    refresh_token_value bytea DEFAULT NULL,
    refresh_token_issued_at timestamp without time zone DEFAULT NULL,
    refresh_token_expires_at timestamp without time zone DEFAULT NULL,
    refresh_token_metadata bytea DEFAULT NULL,
    user_code_value bytea DEFAULT NULL,
    user_code_issued_at timestamp without time zone DEFAULT NULL,
    user_code_expires_at timestamp without time zone DEFAULT NULL,
    user_code_metadata bytea DEFAULT NULL,
    device_code_value bytea DEFAULT NULL,
    device_code_issued_at timestamp without time zone DEFAULT NULL,
    device_code_expires_at timestamp without time zone DEFAULT NULL,
    device_code_metadata bytea DEFAULT NULL
);

ALTER TABLE ONLY public.oauth2_authorization
    ADD CONSTRAINT oauth2_authorization_pkey PRIMARY KEY (id);

CREATE INDEX idx_oauth2_authorization_registered_client_id
    ON public.oauth2_authorization USING btree (registered_client_id);

CREATE INDEX idx_oauth2_authorization_principal_name
    ON public.oauth2_authorization USING btree (principal_name);

CREATE TABLE public.oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL
);

ALTER TABLE ONLY public.oauth2_authorization_consent
    ADD CONSTRAINT oauth2_authorization_consent_pkey PRIMARY KEY (registered_client_id, principal_name);

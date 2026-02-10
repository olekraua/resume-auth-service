DO
$$
DECLARE
    auth_lob_column text;
BEGIN
    FOREACH auth_lob_column IN ARRAY ARRAY [
        'attributes',
        'authorization_code_value',
        'authorization_code_metadata',
        'access_token_value',
        'access_token_metadata',
        'oidc_id_token_value',
        'oidc_id_token_metadata',
        'refresh_token_value',
        'refresh_token_metadata',
        'user_code_value',
        'user_code_metadata',
        'device_code_value',
        'device_code_metadata'
    ]
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns c
            WHERE c.table_schema = 'public'
              AND c.table_name = 'oauth2_authorization'
              AND c.column_name = auth_lob_column
              AND c.udt_name = 'bytea'
        ) THEN
            EXECUTE format(
                'ALTER TABLE public.oauth2_authorization ALTER COLUMN %1$I TYPE text USING convert_from(%1$I, ''UTF8'')',
                auth_lob_column
            );
        END IF;
    END LOOP;
END;
$$;

ALTER TABLE profile
    ADD COLUMN IF NOT EXISTS connections_visible boolean NOT NULL DEFAULT true;

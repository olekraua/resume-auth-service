-- Deduplicate to allow adding a composite primary key.
DELETE FROM profile_hobby a
USING profile_hobby b
WHERE a.id_profile = b.id_profile
  AND a.id_hobby = b.id_hobby
  AND a.ctid < b.ctid;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'profile_hobby_pkey'
      AND conrelid = 'profile_hobby'::regclass
  ) THEN
    ALTER TABLE profile_hobby
      ADD CONSTRAINT profile_hobby_pkey PRIMARY KEY (id_profile, id_hobby);
  END IF;
END
$$;

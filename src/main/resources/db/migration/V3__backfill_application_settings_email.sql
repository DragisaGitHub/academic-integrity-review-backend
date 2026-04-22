-- Backfill defaults for application_settings after V2 column additions

UPDATE application_settings
SET email = ''
WHERE email IS NULL;

ALTER TABLE application_settings
    MODIFY COLUMN email VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE application_settings
    ADD COLUMN user_id BIGINT NULL AFTER id;

UPDATE application_settings
SET user_id = (
    SELECT COALESCE(
        (SELECT preferred_user.id
         FROM (SELECT id FROM users WHERE enabled = TRUE AND role = 'USER' ORDER BY id LIMIT 1) preferred_user),
        (SELECT enabled_user.id
         FROM (SELECT id FROM users WHERE enabled = TRUE ORDER BY id LIMIT 1) enabled_user),
        (SELECT any_user.id
         FROM (SELECT id FROM users ORDER BY id LIMIT 1) any_user)
    )
)
WHERE user_id IS NULL
  AND id = (
      SELECT legacy_settings.id
      FROM (
          SELECT id
          FROM application_settings
          WHERE user_id IS NULL
          ORDER BY id
          LIMIT 1
      ) legacy_settings
  );

ALTER TABLE application_settings
    ADD CONSTRAINT uk_application_settings_user_id UNIQUE (user_id),
    ADD CONSTRAINT fk_application_settings_user FOREIGN KEY (user_id) REFERENCES users (id);

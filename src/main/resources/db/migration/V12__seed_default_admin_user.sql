UPDATE users
SET password_hash = '$2a$10$QbbhkO8nVFflw2nBZbkgt.AwCSc61s4d0qlj3e5QEPFv7xm/HAtIq',
    display_name = 'Administrator',
    role = 'ADMIN',
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM (
          SELECT id
          FROM users
          WHERE role = 'ADMIN'
            AND enabled = TRUE
      ) existing_admins
  );

INSERT INTO users (username, password_hash, display_name, role, enabled, created_at, updated_at)
SELECT 'admin',
       '$2a$10$QbbhkO8nVFflw2nBZbkgt.AwCSc61s4d0qlj3e5QEPFv7xm/HAtIq',
       'Administrator',
       'ADMIN',
       TRUE,
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE role = 'ADMIN'
      AND enabled = TRUE
)
  AND NOT EXISTS (
      SELECT 1
      FROM users
      WHERE username = 'admin'
  );
ALTER TABLE findings
    ADD COLUMN professor_notes LONGTEXT NULL AFTER suggested_action,
    ADD COLUMN reviewed BOOLEAN NOT NULL DEFAULT FALSE AFTER professor_notes,
    ADD COLUMN flagged_for_follow_up BOOLEAN NOT NULL DEFAULT FALSE AFTER reviewed;
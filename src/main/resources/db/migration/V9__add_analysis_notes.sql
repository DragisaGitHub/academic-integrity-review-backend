ALTER TABLE analyses
    ADD COLUMN analysis_notes LONGTEXT NULL AFTER error_message;
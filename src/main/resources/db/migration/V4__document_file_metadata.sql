-- Add stored document file metadata columns

ALTER TABLE documents
    ADD COLUMN original_filename VARCHAR(512) NULL AFTER submission_date,
    ADD COLUMN stored_filename   VARCHAR(512) NULL AFTER original_filename,
    ADD COLUMN stored_path       VARCHAR(1024) NULL AFTER stored_filename,
    ADD COLUMN content_type      VARCHAR(255) NULL AFTER stored_path,
    ADD COLUMN file_size         BIGINT NULL AFTER content_type;

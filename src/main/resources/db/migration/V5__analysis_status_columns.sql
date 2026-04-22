ALTER TABLE analyses
    ADD COLUMN analysis_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' AFTER full_text,
    ADD COLUMN error_message LONGTEXT NULL AFTER analysis_status,
    ADD COLUMN started_at TIMESTAMP(6) NULL AFTER updated_at,
    ADD COLUMN completed_at TIMESTAMP(6) NULL AFTER started_at,
    ADD COLUMN model_name VARCHAR(100) NULL AFTER completed_at,
    ADD COLUMN total_tokens_used INT NULL AFTER model_name;

UPDATE analyses
SET analysis_status = CASE
    WHEN full_text IS NOT NULL AND TRIM(full_text) <> '' THEN 'COMPLETED'
    ELSE 'PENDING'
END,
started_at = CASE
    WHEN full_text IS NOT NULL AND TRIM(full_text) <> '' THEN created_at
    ELSE started_at
END,
completed_at = CASE
    WHEN full_text IS NOT NULL AND TRIM(full_text) <> '' THEN updated_at
    ELSE completed_at
END;
-- Extend application_settings to match frontend Settings page

ALTER TABLE application_settings
    ADD COLUMN email VARCHAR(255) NULL AFTER university,

    ADD COLUMN citation_analysis BOOLEAN NOT NULL DEFAULT FALSE AFTER email,
    ADD COLUMN reference_validation BOOLEAN NOT NULL DEFAULT FALSE AFTER citation_analysis,
    ADD COLUMN factual_consistency_review BOOLEAN NOT NULL DEFAULT FALSE AFTER reference_validation,
    ADD COLUMN writing_style_consistency BOOLEAN NOT NULL DEFAULT FALSE AFTER factual_consistency_review,
    ADD COLUMN ai_review_assistance BOOLEAN NOT NULL DEFAULT FALSE AFTER writing_style_consistency,

    ADD COLUMN auto_delete_reviewed_documents BOOLEAN NOT NULL DEFAULT FALSE AFTER document_retention_days,

    ADD COLUMN color_theme VARCHAR(50) NOT NULL DEFAULT 'DARK' AFTER auto_delete_reviewed_documents,
    ADD COLUMN display_density VARCHAR(50) NOT NULL DEFAULT 'COMFORTABLE' AFTER color_theme,
    ADD COLUMN show_severity_badges BOOLEAN NOT NULL DEFAULT TRUE AFTER display_density;

-- Preserve existing theme preference if it exists (legacy: light_theme_enabled)
UPDATE application_settings
SET color_theme = CASE
    WHEN light_theme_enabled THEN 'LIGHT'
    ELSE 'DARK'
END;

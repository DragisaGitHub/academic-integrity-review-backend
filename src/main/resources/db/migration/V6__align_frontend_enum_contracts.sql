UPDATE documents
SET review_status = CASE review_status
    WHEN 'COMPLETED' THEN 'REVIEWED'
    ELSE review_status
END;

UPDATE review_notes
SET final_decision = CASE final_decision
    WHEN 'ESCALATE_FOR_FURTHER_REVIEW' THEN 'ESCALATE'
    ELSE final_decision
END;

UPDATE application_settings
SET reading_layout = CASE reading_layout
    WHEN 'COMPACT' THEN 'CONTINUOUS'
    WHEN 'COMFORTABLE' THEN 'DEFAULT'
    WHEN 'DEFAULT' THEN 'DEFAULT'
    WHEN 'CONTINUOUS' THEN 'CONTINUOUS'
    WHEN 'PAGED' THEN 'PAGED'
    WHEN 'WIDE' THEN 'WIDE'
    ELSE 'DEFAULT'
END;

ALTER TABLE application_settings
    MODIFY COLUMN reading_layout VARCHAR(50) NOT NULL DEFAULT 'DEFAULT';
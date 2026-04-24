ALTER TABLE findings
    ADD COLUMN segment_index INT NULL AFTER excerpt,
    ADD COLUMN excerpt_start_offset INT NULL AFTER segment_index,
    ADD COLUMN excerpt_end_offset INT NULL AFTER excerpt_start_offset;
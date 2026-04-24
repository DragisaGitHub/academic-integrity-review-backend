CREATE TABLE IF NOT EXISTS text_segments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    content LONGTEXT NOT NULL,
    start_offset INT NOT NULL,
    end_offset INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_text_segments_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id),
    CONSTRAINT uq_text_segments_analysis_segment UNIQUE (analysis_id, segment_index)
);

CREATE INDEX idx_text_segments_analysis_id ON text_segments (analysis_id);
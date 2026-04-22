CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(512) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    is_read BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    document_id BIGINT NULL,
    analysis_id BIGINT NULL,
    route VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications (is_read);
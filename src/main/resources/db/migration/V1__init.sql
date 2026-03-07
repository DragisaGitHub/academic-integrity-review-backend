-- Initial schema for Academic Integrity Review (MySQL)

CREATE TABLE IF NOT EXISTS documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    student_name VARCHAR(255) NOT NULL,
    course VARCHAR(255) NOT NULL,
    submission_date DATE NULL,
    review_priority VARCHAR(50) NOT NULL,
    review_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    analysis_date DATE NULL,
    full_text LONGTEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_analyses_document_id UNIQUE (document_id),
    CONSTRAINT fk_analyses_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE TABLE IF NOT EXISTS findings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    explanation LONGTEXT NULL,
    excerpt LONGTEXT NULL,
    paragraph_location VARCHAR(255) NULL,
    suggested_action LONGTEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_findings_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id)
);

CREATE TABLE IF NOT EXISTS review_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    notes LONGTEXT NULL,
    references_checked BOOLEAN NOT NULL,
    oral_defense_required BOOLEAN NOT NULL,
    factual_issues_discussed BOOLEAN NOT NULL,
    final_review_completed BOOLEAN NOT NULL,
    final_decision VARCHAR(50) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_review_notes_document_id UNIQUE (document_id),
    CONSTRAINT fk_review_notes_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE TABLE IF NOT EXISTS application_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    professor_name VARCHAR(255) NULL,
    department VARCHAR(255) NULL,
    university VARCHAR(255) NULL,
    local_ai_enabled BOOLEAN NOT NULL,
    document_retention_days INT NOT NULL,
    storage_location VARCHAR(255) NULL,
    light_theme_enabled BOOLEAN NOT NULL,
    reading_layout VARCHAR(50) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

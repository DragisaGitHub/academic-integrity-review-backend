ALTER TABLE documents
    ADD COLUMN user_id BIGINT NULL AFTER id;

UPDATE documents
SET user_id = (
    SELECT COALESCE(
        (SELECT preferred_user.id
         FROM (SELECT id FROM users WHERE enabled = TRUE AND role = 'USER' ORDER BY id LIMIT 1) preferred_user),
        (SELECT enabled_user.id
         FROM (SELECT id FROM users WHERE enabled = TRUE ORDER BY id LIMIT 1) enabled_user),
        (SELECT any_user.id
         FROM (SELECT id FROM users ORDER BY id LIMIT 1) any_user)
    )
)
WHERE user_id IS NULL;

ALTER TABLE analyses
    ADD COLUMN user_id BIGINT NULL AFTER id;

UPDATE analyses analysis
JOIN documents document ON document.id = analysis.document_id
SET analysis.user_id = document.user_id
WHERE analysis.user_id IS NULL;

ALTER TABLE review_notes
    ADD COLUMN user_id BIGINT NULL AFTER id;

UPDATE review_notes review_note
JOIN documents document ON document.id = review_note.document_id
SET review_note.user_id = document.user_id
WHERE review_note.user_id IS NULL;

ALTER TABLE documents
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE analyses
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_analyses_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE review_notes
    MODIFY COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_review_notes_user FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_documents_user_id ON documents (user_id);
CREATE INDEX idx_analyses_user_id ON analyses (user_id);
CREATE INDEX idx_review_notes_user_id ON review_notes (user_id);
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    course VARCHAR(100),
    active BIT(1) NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE teacher_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    course VARCHAR(100) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    CONSTRAINT fk_assignment_teacher FOREIGN KEY (teacher_id) REFERENCES users(id),
    CONSTRAINT uq_teacher_assignment UNIQUE (teacher_id, course, subject)
);

CREATE TABLE user_sessions (
    token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    revoked_at BIGINT,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE notebooks ADD COLUMN teacher_id BIGINT;
ALTER TABLE notebooks ADD COLUMN subject VARCHAR(100) NOT NULL DEFAULT 'Lenguaje y Comunicación';
ALTER TABLE notebooks ADD COLUMN period_month INT NOT NULL DEFAULT 1;
ALTER TABLE notebooks ADD COLUMN period_year INT NOT NULL DEFAULT 2026;
ALTER TABLE notebooks ADD COLUMN published_at BIGINT;
ALTER TABLE notebooks ADD COLUMN is_closed BIT(1) NOT NULL DEFAULT FALSE;
ALTER TABLE notebooks ADD COLUMN closed_at BIGINT;
ALTER TABLE notebooks ADD COLUMN reminders_enabled BIT(1) NOT NULL DEFAULT TRUE;
ALTER TABLE notebooks ADD COLUMN updated_at BIGINT NOT NULL DEFAULT 0;
UPDATE notebooks SET published_at = created_at WHERE published_at IS NULL;
UPDATE notebooks SET updated_at = created_at WHERE updated_at = 0;
ALTER TABLE notebooks ADD CONSTRAINT fk_notebook_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);
CREATE UNIQUE INDEX uq_notebook_period ON notebooks(teacher_id, course, subject, period_month, period_year);
CREATE INDEX ix_notebook_owner ON notebooks(teacher_email, course);

CREATE TABLE notebook_students (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    notebook_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT uq_notebook_student UNIQUE (notebook_id, student_id),
    CONSTRAINT fk_notebook_student_notebook FOREIGN KEY (notebook_id) REFERENCES notebooks(id),
    CONSTRAINT fk_notebook_student_user FOREIGN KEY (student_id) REFERENCES users(id)
);

ALTER TABLE student_entries ADD COLUMN student_id BIGINT;
ALTER TABLE student_entries ADD COLUMN submitted_at BIGINT;
ALTER TABLE student_entries ADD COLUMN reviewed_at BIGINT;
ALTER TABLE student_entries ADD COLUMN reviewer_id BIGINT;
ALTER TABLE student_entries ADD COLUMN reviewer_email VARCHAR(255);
ALTER TABLE student_entries ADD COLUMN updated_at BIGINT NOT NULL DEFAULT 0;
UPDATE student_entries SET submitted_at = created_at WHERE is_draft = FALSE AND submitted_at IS NULL;
UPDATE student_entries SET updated_at = created_at WHERE updated_at = 0;
ALTER TABLE student_entries ADD CONSTRAINT fk_entry_notebook FOREIGN KEY (notebook_id) REFERENCES notebooks(id);
ALTER TABLE student_entries ADD CONSTRAINT fk_entry_student FOREIGN KEY (student_id) REFERENCES users(id);
ALTER TABLE student_entries ADD CONSTRAINT fk_entry_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id);
CREATE INDEX ix_entry_student ON student_entries(student_id, created_at);
CREATE INDEX ix_entry_notebook ON student_entries(notebook_id, is_draft, is_reviewed);

CREATE TABLE evidences (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    kind VARCHAR(10) NOT NULL,
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    storage_key VARCHAR(100),
    url VARCHAR(2048),
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_evidence_entry FOREIGN KEY (entry_id) REFERENCES student_entries(id),
    CONSTRAINT fk_evidence_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);
CREATE INDEX ix_evidence_entry ON evidences(entry_id);

CREATE TABLE idempotency_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    principal_id BIGINT NOT NULL,
    scope VARCHAR(50) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_idempotency_user FOREIGN KEY (principal_id) REFERENCES users(id),
    CONSTRAINT uq_idempotency UNIQUE (principal_id, scope, key_hash)
);

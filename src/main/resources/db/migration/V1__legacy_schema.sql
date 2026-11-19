CREATE TABLE notebooks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at BIGINT NOT NULL,
    instructions TEXT,
    purpose TEXT,
    questions TEXT,
    teacher_email VARCHAR(255),
    teacher_name VARCHAR(255),
    title VARCHAR(255),
    assigned_student_emails TEXT,
    course VARCHAR(255)
);

CREATE TABLE student_entries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    activity TEXT,
    class_date VARCHAR(255),
    content TEXT,
    created_at BIGINT NOT NULL,
    doubts TEXT,
    is_draft BIT(1) NOT NULL,
    learning TEXT,
    notebook_id BIGINT,
    notebook_title VARCHAR(255),
    student_email VARCHAR(255),
    student_name VARCHAR(255),
    teacher_email VARCHAR(255),
    is_reviewed BIT(1) NOT NULL,
    teacher_feedback TEXT
);

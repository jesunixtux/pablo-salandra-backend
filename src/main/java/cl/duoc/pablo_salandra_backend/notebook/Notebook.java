package cl.duoc.pablo_salandra_backend.notebook;

public record Notebook(
    Long id,
    String title,
    String purpose,
    String instructions,
    String questions,
    String teacherEmail,
    String teacherName,
    long createdAt,
    String course,
    String assignedStudentEmails,
    String subject,
    int month,
    int year,
    Long publishedAt,
    boolean isClosed,
    Long closedAt,
    boolean remindersEnabled,
    long updatedAt
) {}

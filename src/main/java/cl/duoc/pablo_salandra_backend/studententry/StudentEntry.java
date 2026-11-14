package cl.duoc.pablo_salandra_backend.studententry;

public record StudentEntry(
    Long id,
    long notebookId,
    String notebookTitle,
    String teacherEmail,
    String studentEmail,
    String studentName,
    String classDate,
    String content,
    String activity,
    String learning,
    String doubts,
    boolean isDraft,
    long createdAt,
    boolean isReviewed,
    String teacherFeedback
) {}

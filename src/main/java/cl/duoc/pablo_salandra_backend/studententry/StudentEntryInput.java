package cl.duoc.pablo_salandra_backend.studententry;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record StudentEntryInput(
    long notebookId,
    @Size(max = 200) String notebookTitle,
    @Email @Size(max = 255) String studentEmail,
    @Size(max = 255) String studentName,
    @Size(max = 30) String classDate,
    @Size(max = 10000) String content,
    @Size(max = 10000) String activity,
    @Size(max = 10000) String learning,
    @Size(max = 10000) String doubts,
    boolean isDraft,
    boolean isReviewed,
    @Size(max = 10000) String teacherFeedback
) {}

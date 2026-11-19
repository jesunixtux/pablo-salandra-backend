package cl.duoc.pablo_salandra_backend.studententry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import cl.duoc.pablo_salandra_backend.evidence.EvidenceDto;

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
    @JsonProperty("isDraft") boolean isDraft,
    long createdAt,
    @JsonProperty("isReviewed") boolean isReviewed,
    String teacherFeedback,
    Long submittedAt,
    Long reviewedAt,
    String reviewerEmail,
    List<EvidenceDto> evidences,
    long updatedAt
) {}

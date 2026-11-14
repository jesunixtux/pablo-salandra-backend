package cl.duoc.pablo_salandra_backend.studententry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_entries")
public class StudentEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long notebookId;
    private String notebookTitle;
    private String teacherEmail;
    private String studentEmail;
    private String studentName;
    private String classDate;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(columnDefinition = "TEXT")
    private String activity;
    @Column(columnDefinition = "TEXT")
    private String learning;
    @Column(columnDefinition = "TEXT")
    private String doubts;
    private boolean isDraft;
    private long createdAt;
    private boolean isReviewed;
    @Column(columnDefinition = "TEXT")
    private String teacherFeedback = "";

    protected StudentEntryEntity() {}

    public StudentEntryEntity(Long notebookId, String notebookTitle, String teacherEmail,
            String studentEmail, String studentName, String classDate, String content,
            String activity, String learning, String doubts, boolean isDraft, long createdAt,
            boolean isReviewed, String teacherFeedback) {
        replace(notebookId, notebookTitle, teacherEmail, studentEmail, studentName, classDate,
            content, activity, learning, doubts, isDraft, isReviewed, teacherFeedback);
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getNotebookId() { return notebookId; }
    public String getNotebookTitle() { return notebookTitle; }
    public String getTeacherEmail() { return teacherEmail; }
    public String getStudentEmail() { return studentEmail; }
    public String getStudentName() { return studentName; }
    public String getClassDate() { return classDate; }
    public String getContent() { return content; }
    public String getActivity() { return activity; }
    public String getLearning() { return learning; }
    public String getDoubts() { return doubts; }
    public boolean isDraft() { return isDraft; }
    public long getCreatedAt() { return createdAt; }
    public boolean isReviewed() { return isReviewed; }
    public String getTeacherFeedback() { return teacherFeedback; }

    public void replace(Long notebookId, String notebookTitle, String teacherEmail,
            String studentEmail, String studentName, String classDate, String content,
            String activity, String learning, String doubts, boolean isDraft,
            boolean isReviewed, String teacherFeedback) {
        this.notebookId = notebookId;
        this.notebookTitle = notebookTitle;
        this.teacherEmail = teacherEmail;
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.classDate = classDate;
        this.content = content;
        this.activity = activity;
        this.learning = learning;
        this.doubts = doubts;
        this.isDraft = isDraft;
        this.isReviewed = isReviewed;
        this.teacherFeedback = teacherFeedback;
    }
}

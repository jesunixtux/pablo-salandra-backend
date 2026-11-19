package cl.duoc.pablo_salandra_backend.notebook;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notebooks")
public class NotebookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String purpose;
    @Column(columnDefinition = "TEXT")
    private String instructions;
    @Column(columnDefinition = "TEXT")
    private String questions;
    private String teacherEmail;
    private String teacherName;
    private long createdAt;
    private String course = "General";
    @Column(columnDefinition = "TEXT")
    private String assignedStudentEmails = "";
    private Long teacherId;
    private String subject;
    @Column(name = "period_month")
    private int month;
    @Column(name = "period_year")
    private int year;
    private Long publishedAt;
    private boolean isClosed;
    private Long closedAt;
    private boolean remindersEnabled;
    private long updatedAt;

    protected NotebookEntity() {}

    public NotebookEntity(String title, String purpose, String instructions, String questions,
            String teacherEmail, String teacherName, long createdAt, String course, String assignedStudentEmails) {
        this.title = title;
        this.purpose = purpose;
        this.instructions = instructions;
        this.questions = questions;
        this.teacherEmail = teacherEmail;
        this.teacherName = teacherName;
        this.createdAt = createdAt;
        this.course = course;
        this.assignedStudentEmails = assignedStudentEmails;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public String getInstructions() { return instructions; }
    public String getQuestions() { return questions; }
    public String getTeacherEmail() { return teacherEmail; }
    public String getTeacherName() { return teacherName; }
    public long getCreatedAt() { return createdAt; }
    public String getCourse() { return course; }
    public String getAssignedStudentEmails() { return assignedStudentEmails; }
    public Long getTeacherId() { return teacherId; }
    public String getSubject() { return subject; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public Long getPublishedAt() { return publishedAt; }
    public boolean isClosed() { return isClosed; }
    public Long getClosedAt() { return closedAt; }
    public boolean isRemindersEnabled() { return remindersEnabled; }
    public long getUpdatedAt() { return updatedAt; }

    public void configure(Long teacherId, String subject, int month, int year) {
        this.teacherId = teacherId;
        this.subject = subject;
        this.month = month;
        this.year = year;
        this.remindersEnabled = true;
        touch();
    }

    public void updatePeriod(Long teacherId, String subject, int month, int year) {
        this.teacherId = teacherId;
        this.subject = subject;
        this.month = month;
        this.year = year;
        touch();
    }

    public void publish() { this.publishedAt = System.currentTimeMillis(); touch(); }
    public void close() { this.isClosed = true; this.closedAt = System.currentTimeMillis(); touch(); }
    public void setRemindersEnabled(boolean enabled) { this.remindersEnabled = enabled; touch(); }
    public void touch() { updatedAt = Math.max(System.currentTimeMillis(), updatedAt + 1); }

    public void replace(String title, String purpose, String instructions, String questions,
            String teacherEmail, String teacherName, String course, String assignedStudentEmails) {
        this.title = title;
        this.purpose = purpose;
        this.instructions = instructions;
        this.questions = questions;
        this.teacherEmail = teacherEmail;
        this.teacherName = teacherName;
        this.course = course;
        this.assignedStudentEmails = assignedStudentEmails;
        touch();
    }
}

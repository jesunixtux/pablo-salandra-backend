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
    }
}

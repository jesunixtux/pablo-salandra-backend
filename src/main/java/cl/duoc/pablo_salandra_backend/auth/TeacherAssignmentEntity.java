package cl.duoc.pablo_salandra_backend.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_assignments")
public class TeacherAssignmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long teacherId;
    private String course;
    private String subject;

    protected TeacherAssignmentEntity() {}

    public TeacherAssignmentEntity(Long teacherId, String course, String subject) {
        this.teacherId = teacherId;
        this.course = course;
        this.subject = subject;
    }

    public Long getTeacherId() { return teacherId; }
    public String getCourse() { return course; }
    public String getSubject() { return subject; }
}

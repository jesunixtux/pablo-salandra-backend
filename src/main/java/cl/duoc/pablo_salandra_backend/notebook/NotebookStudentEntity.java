package cl.duoc.pablo_salandra_backend.notebook;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notebook_students")
public class NotebookStudentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long notebookId;
    private Long studentId;

    protected NotebookStudentEntity() {}
    public NotebookStudentEntity(Long notebookId, Long studentId) {
        this.notebookId = notebookId;
        this.studentId = studentId;
    }
}

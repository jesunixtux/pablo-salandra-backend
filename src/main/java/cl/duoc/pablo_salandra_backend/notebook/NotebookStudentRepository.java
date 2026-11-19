package cl.duoc.pablo_salandra_backend.notebook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotebookStudentRepository extends JpaRepository<NotebookStudentEntity, Long> {
    void deleteByNotebookId(Long notebookId);
}

package cl.duoc.pablo_salandra_backend.notebook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotebookRepository extends JpaRepository<NotebookEntity, Long> {
    java.util.List<NotebookEntity> findAllByOrderByCreatedAtDesc();
}

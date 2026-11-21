package cl.duoc.pablo_salandra_backend.notebook;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotebookRepository extends JpaRepository<NotebookEntity, Long> {
    java.util.List<NotebookEntity> findAllByOrderByCreatedAtDesc();
    boolean existsByTeacherIdAndCourseAndSubjectAndMonthAndYear(Long teacherId,
        String course, String subject, int month, int year);
    Optional<NotebookEntity> findByTeacherIdAndCourseAndSubjectAndMonthAndYear(Long teacherId,
        String course, String subject, int month, int year);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotebookEntity n where n.id = :id")
    Optional<NotebookEntity> findLockedById(@Param("id") Long id);
}

package cl.duoc.pablo_salandra_backend.studententry;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentEntryRepository extends JpaRepository<StudentEntryEntity, Long> {
    boolean existsByNotebookId(Long notebookId);
    java.util.List<StudentEntryEntity> findByNotebookId(Long notebookId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from StudentEntryEntity e where e.id = :id")
    Optional<StudentEntryEntity> findLockedById(@Param("id") Long id);
}

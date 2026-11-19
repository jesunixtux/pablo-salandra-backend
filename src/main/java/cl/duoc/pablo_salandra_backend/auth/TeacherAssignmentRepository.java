package cl.duoc.pablo_salandra_backend.auth;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignmentEntity, Long> {
    boolean existsByTeacherIdAndCourseAndSubject(Long teacherId, String course, String subject);
    List<TeacherAssignmentEntity> findByTeacherId(Long teacherId);
}

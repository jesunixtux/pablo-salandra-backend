package cl.duoc.pablo_salandra_backend.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TeacherBootstrap implements ApplicationRunner {
    private final Environment env;
    private final UserRepository users;
    private final TeacherAssignmentRepository assignments;
    private final PasswordEncoder passwords;

    public TeacherBootstrap(Environment env, UserRepository users,
            TeacherAssignmentRepository assignments, PasswordEncoder passwords) {
        this.env = env;
        this.users = users;
        this.assignments = assignments;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = env.getProperty("BOOTSTRAP_TEACHER_EMAIL", "").trim().toLowerCase();
        if (email.isEmpty()) return;
        String password = env.getProperty("BOOTSTRAP_TEACHER_PASSWORD", "");
        String name = env.getProperty("BOOTSTRAP_TEACHER_NAME", "").trim();
        String course = env.getProperty("BOOTSTRAP_TEACHER_COURSE", "");
        String subject = env.getProperty("BOOTSTRAP_TEACHER_SUBJECT", "");
        if (password.length() < 8 || name.isEmpty() ||
                !SchoolCatalog.COURSES.contains(course) || !SchoolCatalog.SUBJECTS.contains(subject)) {
            throw new IllegalStateException("Teacher bootstrap requires valid name, password, course and subject");
        }
        UserEntity teacher = users.findByEmailIgnoreCase(email)
            .orElseGet(() -> users.save(new UserEntity(name, email, passwords.encode(password), "TEACHER", null)));
        if (!"TEACHER".equals(teacher.getRole())) throw new IllegalStateException("Bootstrap email belongs to a student");
        if (!assignments.existsByTeacherIdAndCourseAndSubject(teacher.getId(), course, subject)) {
            assignments.save(new TeacherAssignmentEntity(teacher.getId(), course, subject));
        }
    }
}

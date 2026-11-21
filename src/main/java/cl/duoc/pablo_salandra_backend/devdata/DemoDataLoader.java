package cl.duoc.pablo_salandra_backend.devdata;

import cl.duoc.pablo_salandra_backend.auth.TeacherAssignmentEntity;
import cl.duoc.pablo_salandra_backend.auth.TeacherAssignmentRepository;
import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.auth.UserRepository;
import cl.duoc.pablo_salandra_backend.evidence.EvidenceEntity;
import cl.duoc.pablo_salandra_backend.evidence.EvidenceRepository;
import cl.duoc.pablo_salandra_backend.notebook.NotebookEntity;
import cl.duoc.pablo_salandra_backend.notebook.NotebookRepository;
import cl.duoc.pablo_salandra_backend.notebook.NotebookStudentEntity;
import cl.duoc.pablo_salandra_backend.notebook.NotebookStudentRepository;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryEntity;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DemoDataLoader implements ApplicationRunner {
    static final String TEACHER_MATH = "docente.matematica@demo.dsy1105.test";
    static final String TEACHER_SCIENCE = "docente.ciencias@demo.dsy1105.test";
    static final String STUDENT_ALBA = "alba.5a@demo.dsy1105.test";
    static final String STUDENT_BENJAMIN = "benjamin.5a@demo.dsy1105.test";
    static final String STUDENT_CAMILA = "camila.6a@demo.dsy1105.test";
    static final String STUDENT_DIEGO = "diego.6a@demo.dsy1105.test";
    private static final byte[] PHOTO = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    private static final byte[] NOTE = "Evidencia ficticia DSY1105\n".getBytes(StandardCharsets.UTF_8);
    private static final Logger LOG = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository users;
    private final TeacherAssignmentRepository assignments;
    private final NotebookRepository notebooks;
    private final NotebookStudentRepository notebookStudents;
    private final StudentEntryRepository entries;
    private final EvidenceRepository evidences;
    private final PasswordEncoder passwords;
    private final String demoPassword;
    private final Path evidenceDirectory;

    public DemoDataLoader(UserRepository users, TeacherAssignmentRepository assignments,
            NotebookRepository notebooks, NotebookStudentRepository notebookStudents,
            StudentEntryRepository entries, EvidenceRepository evidences,
            PasswordEncoder passwords, @Value("${app.demo.password}") String demoPassword,
            @Value("${app.evidence-dir}") String evidenceDirectory) {
        this.users = users;
        this.assignments = assignments;
        this.notebooks = notebooks;
        this.notebookStudents = notebookStudents;
        this.entries = entries;
        this.evidences = evidences;
        this.passwords = passwords;
        this.demoPassword = demoPassword;
        this.evidenceDirectory = Path.of(evidenceDirectory).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoPassword.length() < 8) throw new IllegalStateException("DEMO_PASSWORD must have at least 8 characters");

        UserEntity mathTeacher = user("Docente Ada Demo", TEACHER_MATH, "TEACHER", null);
        UserEntity scienceTeacher = user("Docente Bruno Demo", TEACHER_SCIENCE, "TEACHER", null);
        UserEntity alba = user("Alba Demo", STUDENT_ALBA, "STUDENT", "5° Básico A");
        UserEntity benjamin = user("Benjamín Demo", STUDENT_BENJAMIN, "STUDENT", "5° Básico A");
        UserEntity camila = user("Camila Demo", STUDENT_CAMILA, "STUDENT", "6° Básico A");
        UserEntity diego = user("Diego Demo", STUDENT_DIEGO, "STUDENT", "6° Básico A");

        assignment(mathTeacher, "5° Básico A", "Matemática");
        assignment(scienceTeacher, "6° Básico A", "Ciencias Naturales");

        NotebookEntity mathOpen = notebook(mathTeacher, "Matemática - Noviembre 2026",
            "5° Básico A", "Matemática", 11, 2026, STUDENT_ALBA + "," + STUDENT_BENJAMIN, false);
        NotebookEntity mathClosed = notebook(mathTeacher, "Matemática - Octubre 2026",
            "5° Básico A", "Matemática", 10, 2026, STUDENT_ALBA + "," + STUDENT_BENJAMIN, true);
        NotebookEntity scienceOpen = notebook(scienceTeacher, "Ciencias - Noviembre 2026",
            "6° Básico A", "Ciencias Naturales", 11, 2026, STUDENT_CAMILA + "," + STUDENT_DIEGO, false);
        members(mathOpen, alba, benjamin);
        members(mathClosed, alba, benjamin);
        members(scienceOpen, camila, diego);

        StudentEntryEntity draft = entry(mathOpen, alba, "12/11/2026", "DRAFT", mathTeacher);
        StudentEntryEntity reviewed = entry(mathClosed, benjamin, "15/10/2026", "REVIEWED", mathTeacher);
        StudentEntryEntity submitted = entry(scienceOpen, camila, "18/11/2026", "SUBMITTED", scienceTeacher);

        fileEvidence(draft, alba, "PHOTO", "demo-foto.png", "image/png", "demo-photo.png", PHOTO);
        fileEvidence(reviewed, benjamin, "FILE", "demo-nota.txt", "text/plain", "demo-note.txt", NOTE);
        linkEvidence(submitted, camila, "https://example.test/dsy1105/evidencia-demo");
        LOG.info("Datos ficticios DSY1105 listos: 2 docentes, 4 estudiantes, 3 bitácoras y 3 registros");
    }

    private UserEntity user(String name, String email, String role, String course) {
        return users.findByEmailIgnoreCase(email).map(existing -> {
            if (!role.equals(existing.getRole()) || !Objects.equals(course, existing.getCourse()))
                throw conflict("cuenta", email);
            return existing;
        }).orElseGet(() -> users.save(new UserEntity(name, email, passwords.encode(demoPassword), role, course)));
    }

    private void assignment(UserEntity teacher, String course, String subject) {
        if (!assignments.existsByTeacherIdAndCourseAndSubject(teacher.getId(), course, subject))
            assignments.save(new TeacherAssignmentEntity(teacher.getId(), course, subject));
    }

    private NotebookEntity notebook(UserEntity teacher, String title, String course, String subject,
            int month, int year, String studentEmails, boolean closed) {
        return notebooks.findByTeacherIdAndCourseAndSubjectAndMonthAndYear(
                teacher.getId(), course, subject, month, year)
            .map(existing -> {
                if (!title.equals(existing.getTitle()) || existing.getPublishedAt() == null || existing.isClosed() != closed)
                    throw conflict("bitácora", title);
                return existing;
            }).orElseGet(() -> {
                NotebookEntity created = new NotebookEntity(title, "Demostrar aprendizajes del mes",
                    "Registra la actividad, lo aprendido y tus dudas", "¿Qué aprendiste y qué necesitas reforzar?",
                    teacher.getEmail(), teacher.getName(), System.currentTimeMillis(), course, studentEmails);
                created.configure(teacher.getId(), subject, month, year);
                created.publish();
                if (closed) created.close();
                return notebooks.save(created);
            });
    }

    private void members(NotebookEntity notebook, UserEntity... students) {
        for (UserEntity student : students) {
            if (!notebookStudents.existsByNotebookIdAndStudentId(notebook.getId(), student.getId()))
                notebookStudents.save(new NotebookStudentEntity(notebook.getId(), student.getId()));
        }
    }

    private StudentEntryEntity entry(NotebookEntity notebook, UserEntity student, String date,
            String state, UserEntity teacher) {
        return entries.findByNotebookId(notebook.getId()).stream()
            .filter(existing -> Objects.equals(existing.getStudentId(), student.getId()) && date.equals(existing.getClassDate()))
            .findFirst().map(existing -> {
                boolean valid = switch (state) {
                    case "DRAFT" -> existing.isDraft() && !existing.isReviewed();
                    case "SUBMITTED" -> !existing.isDraft() && !existing.isReviewed();
                    case "REVIEWED" -> !existing.isDraft() && existing.isReviewed();
                    default -> false;
                };
                if (!valid) throw conflict("registro", student.getEmail() + " " + date);
                return existing;
            }).orElseGet(() -> {
                StudentEntryEntity created = new StudentEntryEntity(notebook.getId(), notebook.getTitle(),
                    teacher.getEmail(), student.getEmail(), student.getName(), date,
                    "Contenido ficticio de la clase", "Actividad ficticia del curso",
                    "Aprendizaje ficticio del mes", "", true, System.currentTimeMillis(), false, "");
                created.assignStudent(student.getId());
                if (!"DRAFT".equals(state)) created.submit();
                if ("REVIEWED".equals(state))
                    created.review(teacher.getId(), teacher.getEmail(), "Buen trabajo; explica con más detalle el procedimiento.");
                return entries.save(created);
            });
    }

    private void fileEvidence(StudentEntryEntity entry, UserEntity owner, String kind, String name,
            String mime, String storageKey, byte[] bytes) {
        EvidenceEntity existing = evidences.findByEntryId(entry.getId()).stream()
            .filter(evidence -> kind.equals(evidence.getKind())).findFirst().orElse(null);
        if (existing != null) {
            if (!storageKey.equals(existing.getStorageKey())) throw conflict("evidencia", kind);
            ensureFile(storageKey, bytes);
            return;
        }
        ensureFile(storageKey, bytes);
        evidences.save(new EvidenceEntity(entry.getId(), owner.getId(), kind, name, mime,
            (long) bytes.length, storageKey, null));
    }

    private void linkEvidence(StudentEntryEntity entry, UserEntity owner, String url) {
        EvidenceEntity existing = evidences.findByEntryId(entry.getId()).stream()
            .filter(evidence -> "LINK".equals(evidence.getKind())).findFirst().orElse(null);
        if (existing != null) {
            if (!url.equals(existing.getUrl())) throw conflict("evidencia", "LINK");
            return;
        }
        evidences.save(new EvidenceEntity(entry.getId(), owner.getId(), "LINK", null, null, null, null, url));
    }

    private void ensureFile(String storageKey, byte[] bytes) {
        Path file = evidenceDirectory.resolve(storageKey).normalize();
        if (!file.startsWith(evidenceDirectory)) throw new IllegalStateException("Invalid demo evidence path");
        try {
            Files.createDirectories(evidenceDirectory);
            if (Files.exists(file)) {
                if (!Arrays.equals(bytes, Files.readAllBytes(file))) throw conflict("archivo", storageKey);
            } else {
                Files.write(file, bytes, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare demo evidence " + storageKey, e);
        }
    }

    private IllegalStateException conflict(String kind, String value) {
        return new IllegalStateException("Existing " + kind + " conflicts with demo data: " + value);
    }
}

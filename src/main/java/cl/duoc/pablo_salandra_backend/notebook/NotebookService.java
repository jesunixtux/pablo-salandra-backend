package cl.duoc.pablo_salandra_backend.notebook;

import cl.duoc.pablo_salandra_backend.auth.CurrentUser;
import cl.duoc.pablo_salandra_backend.auth.SchoolCatalog;
import cl.duoc.pablo_salandra_backend.auth.TeacherAssignmentRepository;
import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.auth.UserRepository;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryRepository;
import cl.duoc.pablo_salandra_backend.sync.IdempotencyService;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookService {
    private final NotebookRepository notebooks;
    private final NotebookStudentRepository students;
    private final StudentEntryRepository entries;
    private final UserRepository users;
    private final TeacherAssignmentRepository assignments;
    private final CurrentUser current;
    private final IdempotencyService idempotency;

    public NotebookService(NotebookRepository notebooks, NotebookStudentRepository students,
            StudentEntryRepository entries, UserRepository users,
            TeacherAssignmentRepository assignments, CurrentUser current,
            IdempotencyService idempotency) {
        this.notebooks = notebooks;
        this.students = students;
        this.entries = entries;
        this.users = users;
        this.assignments = assignments;
        this.current = current;
        this.idempotency = idempotency;
    }

    public List<Notebook> findAll() {
        UserEntity user = current.get();
        return notebooks.findAllByOrderByCreatedAtDesc().stream()
            .filter(n -> visibleTo(n, user)).map(this::toDto).toList();
    }

    public Notebook findById(long id) {
        UserEntity user = current.get();
        NotebookEntity notebook = notebooks.findById(id).orElseThrow(this::notFound);
        if (!visibleTo(notebook, user)) throw notFound();
        return toDto(notebook);
    }

    public Participation participation(long id) {
        NotebookEntity notebook = owned(id, current.require("TEACHER"));
        Set<String> assigned = emails(notebook.getAssignedStudentEmails());
        List<UserEntity> cohort = users.findByRoleAndCourseAndActiveTrue("STUDENT", notebook.getCourse())
            .stream().filter(user -> assigned.isEmpty() || assigned.contains(user.getEmail())).toList();
        var submitted = entries.findByNotebookId(id).stream().filter(entry -> !entry.isDraft()).toList();
        Set<String> participating = submitted.stream().map(entry -> entry.getStudentEmail().toLowerCase())
            .collect(Collectors.toSet());
        List<String> missing = cohort.stream().map(UserEntity::getEmail)
            .filter(email -> !participating.contains(email)).sorted().toList();
        long reviewed = submitted.stream().filter(entry -> entry.isReviewed()).count();
        return new Participation(cohort.size(), cohort.size() - missing.size(), missing.size(),
            submitted.size(), reviewed, submitted.size() - reviewed, missing);
    }

    public record Participation(int totalStudents, int participatingStudents, int missingStudents,
        long totalEntries, long reviewedEntries, long pendingEntries, List<String> missingStudentEmails) {}

    @Transactional
    public Notebook create(NotebookInput input, String key) {
        UserEntity teacher = current.require("TEACHER");
        validatePeriodAndAssignment(teacher, input);
        Long replay = idempotency.find(teacher, "notebook:create", key, input.toString());
        if (replay != null) return findById(replay);
        if (notebooks.existsByTeacherIdAndCourseAndSubjectAndMonthAndYear(
                teacher.getId(), input.course(), input.subject(), input.month(), input.year())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook already exists for this period");
        }
        String assigned = validatedStudents(input.assignedStudentEmails(), input.course());
        NotebookEntity entity = new NotebookEntity(value(input.title()), value(input.purpose()),
            value(input.instructions()), value(input.questions()), teacher.getEmail(), teacher.getName(),
            System.currentTimeMillis(), input.course(), assigned);
        entity.configure(teacher.getId(), input.subject(), input.month(), input.year());
        entity = notebooks.save(entity);
        saveStudents(entity);
        idempotency.remember(teacher, "notebook:create", key, input.toString(), entity.getId());
        return toDto(entity);
    }

    @Transactional
    public Notebook update(long id, NotebookInput input, String ifMatch) {
        UserEntity teacher = current.require("TEACHER");
        NotebookEntity entity = ownedForUpdate(id, teacher);
        requireVersion(entity, ifMatch);
        if (entity.isClosed() || entity.getPublishedAt() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Published or closed notebook cannot be reconfigured");
        validatePeriodAndAssignment(teacher, input);
        if ((!entity.getCourse().equals(input.course()) || !entity.getSubject().equals(input.subject())
                || entity.getMonth() != input.month() || entity.getYear() != input.year())
            && notebooks.existsByTeacherIdAndCourseAndSubjectAndMonthAndYear(
                teacher.getId(), input.course(), input.subject(), input.month(), input.year())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook already exists for this period");
        }
        String assigned = validatedStudents(input.assignedStudentEmails(), input.course());
        entity.replace(value(input.title()), value(input.purpose()), value(input.instructions()),
            value(input.questions()), teacher.getEmail(), teacher.getName(), input.course(), assigned);
        entity.updatePeriod(teacher.getId(), input.subject(), input.month(), input.year());
        saveStudents(entity);
        return toDto(notebooks.save(entity));
    }

    @Transactional
    public Notebook publish(long id, String ifMatch) {
        NotebookEntity entity = ownedForUpdate(id, current.require("TEACHER"));
        requireVersion(entity, ifMatch);
        if (entity.isClosed() || entity.getPublishedAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT);
        entity.publish();
        return toDto(notebooks.save(entity));
    }

    @Transactional
    public Notebook close(long id, String ifMatch) {
        NotebookEntity entity = ownedForUpdate(id, current.require("TEACHER"));
        requireVersion(entity, ifMatch);
        if (entity.isClosed() || entity.getPublishedAt() == null) throw new ResponseStatusException(HttpStatus.CONFLICT);
        entity.close();
        return toDto(notebooks.save(entity));
    }

    @Transactional
    public Notebook reminders(long id, boolean enabled, String ifMatch) {
        NotebookEntity entity = ownedForUpdate(id, current.require("TEACHER"));
        requireVersion(entity, ifMatch);
        if (entity.isClosed()) throw new ResponseStatusException(HttpStatus.CONFLICT);
        entity.setRemindersEnabled(enabled);
        return toDto(notebooks.save(entity));
    }

    @Transactional
    public void delete(long id, String ifMatch) {
        NotebookEntity entity = ownedForUpdate(id, current.require("TEACHER"));
        requireVersion(entity, ifMatch);
        if (entity.getPublishedAt() != null || entries.existsByNotebookId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook history must be preserved");
        students.deleteByNotebookId(id);
        notebooks.delete(entity);
    }

    public NotebookEntity accessibleForEntry(long id, UserEntity student) {
        NotebookEntity notebook = notebooks.findLockedById(id).orElseThrow(this::notFound);
        if (!"STUDENT".equals(student.getRole()) || !visibleTo(notebook, student)) throw notFound();
        return notebook;
    }

    public boolean owns(NotebookEntity notebook, UserEntity teacher) {
        return "TEACHER".equals(teacher.getRole()) &&
            assignments.existsByTeacherIdAndCourseAndSubject(
                teacher.getId(), notebook.getCourse(), notebook.getSubject()) &&
            (Objects.equals(notebook.getTeacherId(), teacher.getId()) ||
                (notebook.getTeacherId() == null &&
                    teacher.getEmail().equalsIgnoreCase(notebook.getTeacherEmail())));
    }

    private boolean visibleTo(NotebookEntity notebook, UserEntity user) {
        if ("TEACHER".equals(user.getRole())) return owns(notebook, user);
        if (!"STUDENT".equals(user.getRole()) || notebook.getPublishedAt() == null ||
                !Objects.equals(notebook.getCourse(), user.getCourse())) return false;
        Set<String> assigned = emails(notebook.getAssignedStudentEmails());
        return assigned.isEmpty() || assigned.contains(user.getEmail());
    }

    private NotebookEntity owned(long id, UserEntity teacher) {
        NotebookEntity notebook = notebooks.findById(id).orElseThrow(this::notFound);
        if (!owns(notebook, teacher)) throw notFound();
        return notebook;
    }

    private NotebookEntity ownedForUpdate(long id, UserEntity teacher) {
        NotebookEntity notebook = notebooks.findLockedById(id).orElseThrow(this::notFound);
        if (!owns(notebook, teacher)) throw notFound();
        return notebook;
    }

    private void validatePeriodAndAssignment(UserEntity teacher, NotebookInput input) {
        if (!SchoolCatalog.COURSES.contains(input.course()) ||
                !SchoolCatalog.SUBJECTS.contains(input.subject()) ||
                !SchoolCatalog.validPeriod(input.month(), input.year())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course, subject or period");
        }
        if (!assignments.existsByTeacherIdAndCourseAndSubject(
                teacher.getId(), input.course(), input.subject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course and subject not assigned");
        }
    }

    private String validatedStudents(String serialized, String course) {
        Set<String> requested = emails(serialized);
        for (String email : requested) {
            UserEntity student = users.findByEmailIgnoreCase(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned student not found"));
            if (!"STUDENT".equals(student.getRole()) || !course.equals(student.getCourse()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned student has different course");
        }
        return requested.stream().sorted().collect(Collectors.joining(","));
    }

    private void saveStudents(NotebookEntity notebook) {
        students.deleteByNotebookId(notebook.getId());
        for (String email : emails(notebook.getAssignedStudentEmails())) {
            UserEntity student = users.findByEmailIgnoreCase(email).orElseThrow(this::notFound);
            students.save(new NotebookStudentEntity(notebook.getId(), student.getId()));
        }
    }

    private Set<String> emails(String serialized) {
        if (serialized == null || serialized.isBlank()) return Set.of();
        return Arrays.stream(serialized.split(",")).map(String::trim).map(String::toLowerCase)
            .filter(s -> !s.isBlank()).collect(Collectors.toSet());
    }

    private void requireVersion(NotebookEntity entity, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank())
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "If-Match required");
        String candidate = ifMatch.replace("\"", "").trim();
        if (!candidate.equals(Long.toString(entity.getUpdatedAt())))
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Notebook changed since last read");
    }

    private Notebook toDto(NotebookEntity n) {
        UserEntity viewer = current.get();
        String visibleStudents = "TEACHER".equals(viewer.getRole()) ? n.getAssignedStudentEmails()
            : emails(n.getAssignedStudentEmails()).isEmpty() ? "" : viewer.getEmail();
        return new Notebook(n.getId(), n.getTitle(), n.getPurpose(), n.getInstructions(),
            n.getQuestions(), n.getTeacherEmail(), n.getTeacherName(), n.getCreatedAt(),
            n.getCourse(), visibleStudents, n.getSubject(), n.getMonth(),
            n.getYear(), n.getPublishedAt(), n.isClosed(), n.getClosedAt(),
            n.isRemindersEnabled(), n.getUpdatedAt());
    }

    private String value(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND); }
}

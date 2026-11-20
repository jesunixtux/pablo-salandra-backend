package cl.duoc.pablo_salandra_backend.studententry;

import cl.duoc.pablo_salandra_backend.auth.CurrentUser;
import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.evidence.EvidenceDto;
import cl.duoc.pablo_salandra_backend.evidence.EvidenceRepository;
import cl.duoc.pablo_salandra_backend.notebook.NotebookEntity;
import cl.duoc.pablo_salandra_backend.notebook.NotebookRepository;
import cl.duoc.pablo_salandra_backend.notebook.NotebookService;
import cl.duoc.pablo_salandra_backend.sync.IdempotencyService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentEntryService {
    private static final DateTimeFormatter CLASS_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT);
    private final StudentEntryRepository entries;
    private final NotebookRepository notebooks;
    private final NotebookService notebookService;
    private final CurrentUser current;
    private final IdempotencyService idempotency;
    private final EvidenceRepository evidences;

    public StudentEntryService(StudentEntryRepository entries, NotebookRepository notebooks,
            NotebookService notebookService, CurrentUser current, IdempotencyService idempotency,
            EvidenceRepository evidences) {
        this.entries = entries;
        this.notebooks = notebooks;
        this.notebookService = notebookService;
        this.current = current;
        this.idempotency = idempotency;
        this.evidences = evidences;
    }

    public List<StudentEntry> findAll(String studentEmail, String teacherEmail,
            String classDate, String status) {
        UserEntity user = current.get();
        if (status != null && !List.of("DRAFT", "SUBMITTED", "PENDING", "REVIEWED")
                .contains(status.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        if ("STUDENT".equals(user.getRole()) && studentEmail != null &&
                !studentEmail.equalsIgnoreCase(user.getEmail())) throw forbidden();
        if ("TEACHER".equals(user.getRole()) && teacherEmail != null &&
                !teacherEmail.equalsIgnoreCase(user.getEmail())) throw forbidden();
        return entries.findAll().stream().filter(entry -> visibleTo(entry, user))
            .filter(entry -> studentEmail == null || studentEmail.equalsIgnoreCase(entry.getStudentEmail()))
            .filter(entry -> teacherEmail == null || teacherEmail.equalsIgnoreCase(entry.getTeacherEmail()))
            .filter(entry -> classDate == null || classDate.equals(entry.getClassDate()))
            .filter(entry -> status == null || switch (status.toUpperCase()) {
                case "DRAFT" -> entry.isDraft();
                case "SUBMITTED" -> !entry.isDraft();
                case "PENDING" -> !entry.isDraft() && !entry.isReviewed();
                case "REVIEWED" -> !entry.isDraft() && entry.isReviewed();
                default -> false;
            })
            .sorted(Comparator.comparingLong(StudentEntryEntity::getCreatedAt).reversed())
            .map(this::toDto).toList();
    }

    public StudentEntry findById(long id) {
        UserEntity user = current.get();
        StudentEntryEntity entry = entries.findById(id).orElseThrow(this::notFound);
        if (!visibleTo(entry, user)) throw notFound();
        return toDto(entry);
    }

    @Transactional
    public StudentEntry create(StudentEntryInput input, String key) {
        UserEntity student = current.require("STUDENT");
        Long replay = idempotency.find(student, "entry:create", key, input.toString());
        if (replay != null) return findById(replay);
        NotebookEntity notebook = notebookService.accessibleForEntry(input.notebookId(), student);
        if (notebook.isClosed()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook is closed");
        validateContents(input, !input.isDraft(), notebook);
        StudentEntryEntity entry = new StudentEntryEntity(notebook.getId(), notebook.getTitle(),
            notebook.getTeacherEmail(), student.getEmail(), student.getName(), value(input.classDate()),
            value(input.content()), value(input.activity()), value(input.learning()), value(input.doubts()),
            true, System.currentTimeMillis(), false, "");
        entry.assignStudent(student.getId());
        if (!input.isDraft()) entry.submit();
        entry = entries.save(entry);
        idempotency.remember(student, "entry:create", key, input.toString(), entry.getId());
        return toDto(entry);
    }

    @Transactional
    public StudentEntry update(long id, StudentEntryInput input, String ifMatch) {
        UserEntity student = current.require("STUDENT");
        StudentEntryEntity entry = ownDraft(id, student);
        requireVersion(entry, ifMatch);
        if (entry.getNotebookId() != input.notebookId())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notebook cannot change");
        NotebookEntity notebook = notebookService.accessibleForEntry(entry.getNotebookId(), student);
        if (notebook.isClosed()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook is closed");
        validateContents(input, !input.isDraft(), notebook);
        entry.replace(entry.getNotebookId(), notebook.getTitle(), notebook.getTeacherEmail(),
            student.getEmail(), student.getName(), value(input.classDate()), value(input.content()),
            value(input.activity()), value(input.learning()), value(input.doubts()), true, false, "");
        if (!input.isDraft()) entry.submit();
        return toDto(entries.save(entry));
    }

    @Transactional
    public StudentEntry submit(long id, String ifMatch) {
        UserEntity student = current.require("STUDENT");
        StudentEntryEntity entry = ownDraft(id, student);
        requireVersion(entry, ifMatch);
        NotebookEntity notebook = notebookService.accessibleForEntry(entry.getNotebookId(), student);
        if (notebook.isClosed()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook is closed");
        validateContents(new StudentEntryInput(entry.getNotebookId(), entry.getNotebookTitle(),
            entry.getStudentEmail(), entry.getStudentName(), entry.getClassDate(), entry.getContent(),
            entry.getActivity(), entry.getLearning(), entry.getDoubts(), false, false, ""), true, notebook);
        entry.submit();
        return toDto(entries.save(entry));
    }

    @Transactional
    public StudentEntry review(long id, String feedback, String ifMatch) {
        UserEntity teacher = current.require("TEACHER");
        StudentEntryEntity entry = entries.findLockedById(id).orElseThrow(this::notFound);
        Long entryNotebookId = entry.getNotebookId();
        if (entryNotebookId == null) throw notFound(); // fila legacy huérfana
        NotebookEntity notebook = notebooks.findById(entryNotebookId).orElseThrow(this::notFound);
        if (!notebookService.owns(notebook, teacher)) throw notFound();
        requireVersion(entry, ifMatch);
        if (entry.isDraft()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft cannot be reviewed");
        if (feedback == null || feedback.isBlank() || feedback.length() > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback required");
        entry.review(teacher.getId(), teacher.getEmail(), feedback.trim());
        return toDto(entries.save(entry));
    }

    @Transactional
    public void delete(long id, String ifMatch) {
        StudentEntryEntity entry = ownDraft(id, current.require("STUDENT"));
        requireVersion(entry, ifMatch);
        if (evidences.existsByEntryId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entry with evidence cannot be deleted");
        entries.delete(entry);
    }

    public StudentEntryEntity accessibleForEvidence(long id, UserEntity user, boolean write) {
        StudentEntryEntity entry = (write ? entries.findLockedById(id) : entries.findById(id))
            .orElseThrow(this::notFound);
        if (!visibleTo(entry, user) || (write && (!"STUDENT".equals(user.getRole()) || !entry.isDraft())))
            throw notFound();
        if (write) {
            NotebookEntity notebook = notebookService.accessibleForEntry(entry.getNotebookId(), user);
            if (notebook.isClosed()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Notebook is closed");
        }
        return entry;
    }

    private StudentEntryEntity ownDraft(long id, UserEntity student) {
        StudentEntryEntity entry = entries.findLockedById(id).orElseThrow(this::notFound);
        if (!owns(entry, student)) throw notFound();
        if (!entry.isDraft()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Submitted entry is immutable");
        return entry;
    }

    private boolean visibleTo(StudentEntryEntity entry, UserEntity user) {
        if ("STUDENT".equals(user.getRole())) return owns(entry, user);
        if (!"TEACHER".equals(user.getRole()) || entry.isDraft()) return false;
        Long notebookId = entry.getNotebookId();
        if (notebookId == null) return false; // fila legacy huérfana: nunca visible
        return notebooks.findById(notebookId)
            .map(notebook -> notebookService.owns(notebook, user)).orElse(false);
    }

    private boolean owns(StudentEntryEntity entry, UserEntity student) {
        // Sin fallback por correo: una cuenta registrada con un correo ajeno no puede
        // apropiarse de filas legacy sin student_id (la identidad la fija el servidor).
        return Objects.equals(entry.getStudentId(), student.getId());
    }

    private void validateContents(StudentEntryInput input, boolean submitted, NotebookEntity notebook) {
        if (!submitted) return;
        if (blank(input.content()) || blank(input.activity()) || blank(input.learning()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required entry fields missing");
        LocalDate date;
        try {
            date = LocalDate.parse(value(input.classDate()), CLASS_DATE);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid class date");
        }
        if (date.getMonthValue() != notebook.getMonth() || date.getYear() != notebook.getYear())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class date outside notebook period");
    }

    private void requireVersion(StudentEntryEntity entry, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank())
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "If-Match required");
        if (!ifMatch.replace("\"", "").trim().equals(Long.toString(entry.getUpdatedAt())))
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Entry changed since last read");
    }

    private StudentEntry toDto(StudentEntryEntity e) {
        return new StudentEntry(e.getId(), e.getNotebookId(), e.getNotebookTitle(), e.getTeacherEmail(),
            e.getStudentEmail(), e.getStudentName(), e.getClassDate(), e.getContent(), e.getActivity(),
            e.getLearning(), e.getDoubts(), e.isDraft(), e.getCreatedAt(), e.isReviewed(),
            e.getTeacherFeedback(), e.getSubmittedAt(), e.getReviewedAt(), e.getReviewerEmail(),
            evidences.findByEntryId(e.getId()).stream().map(ev -> ev.toDto()).toList(),
            e.getUpdatedAt());
    }

    private String value(String value) { return value == null ? "" : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND); }
    private ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN); }
}

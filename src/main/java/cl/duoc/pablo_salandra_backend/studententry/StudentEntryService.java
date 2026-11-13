package cl.duoc.pablo_salandra_backend.studententry;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import cl.duoc.pablo_salandra_backend.notebook.NotebookEntity;
import cl.duoc.pablo_salandra_backend.notebook.NotebookRepository;

@Service
public class StudentEntryService {
    private static final DateTimeFormatter CLASS_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT);
    private final StudentEntryRepository entries;
    private final NotebookRepository notebooks;

    public StudentEntryService(StudentEntryRepository entries, NotebookRepository notebooks) {
        this.entries = entries;
        this.notebooks = notebooks;
    }

    public List<StudentEntry> findAll(String studentEmail, String teacherEmail) {
        return entries.findAll().stream()
            .filter(entry -> studentEmail == null || entry.getStudentEmail().equalsIgnoreCase(studentEmail))
            .filter(entry -> teacherEmail == null || entry.getTeacherEmail().equalsIgnoreCase(teacherEmail))
            .sorted((left, right) -> Long.compare(right.getCreatedAt(), left.getCreatedAt()))
            .map(this::toDto)
            .toList();
    }

    public StudentEntry findById(long id) {
        return toDto(entries.findById(id).orElseThrow(this::notFound));
    }

    public StudentEntry create(StudentEntryInput input) {
        return toDto(entries.save(from(input, System.currentTimeMillis())));
    }

    public StudentEntry update(long id, StudentEntryInput input) {
        StudentEntryEntity existing = entries.findById(id).orElseThrow(this::notFound);
        StudentEntryEntity updated = from(input, existing.getCreatedAt());
        existing.replace(updated.getNotebookId(), updated.getNotebookTitle(), updated.getTeacherEmail(),
            updated.getStudentEmail(), updated.getStudentName(), updated.getClassDate(),
            updated.getContent(), updated.getActivity(), updated.getLearning(), updated.getDoubts(),
            updated.isDraft());
        return toDto(entries.save(existing));
    }

    public void delete(long id) {
        entries.delete(entries.findById(id).orElseThrow(this::notFound));
    }

    private StudentEntryEntity from(StudentEntryInput input, long createdAt) {
        if (blank(input.studentEmail()) || blank(input.studentName())) badRequest();
        NotebookEntity notebook = notebooks.findById(input.notebookId()).orElseThrow(this::notFound);
        String classDate = value(input.classDate());
        String content = value(input.content());
        String activity = value(input.activity());
        String learning = value(input.learning());
        String doubts = value(input.doubts());
        if (input.isDraft()) {
            if (content.isBlank() && activity.isBlank() && learning.isBlank() && doubts.isBlank()) badRequest();
        } else {
            if (content.isBlank() || activity.isBlank() || learning.isBlank()) badRequest();
            try {
                LocalDate.parse(classDate, CLASS_DATE);
            } catch (RuntimeException exception) {
                badRequest();
            }
        }
        return new StudentEntryEntity(notebook.getId(), notebook.getTitle(), notebook.getTeacherEmail(),
            email(input.studentEmail()), value(input.studentName()), classDate, content, activity,
            learning, doubts, input.isDraft(), createdAt);
    }

    private StudentEntry toDto(StudentEntryEntity entry) {
        return new StudentEntry(entry.getId(), entry.getNotebookId(), entry.getNotebookTitle(),
            entry.getTeacherEmail(), entry.getStudentEmail(), entry.getStudentName(), entry.getClassDate(),
            entry.getContent(), entry.getActivity(), entry.getLearning(), entry.getDoubts(),
            entry.isDraft(), entry.getCreatedAt());
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String email(String value) {
        return value.trim().toLowerCase();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void badRequest() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}

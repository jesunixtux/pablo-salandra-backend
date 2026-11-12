package cl.duoc.pablo_salandra_backend.studententry;

import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import cl.duoc.pablo_salandra_backend.notebook.Notebook;
import cl.duoc.pablo_salandra_backend.notebook.NotebookService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentEntryService {
    private final ConcurrentHashMap<Long, StudentEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final NotebookService notebooks;
    private static final DateTimeFormatter CLASS_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT);

    public StudentEntryService(NotebookService notebooks) {
        this.notebooks = notebooks;
    }

    public List<StudentEntry> findAll(String studentEmail, String teacherEmail) {
        return entries.values().stream()
            .filter(entry -> studentEmail == null || entry.studentEmail().equalsIgnoreCase(studentEmail))
            .filter(entry -> teacherEmail == null || entry.teacherEmail().equalsIgnoreCase(teacherEmail))
            .sorted(Comparator.comparingLong(StudentEntry::createdAt).reversed())
            .toList();
    }

    public StudentEntry findById(long id) {
        StudentEntry entry = entries.get(id);
        if (entry == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return entry;
    }

    public StudentEntry create(StudentEntryInput input) {
        long id = nextId.getAndIncrement();
        StudentEntry entry = from(id, input, System.currentTimeMillis());
        entries.put(id, entry);
        return entry;
    }

    public StudentEntry update(long id, StudentEntryInput input) {
        StudentEntry existing = findById(id);
        StudentEntry updated = from(id, input, existing.createdAt());
        entries.put(id, updated);
        return updated;
    }

    public void delete(long id) {
        if (entries.remove(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private StudentEntry from(long id, StudentEntryInput input, long createdAt) {
        if (blank(input.studentEmail()) || blank(input.studentName())) badRequest();
        Notebook notebook = notebooks.findById(input.notebookId());
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
        return new StudentEntry(
            id,
            input.notebookId(),
            notebook.title(),
            notebook.teacherEmail(),
            input.studentEmail().trim().toLowerCase(),
            input.studentName().trim(),
            classDate,
            content,
            activity,
            learning,
            doubts,
            input.isDraft(),
            createdAt
        );
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void badRequest() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
}

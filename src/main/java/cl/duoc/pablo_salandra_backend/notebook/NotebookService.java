package cl.duoc.pablo_salandra_backend.notebook;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookService {
    private final NotebookRepository notebooks;

    public NotebookService(NotebookRepository notebooks) {
        this.notebooks = notebooks;
    }

    public List<Notebook> findAll() {
        return notebooks.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    public Notebook findById(long id) {
        return toDto(notebooks.findById(id).orElseThrow(this::notFound));
    }

    public Notebook create(NotebookInput input) {
        return toDto(notebooks.save(from(input, System.currentTimeMillis())));
    }

    public Notebook update(long id, NotebookInput input) {
        NotebookEntity existing = notebooks.findById(id).orElseThrow(this::notFound);
        existing.replace(value(input.title()), value(input.purpose()), value(input.instructions()),
            value(input.questions()), email(input.teacherEmail()), value(input.teacherName()),
            course(input.course()), value(input.assignedStudentEmails()));
        return toDto(notebooks.save(existing));
    }

    public void delete(long id) {
        notebooks.delete(notebooks.findById(id).orElseThrow(this::notFound));
    }

    private NotebookEntity from(NotebookInput input, long createdAt) {
        return new NotebookEntity(value(input.title()), value(input.purpose()), value(input.instructions()),
            value(input.questions()), email(input.teacherEmail()), value(input.teacherName()), createdAt,
            course(input.course()), value(input.assignedStudentEmails()));
    }

    private Notebook toDto(NotebookEntity notebook) {
        return new Notebook(notebook.getId(), notebook.getTitle(), notebook.getPurpose(),
            notebook.getInstructions(), notebook.getQuestions(), notebook.getTeacherEmail(),
            notebook.getTeacherName(), notebook.getCreatedAt(), course(notebook.getCourse()),
            value(notebook.getAssignedStudentEmails()));
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String email(String value) {
        return value.trim().toLowerCase();
    }

    private String course(String value) {
        return value == null || value.isBlank() ? "General" : value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}

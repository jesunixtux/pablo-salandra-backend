package cl.duoc.pablo_salandra_backend.notebook;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookService {
    private final ConcurrentHashMap<Long, Notebook> notebooks = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Notebook> findAll() {
        return notebooks.values().stream()
            .sorted(Comparator.comparingLong(Notebook::createdAt).reversed())
            .toList();
    }

    public Notebook findById(long id) {
        Notebook notebook = notebooks.get(id);
        if (notebook == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return notebook;
    }

    public Notebook create(NotebookInput input) {
        long id = nextId.getAndIncrement();
        Notebook notebook = from(id, input, System.currentTimeMillis());
        notebooks.put(id, notebook);
        return notebook;
    }

    public Notebook update(long id, NotebookInput input) {
        Notebook existing = findById(id);
        Notebook updated = from(id, input, existing.createdAt());
        notebooks.put(id, updated);
        return updated;
    }

    public void delete(long id) {
        if (notebooks.remove(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private Notebook from(long id, NotebookInput input, long createdAt) {
        return new Notebook(
            id,
            input.title().trim(),
            input.purpose().trim(),
            input.instructions().trim(),
            input.questions() == null ? "" : input.questions().trim(),
            input.teacherEmail().trim().toLowerCase(),
            input.teacherName().trim(),
            createdAt
        );
    }
}

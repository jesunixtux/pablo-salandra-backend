package cl.duoc.pablo_salandra_backend.notebook;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notebooks")
public class NotebookController {
    private final NotebookService service;

    public NotebookController(NotebookService service) { this.service = service; }

    @GetMapping
    public List<Notebook> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public Notebook findById(@PathVariable long id) { return service.findById(id); }

    @GetMapping("/{id}/participation")
    public NotebookService.Participation participation(@PathVariable long id) {
        return service.participation(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notebook create(@Valid @RequestBody NotebookInput input,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return service.create(input, key);
    }

    @PutMapping("/{id}")
    public Notebook update(@PathVariable long id, @Valid @RequestBody NotebookInput input,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return service.update(id, input, ifMatch);
    }

    @PostMapping("/{id}/publish")
    public Notebook publish(@PathVariable long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return service.publish(id, ifMatch);
    }

    @PostMapping("/{id}/close")
    public Notebook close(@PathVariable long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return service.close(id, ifMatch);
    }

    @PutMapping("/{id}/reminders")
    public Notebook reminders(@PathVariable long id, @RequestBody RemindersInput input,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return service.reminders(id, input.enabled(), ifMatch);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        service.delete(id, ifMatch);
    }

    public record RemindersInput(boolean enabled) {}
}

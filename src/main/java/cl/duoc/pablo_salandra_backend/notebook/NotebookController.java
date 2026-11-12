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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notebooks")
public class NotebookController {
    private final NotebookService service;

    public NotebookController(NotebookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notebook> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Notebook findById(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notebook create(@Valid @RequestBody NotebookInput input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    public Notebook update(@PathVariable long id, @Valid @RequestBody NotebookInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}

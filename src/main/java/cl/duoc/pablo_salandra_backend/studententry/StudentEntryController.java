package cl.duoc.pablo_salandra_backend.studententry;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student-entries")
public class StudentEntryController {
    private final StudentEntryService service;

    public StudentEntryController(StudentEntryService service) {
        this.service = service;
    }

    @GetMapping
    public List<StudentEntry> findAll(
        @RequestParam(required = false) String studentEmail,
        @RequestParam(required = false) String teacherEmail
    ) {
        return service.findAll(studentEmail, teacherEmail);
    }

    @GetMapping("/{id}")
    public StudentEntry findById(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentEntry create(@Valid @RequestBody StudentEntryInput input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    public StudentEntry update(@PathVariable long id, @Valid @RequestBody StudentEntryInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}

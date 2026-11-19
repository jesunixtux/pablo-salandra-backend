package cl.duoc.pablo_salandra_backend.evidence;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evidences")
public class EvidenceController {
    private final EvidenceService service;
    public EvidenceController(EvidenceService service) { this.service = service; }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceDto create(@RequestParam long entryRemoteId, @RequestParam String kind,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String url,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return service.create(entryRemoteId, kind, file, url, key);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) { return service.get(id); }
}

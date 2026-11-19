package cl.duoc.pablo_salandra_backend.sync;

import cl.duoc.pablo_salandra_backend.auth.AuthService;
import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.auth.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdempotencyService {
    private final IdempotencyRepository records;
    private final UserRepository users;

    public IdempotencyService(IdempotencyRepository records, UserRepository users) {
        this.records = records;
        this.users = users;
    }

    // Caller holds a transaction: the user row lock serializes same-user retries.
    public Long find(UserEntity user, String scope, String key, String request) {
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
            "Idempotency-Key required");
        if (key.length() > 128) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        users.findLockedById(user.getId()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return records.findByPrincipalIdAndScopeAndKeyHash(user.getId(), scope, AuthService.hash(key))
            .map(record -> {
                if (!record.getRequestHash().equals(AuthService.hash(request))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key reused with different request");
                }
                return record.getResponseId();
            }).orElse(null);
    }

    public void remember(UserEntity user, String scope, String key, String request, long responseId) {
        records.save(new IdempotencyRecord(user.getId(), scope, AuthService.hash(key),
            AuthService.hash(request), responseId));
    }
}

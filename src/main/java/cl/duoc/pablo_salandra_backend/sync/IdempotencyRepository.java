package cl.duoc.pablo_salandra_backend.sync;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByPrincipalIdAndScopeAndKeyHash(Long principalId,
        String scope, String keyHash);
}

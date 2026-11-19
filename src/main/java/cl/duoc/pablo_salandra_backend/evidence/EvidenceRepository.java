package cl.duoc.pablo_salandra_backend.evidence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, Long> {
    List<EvidenceEntity> findByEntryId(Long entryId);
    boolean existsByEntryId(Long entryId);
}

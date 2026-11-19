package cl.duoc.pablo_salandra_backend.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long principalId;
    private String scope;
    private String keyHash;
    private String requestHash;
    private long responseId;
    private long createdAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(Long principalId, String scope, String keyHash,
            String requestHash, long responseId) {
        this.principalId = principalId;
        this.scope = scope;
        this.keyHash = keyHash;
        this.requestHash = requestHash;
        this.responseId = responseId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getRequestHash() { return requestHash; }
    public long getResponseId() { return responseId; }
}

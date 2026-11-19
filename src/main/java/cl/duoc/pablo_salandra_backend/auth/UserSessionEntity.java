package cl.duoc.pablo_salandra_backend.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_sessions")
public class UserSessionEntity {
    @Id
    private String tokenHash;
    private Long userId;
    private long createdAt;
    private long expiresAt;
    private Long revokedAt;

    protected UserSessionEntity() {}

    public UserSessionEntity(String tokenHash, Long userId, long now, long expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() { return userId; }
    public long getExpiresAt() { return expiresAt; }
    public Long getRevokedAt() { return revokedAt; }
    public void revoke(long now) { revokedAt = now; }
}

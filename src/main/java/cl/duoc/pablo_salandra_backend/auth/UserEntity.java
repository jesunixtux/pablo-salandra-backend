package cl.duoc.pablo_salandra_backend.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private String course;
    private boolean active;
    private long createdAt;

    protected UserEntity() {}

    public UserEntity(String name, String email, String passwordHash, String role, String course) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.course = course;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getCourse() { return course; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
}

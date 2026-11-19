package cl.duoc.pablo_salandra_backend.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final long SESSION_MILLIS = 12L * 60 * 60 * 1000;
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final TeacherAssignmentRepository assignments;
    private final PasswordEncoder passwords;

    public AuthService(UserRepository users, UserSessionRepository sessions,
            TeacherAssignmentRepository assignments, PasswordEncoder passwords) {
        this.users = users;
        this.sessions = sessions;
        this.assignments = assignments;
        this.passwords = passwords;
    }

    @Transactional
    public AuthResponse register(RegisterInput input) {
        String email = input.email().trim().toLowerCase();
        if (!SchoolCatalog.COURSES.contains(input.course()) || users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course or email already registered");
        }
        UserEntity user = users.save(new UserEntity(input.name().trim(), email,
            passwords.encode(input.password()), "STUDENT", input.course()));
        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(LoginInput input) {
        UserEntity user = users.findByEmailIgnoreCase(input.email().trim()).orElse(null);
        if (user == null || !user.isActive() || !passwords.matches(input.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueToken(user);
    }

    @Transactional
    public void logout(String token) {
        if (token == null) return;
        sessions.findById(hash(token)).ifPresent(session -> session.revoke(System.currentTimeMillis()));
    }

    public UserDto identity(UserEntity user) {
        List<AssignmentDto> scoped = assignments.findByTeacherId(user.getId()).stream()
            .map(a -> new AssignmentDto(a.getCourse(), a.getSubject())).toList();
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(),
            user.getCourse(), user.isActive(), scoped);
    }

    private AuthResponse issueToken(UserEntity user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long now = System.currentTimeMillis();
        sessions.save(new UserSessionEntity(hash(token), user.getId(), now, now + SESSION_MILLIS));
        return new AuthResponse(token, now + SESSION_MILLIS, identity(user));
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

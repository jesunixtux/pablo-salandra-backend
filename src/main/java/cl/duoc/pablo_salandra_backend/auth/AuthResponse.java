package cl.duoc.pablo_salandra_backend.auth;

import java.util.List;

public record AuthResponse(String token, long expiresAt, UserDto user) {}
record UserDto(Long id, String name, String email, String role, String course,
    boolean active, List<AssignmentDto> assignments) {}
record AssignmentDto(String course, String subject) {}

package cl.duoc.pablo_salandra_backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {
    public UserEntity get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof UserEntity user && user.isActive()) return user;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    public UserEntity require(String role) {
        UserEntity user = get();
        if (!role.equals(user.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }
}

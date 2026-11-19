package cl.duoc.pablo_salandra_backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final CurrentUser current;

    public AuthController(AuthService service, CurrentUser current) {
        this.service = service;
        this.current = current;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterInput input) {
        return service.register(input);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginInput input) {
        return service.login(input);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        current.get();
        service.logout(authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7) : null);
    }

    @GetMapping("/me")
    public UserDto me() {
        return service.identity(current.get());
    }
}

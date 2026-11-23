package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    private final AuthService auth;
    private final LabMode mode;

    public AuthController(AuthService auth, LabMode mode) {
        this.auth = auth;
        this.mode = mode;
    }

    @PostMapping("/login")
    public ResponseEntity<Actor> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest servletRequest) {
        var result = auth.login(request.username(), request.password(), servletRequest.getRemoteAddr());
        Duration age = mode.vulnerable() ? Duration.ofDays(365) : Duration.ofMinutes(15);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(result.token(), age).toString())
                .body(result.user());
    }

    @GetMapping("/me")
    public Actor me(Authentication authentication) {
        return (Actor) authentication.getPrincipal();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    private ResponseCookie cookie(String value, Duration age) {
        return ResponseCookie.from("LAB_TOKEN", value).httpOnly(true).secure(true).path("/")
                .sameSite(mode.vulnerable() ? "None" : "Strict").maxAge(age).build();
    }
}

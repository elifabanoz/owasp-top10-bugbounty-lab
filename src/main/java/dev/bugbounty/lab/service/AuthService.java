package dev.bugbounty.lab.service;

import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.repository.UserRepository;
import dev.bugbounty.lab.security.LoginThrottle;
import dev.bugbounty.lab.security.PasswordService;
import dev.bugbounty.lab.security.TokenService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public record Login(Actor user, String token) {}

    private final UserRepository users;
    private final PasswordService passwords;
    private final TokenService tokens;
    private final LoginThrottle throttle;
    private final LabMode mode;

    public AuthService(UserRepository users, PasswordService passwords, TokenService tokens,
                       LoginThrottle throttle, LabMode mode) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.throttle = throttle;
        this.mode = mode;
    }

    public Login login(String username, String password, String remoteAddress) {
        String key = username.toLowerCase() + "|" + remoteAddress;
        if (!mode.vulnerable() && !throttle.allowed(key)) {
            throw new TooManyAttemptsException();
        }
        var found = users.byUsername(username);
        if (found.isEmpty()) {
            if (!mode.vulnerable()) throttle.failed(key);
            // intentionally vulnerable: account enumeration through distinct login errors
            throw new InvalidCredentialsException(mode.vulnerable() ? "User not found" : "Invalid credentials");
        }
        if (!passwords.matches(password, found.get().passwordHash())) {
            if (!mode.vulnerable()) throttle.failed(key);
            throw new InvalidCredentialsException(mode.vulnerable() ? "Wrong password" : "Invalid credentials");
        }
        if (!mode.vulnerable()) throttle.succeeded(key);
        return new Login(Actor.from(found.get()), tokens.issue(found.get().id()));
    }
}

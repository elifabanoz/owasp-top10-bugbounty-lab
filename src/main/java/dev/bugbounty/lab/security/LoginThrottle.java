package dev.bugbounty.lab.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginThrottle {
    private record Attempts(int count, Instant resetAt) {}
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean allowed(String key) {
        var state = attempts.get(key);
        return state == null || Instant.now().isAfter(state.resetAt()) || state.count() < 5;
    }

    public void failed(String key) {
        attempts.compute(key, (ignored, state) -> {
            if (state == null || Instant.now().isAfter(state.resetAt())) {
                return new Attempts(1, Instant.now().plusSeconds(900));
            }
            return new Attempts(state.count() + 1, state.resetAt());
        });
    }

    public void succeeded(String key) {
        attempts.remove(key);
    }
}

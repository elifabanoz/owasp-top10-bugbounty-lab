package dev.bugbounty.lab.service;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("secure")
public class SecureContentPolicy implements ContentPolicy {
    private final PolicyFactory allowed = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    public String onWrite(String input) {
        return allowed.sanitize(input);
    }
}

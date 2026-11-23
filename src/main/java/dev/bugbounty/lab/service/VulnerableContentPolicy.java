package dev.bugbounty.lab.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("vulnerable")
public class VulnerableContentPolicy implements ContentPolicy {
    @Override
    public String onWrite(String input) {
        // intentionally vulnerable: content is stored unchanged in the local lab profile
        return input;
    }
}

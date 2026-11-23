package dev.bugbounty.lab.config;

import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LabMode {
    private final boolean vulnerable;

    public LabMode(Environment environment) {
        this.vulnerable = Arrays.asList(environment.getActiveProfiles()).contains("vulnerable");
        if (vulnerable && Arrays.asList(environment.getActiveProfiles()).contains("secure")) {
            throw new IllegalStateException("Choose exactly one lab profile");
        }
        if (!vulnerable && !Arrays.asList(environment.getActiveProfiles()).contains("secure")) {
            throw new IllegalStateException("Set SPRING_PROFILES_ACTIVE to secure or vulnerable");
        }
        if (vulnerable && !"127.0.0.1".equals(environment.getProperty("server.address"))) {
            throw new IllegalStateException("Vulnerable mode must bind to 127.0.0.1");
        }
    }

    public boolean vulnerable() {
        return vulnerable;
    }

    public String name() {
        return vulnerable ? "vulnerable" : "secure";
    }
}

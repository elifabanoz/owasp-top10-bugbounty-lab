package dev.bugbounty.lab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import dev.bugbounty.lab.repository.ProgramRepository;
import dev.bugbounty.lab.repository.ReportRepository;
import dev.bugbounty.lab.repository.UserRepository;
import dev.bugbounty.lab.security.PasswordService;

@Component
public class DemoSeed implements CommandLineRunner {
    private final boolean enabled;
    private final UserRepository users;
    private final PasswordService passwords;
    private final ProgramRepository programs;
    private final ReportRepository reports;

    public DemoSeed(@Value("${lab.seed:false}") boolean enabled, UserRepository users,
                    PasswordService passwords, ProgramRepository programs, ReportRepository reports) {
        this.enabled = enabled;
        this.users = users;
        this.passwords = passwords;
        this.programs = programs;
        this.reports = reports;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || !users.isEmpty()) return;
        users.create("admin", "admin@example.test", passwords.hash("admin-demo-123"), "ADMIN");
        users.create("triager", "triager@example.test", passwords.hash("triager-demo-123"), "TRIAGER");
        users.create("alice", "alice@example.test", passwords.hash("alice-demo-123"), "RESEARCHER");
        users.create("bob", "bob@example.test", passwords.hash("bob-demo-123"), "RESEARCHER");
        long programId = programs.create("Demo Program", "Local training data only");
        long aliceId = users.byUsername("alice").orElseThrow().id();
        long bobId = users.byUsername("bob").orElseThrow().id();
        reports.create(programId, aliceId, "Alice report", "Sample description", "MEDIUM");
        reports.create(programId, bobId, "Bob report", "Private sample", "LOW");
    }
}

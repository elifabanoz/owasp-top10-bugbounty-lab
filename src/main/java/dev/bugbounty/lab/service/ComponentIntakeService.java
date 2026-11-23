package dev.bugbounty.lab.service;

import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.ApprovedComponent;
import dev.bugbounty.lab.repository.LabExerciseRepository;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ComponentIntakeService {
    public record Manifest(String name, String version, String source, String sha256, String content) {}

    private static final String TRUSTED_SOURCE = "https://registry.example.test/demo-parser";
    private final LabMode mode;
    private final LabExerciseRepository repository;
    private final Manifest trusted;

    public ComponentIntakeService(LabMode mode, LabExerciseRepository repository) {
        this.mode = mode;
        this.repository = repository;
        try (InputStream input = new ClassPathResource("lab-fixtures/demo-parser-2.0.0.txt")
                .getInputStream()) {
            byte[] fixture = input.readAllBytes();
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fixture));
            trusted = new Manifest("demo-parser", "2.0.0", TRUSTED_SOURCE, digest,
                    new String(fixture, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Local component fixture is unavailable", error);
        }
    }

    public Manifest trustedFixture() {
        return trusted;
    }

    public void approve(Manifest candidate) {
        if (!mode.vulnerable()) {
            String actual;
            try {
                actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(candidate.content().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException error) {
                throw new IllegalStateException("SHA-256 is unavailable", error);
            }
            if (!trusted.equals(candidate) || !actual.equals(candidate.sha256())) {
                throw new LabConflictException("Component content or provenance does not match the trusted fixture");
            }
        }
        repository.approveComponent(candidate.name(), candidate.version(), candidate.source(), candidate.sha256());
    }

    public List<ApprovedComponent> approved() {
        return repository.components();
    }
}

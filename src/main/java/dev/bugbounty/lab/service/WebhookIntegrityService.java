package dev.bugbounty.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.IntegrationEvent;
import dev.bugbounty.lab.repository.LabExerciseRepository;
import dev.bugbounty.lab.repository.ReportRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIntegrityService {
    public record Decision(long reportId, String decision) {}

    private final LabMode mode;
    private final ReportRepository reports;
    private final LabExerciseRepository repository;
    private final ObjectMapper json;
    private final byte[] secret;

    public WebhookIntegrityService(LabMode mode, ReportRepository reports, LabExerciseRepository repository,
                                   ObjectMapper json, @Value("${lab.webhook-secret:}") String secret) {
        this.mode = mode;
        this.reports = reports;
        this.repository = repository;
        this.json = json;
        if (!mode.vulnerable() && secret.length() < 32) {
            throw new IllegalStateException("Secure profile requires a 32-character webhook secret");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void receive(String payload, String signature) {
        if (payload == null || payload.length() > 512) {
            throw new InvalidRequestException("Webhook payload is limited to 512 characters");
        }
        if (!mode.vulnerable() && !validSignature(payload, signature)) {
            throw new LabUnauthorizedException("Invalid webhook signature");
        }
        Decision event;
        try {
            event = json.readValue(payload, Decision.class);
        } catch (Exception error) {
            throw new InvalidRequestException("Invalid webhook payload");
        }
        if (event == null || event.reportId() <= 0 || !"TRIAGED".equals(event.decision())) {
            throw new InvalidRequestException("Invalid webhook decision");
        }
        var report = reports.byId(event.reportId()).orElseThrow(ResourceNotFoundException::new);
        if (!"NEW".equals(report.status())) {
            throw new LabConflictException("Only a new report can receive a triage decision");
        }
        repository.recordIntegration(event.reportId(), event.decision());
        repository.updateReportStatus(event.reportId(), "TRIAGED");
    }

    public List<IntegrationEvent> received() {
        return repository.integrations();
    }

    private boolean validSignature(String payload, String signature) {
        if (signature == null || !signature.matches("[0-9a-fA-F]{64}")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature));
        } catch (Exception error) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", error);
        }
    }
}

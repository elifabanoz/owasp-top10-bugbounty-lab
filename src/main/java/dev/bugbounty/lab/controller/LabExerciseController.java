package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.ApprovedComponent;
import dev.bugbounty.lab.model.IntegrationEvent;
import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.SecurityEvent;
import dev.bugbounty.lab.model.UserSummary;
import dev.bugbounty.lab.service.AdminService;
import dev.bugbounty.lab.service.ComponentIntakeService;
import dev.bugbounty.lab.service.ReportWorkflowService;
import dev.bugbounty.lab.service.ResourceNotFoundException;
import dev.bugbounty.lab.service.WebhookIntegrityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LabExerciseController {
    public record ComponentRequest(@NotBlank @Size(max = 80) String name,
                                   @NotBlank @Size(max = 40) String version,
                                   @NotBlank @Size(max = 255) String source,
                                   @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String sha256,
                                   @NotBlank @Size(max = 2048) String content) {}

    private final LabMode mode;
    private final AdminService admin;
    private final ComponentIntakeService components;
    private final ReportWorkflowService workflow;
    private final WebhookIntegrityService webhook;

    public LabExerciseController(LabMode mode, AdminService admin, ComponentIntakeService components,
                                 ReportWorkflowService workflow, WebhookIntegrityService webhook) {
        this.mode = mode;
        this.admin = admin;
        this.components = components;
        this.workflow = workflow;
        this.webhook = webhook;
    }

    @GetMapping("/api/lab/debug/users")
    public List<UserSummary> exposedDebugUsers() {
        if (!mode.vulnerable()) throw new ResourceNotFoundException();
        return admin.users();
    }

    @GetMapping("/api/admin/lab/component-fixture")
    public ComponentIntakeService.Manifest trustedComponent() {
        return components.trustedFixture();
    }

    @GetMapping("/api/admin/lab/components")
    public List<ApprovedComponent> approvedComponents() {
        return components.approved();
    }

    @PostMapping("/api/admin/lab/components")
    @ResponseStatus(HttpStatus.CREATED)
    public void approveComponent(@Valid @RequestBody ComponentRequest request) {
        components.approve(new ComponentIntakeService.Manifest(
                request.name(), request.version(), request.source(), request.sha256().toLowerCase(),
                request.content()));
    }

    @PostMapping("/api/admin/lab/reports/{id}/triage")
    public Report triage(@PathVariable long id) {
        return workflow.triage(id);
    }

    @PostMapping("/api/admin/lab/reports/{id}/close")
    public Report close(@PathVariable long id,
                        @RequestParam(defaultValue = "false") boolean simulateReviewFailure) {
        return workflow.close(id, simulateReviewFailure);
    }

    @PostMapping("/api/lab/webhook/triage")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void webhook(@RequestBody String payload,
                        @RequestHeader(value = "X-Lab-Signature", required = false) String signature) {
        webhook.receive(payload, signature);
    }

    @GetMapping("/api/admin/lab/integration-events")
    public List<IntegrationEvent> integrationEvents() {
        return webhook.received();
    }

    @GetMapping("/api/admin/lab/security-events")
    public List<SecurityEvent> securityEvents() {
        return admin.securityEvents();
    }
}

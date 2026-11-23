package dev.bugbounty.lab.controller;

import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReportController {
    public record NewReport(@NotNull Long programId, @NotBlank @Size(max = 200) String title,
                            @NotBlank @Size(max = 10000) String description,
                            @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL") String severity) {}
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports/search")
    public List<Report> search(@RequestParam(defaultValue = "") String q, Authentication authentication) {
        return reports.search(q, actor(authentication));
    }

    @GetMapping("/reports/{id}")
    public Report detail(@PathVariable long id, Authentication authentication) {
        return reports.detail(id, actor(authentication));
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody NewReport request, Authentication authentication) {
        reports.create(request.programId(), actor(authentication), request.title(), request.description(), request.severity());
    }

    private Actor actor(Authentication authentication) {
        return (Actor) authentication.getPrincipal();
    }
}

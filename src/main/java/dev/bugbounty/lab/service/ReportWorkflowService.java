package dev.bugbounty.lab.service;

import dev.bugbounty.lab.config.LabMode;
import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.repository.LabExerciseRepository;
import dev.bugbounty.lab.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportWorkflowService {
    private final ReportRepository reports;
    private final LabExerciseRepository exercises;
    private final LabMode mode;
    private final ReviewGateway reviewGateway;

    public ReportWorkflowService(ReportRepository reports, LabExerciseRepository exercises, LabMode mode,
                                 ReviewGateway reviewGateway) {
        this.reports = reports;
        this.exercises = exercises;
        this.mode = mode;
        this.reviewGateway = reviewGateway;
    }

    @Transactional
    public Report triage(long id) {
        Report report = reports.byId(id).orElseThrow(ResourceNotFoundException::new);
        if (!"NEW".equals(report.status())) {
            throw new LabConflictException("Only a new report can be triaged");
        }
        exercises.updateReportStatus(id, "TRIAGED");
        return reports.byId(id).orElseThrow();
    }

    @Transactional
    public Report close(long id, boolean reviewUnavailable) {
        Report report = reports.byId(id).orElseThrow(ResourceNotFoundException::new);
        if ("CLOSED".equals(report.status())) {
            throw new LabConflictException("Report is already closed");
        }
        if (!mode.vulnerable() && !"TRIAGED".equals(report.status())) {
            throw new LabConflictException("Report must be triaged before closing");
        }
        try {
            reviewGateway.verify(reviewUnavailable);
        } catch (LabUnavailableException error) {
            if (!mode.vulnerable()) throw error;
        }
        // Vulnerable profile skips the workflow rule and fails open on a review outage.
        exercises.updateReportStatus(id, "CLOSED");
        return reports.byId(id).orElseThrow();
    }
}

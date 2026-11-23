package dev.bugbounty.lab.service;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;
import dev.bugbounty.lab.repository.ReportRepository;
import dev.bugbounty.lab.repository.ReportSearch;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportRepository reports;
    private final ReportSearch search;
    private final ReportAccessPolicy access;
    private final ContentPolicy content;

    public ReportService(ReportRepository reports, ReportSearch search,
                         ReportAccessPolicy access, ContentPolicy content) {
        this.reports = reports;
        this.search = search;
        this.access = access;
        this.content = content;
    }

    public Report detail(long id, Actor actor) {
        var report = reports.byId(id).orElseThrow(ResourceNotFoundException::new);
        access.requireRead(report, actor);
        return report;
    }

    public List<Report> search(String query, Actor actor) {
        if (query == null || query.length() > 120) {
            throw new InvalidRequestException("Search is limited to 120 characters");
        }
        return search.search(query, actor);
    }

    public void create(long programId, Actor actor, String title, String description, String severity) {
        reports.create(programId, actor.id(), title, content.onWrite(description), severity);
    }
}

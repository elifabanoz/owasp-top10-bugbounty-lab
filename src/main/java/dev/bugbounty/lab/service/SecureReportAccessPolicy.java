package dev.bugbounty.lab.service;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("secure")
public class SecureReportAccessPolicy implements ReportAccessPolicy {
    @Override
    public void requireRead(Report report, Actor actor) {
        if ("RESEARCHER".equals(actor.role()) && report.authorId() != actor.id()) {
            throw new ResourceNotFoundException();
        }
    }
}

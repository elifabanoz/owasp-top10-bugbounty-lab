package dev.bugbounty.lab.service;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("vulnerable")
public class VulnerableReportAccessPolicy implements ReportAccessPolicy {
    @Override
    public void requireRead(Report report, Actor actor) {
        // intentionally vulnerable: ownership is not checked in the local lab profile
    }
}

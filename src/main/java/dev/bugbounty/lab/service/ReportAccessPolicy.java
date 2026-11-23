package dev.bugbounty.lab.service;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;

public interface ReportAccessPolicy {
    void requireRead(Report report, Actor actor);
}

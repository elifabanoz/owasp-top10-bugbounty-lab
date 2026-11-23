package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;
import java.util.List;

public interface ReportSearch {
    List<Report> search(String query, Actor actor);
}

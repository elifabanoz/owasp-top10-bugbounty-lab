package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("vulnerable")
public class VulnerableReportSearch implements ReportSearch {
    private final JdbcTemplate jdbc;

    public VulnerableReportSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Report> search(String query, Actor actor) {
        // intentionally vulnerable: user input is interpolated into SQL in the local lab profile
        String sql = "SELECT * FROM report WHERE title ILIKE '%" + query + "%' ORDER BY id";
        return jdbc.query(sql, ReportRepository.MAPPER);
    }
}

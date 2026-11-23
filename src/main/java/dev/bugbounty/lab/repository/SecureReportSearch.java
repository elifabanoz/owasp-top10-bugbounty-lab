package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.Report;
import dev.bugbounty.lab.model.Actor;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("secure")
public class SecureReportSearch implements ReportSearch {
    private final JdbcTemplate jdbc;

    public SecureReportSearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Report> search(String query, Actor actor) {
        String scope = "RESEARCHER".equals(actor.role()) ? " AND author_id = ?" : "";
        String sql = "SELECT * FROM report WHERE title ILIKE ?" + scope + " ORDER BY id";
        String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_") + "%";
        return "RESEARCHER".equals(actor.role())
                ? jdbc.query(sql, ReportRepository.MAPPER, pattern, actor.id())
                : jdbc.query(sql, ReportRepository.MAPPER, pattern);
    }
}

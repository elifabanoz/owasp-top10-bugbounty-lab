package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.Report;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {
    static final RowMapper<Report> MAPPER = (rs, row) -> new Report(
            rs.getLong("id"), rs.getLong("program_id"), rs.getLong("author_id"),
            rs.getString("title"), rs.getString("description"), rs.getString("severity"),
            rs.getString("status"), rs.getObject("created_at", java.time.OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Report> byId(long id) {
        return jdbc.query("SELECT * FROM report WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public void create(long programId, long authorId, String title, String description, String severity) {
        jdbc.update("INSERT INTO report(program_id, author_id, title, description, severity, status) VALUES (?, ?, ?, ?, ?, 'NEW')",
                programId, authorId, title, description, severity);
    }
}

package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.Comment;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository {
    private static final RowMapper<Comment> MAPPER = (rs, row) -> new Comment(
            rs.getLong("id"), rs.getLong("report_id"), rs.getLong("author_id"),
            rs.getString("body"), rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public CommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Comment> forReport(long reportId) {
        return jdbc.query("SELECT * FROM report_comment WHERE report_id = ? ORDER BY id", MAPPER, reportId);
    }

    public void create(long reportId, long authorId, String body) {
        jdbc.update("INSERT INTO report_comment(report_id, author_id, body) VALUES (?, ?, ?)",
                reportId, authorId, body);
    }
}

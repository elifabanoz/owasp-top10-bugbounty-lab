package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.ApprovedComponent;
import dev.bugbounty.lab.model.IntegrationEvent;
import dev.bugbounty.lab.model.SecurityEvent;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LabExerciseRepository {
    private final JdbcTemplate jdbc;

    public LabExerciseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void approveComponent(String name, String version, String source, String sha256) {
        jdbc.update("INSERT INTO approved_component(name, version, source, sha256) VALUES (?, ?, ?, ?)",
                name, version, source, sha256);
    }

    public List<ApprovedComponent> components() {
        return jdbc.query("SELECT * FROM approved_component ORDER BY id", (rs, row) ->
                new ApprovedComponent(rs.getLong("id"), rs.getString("name"),
                        rs.getString("version"), rs.getString("source"), rs.getString("sha256")));
    }

    public void recordIntegration(long reportId, String decision) {
        jdbc.update("INSERT INTO integration_event(report_id, decision) VALUES (?, ?)", reportId, decision);
    }

    public List<IntegrationEvent> integrations() {
        return jdbc.query("SELECT * FROM integration_event ORDER BY id", (rs, row) ->
                new IntegrationEvent(rs.getLong("id"), rs.getLong("report_id"), rs.getString("decision")));
    }

    public void recordSecurity(String kind, long actorId, long targetId) {
        jdbc.update("INSERT INTO security_event(kind, actor_id, target_id) VALUES (?, ?, ?)",
                kind, actorId, targetId);
    }

    public List<SecurityEvent> securityEvents() {
        return jdbc.query("SELECT * FROM security_event ORDER BY id", (rs, row) ->
                new SecurityEvent(rs.getLong("id"), rs.getString("kind"),
                        rs.getLong("actor_id"), rs.getLong("target_id")));
    }

    public void updateReportStatus(long reportId, String status) {
        jdbc.update("UPDATE report SET status = ? WHERE id = ?", status, reportId);
    }
}

package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.Program;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProgramRepository {
    private final JdbcTemplate jdbc;

    public ProgramRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Program> all() {
        return jdbc.query("SELECT id, name, description FROM program ORDER BY id", (rs, row) ->
                new Program(rs.getLong("id"), rs.getString("name"), rs.getString("description")));
    }

    public long create(String name, String description) {
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO program(name, description) VALUES (?, ?)", new String[] {"id"});
            statement.setString(1, name);
            statement.setString(2, description);
            return statement;
        }, key);
        return key.getKey().longValue();
    }
}

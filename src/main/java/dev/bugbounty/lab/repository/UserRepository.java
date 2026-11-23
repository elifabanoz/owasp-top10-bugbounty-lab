package dev.bugbounty.lab.repository;

import dev.bugbounty.lab.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private static final RowMapper<User> MAPPER = (rs, row) -> new User(
            rs.getLong("id"), rs.getString("username"), rs.getString("email"),
            rs.getString("password_hash"), rs.getString("role"));

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> byUsername(String username) {
        return jdbc.query("SELECT * FROM app_user WHERE username = ?", MAPPER, username).stream().findFirst();
    }

    public Optional<User> byId(long id) {
        return jdbc.query("SELECT * FROM app_user WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<User> all() {
        return jdbc.query("SELECT * FROM app_user ORDER BY id", MAPPER);
    }

    public boolean isEmpty() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class) == 0;
    }

    public void changeRole(long id, String role) {
        jdbc.update("UPDATE app_user SET role = ? WHERE id = ?", role, id);
    }

    public void create(String username, String email, String hash, String role) {
        jdbc.update("INSERT INTO app_user(username, email, password_hash, role) VALUES (?, ?, ?, ?)",
                username, email, hash, role);
    }
}

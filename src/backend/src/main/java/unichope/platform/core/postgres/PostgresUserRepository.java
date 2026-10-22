package unichope.platform.core.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.user.Admin;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.model.user.Student;
import unichope.platform.core.model.user.Tutor;
import unichope.platform.core.model.user.User;
import unichope.platform.core.repository.UserRepository;

public final class PostgresUserRepository implements UserRepository {
    private static final String SELECT = "SELECT id, name, email, role, active FROM users";
    private final JdbcClient jdbc;

    public PostgresUserRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE id=:id").param("id", id).query(PostgresUserRepository::map).optional();
    }

    @Override
    public List<User> findAll() {
        return jdbc.sql(SELECT + " ORDER BY id").query(PostgresUserRepository::map).list();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String canonical = email.strip().toLowerCase(Locale.ROOT);
        Optional<User> exact = jdbc.sql(SELECT + " WHERE email=:email").param("email", canonical)
                .query(PostgresUserRepository::map).optional();
        if (exact.isPresent()) return exact;
        return jdbc.sql(SELECT + " WHERE lower(btrim(email))=:email")
                .param("email", canonical).query(PostgresUserRepository::map).optional()
                .filter(user -> user.email().strip().toLowerCase(Locale.ROOT).equals(canonical));
    }

    static User map(ResultSet row, int index) throws SQLException {
        UUID id = row.getObject("id", UUID.class);
        String name = row.getString("name");
        String email = row.getString("email");
        boolean active = row.getBoolean("active");
        return switch (Role.valueOf(row.getString("role"))) {
            case STUDENT -> new Student(id, name, email, active);
            case TUTOR -> new Tutor(id, name, email, active);
            case ADMIN -> new Admin(id, name, email, active);
        };
    }
}

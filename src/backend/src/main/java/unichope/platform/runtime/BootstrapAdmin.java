package unichope.platform.runtime;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.auth.Argon2PasswordHasher;

/** Run explicitly through Gradle bootstrapAdmin; never an HTTP endpoint or startup seed. */
public final class BootstrapAdmin {
  private BootstrapAdmin() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 0)
      throw new IllegalArgumentException(
          "Use environment configuration, not command-line credentials");
    String jdbcUrl = required("UNICHOPE_JDBC_URL");
    if (!jdbcUrl.startsWith("jdbc:postgresql:"))
      throw new IllegalArgumentException("PostgreSQL configuration is required");
    var source =
        new DriverManagerDataSource(
            jdbcUrl, required("UNICHOPE_JDBC_USER"), required("UNICHOPE_JDBC_PASSWORD"));
    String name = required("UNICHOPE_ADMIN_NAME").strip();
    String email =
        unichope.platform.core.model.Validation.email(
            required("UNICHOPE_ADMIN_EMAIL").strip().toLowerCase(java.util.Locale.ROOT));
    String password = required("UNICHOPE_ADMIN_PASSWORD");
    if (name.codePointCount(0, name.length()) > 100
        || name.isBlank()
        || name.codePoints().anyMatch(Character::isISOControl)
        || password.codePointCount(0, password.length()) < 8
        || password.codePointCount(0, password.length()) > 128)
      throw new IllegalArgumentException("Invalid initial account input");
    String encoded = new Argon2PasswordHasher().hash(password);
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    create(source, name, email, encoded);
    System.out.println("Initial Admin created");
  }

  static void create(javax.sql.DataSource source, String name, String email, String encoded)
      throws Exception {
    try (Connection connection = source.getConnection()) {
      connection.setAutoCommit(false);
      try {
        try (var lock = connection.createStatement()) {
          lock.execute("SET LOCAL lock_timeout='5s'");
          lock.execute("SET LOCAL statement_timeout='10s'");
          lock.execute("SELECT pg_advisory_xact_lock(32272610)");
        }
        try (var query = connection.createStatement();
            var rows =
                query.executeQuery(
                    "SELECT EXISTS(SELECT 1 FROM bootstrap_state) OR EXISTS(SELECT 1 FROM users"
                        + " WHERE role='ADMIN')")) {
          rows.next();
          if (rows.getBoolean(1)) throw new IllegalStateException("Initial Admin already exists");
        }
        try (var insert =
            connection.prepareStatement(
                "INSERT INTO users(id,name,email,role,active,password_hash,session_generation)"
                    + " VALUES (?, ?, ?, 'ADMIN', true, ?, 0)")) {
          insert.setObject(1, java.util.UUID.randomUUID());
          insert.setString(2, name);
          insert.setString(3, email);
          insert.setString(4, encoded);
          insert.executeUpdate();
        }
        try (var marker = connection.createStatement()) {
          marker.executeUpdate("INSERT INTO bootstrap_state(singleton) VALUES(true)");
        }
        connection.commit();
      } catch (Exception failure) {
        connection.rollback();
        throw new IllegalStateException("Initial Admin setup failed; no account was committed");
      }
    }
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null) throw new IllegalArgumentException("Missing " + name);
    return value;
  }
}

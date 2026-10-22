package unichope.platform.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.web.ApiFailure;

@Tag("postgres")
class PostgresAiModelSettingsPgTest {
  private static final Instant T0 = Instant.parse("2026-10-23T03:00:00Z");
  private DriverManagerDataSource source;
  private String schema;
  private JdbcClient db;
  private PostgresAiModelSettings settings;

  @BeforeEach
  void prepare() throws Exception {
    String url = System.getProperty("core.test.jdbcUrl");
    assertNotNull(url, "Explicit disposable core.test.jdbcUrl is required");
    source = new DriverManagerDataSource(url, System.getProperty("core.test.user", ""), System.getProperty("core.test.password", ""));
    schema = "ai_model_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection c = source.getConnection(); var s = c.createStatement()) { s.execute("CREATE SCHEMA " + schema); }
    Properties properties = new Properties();
    properties.setProperty("currentSchema", schema);
    source.setConnectionProperties(properties);
    Flyway.configure().dataSource(source).defaultSchema(schema).schemas(schema).locations("classpath:db/migration").load().migrate();
    db = JdbcClient.create(source);
    settings = new PostgresAiModelSettings(source, Clock.fixed(T0, ZoneOffset.UTC));
  }

  @AfterEach
  void cleanup() throws Exception {
    try (Connection c = source.getConnection(); var s = c.createStatement()) { s.execute("DROP SCHEMA " + schema + " CASCADE"); }
  }

  @Test
  void defaultsToQwen35WithNoUpdateTime() {
    var current = settings.current();
    assertEquals("qwen3.5:9b", current.model().id());
    assertNull(current.updatedAt());
  }

  @Test
  void adminSelectionPersistsAndCanChangeAgain() {
    UUID admin = UUID.randomUUID();
    var chosen = settings.select("gemma4:26b", admin);
    assertEquals("gemma4:26b", chosen.model().id());
    assertEquals(T0, chosen.updatedAt());
    assertEquals("gemma4:26b", new PostgresAiModelSettings(source, Clock.systemUTC()).current().model().id());
    settings.select("llama3.1:8b", admin);
    assertEquals("llama3.1:8b", settings.current().model().id());
    assertEquals(1L, db.sql("SELECT count(*) FROM ai_settings").query(Long.class).single());
    assertEquals(admin, db.sql("SELECT updated_by FROM ai_settings").query(UUID.class).single());
  }

  @Test
  void unknownModelIsRejectedAndLeavesSettingUnchanged() {
    settings.select("gemma4:26b", UUID.randomUUID());
    for (String bad : new String[] {"gpt-4", "", "QWEN3.5:9B", "qwen3-vl:32b"}) {
      var failure = assertThrows(ApiFailure.class, () -> settings.select(bad, UUID.randomUUID()));
      assertEquals("VALIDATION_FAILED", failure.code());
    }
    assertEquals("gemma4:26b", settings.current().model().id());
  }

  @Test
  void storedModelNoLongerInCatalogReadsAsDefault() {
    db.sql("INSERT INTO ai_settings (singleton, model, updated_at) VALUES (true, 'retired-model', now())").update();
    assertEquals("qwen3.5:9b", settings.current().model().id());
  }
}

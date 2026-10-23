package unichope.platform.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.UnichopeApplication;
import unichope.platform.ai.AdmittedAiGateway;
import unichope.platform.ai.AiGateway;
import unichope.platform.ai.AiProviderBudget;
import unichope.platform.ai.AiProviderKey;
import unichope.platform.ai.AiTaskPolicy;
import unichope.platform.ai.postgres.PostgresAiAdmissionService;

@Tag("postgres")
class LocalSocRuntimePgTest {
  private DriverManagerDataSource base;
  private ConfigurableApplicationContext context;
  private JdbcClient database;
  private String schema;
  private String jdbcUrl;
  private String user;
  private String password;

  @BeforeEach
  void startLocalSocProfile() throws Exception {
    jdbcUrl = System.getProperty("core.test.jdbcUrl");
    if (jdbcUrl == null) throw new IllegalStateException("Explicit disposable PostgreSQL required");
    user = System.getProperty("core.test.user", "");
    password = System.getProperty("core.test.password", "");
    base = new DriverManagerDataSource(jdbcUrl, user, password);
    schema = "local_soc_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection connection = base.getConnection(); var statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
    }

    String schemaUrl = schemaUrl(schema);
    context =
        new SpringApplicationBuilder(UnichopeApplication.class)
            .web(WebApplicationType.SERVLET)
            .profiles("app-api", "local", "local-soc")
            .properties(
                Map.ofEntries(
                    Map.entry("server.port", "0"),
                    Map.entry("unichope.jdbc.url", schemaUrl),
                    Map.entry("unichope.jdbc.user", user),
                    Map.entry("unichope.jdbc.password", password),
                    Map.entry("SOC_LLM_API_KEY_TUTOR", "synthetic-key"),
                    Map.entry("SOC_LLM_MODEL", "qwen3.6:35b"),
                    Map.entry("SOC_LLM_RPM", "2"),
                    Map.entry("SOC_LLM_TPM", "2048")))
            .run();
    database = JdbcClient.create(context.getBean(javax.sql.DataSource.class));
  }

  @AfterEach
  void stopLocalSocProfile() throws Exception {
    if (context != null) context.close();
    if (schema != null) {
      try (Connection connection = base.getConnection(); var statement = connection.createStatement()) {
        statement.execute("DROP SCHEMA " + schema + " CASCADE");
      }
    }
  }

  @Test
  void localSocProfileUsesSharedMigrationAndWiresAdmittedTutorProvider() {
    assertEquals("qwen3.6:35b", context.getEnvironment().getProperty("SOC_LLM_MODEL"));
    assertEquals("1024", context.getEnvironment()
        .getProperty("unichope.ai.tutor.note-draft.max-output-tokens"));
    context.getBean(unichope.tutor.TutorNoteDraftService.class);
    assertInstanceOf(AdmittedAiGateway.class, context.getBean(AiGateway.class));
    assertEquals(1, database.sql("SELECT count(*) FROM ai_quota_state").query(Integer.class).single());
    assertEquals(0, database.sql("SELECT count(*) FROM ai_attempt").query(Integer.class).single());
    assertEquals(8, database.sql("SELECT count(*) FROM flyway_schema_history WHERE success")
        .query(Integer.class).single());
  }

  @Test
  void localSocTutorRouterDoesNotUseAnotherRolesProviderKey() {
    AiGateway gateway = context.getBean(AiGateway.class);
    assertEquals(
        AiGateway.FailureCode.UNAVAILABLE,
        assertThrows(
                AiGateway.Failure.class,
                () -> gateway.generate(new AiGateway.Request(
                    UUID.randomUUID(), "STU-AGENDA-1", "fixed", "test", 32)))
            .code());
    assertEquals(
        AiGateway.FailureCode.UNAVAILABLE,
        assertThrows(
                AiGateway.Failure.class,
                () -> gateway.generate(new AiGateway.Request(
                    UUID.randomUUID(), "ADM-REPORT-1", "fixed", "test", 32)))
            .code());
    assertEquals(0, database.sql("SELECT count(*) FROM ai_attempt").query(Integer.class).single());
  }

  @Test
  void v6MigrationKeepsExistingV5AttemptsInSuccessfulDraftQuota() throws Exception {
    String upgradeSchema = "ai_v6_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    try {
      try (Connection connection = base.getConnection(); var statement = connection.createStatement()) {
        statement.execute("CREATE SCHEMA " + upgradeSchema);
      }
      var source = new DriverManagerDataSource(schemaUrl(upgradeSchema), user, password);
      Flyway.configure()
          .dataSource(source)
          .locations("classpath:db/migration")
          .target(MigrationVersion.fromVersion("5"))
          .load()
          .migrate();

      UUID actorId = UUID.randomUUID();
      JdbcClient upgradeDb = JdbcClient.create(source);
      upgradeDb.sql("""
          INSERT INTO ai_attempt(request_id,actor_id,task,provider_key,admitted_at,reserved_tokens)
          VALUES (:request,:actor,'tutor.note-draft.v1','TUTOR',:admitted,32)
          """)
          .param("request", UUID.randomUUID())
          .param("actor", actorId)
          .param("admitted", OffsetDateTime.now(ZoneOffset.UTC))
          .update();

      Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();

      assertEquals(1, upgradeDb.sql("SELECT count(*) FROM ai_attempt WHERE actor_quota_counted")
          .query(Integer.class).single());
      var admissions = new PostgresAiAdmissionService(source, Clock.systemUTC(), Map.of(
          AiProviderKey.TUTOR,
          new AiProviderBudget(2, 2048, Duration.ofMinutes(1), 1)));
      var policy = new AiTaskPolicy("tutor.note-draft.v1", AiProviderKey.TUTOR, 1,
          Duration.ofMinutes(5), 1, 500, 1024, 4096, Duration.ofSeconds(20));
      var failure = assertThrows(
          AiGateway.Failure.class,
          () -> admissions.admit(
              new unichope.platform.ai.AiAdmissionService.Attempt(
                  actorId, policy.task(), policy.providerKey(), 32), policy));
      assertEquals(AiGateway.FailureCode.RATE_LIMITED, failure.code());
    } finally {
      try (Connection connection = base.getConnection(); var statement = connection.createStatement()) {
        statement.execute("DROP SCHEMA IF EXISTS " + upgradeSchema + " CASCADE");
      }
    }
  }

  private String schemaUrl(String targetSchema) {
    return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + targetSchema;
  }
}

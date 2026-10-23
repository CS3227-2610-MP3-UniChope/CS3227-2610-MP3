package unichope.platform.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.web.ApiFailure;

/** Real PostgreSQL admission checks using the runtime migrations in an isolated schema. */
@Tag("postgres")
class PostgresAiAdmissionPgTest {
  private static final Instant T0 = Instant.parse("2026-10-23T02:00:00Z");
  private final AtomicReference<Instant> now = new AtomicReference<>(T0);
  private final Clock clock = new Clock() {
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now.get(); }
  };
  private DriverManagerDataSource source;
  private String schema;
  private JdbcClient db;

  @BeforeEach
  void prepare() throws Exception {
    String url = System.getProperty("core.test.jdbcUrl");
    assertNotNull(url, "Explicit disposable core.test.jdbcUrl is required");
    source = new DriverManagerDataSource(url, System.getProperty("core.test.user", ""), System.getProperty("core.test.password", ""));
    schema = "ai_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection c = source.getConnection(); var s = c.createStatement()) { s.execute("CREATE SCHEMA " + schema); }
    Properties properties = new Properties();
    properties.setProperty("currentSchema", schema);
    source.setConnectionProperties(properties);
    Flyway.configure().dataSource(source).defaultSchema(schema).schemas(schema).locations("classpath:db/migration").load().migrate();
    db = JdbcClient.create(source);
  }

  @AfterEach
  void cleanup() throws Exception {
    try (Connection c = source.getConnection(); var s = c.createStatement()) { s.execute("DROP SCHEMA " + schema + " CASCADE"); }
  }

  private PostgresAiAdmission admission(AiQuotaPolicy policy) {
    return new PostgresAiAdmission(source, clock, policy);
  }

  private static AiQuotaPolicy policy(int rpm, long daily, long monthly) {
    return new AiQuotaPolicy(rpm, daily, monthly, 3, Duration.ofMinutes(5), Duration.ofSeconds(60));
  }

  private static String code(Runnable action) {
    return assertThrows(ApiFailure.class, action::run).code();
  }

  @Test
  void oneInFlightPerActorUntilCompletedOrLeaseExpires() {
    var a = admission(AiQuotaPolicy.defaults());
    UUID actor = UUID.randomUUID();
    UUID first = a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100);
    assertEquals("AI_BUSY", code(() -> a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100)));
    a.complete(first, 40L);
    a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100);
    now.set(T0.plusSeconds(61));
    a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100);
    assertEquals(40L, db.sql("SELECT charged_micros FROM ai_attempts WHERE id=:id").param("id", first).query(Long.class).single());
  }

  @Test
  void perActorWindowAllowsThreeIn5MinutesCountingFailures() {
    var a = admission(AiQuotaPolicy.defaults());
    UUID actor = UUID.randomUUID();
    for (int i = 0; i < 3; i++) {
      a.complete(a.admit(AiKeySlot.TUTOR, actor, AiTask.TUTOR_NOTE_DRAFT, 100), null);
      now.set(now.get().plusSeconds(1));
    }
    assertEquals("AI_RATE_LIMITED", code(() -> a.admit(AiKeySlot.TUTOR, actor, AiTask.TUTOR_NOTE_DRAFT, 100)));
    now.set(T0.plus(Duration.ofMinutes(5)).plusSeconds(1));
    a.admit(AiKeySlot.TUTOR, actor, AiTask.TUTOR_NOTE_DRAFT, 100);
  }

  @Test
  void requestsPerMinuteAreCountedPerKeyIndependently() {
    var a = admission(policy(2, 70_000_000, 700_000_000));
    for (int i = 0; i < 2; i++) a.complete(a.admit(AiKeySlot.ADMIN, UUID.randomUUID(), AiTask.ADMIN_ACTIVITY_REPORT, 10), null);
    assertEquals("AI_RATE_LIMITED", code(() -> a.admit(AiKeySlot.ADMIN, UUID.randomUUID(), AiTask.ADMIN_ACTIVITY_REPORT, 10)));
    a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 10);
    now.set(T0.plusSeconds(61));
    a.admit(AiKeySlot.ADMIN, UUID.randomUUID(), AiTask.ADMIN_ACTIVITY_REPORT, 10);
  }

  @Test
  void dailyAndMonthlyBudgetsBlockOnlyTheExhaustedKey() {
    var a = admission(policy(1000, 1000, 1500));
    a.complete(a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 900), null);
    assertEquals("AI_UNAVAILABLE", code(() -> a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 200)));
    a.admit(AiKeySlot.TUTOR, UUID.randomUUID(), AiTask.TUTOR_NOTE_DRAFT, 200);
    now.set(T0.plus(Duration.ofHours(25)));
    a.complete(a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 500), null);
    now.set(T0.plus(Duration.ofHours(50)));
    assertEquals("AI_UNAVAILABLE", code(() -> a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 200)),
        "monthly total 1400 + 200 exceeds 1500");
  }

  @Test
  void concurrentAdmissionsForOneKeyCannotExceedTheMinuteLimit() throws Exception {
    var a = admission(policy(5, 70_000_000, 700_000_000));
    var pool = Executors.newFixedThreadPool(8);
    try {
      List<Callable<Boolean>> calls = new ArrayList<>();
      for (int i = 0; i < 12; i++) calls.add(() -> {
        try { a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 10); return true; }
        catch (ApiFailure rejected) { return false; }
      });
      long admitted = 0;
      for (var f : pool.invokeAll(calls)) if (f.get()) admitted++;
      assertEquals(5, admitted);
      assertEquals(5L, db.sql("SELECT count(*) FROM ai_attempts").query(Long.class).single());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void storesMetadataOnlyAndUnusableStoreFailsClosed() {
    var a = admission(AiQuotaPolicy.defaults());
    a.admit(AiKeySlot.ADMIN, UUID.randomUUID(), AiTask.ADMIN_ACTIVITY_REPORT, 10);
    var columns = db.sql("SELECT column_name FROM information_schema.columns WHERE table_schema=:s AND table_name='ai_attempts'")
        .param("s", schema).query(String.class).list();
    assertFalse(columns.stream().anyMatch(c -> c.contains("prompt") || c.contains("input") || c.contains("output") || c.contains("key_value")));
    db.sql("DROP TABLE ai_attempts").update();
    var failure = assertThrows(AiGateway.Failure.class, () -> a.admit(AiKeySlot.ADMIN, UUID.randomUUID(), AiTask.ADMIN_ACTIVITY_REPORT, 10));
    assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
  }

  // AG1: public retry information is required by the existing HTTP integration contract.
  // Reflection keeps the pre-implementation baseline executable without binding a new API.
  private static long retrySeconds(ApiFailure failure) {
    try {
      Object value = failure.getClass().getMethod("retryAfterSeconds").invoke(failure);
      if (value instanceof java.util.Optional<?> optional) value = optional.orElse(null);
      assertInstanceOf(Number.class, value, "Admission rejection must expose positive retry seconds");
      return ((Number) value).longValue();
    } catch (ReflectiveOperationException missing) {
      return fail("Admission rejection does not expose retryAfterSeconds", missing);
    }
  }

  @Test
  void agendaBusyRetryCeilsRemainingLeaseAndAdmissionWorksAtBoundary() {
    var a = admission(AiQuotaPolicy.defaults()); UUID actor = UUID.randomUUID();
    a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100);
    now.set(T0.plusMillis(1));
    var busy = assertThrows(ApiFailure.class, () -> a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100));
    assertEquals("AI_BUSY", busy.code()); assertEquals(60, retrySeconds(busy));
    now.set(T0.plusSeconds(60)); a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100);
  }

  @Test
  void agendaActorRateLimitRetryUsesEarliestWindowExpiry() {
    var a = admission(AiQuotaPolicy.defaults()); UUID actor = UUID.randomUUID();
    for (int i = 0; i < 3; i++) {
      now.set(T0.plusSeconds(i)); a.complete(a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100), null);
    }
    now.set(T0.plusMillis(2001));
    var limited = assertThrows(ApiFailure.class, () -> a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100));
    assertEquals("AI_RATE_LIMITED", limited.code()); assertEquals(298, retrySeconds(limited));
    now.set(T0.plusSeconds(300)); a.admit(AiKeySlot.STUDENT, actor, AiTask.STUDENT_AGENDA, 100);
  }

  @Test
  void agendaKeyMinuteLimitRetryUsesEarliestWindowExpiry() {
    var a = admission(policy(1, 70_000_000, 700_000_000));
    a.complete(a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 100), null);
    now.set(T0.plusMillis(1));
    var limited = assertThrows(ApiFailure.class, () -> a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 100));
    assertEquals("AI_RATE_LIMITED", limited.code()); assertEquals(60, retrySeconds(limited));
    now.set(T0.plusSeconds(60)); a.admit(AiKeySlot.STUDENT, UUID.randomUUID(), AiTask.STUDENT_AGENDA, 100);
  }
}

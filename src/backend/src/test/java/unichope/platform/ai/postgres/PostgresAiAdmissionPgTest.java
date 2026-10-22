package unichope.platform.ai.postgres;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.ai.AiAdmissionService;
import unichope.platform.ai.AiGateway;
import unichope.platform.ai.AiProviderBudget;
import unichope.platform.ai.AiProviderKey;
import unichope.platform.ai.AiTaskPolicy;

@Tag("postgres")
class PostgresAiAdmissionPgTest {
  private static final Instant START = Instant.parse("2026-10-22T12:00:00Z");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private DriverManagerDataSource source;
  private JdbcClient db;
  private String schema;
  private MovingClock clock;
  private PostgresAiAdmissionService admissions;
  private AiProviderBudget budget;

  @BeforeEach
  void prepare() throws Exception {
    String url = System.getProperty("core.test.jdbcUrl");
    assertNotNull(url, "Explicit disposable core.test.jdbcUrl is required");
    source = new DriverManagerDataSource(url,
        System.getProperty("core.test.user", ""), System.getProperty("core.test.password", ""));
    schema = "ai_admission_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
    }
    Properties properties = new Properties();
    properties.setProperty("currentSchema", schema);
    source.setConnectionProperties(properties);
    Flyway.configure().dataSource(source).defaultSchema(schema).schemas(schema)
        .locations("classpath:db/ai-review").load().migrate();
    db = JdbcClient.create(source);
    clock = new MovingClock();
    budget = new AiProviderBudget(20, 100, Duration.ofMinutes(5), 10);
    admissions = service(budget);
  }

  @AfterEach
  void cleanup() throws Exception {
    if (schema != null) {
      try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
        statement.execute("DROP SCHEMA " + schema + " CASCADE");
      }
    }
  }

  @Test
  void actorWindowAndInFlightLeaseAreAtomicAndExpire() {
    AiTaskPolicy policy = policy("tutor.note-draft.v1", 3, 1, 100);
    AiAdmissionService.Reservation first = admit(ACTOR, policy, 20);
    AiGateway.Failure busy = assertThrows(AiGateway.Failure.class, () -> admit(ACTOR, policy, 20));
    assertEquals(AiGateway.FailureCode.BUSY, busy.code());
    assertTrue(busy.retryAfter().isPresent());
    admissions.complete(first);

    admissions.complete(admit(ACTOR, policy, 20));
    admissions.complete(admit(ACTOR, policy, 20));
    AiGateway.Failure limited = assertThrows(AiGateway.Failure.class, () -> admit(ACTOR, policy, 20));
    assertEquals(AiGateway.FailureCode.RATE_LIMITED, limited.code());
    assertTrue(limited.retryAfter().orElseThrow().compareTo(Duration.ZERO) > 0);

    clock.advance(Duration.ofMinutes(5));
    assertNotNull(admit(ACTOR, policy, 20));
    assertEquals(1, db.sql("SELECT count(*) FROM ai_attempt WHERE actor_id=:actor AND in_flight_until IS NOT NULL")
        .param("actor", ACTOR).query(Long.class).single());
  }

  @Test
  void pruningKeepsAnActiveLeaseWhenItsWindowHasExpired() {
    AiProviderBudget shortBudget = new AiProviderBudget(20, 100, Duration.ofSeconds(1), 1);
    PostgresAiAdmissionService shortWindow = service(shortBudget);
    AiTaskPolicy firstPolicy = new AiTaskPolicy("tutor.short-window.v1", AiProviderKey.TUTOR, 5,
        Duration.ofSeconds(1), 1, 500, 100, 4096, Duration.ofSeconds(20));
    AiTaskPolicy secondPolicy = new AiTaskPolicy("tutor.other-task.v1", AiProviderKey.TUTOR, 5,
        Duration.ofSeconds(1), 1, 500, 100, 4096, Duration.ofSeconds(20));
    shortWindow.admit(attempt(ACTOR, firstPolicy, 20), firstPolicy);
    clock.advance(Duration.ofSeconds(2));

    AiGateway.Failure failure = assertThrows(AiGateway.Failure.class,
        () -> shortWindow.admit(attempt(OTHER, secondPolicy, 20), secondPolicy));
    assertEquals(AiGateway.FailureCode.BUSY, failure.code());
    assertEquals(1, db.sql("SELECT count(*) FROM ai_attempt WHERE in_flight_until > :now")
        .param("now", clock.instant().atOffset(ZoneOffset.UTC)).query(Long.class).single());
  }

  @Test
  void providerKeyTokenReservationIsSharedAcrossActorsAndTasks() {
    AiTaskPolicy tutor = policy("tutor.note-draft.v1", 10, 1, 100);
    AiTaskPolicy admin = new AiTaskPolicy("admin.report.v1", AiProviderKey.TUTOR, 10,
        Duration.ofMinutes(5), 1, 500, 100, 4096, Duration.ofSeconds(20));
    AiAdmissionService.Reservation first = admit(ACTOR, tutor, 70);
    AiGateway.Failure limit = assertThrows(AiGateway.Failure.class, () -> admit(OTHER, admin, 40));
    assertEquals(AiGateway.FailureCode.RATE_LIMITED, limit.code());
    assertEquals(1, db.sql("SELECT count(*) FROM ai_attempt").query(Long.class).single());

    admissions.complete(first);
    clock.advance(Duration.ofMinutes(5));
    assertNotNull(admit(OTHER, admin, 40));
  }

  @Test
  void providerKeyRequestBudgetIsSharedAcrossTasks() {
    AiProviderBudget requestBudget = new AiProviderBudget(2, 500, Duration.ofMinutes(5), 10);
    PostgresAiAdmissionService requestAdmissions = service(requestBudget);
    AiTaskPolicy tutor = policy("tutor.note-draft.v1", 10, 1, 100);
    AiTaskPolicy admin = new AiTaskPolicy("admin.report.v1", AiProviderKey.TUTOR, 10,
        Duration.ofMinutes(5), 1, 500, 100, 4096, Duration.ofSeconds(20));
    requestAdmissions.complete(requestAdmissions.admit(attempt(ACTOR, tutor, 20), tutor));
    requestAdmissions.complete(requestAdmissions.admit(attempt(OTHER, admin, 20), admin));

    AiGateway.Failure failure = assertThrows(AiGateway.Failure.class,
        () -> requestAdmissions.admit(attempt(UUID.randomUUID(), tutor, 20), tutor));
    assertEquals(AiGateway.FailureCode.RATE_LIMITED, failure.code());
    assertEquals(2, db.sql("SELECT count(*) FROM ai_attempt").query(Long.class).single());
  }

  @Test
  void providerKeyInFlightLimitIsSharedAcrossActors() {
    PostgresAiAdmissionService onePerKey = service(new AiProviderBudget(20, 500,
        Duration.ofMinutes(5), 1));
    AiTaskPolicy policy = policy("tutor.note-draft.v1", 10, 1, 100);
    onePerKey.admit(attempt(ACTOR, policy, 20), policy);

    AiGateway.Failure failure = assertThrows(AiGateway.Failure.class,
        () -> onePerKey.admit(attempt(OTHER, policy, 20), policy));
    assertEquals(AiGateway.FailureCode.BUSY, failure.code());
    assertTrue(failure.retryAfter().isPresent());
  }

  @Test
  void roleKeyBudgetsRemainIsolated() {
    PostgresAiAdmissionService perRole = new PostgresAiAdmissionService(source, clock, Map.of(
        AiProviderKey.TUTOR, new AiProviderBudget(1, 100, Duration.ofMinutes(5), 10),
        AiProviderKey.STUDENT, new AiProviderBudget(1, 100, Duration.ofMinutes(5), 10)));
    AiTaskPolicy tutor = policy("tutor.note-draft.v1", 5, 1, 100);
    AiTaskPolicy student = new AiTaskPolicy("student.agenda.v1", AiProviderKey.STUDENT, 5,
        Duration.ofMinutes(5), 1, 500, 100, 4096, Duration.ofSeconds(20));
    perRole.complete(perRole.admit(attempt(ACTOR, tutor, 20), tutor));

    AiGateway.Failure tutorLimit = assertThrows(AiGateway.Failure.class,
        () -> perRole.admit(attempt(OTHER, tutor, 20), tutor));
    assertEquals(AiGateway.FailureCode.RATE_LIMITED, tutorLimit.code());
    assertNotNull(perRole.admit(attempt(OTHER, student, 20), student));
    assertEquals(2, db.sql("SELECT count(*) FROM ai_attempt").query(Long.class).single());
  }

  @Test
  void missingProviderBudgetFailsBeforeCreatingReservation() {
    PostgresAiAdmissionService studentOnly = new PostgresAiAdmissionService(source, clock, Map.of(
        AiProviderKey.STUDENT, budget));
    AiTaskPolicy tutor = policy("tutor.note-draft.v1", 3, 1, 100);

    AiGateway.Failure failure = assertThrows(AiGateway.Failure.class,
        () -> studentOnly.admit(attempt(ACTOR, tutor, 20), tutor));

    assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
    assertEquals(0, db.sql("SELECT count(*) FROM ai_attempt").query(Long.class).single());
  }

  @Test
  void serializedAdmissionsAllowOnlyOneConcurrentRequestForActor() throws Exception {
    AiTaskPolicy policy = policy("tutor.note-draft.v1", 10, 1, 100);
    try (var executor = Executors.newFixedThreadPool(2)) {
      CyclicBarrier start = new CyclicBarrier(2);
      var calls = List.of(
          executor.submit(() -> concurrentAdmit(start, policy)),
          executor.submit(() -> concurrentAdmit(start, policy)));
      List<Object> outcomes = List.of(calls.get(0).get(10, TimeUnit.SECONDS), calls.get(1).get(10, TimeUnit.SECONDS));
      assertEquals(1, outcomes.stream().filter(AiAdmissionService.Reservation.class::isInstance).count());
      assertEquals(1, outcomes.stream().filter(AiGateway.Failure.class::isInstance).count());
      assertEquals(AiGateway.FailureCode.BUSY, outcomes.stream().filter(AiGateway.Failure.class::isInstance)
          .map(AiGateway.Failure.class::cast).findFirst().orElseThrow().code());
      assertEquals(1, db.sql("SELECT count(*) FROM ai_attempt").query(Long.class).single());
    }
  }

  @Test
  void unavailableSchemaFailsClosedWithSanitizedFailure() throws Exception {
    try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
      statement.execute("DROP TABLE ai_quota_state");
    }
    AiGateway.Failure failure = assertThrows(AiGateway.Failure.class,
        () -> admit(ACTOR, policy("tutor.note-draft.v1", 3, 1, 100), 100));
    assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
    assertFalse(failure.getMessage().contains("ai_quota_state"));
  }

  @Test
  void quotaLockWaitIsBoundedAndFailsClosed() throws Exception {
    try (Connection holder = source.getConnection()) {
      holder.setAutoCommit(false);
      try (var statement = holder.createStatement();
          var result = statement.executeQuery("SELECT id FROM ai_quota_state WHERE id=1 FOR UPDATE")) {
        assertTrue(result.next());
        long started = System.nanoTime();
        AiGateway.Failure failure = assertThrows(AiGateway.Failure.class,
            () -> admit(ACTOR, policy("tutor.note-draft.v1", 3, 1, 100), 20));
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
        assertTrue(elapsedMillis < 2_000, "Admission lock wait exceeded its bounded timeout");
        assertFalse(failure.getMessage().contains("lock_timeout"));
        assertEquals(0, db.sql("SELECT count(*) FROM ai_attempt").query(Long.class).single());
      } finally {
        holder.rollback();
      }
    }
  }

  private Object concurrentAdmit(CyclicBarrier start, AiTaskPolicy policy) {
    try {
      start.await(5, TimeUnit.SECONDS);
      return admit(ACTOR, policy, 20);
    } catch (AiGateway.Failure failure) {
      return failure;
    } catch (Exception failure) {
      throw new RuntimeException(failure);
    }
  }

  private AiAdmissionService.Reservation admit(UUID actor, AiTaskPolicy policy, int tokens) {
    return admissions.admit(attempt(actor, policy, tokens), policy);
  }

  private AiAdmissionService.Attempt attempt(UUID actor, AiTaskPolicy policy, int tokens) {
    return new AiAdmissionService.Attempt(actor, policy.task(), policy.providerKey(), tokens);
  }

  private AiTaskPolicy policy(String task, int limit, int inFlight, int maxTokens) {
    return new AiTaskPolicy(task, AiProviderKey.TUTOR, limit, Duration.ofMinutes(5), inFlight,
        500, maxTokens, 4096, Duration.ofSeconds(20));
  }

  private PostgresAiAdmissionService service(AiProviderBudget providerBudget) {
    return new PostgresAiAdmissionService(source, clock, Map.of(AiProviderKey.TUTOR, providerBudget));
  }

  private static final class MovingClock extends Clock {
    private final AtomicReference<Instant> now = new AtomicReference<>(START);
    void advance(Duration duration) { now.updateAndGet(value -> value.plus(duration)); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now.get(); }
  }
}

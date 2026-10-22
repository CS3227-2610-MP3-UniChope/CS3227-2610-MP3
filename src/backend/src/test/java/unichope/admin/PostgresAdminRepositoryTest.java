package unichope.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.core.AdminContext;
import unichope.platform.web.ApiFailure;

@Tag("postgres")
class PostgresAdminRepositoryTest {
  DriverManagerDataSource source;
  JdbcClient db;
  String schema;
  PostgresAdminRepository repo;
  UUID admin = UUID.randomUUID(),
      tutor = UUID.randomUUID(),
      student = UUID.randomUUID(),
      module = UUID.randomUUID();
  Instant now = Instant.parse("2026-10-22T00:00:00Z");

  @BeforeEach
  void setup() throws Exception {
    String url = System.getProperty("core.test.jdbcUrl");
    assertNotNull(url);
    source =
        new DriverManagerDataSource(
            url,
            System.getProperty("core.test.user", ""),
            System.getProperty("core.test.password", ""));
    schema = "admin_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection c = source.getConnection();
        var s = c.createStatement()) {
      s.execute("CREATE SCHEMA " + schema);
    }
    var props = new Properties();
    props.setProperty("currentSchema", schema);
    props.setProperty("ApplicationName", schema);
    source.setConnectionProperties(props);
    Flyway.configure()
        .dataSource(source)
        .defaultSchema(schema)
        .schemas(schema)
        .locations("classpath:db/core-review", "classpath:db/auth-review")
        .load()
        .migrate();
    db = JdbcClient.create(source);
    for (var pair : Map.of(admin, "ADMIN", tutor, "TUTOR", student, "STUDENT").entrySet())
      db.sql(
              "INSERT INTO users(id,name,email,role,active,password_hash)"
                  + " VALUES(:id,'Synthetic',:email,:role,true,'synthetic')")
          .param("id", pair.getKey())
          .param("email", pair.getKey() + "@local")
          .param("role", pair.getValue())
          .update();
    db.sql("INSERT INTO modules VALUES(:id,'CS3227','Synthetic',true)")
        .param("id", module)
        .update();
    repo = new PostgresAdminRepository(source, Clock.fixed(now, ZoneOffset.UTC));
  }

  @AfterEach
  void cleanup() throws Exception {
    if (schema != null)
      try (Connection c = source.getConnection();
          var s = c.createStatement()) {
        s.execute("DROP SCHEMA " + schema + " CASCADE");
      }
  }

  AdminContext actor() {
    return new AdminContext(admin, 0);
  }

  UUID booking(String status, Instant start) {
    UUID slot = UUID.randomUUID(), booking = UUID.randomUUID();
    db.sql("INSERT INTO slots VALUES(:id,:t,:m,:s,:e,:status)")
        .param("id", slot)
        .param("t", tutor)
        .param("m", module)
        .param("s", start.atOffset(ZoneOffset.UTC))
        .param("e", start.plusSeconds(1800).atOffset(ZoneOffset.UTC))
        .param("status", status.equals("ACTIVE") ? "BOOKED" : status)
        .update();
    db.sql("INSERT INTO bookings VALUES(:id,:s,:slot,:at,:status)")
        .param("id", booking)
        .param("s", student)
        .param("slot", slot)
        .param("at", now.atOffset(ZoneOffset.UTC))
        .param("status", status)
        .update();
    return booking;
  }

  @Test
  void statisticsCountEveryRecordAndDateIsSingapore() {
    booking("COMPLETED", now);
    booking("COMPLETED", now.plusSeconds(2000));
    booking("CANCELLED", now.minusSeconds(1));
    booking("ACTIVE", now.plusSeconds(4000));
    var s = repo.statistics();
    assertEquals(4, s.total());
    assertEquals("50.00", s.completionRate().toString());
    assertEquals("25.00", s.cancellationRate().toString());
    assertEquals(4, s.byTutor().getFirst().count());
    assertEquals(4, repo.bookings(null, null, LocalDate.of(2026, 10, 22), null).size());
    assertEquals(2, repo.bookings(null, null, null, "COMPLETED").size());
    assertEquals(0, repo.bookings(null, null, LocalDate.of(2026, 10, 21), null).size());
  }

  @Test
  void managementGuardsAndIdempotency() {
    repo.assign(actor(), tutor, module, true);
    repo.assign(actor(), tutor, module, true);
    assertEquals(1, repo.assignments(tutor, module).size());
    booking("ACTIVE", now.minusSeconds(7200));
    assertEquals(
        409,
        assertThrows(ApiFailure.class, () -> repo.assign(actor(), tutor, module, false)).status());
    assertEquals(
        409, assertThrows(ApiFailure.class, () -> repo.deactivateModule(actor(), module)).status());
    assertTrue(repo.modules(true).getFirst().active());
    db.sql("DELETE FROM bookings").update();
    db.sql("DELETE FROM slots").update();
    repo.assign(actor(), tutor, module, false);
    repo.assign(actor(), tutor, module, false);
    assertTrue(repo.assignments(tutor, module).isEmpty());
    assertFalse(repo.deactivateModule(actor(), module).active());
    assertFalse(repo.deactivateModule(actor(), module).active());
  }

  @Test
  void authorizationAndUniquenessAreRecheckedInTransaction() {
    assertEquals(
        403,
        assertThrows(
                ApiFailure.class,
                () -> repo.saveModule(new AdminContext(student, 0), null, "CS1", "Name"))
            .status());
    assertEquals(
        403,
        assertThrows(
                ApiFailure.class,
                () -> repo.saveModule(new AdminContext(admin, 1), null, "CS1", "Name"))
            .status());
    assertEquals(
        409,
        assertThrows(ApiFailure.class, () -> repo.saveModule(actor(), null, "CS3227", "Duplicate"))
            .status());
    var m = repo.saveModule(actor(), module, "CSNEW", "Edited");
    assertEquals(module, m.id());
    assertEquals("CSNEW", m.code());
    assertEquals(1, repo.modules(null).size());
    assertEquals(3, repo.users(null, null).size());
  }

  @Test
  void moduleDeactivationAndSlotCreationUseSameParentLock() throws Exception {
    repo.assign(actor(), tutor, module, true);
    var lifecycle =
        new unichope.platform.core.postgres.PostgresConsultationLifecycle(
            source, Clock.fixed(now, ZoneOffset.UTC));
    var result =
        race(
            () -> repo.deactivateModule(actor(), module),
            () ->
                lifecycle.createSlot(tutor, module, now.plusSeconds(3600), now.plusSeconds(7200)));
    assertInstanceOf(AdminViews.Module.class, result.getFirst());
    assertInstanceOf(unichope.platform.core.LifecycleFailure.class, result.get(1));
    assertEquals(0, db.sql("SELECT count(*) FROM slots").query(Long.class).single());
  }

  @Test
  void assignmentRemovalAndSlotCreationCannotBothSucceed() throws Exception {
    repo.assign(actor(), tutor, module, true);
    var lifecycle =
        new unichope.platform.core.postgres.PostgresConsultationLifecycle(
            source, Clock.fixed(now, ZoneOffset.UTC));
    var result =
        race(
            () -> {
              repo.assign(actor(), tutor, module, false);
              return "removed";
            },
            () ->
                lifecycle.createSlot(tutor, module, now.plusSeconds(3600), now.plusSeconds(7200)));
    assertEquals("removed", result.getFirst());
    assertInstanceOf(unichope.platform.core.LifecycleFailure.class, result.get(1));
    assertTrue(repo.assignments(tutor, module).isEmpty());
    assertEquals(0, db.sql("SELECT count(*) FROM slots").query(Long.class).single());
  }

  @Test
  void publishedSlotBlocksQueuedModuleDeactivation() throws Exception {
    repo.assign(actor(), tutor, module, true);
    var lifecycle =
        new unichope.platform.core.postgres.PostgresConsultationLifecycle(
            source, Clock.fixed(now, ZoneOffset.UTC));
    var result =
        race(
            () -> lifecycle.createSlot(tutor, module, now.plusSeconds(3600), now.plusSeconds(7200)),
            () -> repo.deactivateModule(actor(), module));
    assertInstanceOf(
        unichope.platform.core.model.consultation.ConsultationSlot.class, result.getFirst());
    assertInstanceOf(ApiFailure.class, result.get(1));
    assertEquals(409, ((ApiFailure) result.get(1)).status());
    assertTrue(repo.modules(true).getFirst().active());
  }

  @Test
  void publishedSlotBlocksQueuedAssignmentRemoval() throws Exception {
    repo.assign(actor(), tutor, module, true);
    var lifecycle =
        new unichope.platform.core.postgres.PostgresConsultationLifecycle(
            source, Clock.fixed(now, ZoneOffset.UTC));
    var result =
        race(
            () -> lifecycle.createSlot(tutor, module, now.plusSeconds(3600), now.plusSeconds(7200)),
            () -> {
              repo.assign(actor(), tutor, module, false);
              return "removed";
            });
    assertInstanceOf(
        unichope.platform.core.model.consultation.ConsultationSlot.class, result.getFirst());
    assertInstanceOf(ApiFailure.class, result.get(1));
    assertEquals(409, ((ApiFailure) result.get(1)).status());
    assertEquals(1, repo.assignments(tutor, module).size());
  }

  List<Object> race(java.util.concurrent.Callable<?> first, java.util.concurrent.Callable<?> second)
      throws Exception {
    try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        Connection holder = source.getConnection()) {
      holder.setAutoCommit(false);
      try (var statement =
          holder.prepareStatement("SELECT id FROM modules WHERE id=? FOR UPDATE")) {
        statement.setObject(1, module);
        statement.executeQuery().close();
      }
      var a = workers.submit(() -> outcome(first));
      awaitBlocked(1);
      var b = workers.submit(() -> outcome(second));
      awaitBlocked(2);
      holder.commit();
      return List.of(
          a.get(15, java.util.concurrent.TimeUnit.SECONDS),
          b.get(15, java.util.concurrent.TimeUnit.SECONDS));
    }
  }

  Object outcome(java.util.concurrent.Callable<?> task) throws Exception {
    try {
      return task.call();
    } catch (ApiFailure | unichope.platform.core.LifecycleFailure failure) {
      return failure;
    }
  }

  void awaitBlocked(int count) {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(4);
    while (System.nanoTime() < deadline) {
      long waiting =
          db.sql(
                  "SELECT count(*) FROM pg_stat_activity WHERE application_name=:app AND"
                      + " cardinality(pg_blocking_pids(pid))>0")
              .param("app", schema)
              .query(Long.class)
              .single();
      if (waiting >= count) return;
      java.util.concurrent.locks.LockSupport.parkNanos(
          java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(5));
    }
    fail("Expected independently blocked database callers: " + count);
  }
}

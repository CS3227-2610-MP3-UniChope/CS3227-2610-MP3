package unichope.admin;

import java.math.*;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.core.AdminContext;
import unichope.platform.web.ApiFailure;

/** Management transactions share core parent locks with consultation mutations. */
public final class PostgresAdminRepository implements AdminRepository {
  private final JdbcClient db;
  private final TransactionTemplate tx;
  private final Clock clock;

  public PostgresAdminRepository(DataSource source, Clock clock) {
    db = JdbcClient.create(source);
    tx = new TransactionTemplate(new DataSourceTransactionManager(source));
    tx.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    tx.setPropagationBehavior(
        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  public List<AdminViews.UserSummary> users(String role, Boolean active) {
    return safe(
        () ->
            db.sql(
                    "SELECT id,name,email,role,active FROM users WHERE (:role IS NULL OR"
                        + " role=:role) AND (:active IS NULL OR active=:active) ORDER BY"
                        + " lower(btrim(email)),id")
                .param("role", role, java.sql.Types.VARCHAR)
                .param("active", active, java.sql.Types.BOOLEAN)
                .query(
                    (r, i) ->
                        new AdminViews.UserSummary(
                            r.getObject("id", UUID.class),
                            r.getString("name"),
                            r.getString("email"),
                            r.getString("role"),
                            r.getBoolean("active")))
                .list());
  }

  public List<AdminViews.Module> modules(Boolean active) {
    return safe(
        () ->
            db.sql(
                    "SELECT id,code,name,active FROM modules WHERE (:active IS NULL OR"
                        + " active=:active) ORDER BY code,id")
                .param("active", active, java.sql.Types.BOOLEAN)
                .query(
                    (r, i) ->
                        new AdminViews.Module(
                            r.getObject("id", UUID.class),
                            r.getString("code"),
                            r.getString("name"),
                            r.getBoolean("active")))
                .list());
  }

  public AdminViews.Module saveModule(AdminContext a, UUID id, String code, String name) {
    return mutate(
        () -> {
          lockUsers(a.actorId());
          admin(a);
          UUID target = id == null ? UUID.randomUUID() : id;
          if (id != null) module(id);
          if (id == null)
            db.sql("INSERT INTO modules(id,code,name,active) VALUES(:id,:code,:name,true)")
                .param("id", target)
                .param("code", code)
                .param("name", name)
                .update();
          else
            db.sql("UPDATE modules SET code=:code,name=:name WHERE id=:id")
                .param("id", id)
                .param("code", code)
                .param("name", name)
                .update();
          return module(target);
        });
  }

  public AdminViews.Module deactivateModule(AdminContext a, UUID id) {
    return mutate(
        () -> {
          lockUsers(a.actorId());
          admin(a);
          var m = module(id);
          if (m.active()) {
            if (blocked(id, null)) throw error(409, "INVALID_STATE");
            db.sql("UPDATE modules SET active=false WHERE id=:id").param("id", id).update();
          }
          return module(id);
        });
  }

  public List<AdminViews.Assignment> assignments(UUID tutor, UUID module) {
    return safe(
        () ->
            db.sql(
                    "SELECT tutor_id,module_id FROM tutor_modules WHERE (CAST(:tutor AS uuid) IS"
                        + " NULL OR tutor_id=:tutor) AND (CAST(:module AS uuid) IS NULL OR"
                        + " module_id=:module) ORDER BY module_id,tutor_id")
                .param("tutor", tutor, java.sql.Types.OTHER)
                .param("module", module, java.sql.Types.OTHER)
                .query(
                    (r, i) ->
                        new AdminViews.Assignment(
                            r.getObject(1, UUID.class), r.getObject(2, UUID.class)))
                .list());
  }

  public void assign(AdminContext a, UUID tutor, UUID module, boolean assigned) {
    mutate(
        () -> {
          lockUsers(a.actorId(), tutor);
          admin(a);
          var target =
              db.sql("SELECT role,active FROM users WHERE id=:id")
                  .param("id", tutor)
                  .query((r, i) -> Map.entry(r.getString(1), r.getBoolean(2)))
                  .optional()
                  .orElseThrow(() -> error(404, "NOT_FOUND"));
          var m = module(module);
          boolean exists =
              !db.sql(
                      "SELECT tutor_id FROM tutor_modules WHERE tutor_id=:t AND module_id=:m FOR"
                          + " UPDATE")
                  .param("t", tutor)
                  .param("m", module)
                  .query(UUID.class)
                  .list()
                  .isEmpty();
          if (assigned) {
            if (!target.getKey().equals("TUTOR") || !target.getValue() || !m.active())
              throw error(409, "INVALID_STATE");
            if (!exists)
              db.sql("INSERT INTO tutor_modules(tutor_id,module_id) VALUES(:t,:m)")
                  .param("t", tutor)
                  .param("m", module)
                  .update();
          } else if (exists) {
            if (blocked(module, tutor)) throw error(409, "INVALID_STATE");
            db.sql("DELETE FROM tutor_modules WHERE tutor_id=:t AND module_id=:m")
                .param("t", tutor)
                .param("m", module)
                .update();
          }
          return null;
        });
  }

  public List<AdminViews.BookingSummary> bookings(
      UUID module, UUID tutor, LocalDate date, String status) {
    return safe(
        () -> {
          var q =
              db.sql(
                      "SELECT b.id,b.student_id,u.name student_name,s.tutor_id,t.name"
                          + " tutor_name,s.module_id,m.code,s.start_time,s.end_time,b.status FROM"
                          + " bookings b JOIN slots s ON s.id=b.slot_id LEFT JOIN users u ON"
                          + " u.id=b.student_id LEFT JOIN users t ON t.id=s.tutor_id LEFT JOIN"
                          + " modules m ON m.id=s.module_id WHERE (CAST(:module AS uuid) IS NULL OR"
                          + " s.module_id=:module) AND (CAST(:tutor AS uuid) IS NULL OR"
                          + " s.tutor_id=:tutor) AND (:status IS NULL OR b.status=:status) AND"
                          + " (CAST(:start AS timestamptz) IS NULL OR s.start_time>=:start) AND"
                          + " (CAST(:finish AS timestamptz) IS NULL OR s.start_time<:finish) ORDER"
                          + " BY s.start_time,b.id")
                  .param("module", module, java.sql.Types.OTHER)
                  .param("tutor", tutor, java.sql.Types.OTHER)
                  .param("status", status, java.sql.Types.VARCHAR)
                  .param(
                      "start",
                      date == null
                          ? null
                          : date.atStartOfDay(ZoneId.of("Asia/Singapore")).toOffsetDateTime(),
                      java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                  .param(
                      "finish",
                      date == null
                          ? null
                          : date.plusDays(1)
                              .atStartOfDay(ZoneId.of("Asia/Singapore"))
                              .toOffsetDateTime(),
                      java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
          return q.query(
                  (r, i) ->
                      new AdminViews.BookingSummary(
                          r.getObject("id", UUID.class),
                          r.getObject("student_id", UUID.class),
                          r.getString("student_name"),
                          r.getObject("tutor_id", UUID.class),
                          r.getString("tutor_name"),
                          r.getObject("module_id", UUID.class),
                          r.getString("code"),
                          r.getObject("start_time", OffsetDateTime.class).toInstant(),
                          r.getObject("end_time", OffsetDateTime.class).toInstant(),
                          r.getString("status")))
              .list();
        });
  }

  public AdminViews.Statistics statistics() {
    return safe(
        () -> {
          var rows =
              db.sql(
                      "WITH facts AS (SELECT b.status,m.id module_id,m.code module_label,u.id"
                          + " tutor_id,u.name tutor_label FROM bookings b LEFT JOIN slots s ON"
                          + " s.id=b.slot_id LEFT JOIN modules m ON m.id=s.module_id LEFT JOIN"
                          + " users u ON u.id=s.tutor_id) SELECT 'total' kind,NULL::uuid id,''"
                          + " label,count(*) n,count(*) FILTER(WHERE status='COMPLETED')"
                          + " completed,count(*) FILTER(WHERE status='CANCELLED')"
                          + " cancelled,statement_timestamp() captured FROM facts UNION ALL SELECT"
                          + " 'module',module_id,coalesce(module_label,'Unknown'),count(*),0,0,statement_timestamp()"
                          + " FROM facts GROUP BY module_id,module_label UNION ALL SELECT"
                          + " 'tutor',tutor_id,coalesce(tutor_label,'Unknown'),count(*),0,0,statement_timestamp()"
                          + " FROM facts GROUP BY tutor_id,tutor_label ORDER BY kind,label,id")
                  .query(
                      (r, i) ->
                          new StatRow(
                              r.getString(1),
                              r.getObject(2, UUID.class),
                              r.getString(3),
                              r.getLong(4),
                              r.getLong(5),
                              r.getLong(6),
                              r.getObject(7, OffsetDateTime.class).toInstant()))
                  .list();
          var total = rows.stream().filter(r -> r.kind.equals("total")).findFirst().orElseThrow();
          return new AdminViews.Statistics(
              total.at,
              total.n,
              total.completed,
              total.cancelled,
              rate(total.completed, total.n),
              rate(total.cancelled, total.n),
              rows.stream()
                  .filter(r -> r.kind.equals("module"))
                  .map(r -> new AdminViews.Group(r.id, r.label, r.n))
                  .toList(),
              rows.stream()
                  .filter(r -> r.kind.equals("tutor"))
                  .map(r -> new AdminViews.Group(r.id, r.label, r.n))
                  .toList());
        });
  }

  private record StatRow(
      String kind, UUID id, String label, long n, long completed, long cancelled, Instant at) {}

  private static BigDecimal rate(long n, long total) {
    return total == 0
        ? BigDecimal.ZERO.setScale(2)
        : BigDecimal.valueOf(n)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  private AdminViews.Module module(UUID id) {
    return db.sql("SELECT id,code,name,active FROM modules WHERE id=:id FOR UPDATE")
        .param("id", id)
        .query(
            (r, i) ->
                new AdminViews.Module(
                    r.getObject(1, UUID.class), r.getString(2), r.getString(3), r.getBoolean(4)))
        .optional()
        .orElseThrow(() -> error(404, "NOT_FOUND"));
  }

  private boolean blocked(UUID module, UUID tutor) {
    return db.sql(
            "SELECT EXISTS(SELECT 1 FROM slots s WHERE s.module_id=:m AND (CAST(:t AS uuid) IS NULL"
                + " OR s.tutor_id=:t) AND ((s.status='AVAILABLE' AND s.start_time>:now) OR"
                + " EXISTS(SELECT 1 FROM bookings b WHERE b.slot_id=s.id AND b.status='ACTIVE')))")
        .param("m", module)
        .param("t", tutor, java.sql.Types.OTHER)
        .param("now", clock.instant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC))
        .query(Boolean.class)
        .single();
  }

  private void lockUsers(UUID... ids) {
    db.sql("SELECT id FROM users WHERE id IN(:ids) ORDER BY id FOR UPDATE")
        .param("ids", Arrays.stream(ids).distinct().toList())
        .query(UUID.class)
        .list();
  }

  private void admin(AdminContext a) {
    if (!db.sql(
            "SELECT EXISTS(SELECT 1 FROM users WHERE id=:id AND active AND role='ADMIN' AND"
                + " session_generation=:g)")
        .param("id", a.actorId())
        .param("g", a.expectedGeneration())
        .query(Boolean.class)
        .single()) throw error(403, "FORBIDDEN");
  }

  private <T> T mutate(Supplier<T> work) {
    return safe(
        () ->
            tx.execute(
                s -> {
                  db.sql("SET LOCAL lock_timeout='5s'").update();
                  db.sql("SET LOCAL statement_timeout='10s'").update();
                  return work.get();
                }));
  }

  private <T> T safe(Supplier<T> work) {
    try {
      return work.get();
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException e) {
      for (Throwable c = e; c != null; c = c.getCause())
        if (c instanceof SQLException s) {
          if ("23505".equals(s.getSQLState())) throw error(409, "DUPLICATE_RECORD");
          if (Set.of("55P03", "40P01", "40001", "57014").contains(s.getSQLState()))
            throw error(503, "TEMPORARILY_UNAVAILABLE");
        }
      throw error(500, "INTERNAL_ERROR");
    }
  }

  static ApiFailure error(int status, String code) {
    return new ApiFailure(status, code, "The request could not be completed");
  }
}

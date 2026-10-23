package unichope.platform.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.web.ApiFailure;

/**
 * Admission in one short READ COMMITTED transaction that locks the slot's key-state row, so
 * concurrent requests for the same key cannot both pass a limit. The provider call happens after
 * commit; reservations survive restarts and leases expire on their own.
 */
public final class PostgresAiAdmission implements AiAdmission {
  private static final Duration DAY = Duration.ofHours(24);
  private static final Duration MONTH = Duration.ofDays(31);
  private static final Duration RETENTION = Duration.ofDays(32);
  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;
  private final Clock clock;
  private final AiQuotaPolicy policy;

  public PostgresAiAdmission(DataSource source, Clock clock, AiQuotaPolicy policy) {
    this.jdbc = JdbcClient.create(source);
    this.clock = clock;
    this.policy = policy;
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public UUID admit(AiKeySlot slot, UUID actorId, AiTask task, long reservedMicros) {
    try {
      return transaction.execute(
          status -> {
            jdbc.sql("SET LOCAL lock_timeout='5s'").update();
            jdbc.sql("SET LOCAL statement_timeout='10s'").update();
            jdbc.sql("SELECT key_slot FROM ai_key_state WHERE key_slot=:slot FOR UPDATE")
                .param("slot", slot.name())
                .query(String.class)
                .single();
            Instant now = clock.instant();
            jdbc.sql("DELETE FROM ai_attempts WHERE key_slot=:slot AND admitted_at<:before")
                .param("slot", slot.name())
                .param("before", at(now.minus(RETENTION)))
                .update();

            Instant busyUntil =
                jdbc.sql(
                        "SELECT max(in_flight_until) FROM ai_attempts"
                            + " WHERE actor_id=:actor AND in_flight_until>:now")
                    .param("actor", actorId)
                    .param("now", at(now))
                    .query(OffsetDateTime.class)
                    .optional()
                    .map(OffsetDateTime::toInstant)
                    .orElse(null);
            if (busyUntil != null)
              throw new ApiFailure(409, "AI_BUSY", "A generation is already in progress", retrySeconds(now, busyUntil));

            long recentForActor =
                count(
                    "SELECT count(*) FROM ai_attempts WHERE actor_id=:id AND admitted_at>:since",
                    "id", actorId, now.minus(policy.perActorWindow()));
            if (recentForActor >= policy.perActorRequests())
              throw new ApiFailure(429, "AI_RATE_LIMITED", "AI generation limit reached. Try again later.",
                  windowRetry("actor_id", actorId, now, policy.perActorWindow()));

            long lastMinute =
                count(
                    "SELECT count(*) FROM ai_attempts WHERE key_slot=:id AND admitted_at>:since",
                    "id", slot.name(), now.minus(Duration.ofMinutes(1)));
            if (lastMinute >= policy.requestsPerMinute())
              throw new ApiFailure(429, "AI_RATE_LIMITED", "The AI service is busy. Try again shortly.",
                  windowRetry("key_slot", slot.name(), now, Duration.ofMinutes(1)));

            if (spent(slot, now.minus(DAY)) + reservedMicros > policy.dailyBudgetMicros()
                || spent(slot, now.minus(MONTH)) + reservedMicros > policy.monthlyBudgetMicros())
              throw new ApiFailure(503, "AI_UNAVAILABLE", "The AI budget is used up for now.");

            UUID id = UUID.randomUUID();
            jdbc.sql(
                    "INSERT INTO ai_attempts (id,key_slot,actor_id,task,admitted_at,reserved_micros,"
                        + "charged_micros,in_flight_until) VALUES (:id,:slot,:actor,:task,:at,:r,:r,:lease)")
                .param("id", id)
                .param("slot", slot.name())
                .param("actor", actorId)
                .param("task", task.id())
                .param("at", at(now))
                .param("r", reservedMicros)
                .param("lease", at(now.plus(policy.inFlightLease())))
                .update();
            return id;
          });
    } catch (DataAccessException e) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
  }

  @Override
  public void complete(UUID attemptId, Long actualMicros) {
    try {
      jdbc.sql(
              "UPDATE ai_attempts SET in_flight_until=NULL, completed_at=:now,"
                  + " charged_micros=COALESCE(:actual, reserved_micros) WHERE id=:id")
          .param("now", at(clock.instant()))
          .param("actual", actualMicros)
          .param("id", attemptId)
          .update();
    } catch (DataAccessException e) {
      // The lease expires by itself and the reservation stays charged; never fail the caller here.
    }
  }

  private long count(String sql, String name, Object value, Instant since) {
    return jdbc.sql(sql).param(name, value).param("since", at(since)).query(Long.class).single();
  }

  private long windowRetry(String column, Object value, Instant now, Duration window) {
    // Column names are internal constants; request data is always bound.
    Instant earliest = jdbc.sql("SELECT min(admitted_at) FROM ai_attempts WHERE " + column + "=:id AND admitted_at>:since")
        .param("id", value).param("since", at(now.minus(window)))
        .query(OffsetDateTime.class).single().toInstant();
    return retrySeconds(now, earliest.plus(window));
  }

  private static long retrySeconds(Instant now, Instant until) {
    Duration remaining = Duration.between(now, until);
    return Math.max(1, remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
  }

  private long spent(AiKeySlot slot, Instant since) {
    return jdbc.sql(
            "SELECT COALESCE(sum(charged_micros),0) FROM ai_attempts"
                + " WHERE key_slot=:slot AND admitted_at>:since")
        .param("slot", slot.name())
        .param("since", at(since))
        .query(Long.class)
        .single();
  }

  private static OffsetDateTime at(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}

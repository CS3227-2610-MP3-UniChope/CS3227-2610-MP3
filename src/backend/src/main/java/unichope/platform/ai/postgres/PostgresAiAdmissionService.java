package unichope.platform.ai.postgres;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.ai.AiAdmissionService;
import unichope.platform.ai.AiGateway;
import unichope.platform.ai.AiProviderBudget;
import unichope.platform.ai.AiProviderKey;
import unichope.platform.ai.AiTaskPolicy;

/** PostgreSQL-backed, fail-closed admission with a serialized quota transaction. */
public final class PostgresAiAdmissionService implements AiAdmissionService {
  private static final Duration ADMISSION_LOCK_TIMEOUT = Duration.ofMillis(500);
  private final JdbcClient jdbc;
  private final Clock clock;
  private final Map<AiProviderKey, AiProviderBudget> budgets;
  private final TransactionTemplate transaction;
  private final Duration retentionWindow;

  public PostgresAiAdmissionService(
      DataSource source, Clock clock, Map<AiProviderKey, AiProviderBudget> budgets) {
    this.jdbc = JdbcClient.create(Objects.requireNonNull(source));
    this.clock = Objects.requireNonNull(clock);
    this.budgets = Map.copyOf(budgets);
    if (this.budgets.isEmpty()) throw new IllegalArgumentException("At least one AI provider budget is required.");
    retentionWindow = this.budgets.values().stream().map(AiProviderBudget::rollingWindow)
        .max(Comparator.naturalOrder()).orElseThrow();
    DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
    transaction = new TransactionTemplate(manager);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public Reservation admit(Attempt attempt, AiTaskPolicy policy) {
    Objects.requireNonNull(attempt);
    Objects.requireNonNull(policy);
    AiProviderBudget budget = budgets.get(attempt.providerKey());
    if (budget == null || !attempt.task().equals(policy.task())
        || attempt.providerKey() != policy.providerKey()
        || attempt.reservedOutputTokens() > policy.maxOutputTokens()
        || attempt.reservedOutputTokens() > budget.reservedTokensPerWindow()) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
    try {
      Decision decision = transaction.execute(status -> admitLocked(attempt, policy, budget));
      if (decision == null) throw new IllegalStateException("AI admission returned no decision.");
      if (decision.failure() != null) {
        throw new AiGateway.Failure(decision.failure(), decision.retryAfter());
      }
      return decision.reservation();
    } catch (AiGateway.Failure known) {
      throw known;
    } catch (RuntimeException unavailable) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
  }

  private Decision admitLocked(Attempt attempt, AiTaskPolicy policy, AiProviderBudget budget) {
    jdbc.sql("SELECT set_config('lock_timeout', :timeout, true)")
        .param("timeout", ADMISSION_LOCK_TIMEOUT.toMillis() + "ms")
        .query(String.class).single();
    jdbc.sql("SELECT id FROM ai_quota_state WHERE id=1 FOR UPDATE").query(Integer.class).single();
    Instant now = clock.instant();
    Duration retention = policy.rollingWindow().compareTo(retentionWindow) > 0
        ? policy.rollingWindow() : retentionWindow;
    jdbc.sql("""
        DELETE FROM ai_attempt
        WHERE admitted_at < :cutoff AND (in_flight_until IS NULL OR in_flight_until <= :now)
        """).param("cutoff", now.minus(retention).atOffset(ZoneOffset.UTC))
        .param("now", now.atOffset(ZoneOffset.UTC)).update();
    jdbc.sql("UPDATE ai_attempt SET in_flight_until=NULL WHERE in_flight_until <= :now")
        .param("now", now.atOffset(ZoneOffset.UTC)).update();

    List<Instant> leases = jdbc.sql("""
        SELECT in_flight_until FROM ai_attempt
        WHERE actor_id=:actor AND task=:task AND in_flight_until > :now
        ORDER BY in_flight_until
        """).param("actor", attempt.actorId()).param("task", attempt.task())
        .param("now", now.atOffset(ZoneOffset.UTC))
        .query((rs, row) -> rs.getObject("in_flight_until", java.time.OffsetDateTime.class).toInstant()).list();
    if (leases.size() >= policy.maxInFlightPerActor()) {
      return Decision.denied(AiGateway.FailureCode.BUSY, retry(leases.get(0), now));
    }

    Instant actorCutoff = now.minus(policy.rollingWindow());
    List<AttemptRow> actorAttempts = jdbc.sql("""
        SELECT admitted_at,reserved_tokens FROM ai_attempt
        WHERE actor_id=:actor AND task=:task AND actor_quota_counted=true AND admitted_at > :cutoff
        """).param("actor", attempt.actorId()).param("task", attempt.task())
        .param("cutoff", actorCutoff.atOffset(ZoneOffset.UTC))
        .query((rs, row) -> new AttemptRow(
            rs.getObject("admitted_at", java.time.OffsetDateTime.class).toInstant(),
            rs.getInt("reserved_tokens"))).list();
    if (actorAttempts.size() >= policy.admissionsPerActor()) {
      return Decision.denied(AiGateway.FailureCode.RATE_LIMITED,
          budgetRetry(actorAttempts, policy.admissionsPerActor(), Long.MAX_VALUE, policy.rollingWindow(), now));
    }

    Instant keyCutoff = now.minus(budget.rollingWindow());
    List<AttemptRow> keyAttempts = attempts("provider_key=:key AND admitted_at > :cutoff",
        null, null, attempt.providerKey(), keyCutoff);
    if (keyAttempts.size() >= budget.requestsPerWindow()
        || sumTokens(keyAttempts) + attempt.reservedOutputTokens() > budget.reservedTokensPerWindow()) {
      return Decision.denied(AiGateway.FailureCode.RATE_LIMITED,
          budgetRetry(keyAttempts, budget.requestsPerWindow(),
              (long) budget.reservedTokensPerWindow() - attempt.reservedOutputTokens(), budget.rollingWindow(), now));
    }

    long inFlightForKey = jdbc.sql("""
        SELECT count(*) FROM ai_attempt WHERE provider_key=:key AND in_flight_until > :now
        """).param("key", attempt.providerKey().name()).param("now", now.atOffset(ZoneOffset.UTC))
        .query(Long.class).single();
    if (inFlightForKey >= budget.maxInFlight()) {
      Instant expires = jdbc.sql("""
          SELECT min(in_flight_until) FROM ai_attempt
          WHERE provider_key=:key AND in_flight_until > :now
          """).param("key", attempt.providerKey().name()).param("now", now.atOffset(ZoneOffset.UTC))
          .query((rs, row) -> rs.getObject(1, java.time.OffsetDateTime.class).toInstant()).single();
      return Decision.denied(AiGateway.FailureCode.BUSY, retry(expires, now));
    }

    UUID requestId = UUID.randomUUID();
    jdbc.sql("""
        INSERT INTO ai_attempt(request_id,actor_id,task,provider_key,admitted_at,reserved_tokens,in_flight_until)
        VALUES (:id,:actor,:task,:key,:admitted,:tokens,:lease)
        """).param("id", requestId).param("actor", attempt.actorId()).param("task", attempt.task())
        .param("key", attempt.providerKey().name()).param("admitted", now.atOffset(ZoneOffset.UTC))
        .param("tokens", attempt.reservedOutputTokens())
        .param("lease", now.plus(policy.deadline()).atOffset(ZoneOffset.UTC)).update();
    return Decision.admitted(new Reservation(requestId));
  }

  private List<AttemptRow> attempts(
      String predicate, UUID actorId, String task, AiProviderKey key, Instant cutoff) {
    var query = jdbc.sql("SELECT admitted_at,reserved_tokens FROM ai_attempt WHERE " + predicate);
    if (actorId != null) query = query.param("actor", actorId).param("task", task);
    return query.param("key", key.name()).param("cutoff", cutoff.atOffset(ZoneOffset.UTC))
        .query((rs, row) -> new AttemptRow(rs.getObject("admitted_at", java.time.OffsetDateTime.class).toInstant(),
            rs.getInt("reserved_tokens"))).list();
  }

  @Override
  public void complete(Reservation reservation) {
    Objects.requireNonNull(reservation);
    try {
      jdbc.sql("UPDATE ai_attempt SET in_flight_until=NULL WHERE request_id=:id")
          .param("id", reservation.requestId()).update();
    } catch (RuntimeException unavailable) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
  }

  @Override
  public void failed(Reservation reservation, boolean retainLeaseUntilExpiry) {
    Objects.requireNonNull(reservation);
    try {
      jdbc.sql("""
          UPDATE ai_attempt
          SET actor_quota_counted=false,
              in_flight_until=CASE WHEN :retain THEN in_flight_until ELSE NULL END
          WHERE request_id=:id
          """).param("retain", retainLeaseUntilExpiry).param("id", reservation.requestId()).update();
    } catch (RuntimeException unavailable) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
  }

  private static long sumTokens(List<AttemptRow> attempts) {
    return attempts.stream().mapToLong(AttemptRow::tokens).sum();
  }

  private static Duration budgetRetry(
      List<AttemptRow> attempts, int requestLimit, long tokenLimit, Duration window, Instant now) {
    int remainingRequests = attempts.size();
    long remainingTokens = sumTokens(attempts);
    for (AttemptRow attempt : attempts.stream().sorted(Comparator.comparing(AttemptRow::admittedAt)).toList()) {
      remainingRequests--;
      remainingTokens -= attempt.tokens();
      if (remainingRequests < requestLimit && remainingTokens <= tokenLimit) {
        return retry(attempt.admittedAt().plus(window), now);
      }
    }
    return retry(attempts.get(0).admittedAt().plus(window), now);
  }

  private static Duration retry(Instant expiry, Instant now) {
    long seconds = Math.max(1, (Duration.between(now, expiry).toMillis() + 999) / 1000);
    return Duration.ofSeconds(seconds);
  }

  private record AttemptRow(Instant admittedAt, int tokens) {}
  private record Decision(Reservation reservation, AiGateway.FailureCode failure, Duration retryAfter) {
    static Decision admitted(Reservation reservation) { return new Decision(reservation, null, null); }
    static Decision denied(AiGateway.FailureCode failure, Duration retryAfter) {
      return new Decision(null, failure, retryAfter);
    }
  }
}

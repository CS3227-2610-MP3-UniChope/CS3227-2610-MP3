package unichope.admin;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.Set;
import java.util.concurrent.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.ai.AiGateway;

public final class ActivityReportService implements AutoCloseable {
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();
  private final AdminActorProvider actors;
  private final AdminRepository repository;
  private final AiGateway gateway;
  private final Clock clock;
  private final Duration timeout;
  private final Map<UUID, Lease> inFlight = new HashMap<>();
  private final ThreadPoolExecutor workers =
      new ThreadPoolExecutor(
          4,
          4,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(16),
          task -> {
            Thread thread = new Thread(task, "admin-report");
            thread.setDaemon(true);
            return thread;
          },
          new ThreadPoolExecutor.AbortPolicy());

  private static final class Lease {
    final Instant expires;
    boolean uncertain;
    boolean started;
    boolean completed;

    Lease(Instant expires) {
      this.expires = expires;
    }
  }

  public ActivityReportService(
      AdminActorProvider actors, AdminRepository repository, AiGateway gateway) {
    this(actors, repository, gateway, Clock.systemUTC(), Duration.ofSeconds(30));
  }

  ActivityReportService(
      AdminActorProvider actors,
      AdminRepository repository,
      AiGateway gateway,
      Clock clock,
      Duration timeout) {
    if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofSeconds(30)) > 0)
      throw new IllegalArgumentException("Invalid report timeout");
    this.clock = clock;
    this.timeout = timeout;
    this.actors = actors;
    this.repository = repository;
    this.gateway = gateway;
  }

  public AdminViews.Report generate() {
    var actor = actors.currentAdmin();
    var s = repository.statistics();
    if (s.total() == 0) {
      if (!actor.equals(actors.currentAdmin()))
        throw PostgresAdminRepository.error(401, "UNAUTHENTICATED");
      return new AdminViews.Report(
          s.capturedAt(), s, "No bookings are available to summarize.", true, "none");
    }
    String input =
        "{\"total\":"
            + s.total()
            + ",\"completed\":"
            + s.completed()
            + ",\"cancelled\":"
            + s.cancelled()
            + ",\"completionRate\":"
            + s.completionRate()
            + ",\"cancellationRate\":"
            + s.cancellationRate()
            + "}";
    var result =
        generateBounded(
            new AiGateway.Request(
                actor.actorId(),
                "ADM-REPORT-1",
                "Return only JSON with exact keys summary (text) and caveats (array of text)."
                    + " Summarize supplied counts and percentages only. Do not invent causes,"
                    + " trends or additional facts. No HTML or URLs. Maximum summary 2000"
                    + " characters, 5 caveats of 400 characters each.",
                input,
                2048));
    String draft = parse(result.text());
    var current = actors.currentAdmin();
    if (!actor.equals(current)) throw PostgresAdminRepository.error(401, "UNAUTHENTICATED");
    return new AdminViews.Report(
        s.capturedAt(),
        s,
        draft,
        true,
        result.providerMode().name().toLowerCase(java.util.Locale.ROOT));
  }

  private AiGateway.Result generateBounded(AiGateway.Request request) {
    Lease lease = reserve(request.actorId());
    Future<AiGateway.Result> future;
    try {
      future =
          workers.submit(
              () -> {
                synchronized (this) {
                  if (lease.uncertain) {
                    lease.completed = true;
                    throw new AiGateway.Failure(AiGateway.FailureCode.TIMEOUT);
                  }
                  lease.started = true;
                }
                try {
                  return gateway.generate(request);
                } finally {
                  synchronized (this) {
                    lease.completed = true;
                  }
                }
              });
    } catch (RejectedExecutionException rejected) {
      release(request.actorId(), lease);
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
    try {
      AiGateway.Result result = future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
      release(request.actorId(), lease);
      return result;
    } catch (TimeoutException expired) {
      uncertain(lease);
      future.cancel(true);
      throw new AiGateway.Failure(AiGateway.FailureCode.TIMEOUT);
    } catch (InterruptedException interrupted) {
      uncertain(lease);
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    } catch (ExecutionException failed) {
      release(request.actorId(), lease);
      if (failed.getCause() instanceof AiGateway.Failure known) throw known;
      if (failed.getCause() instanceof unichope.platform.web.ApiFailure known) throw known;
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
  }

  private synchronized Lease reserve(UUID actor) {
    Instant now = clock.instant();
    inFlight
        .values()
        .removeIf(lease -> lease.uncertain && lease.completed && !lease.expires.isAfter(now));
    if (inFlight.containsKey(actor)) throw new ReportBusyFailure();
    if (inFlight.size() >= 10000) throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    Lease lease = new Lease(now.plusSeconds(60));
    inFlight.put(actor, lease);
    return lease;
  }

  private synchronized void release(UUID actor, Lease lease) {
    if (!lease.uncertain) inFlight.remove(actor, lease);
  }

  private synchronized void uncertain(Lease lease) {
    lease.uncertain = true;
    if (!lease.started) lease.completed = true;
  }

  @Override
  public void close() {
    workers.shutdownNow();
  }

  static String parse(String raw) {
    try {
      if (raw.getBytes(StandardCharsets.UTF_8).length > 65536) throw new IllegalArgumentException();
      JsonNode n = JSON.readTree(raw);
      if (n == null
          || !n.isObject()
          || !Set.copyOf(n.propertyNames()).equals(Set.of("summary", "caveats")))
        throw new IllegalArgumentException();
      String summary = text(n.get("summary"), 2000);
      JsonNode caveats = n.get("caveats");
      if (!caveats.isArray() || caveats.size() > 5) throw new IllegalArgumentException();
      StringBuilder out = new StringBuilder(summary);
      for (JsonNode c : caveats) out.append("\n\n").append(text(c, 400));
      if (out.codePointCount(0, out.length()) > 4000) throw new IllegalArgumentException();
      return out.toString();
    } catch (RuntimeException e) {
      throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
    }
  }

  private static String text(JsonNode node, int max) {
    if (node == null || !node.isString()) throw new IllegalArgumentException();
    String s = node.stringValue().strip();
    if (s.isEmpty()
        || s.codePointCount(0, s.length()) > max
        || s.contains("<")
        || s.contains(">")
        || s.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))
      throw new IllegalArgumentException();
    return s;
  }
}

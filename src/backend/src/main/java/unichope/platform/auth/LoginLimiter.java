package unichope.platform.auth;

import java.time.*;
import java.util.*;

/** Synchronized admission reserves each outstanding check and never evicts live history. */
public final class LoginLimiter {
  public record Reservation(String key, UUID id, Instant expires) {}

  private static final class Bucket {
    final List<Instant> failures = new ArrayList<>();
    final Map<UUID, Instant> pending = new HashMap<>();
  }

  private final Clock clock;
  private final int limit, capacity;
  private final Duration window, deadline;
  private final Map<String, Bucket> buckets = new HashMap<>();

  public LoginLimiter(Clock clock) {
    this(clock, 5, 10000, Duration.ofMinutes(15), Duration.ofSeconds(10));
  }

  public LoginLimiter(Clock clock, int limit, int capacity, Duration window, Duration deadline) {
    this.clock = clock;
    this.limit = limit;
    this.capacity = capacity;
    this.window = window;
    this.deadline = deadline;
  }

  private void clean(Bucket b, Instant now) {
    b.failures.removeIf(t -> !t.plus(window).isAfter(now));
    b.pending.values().removeIf(t -> !t.isAfter(now));
  }

  public synchronized Reservation reserve(String key) {
    try {
      key =
          java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable");
    }
    Instant now = clock.instant();
    buckets.values().forEach(b -> clean(b, now));
    buckets.values().removeIf(b -> b.failures.isEmpty() && b.pending.isEmpty());
    Bucket b = buckets.get(key);
    if (b == null) {
      if (buckets.size() >= capacity) throw new AuthFailure(429, "AUTH_RATE_LIMITED", 900);
      b = new Bucket();
      buckets.put(key, b);
    }
    if (b.failures.size() + b.pending.size() >= limit) {
      Instant next = now.plus(window);
      for (Instant t : b.failures) if (t.plus(window).isBefore(next)) next = t.plus(window);
      for (Instant t : b.pending.values()) if (t.isBefore(next)) next = t;
      throw new AuthFailure(
          429,
          "AUTH_RATE_LIMITED",
          Math.max(1, (int) Math.ceil(Duration.between(now, next).toMillis() / 1000.0)));
    }
    var r = new Reservation(key, UUID.randomUUID(), now.plus(deadline));
    b.pending.put(r.id(), r.expires());
    return r;
  }

  public synchronized boolean valid(Reservation r) {
    Bucket b = buckets.get(r.key());
    return b != null
        && r.expires().isAfter(clock.instant())
        && r.expires().equals(b.pending.get(r.id()));
  }

  public synchronized void finish(Reservation r, boolean success) {
    Bucket b = buckets.get(r.key());
    if (b != null
        && r.expires().equals(b.pending.remove(r.id()))
        && r.expires().isAfter(clock.instant())
        && !success) b.failures.add(clock.instant());
  }
}

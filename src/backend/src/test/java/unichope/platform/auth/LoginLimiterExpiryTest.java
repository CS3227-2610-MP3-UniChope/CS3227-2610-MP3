package unichope.platform.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

class LoginLimiterExpiryTest {
  static final class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }

  @Test
  void expiredWorkerCannotConsumeOrEraseNewReservation() {
    var c = new MutableClock();
    var limiter = new LoginLimiter(c, 1, 1, Duration.ofMinutes(15), Duration.ofSeconds(10));
    var old = limiter.reserve("a");
    c.now = c.now.plusSeconds(11);
    var fresh = limiter.reserve("a");
    assertFalse(limiter.valid(old));
    limiter.finish(old, false);
    assertTrue(limiter.valid(fresh));
    limiter.finish(fresh, true);
    assertNotNull(limiter.reserve("a"));
  }

  @Test
  void rollingFailuresExpire() {
    var c = new MutableClock();
    var limiter = new LoginLimiter(c, 1, 1, Duration.ofMinutes(15), Duration.ofSeconds(10));
    limiter.finish(limiter.reserve("a"), false);
    c.now = c.now.plusSeconds(899);
    assertEquals(429, assertThrows(AuthFailure.class, () -> limiter.reserve("a")).status());
    c.now = c.now.plusSeconds(1);
    assertNotNull(limiter.reserve("a"));
  }
}

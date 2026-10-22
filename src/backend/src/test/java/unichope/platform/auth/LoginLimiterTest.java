package unichope.platform.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

class LoginLimiterTest {
  @Test
  void reservationsAndFailuresCannotBypassLimit() {
    var limiter =
        new LoginLimiter(Clock.systemUTC(), 2, 2, Duration.ofMinutes(15), Duration.ofSeconds(10));
    var first = limiter.reserve("a");
    var second = limiter.reserve("a");
    assertThrows(AuthFailure.class, () -> limiter.reserve("a"));
    limiter.finish(first, true);
    limiter.finish(second, false);
    var third = limiter.reserve("a");
    limiter.finish(third, false);
    assertThrows(AuthFailure.class, () -> limiter.reserve("a"));
  }

  @Test
  void capacityDoesNotEvictFailureHistory() {
    var limiter =
        new LoginLimiter(Clock.systemUTC(), 1, 1, Duration.ofMinutes(15), Duration.ofSeconds(10));
    limiter.finish(limiter.reserve("a"), false);
    assertThrows(AuthFailure.class, () -> limiter.reserve("b"));
    assertThrows(AuthFailure.class, () -> limiter.reserve("a"));
  }

  @Test
  void longInvalidEmailDoesNotCreateLargeStoredCounterKeys() {
    var limiter = new LoginLimiter(Clock.systemUTC());
    var reservation = limiter.reserve("x".repeat(65536));
    assertEquals(64, reservation.key().length());
    assertTrue(limiter.valid(reservation));
    limiter.finish(reservation, false);
    assertNotNull(limiter.reserve("normal@example.edu"));
  }
}

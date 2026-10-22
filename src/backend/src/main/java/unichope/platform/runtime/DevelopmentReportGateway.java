package unichope.platform.runtime;

import java.time.*;
import java.util.*;
import unichope.platform.ai.*;
import unichope.platform.web.ApiFailure;

/** Local deterministic report fixture. This is not the production quota implementation. */
final class DevelopmentReportGateway implements AiGateway {
  private final Clock clock;
  private final Map<UUID, Deque<Instant>> attempts = new HashMap<>();
  private final AiGateway fake =
      new FakeProvider(
          "{\"summary\":\"This is a local test report. Review the displayed statistics for actual"
              + " counts.\",\"caveats\":[\"No real model was called.\"]}");

  DevelopmentReportGateway(Clock clock) {
    this.clock = clock;
  }

  @Override
  public synchronized Result generate(Request request) {
    Instant now = clock.instant();
    attempts.values().forEach(queue -> queue.removeIf(at -> !at.plusSeconds(300).isAfter(now)));
    attempts.values().removeIf(Deque::isEmpty);
    if (!attempts.containsKey(request.actorId()) && attempts.size() >= 10000)
      throw new ApiFailure(503, "AI_UNAVAILABLE", "Local test quota is full");
    Deque<Instant> history = attempts.computeIfAbsent(request.actorId(), id -> new ArrayDeque<>());
    if (history.size() >= 3)
      throw new ApiFailure(429, "AI_RATE_LIMITED", "Local test generation limit reached");
    history.add(now);
    return fake.generate(request);
  }
}

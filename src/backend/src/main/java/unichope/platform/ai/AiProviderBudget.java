package unichope.platform.ai;

import java.time.Duration;
import java.util.Objects;

/** Server-configured request, token-reservation, and concurrency budget for one role key. */
public record AiProviderBudget(
    int requestsPerWindow,
    int reservedTokensPerWindow,
    Duration rollingWindow,
    int maxInFlight) {

  public AiProviderBudget {
    Objects.requireNonNull(rollingWindow);
    if (requestsPerWindow <= 0
        || reservedTokensPerWindow <= 0
        || rollingWindow.isZero()
        || rollingWindow.isNegative()
        || maxInFlight <= 0) {
      throw new IllegalArgumentException("Invalid AI provider budget.");
    }
  }
}

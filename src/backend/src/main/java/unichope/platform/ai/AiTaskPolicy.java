package unichope.platform.ai;

import java.time.Duration;
import java.util.Objects;

/** Server-owned limits and role-key routing for one registered task. */
public record AiTaskPolicy(
    String task,
    AiProviderKey providerKey,
    int admissionsPerActor,
    Duration rollingWindow,
    int maxInFlightPerActor,
    int maxInputCodePoints,
    int maxOutputTokens,
    int maxResponseBytes,
    Duration deadline) {

  private static final Duration MAX_DEADLINE = Duration.ofSeconds(30);
  private static final int MAX_RESPONSE_BYTES = 65_536;

  public AiTaskPolicy {
    Objects.requireNonNull(task);
    Objects.requireNonNull(providerKey);
    Objects.requireNonNull(rollingWindow);
    Objects.requireNonNull(deadline);
    if (task.isBlank()
        || admissionsPerActor <= 0
        || rollingWindow.isZero()
        || rollingWindow.isNegative()
        || maxInFlightPerActor <= 0
        || maxInputCodePoints <= 0
        || maxOutputTokens <= 0
        || maxResponseBytes <= 0
        || maxResponseBytes > MAX_RESPONSE_BYTES
        || deadline.isZero()
        || deadline.isNegative()
        || deadline.compareTo(MAX_DEADLINE) > 0) {
      throw new IllegalArgumentException("Invalid AI task policy.");
    }
  }
}

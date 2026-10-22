package unichope.platform.ai;

import java.time.Duration;

/**
 * Per-key provider limits plus per-account limits. Budgets are in microdollars and use rolling
 * windows (24 hours, 31 days), which is stricter than SoCLaaS's UTC calendar day and month, so
 * no provider window can exceed them. Prices come from the selected {@link AiModel}.
 */
public record AiQuotaPolicy(
    int requestsPerMinute,
    long dailyBudgetMicros,
    long monthlyBudgetMicros,
    int perActorRequests,
    Duration perActorWindow,
    Duration inFlightLease) {

  public AiQuotaPolicy {
    if (requestsPerMinute <= 0
        || dailyBudgetMicros <= 0
        || monthlyBudgetMicros <= 0
        || perActorRequests <= 0
        || perActorWindow.isNegative()
        || perActorWindow.isZero()
        || inFlightLease.isNegative()
        || inFlightLease.isZero())
      throw new IllegalArgumentException("AI quota policy values must be positive");
  }

  /** Defaults leave margin under the SoC limits of 90 rpm, $80/day and $800/month per key. */
  public static AiQuotaPolicy defaults() {
    return new AiQuotaPolicy(
        80, 70_000_000L, 700_000_000L, 3, Duration.ofMinutes(5), Duration.ofSeconds(60));
  }
}

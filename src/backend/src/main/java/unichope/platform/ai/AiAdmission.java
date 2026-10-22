package unichope.platform.ai;

import java.util.UUID;

/** Durable admission control: per-key rate and budget, per-account window and one in flight. */
public interface AiAdmission {
  /**
   * Reserves capacity or throws {@link unichope.platform.web.ApiFailure} (409 AI_BUSY, 429
   * AI_RATE_LIMITED, 503 AI_BUDGET_EXHAUSTED) or {@link AiGateway.Failure} UNAVAILABLE when the
   * quota store cannot be used.
   */
  UUID admit(AiKeySlot slot, UUID actorId, AiTask task, long reservedMicros);

  /** Ends the in-flight lease. A null actual cost keeps the full reservation charged. */
  void complete(UUID attemptId, Long actualMicros);
}

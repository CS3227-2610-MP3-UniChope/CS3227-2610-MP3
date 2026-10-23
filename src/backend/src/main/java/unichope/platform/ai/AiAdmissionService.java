package unichope.platform.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * Durable implementations store admission metadata only; never pass prompt text here.
 * Admission is atomic, enforces the task's accepted-output actor window and in-flight
 * limit, applies provider-key request budgets and token reservations, and fails closed
 * if its store is unavailable. Production token accounting must be verified per provider.
 */
public interface AiAdmissionService {
  Reservation admit(Attempt attempt, AiTaskPolicy policy);

  /** Clears the in-flight lease after accepted output; the provider-key attempt remains counted. */
  void complete(Reservation reservation);

  /**
   * Releases the actor's successful-output quota charge after a failed call while retaining its
   * provider-key request/token charge. An uncertain call may keep its in-flight lease until expiry.
   */
  void failed(Reservation reservation, boolean retainLeaseUntilExpiry);

  record Attempt(UUID actorId, String task, AiProviderKey providerKey, int reservedOutputTokens) {
    public Attempt {
      Objects.requireNonNull(actorId);
      Objects.requireNonNull(task);
      Objects.requireNonNull(providerKey);
      if (task.isBlank() || reservedOutputTokens <= 0) {
        throw new IllegalArgumentException("Invalid AI admission metadata.");
      }
    }

    @Override
    public String toString() {
      return "AiAdmissionService.Attempt[actorId=" + actorId + ", task=" + task + ", metadata-only]";
    }
  }

  record Reservation(UUID requestId) {
    public Reservation {
      Objects.requireNonNull(requestId);
    }
  }
}

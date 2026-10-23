package unichope.student;

import java.util.UUID;

/** Ephemeral, reviewed plain-text suggestion; never persisted. */
public record AgendaDraft(UUID bookingId, String agenda, boolean requiresReview, String providerMode) {
    @Override public String toString() { return "AgendaDraft[redacted]"; }
}

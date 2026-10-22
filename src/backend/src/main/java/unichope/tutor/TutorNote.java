package unichope.tutor;

import java.time.Instant;
import java.util.UUID;

/** Private note owned by the tutor who completed a consultation. */
public record TutorNote(UUID bookingId, String content, long version, Instant updatedAt) {}

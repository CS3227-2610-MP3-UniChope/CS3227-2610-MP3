package unichope.platform.core;

import java.util.Objects;
import java.util.UUID;

/** Identifier-only context returned while the caller's transaction retains the row locks. */
public record LockedCompletedBooking(UUID bookingId, UUID tutorId, UUID studentId, UUID moduleId, UUID slotId) {
    public LockedCompletedBooking {
        Objects.requireNonNull(bookingId, "bookingId");
        Objects.requireNonNull(tutorId, "tutorId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(slotId, "slotId");
    }
}

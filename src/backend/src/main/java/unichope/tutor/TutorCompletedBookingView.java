package unichope.tutor;

import java.time.Instant;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;

/** Safe Tutor-facing result of completing an owned booking. */
public record TutorCompletedBookingView(UUID id, UUID slotId, UUID moduleId, String moduleCode,
        String studentName, Instant startTime, Instant endTime, BookingStatus status) {}

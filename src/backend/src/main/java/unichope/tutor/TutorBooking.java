package unichope.tutor;

import java.time.Instant;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;

/** Tutor-safe booking projection; excludes student and tutor identifiers. */
public record TutorBooking(UUID id, UUID slotId, UUID moduleId, String moduleCode,
                           String studentName, Instant startTime, Instant endTime,
                           BookingStatus status) {}

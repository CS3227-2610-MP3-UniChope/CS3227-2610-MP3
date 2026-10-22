package unichope.student;

import java.time.Instant;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;

/**
 * Student {@code StudentBooking} JSON (TECH §3): exactly these fields. {@code tutorName} is the
 * tutor's display name only; no email, student name, student ID or tutor ID is exposed.
 * {@code cancellable} is computed by the server from the injected clock, never by the client.
 */
public record StudentBooking(
        UUID id,
        UUID slotId,
        UUID moduleId,
        String moduleCode,
        String moduleName,
        String tutorName,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        boolean cancellable) {}

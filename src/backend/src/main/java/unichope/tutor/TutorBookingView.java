package unichope.tutor;

import java.time.Instant;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;

public record TutorBookingView(UUID bookingId, UUID slotId, String moduleCode, String studentName,
                               Instant startTime, Instant endTime, BookingStatus status) {}

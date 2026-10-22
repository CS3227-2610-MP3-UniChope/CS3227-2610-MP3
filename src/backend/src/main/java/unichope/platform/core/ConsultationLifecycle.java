package unichope.platform.core;

import java.time.Instant;
import java.util.UUID;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.ConsultationSlot;

/** Guarded atomic mutations. Actor IDs must originate from trusted server authentication. */
public interface ConsultationLifecycle {
    ConsultationSlot createSlot(UUID actorId, UUID moduleId, Instant start, Instant end);
    ConsultationSlot cancelAvailableSlot(UUID actorId, UUID slotId);
    Booking bookAvailableSlot(UUID actorId, UUID slotId);
    Booking cancelActiveBooking(UUID actorId, UUID bookingId);
    Booking completeActiveBooking(UUID actorId, UUID bookingId);
    LockedCompletedBooking lockForPrivateNote(UUID tutorId, UUID bookingId);
}

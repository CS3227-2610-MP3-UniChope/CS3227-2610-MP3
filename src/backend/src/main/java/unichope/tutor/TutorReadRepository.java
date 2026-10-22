package unichope.tutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;

/** Read boundary for authoritative account state and Tutor-owned data. */
public interface TutorReadRepository {
    Optional<TutorAccount> findAccount(UUID actorId);

    List<TutorSlot> findSlotsByTutor(UUID tutorId);

    List<AssignedModule> findAssignedModulesByTutor(UUID tutorId);

    List<TutorBooking> findBookingsByTutor(UUID tutorId, Optional<UUID> moduleId,
            Optional<LocalDate> date, Optional<BookingStatus> status);

    Optional<TutorBooking> findBookingByTutor(UUID tutorId, UUID bookingId);

    List<TutorSlot> findSlotHistoryByTutor(UUID tutorId, SlotStatus status, Optional<LocalDate> date);

    default Optional<TutorSlot> findSlotByTutor(UUID tutorId, UUID slotId) {
        return findSlotsByTutor(tutorId).stream().filter(slot -> slot.slot().id().equals(slotId)).findFirst();
    }
}

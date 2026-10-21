package unichope.platform.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import unichope.platform.core.model.consultation.Booking;

/** Shared reads only; state changes belong to guarded management/lifecycle operations. */
public interface BookingRepository {
    Optional<Booking> findById(UUID id);
    List<Booking> findAll();
    List<Booking> findByStudentId(UUID studentId);
    List<Booking> findBySlotId(UUID slotId);
}

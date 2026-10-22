package unichope.platform.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import unichope.platform.core.model.consultation.ConsultationSlot;

/** Shared reads only; state changes belong to guarded management/lifecycle operations. */
public interface SlotRepository {
    Optional<ConsultationSlot> findById(UUID id);
    List<ConsultationSlot> findAll();
    List<ConsultationSlot> findByTutorId(UUID tutorId);
    List<ConsultationSlot> findByModuleId(UUID moduleId);
}

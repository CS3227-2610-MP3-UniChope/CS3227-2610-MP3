package unichope.tutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read boundary for authoritative account state and Tutor-owned data. */
public interface TutorReadRepository {
    Optional<TutorAccount> findAccount(UUID actorId);

    List<TutorSlot> findSlotsByTutor(UUID tutorId);

    List<AssignedModule> findAssignedModulesByTutor(UUID tutorId);

    default Optional<TutorSlot> findSlotByTutor(UUID tutorId, UUID slotId) {
        return findSlotsByTutor(tutorId).stream().filter(slot -> slot.slot().id().equals(slotId)).findFirst();
    }
}

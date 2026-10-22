package unichope.tutor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.LifecycleFailure;

public final class TutorReadService {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private final TutorAccess access;
    private final TutorReadRepository reads;
    private final Clock clock;

    public TutorReadService(CurrentActorProvider currentActor, TutorReadRepository reads, Clock clock) {
        this.access = new TutorAccess(currentActor, reads);
        this.reads = reads;
        this.clock = clock;
    }

    public List<SlotView> upcomingSlots() {
        return upcomingSlots(Optional.empty());
    }

    public List<SlotView> upcomingSlots(LocalDate date) {
        return upcomingSlots(Optional.of(date));
    }

    private List<SlotView> upcomingSlots(Optional<LocalDate> date) {
        UUID tutorId = requireActiveTutor();
        var now = clock.instant();
        return reads.findSlotsByTutor(tutorId).stream()
                .filter(slot -> slot.slot().tutorId().equals(tutorId))
                .filter(slot -> slot.slot().startTime().isAfter(now))
                .filter(slot -> slot.slot().status() == SlotStatus.AVAILABLE || slot.slot().status() == SlotStatus.BOOKED)
                .filter(slot -> date.isEmpty() || slot.slot().startTime().atZone(SINGAPORE).toLocalDate().equals(date.get()))
                .sorted(Comparator.comparing((TutorSlot projection) -> projection.slot().startTime())
                        .thenComparing(projection -> projection.slot().id()))
                .map(slot -> new SlotView(slot.slot().id(), slot.slot().moduleId(), slot.moduleCode(),
                        slot.slot().startTime(), slot.slot().endTime(), slot.slot().status()))
                .toList();
    }

    public List<ModuleOption> activeAssignedModules() {
        UUID tutorId = requireActiveTutor();
        return reads.findAssignedModulesByTutor(tutorId).stream()
                .filter(AssignedModule::active)
                .sorted(Comparator.comparing(AssignedModule::code).thenComparing(AssignedModule::id))
                .map(module -> new ModuleOption(module.id(), module.code(), module.name()))
                .toList();
    }

    public SlotView slotDetail(UUID slotId) {
        UUID tutorId = requireActiveTutor();
        TutorSlot projection = reads.findSlotByTutor(tutorId, slotId)
                .filter(found -> found.slot().tutorId().equals(tutorId))
                .orElseThrow(() -> new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND));
        var slot = projection.slot();
        return new SlotView(slot.id(), slot.moduleId(), projection.moduleCode(), slot.startTime(), slot.endTime(), slot.status());
    }

    private UUID requireActiveTutor() { return access.requireActiveTutor(); }
}

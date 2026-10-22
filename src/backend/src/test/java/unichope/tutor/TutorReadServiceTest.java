package unichope.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.model.user.Role;

class TutorReadServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-24T00:00:00Z");
    private static final UUID TUTOR = id(1);

    @Test
    void upcomingSlotsRequiresAnActiveTutorAndFiltersAndOrdersOwnFutureSlots() {
        FakeReads reads = new FakeReads();
        TutorReadService service = service(TUTOR, Role.TUTOR, true, reads);
        UUID laterId = id(20);
        UUID earlierId = id(30);
        reads.slots.add(slot(laterId, TUTOR, "2026-09-24T02:00:00Z", SlotStatus.BOOKED));
        reads.slots.add(slot(earlierId, TUTOR, "2026-09-24T01:00:00Z", SlotStatus.AVAILABLE));
        reads.slots.add(slot(id(40), TUTOR, NOW.toString(), SlotStatus.AVAILABLE));
        reads.slots.add(slot(id(41), TUTOR, "2026-09-23T23:59:59Z", SlotStatus.BOOKED));
        reads.slots.add(slot(id(42), TUTOR, "2026-09-24T03:00:00Z", SlotStatus.CANCELLED));
        reads.slots.add(slot(id(43), TUTOR, "2026-09-24T03:00:00Z", SlotStatus.COMPLETED));
        reads.slots.add(slot(id(44), id(2), "2026-09-24T03:00:00Z", SlotStatus.AVAILABLE));

        assertEquals(List.of(earlierId, laterId), service.upcomingSlots().stream().map(SlotView::id).toList());
        assertEquals(TUTOR, reads.lastSlotTutor);
    }

    @Test
    void dateUsesSingaporeStartDateAndSlotsSortByIdWhenStartsTie() {
        FakeReads reads = new FakeReads();
        TutorReadService service = service(TUTOR, Role.TUTOR, true, reads);
        reads.slots.add(slot(id(9), TUTOR, "2026-09-24T16:30:00Z", SlotStatus.AVAILABLE));
        reads.slots.add(slot(id(3), TUTOR, "2026-09-24T16:30:00Z", SlotStatus.BOOKED));
        reads.slots.add(slot(id(1), TUTOR, "2026-09-24T15:59:59Z", SlotStatus.AVAILABLE));

        assertEquals(List.of(id(3), id(9)), service.upcomingSlots(LocalDate.parse("2026-09-25"))
                .stream().map(SlotView::id).toList());
        assertEquals(List.of(id(1)), service.upcomingSlots(LocalDate.parse("2026-09-24"))
                .stream().map(SlotView::id).toList());
        assertEquals(List.of(), service.upcomingSlots(LocalDate.parse("2026-09-26")));
    }

    @Test
    void modulesAreActiveAssignedAndOrderedWithExactProjection() {
        FakeReads reads = new FakeReads();
        TutorReadService service = service(TUTOR, Role.TUTOR, true, reads);
        reads.modules.add(new AssignedModule(id(8), "CS200", "Systems", true));
        reads.modules.add(new AssignedModule(id(3), "CS100", "Intro", true));
        reads.modules.add(new AssignedModule(id(2), "CS100", "Intro alt", true));
        reads.modules.add(new AssignedModule(id(1), "CS050", "Inactive", false));

        assertEquals(List.of(new ModuleOption(id(2), "CS100", "Intro alt"),
                        new ModuleOption(id(3), "CS100", "Intro"),
                        new ModuleOption(id(8), "CS200", "Systems")),
                service.activeAssignedModules());
        assertEquals(TUTOR, reads.lastModuleTutor);
        reads.modules.clear();
        assertEquals(List.of(), service.activeAssignedModules());
    }

    @Test
    void deniesMissingActorAccountNonTutorRolesAndInactiveTutor() {
        FakeReads reads = new FakeReads();
        assertThrows(TutorAccessDeniedException.class, () -> new TutorReadService(
                () -> Optional.empty(), reads, Clock.fixed(NOW, ZoneId.of("UTC"))).upcomingSlots());
        reads.account = Optional.empty();
        TutorReadService missingAccount = new TutorReadService(
                () -> Optional.of(TUTOR), reads, Clock.fixed(NOW, ZoneId.of("UTC")));
        assertThrows(TutorAccessDeniedException.class, missingAccount::upcomingSlots);
        assertThrows(TutorAccessDeniedException.class, () -> service(TUTOR, Role.STUDENT, true, reads).activeAssignedModules());
        assertThrows(TutorAccessDeniedException.class, () -> service(TUTOR, Role.ADMIN, true, reads).upcomingSlots());
        assertThrows(TutorAccessDeniedException.class, () -> service(TUTOR, Role.TUTOR, false, reads).activeAssignedModules());
    }

    private static TutorReadService service(UUID id, Role role, boolean active, FakeReads reads) {
        reads.account = Optional.of(new TutorAccount(id, role, active));
        return new TutorReadService(() -> Optional.of(id), reads, Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    private static TutorSlot slot(UUID id, UUID owner, String start, SlotStatus status) {
        return new TutorSlot(new ConsultationSlot(id, owner, id(99), Instant.parse(start),
                Instant.parse(start).plusSeconds(1800), status), "CS3227");
    }

    private static UUID id(int value) {
        return new UUID(0, value);
    }

    private static final class FakeReads implements TutorReadRepository {
        private Optional<TutorAccount> account = Optional.empty();
        private final List<TutorSlot> slots = new ArrayList<>();
        private final List<AssignedModule> modules = new ArrayList<>();
        private UUID lastSlotTutor;
        private UUID lastModuleTutor;

        @Override
        public Optional<TutorAccount> findAccount(UUID actorId) {
            return account.filter(found -> found.id().equals(actorId));
        }

        @Override
        public List<TutorSlot> findSlotsByTutor(UUID tutorId) {
            lastSlotTutor = tutorId;
            return slots;
        }

        @Override
        public List<AssignedModule> findAssignedModulesByTutor(UUID tutorId) {
            lastModuleTutor = tutorId;
            return modules;
        }

        @Override
        public List<TutorBooking> findBookingsByTutor(UUID tutorId, Optional<UUID> moduleId,
                Optional<LocalDate> date, Optional<unichope.platform.core.model.consultation.BookingStatus> status) {
            return List.of();
        }

        @Override
        public Optional<TutorBooking> findBookingByTutor(UUID tutorId, UUID bookingId) { return Optional.empty(); }

        @Override
        public List<TutorSlot> findSlotHistoryByTutor(UUID tutorId, SlotStatus status, Optional<LocalDate> date) {
            return List.of();
        }
    }
}

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
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.BookingStatus;
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
    void bookingQueryAppliesOwnerFiltersAndStableStartOrder() {
        FakeReads reads = new FakeReads();
        TutorReadService service = service(TUTOR, Role.TUTOR, true, reads);
        reads.bookings.add(booking(id(10), TUTOR, "2026-09-20T00:00:00Z", BookingStatus.COMPLETED, SlotStatus.COMPLETED));
        reads.bookings.add(booking(id(15), TUTOR, "2026-09-20T00:00:00Z", BookingStatus.COMPLETED, SlotStatus.COMPLETED));
        reads.bookings.add(booking(id(11), TUTOR, "2026-09-22T00:00:00Z", BookingStatus.COMPLETED, SlotStatus.COMPLETED));
        reads.bookings.add(booking(id(12), TUTOR, "2026-09-23T00:00:00Z", BookingStatus.ACTIVE, SlotStatus.BOOKED));
        reads.bookings.add(booking(id(13), id(2), "2026-09-24T00:00:00Z", BookingStatus.COMPLETED, SlotStatus.COMPLETED));
        reads.bookings.add(booking(id(14), TUTOR, "2026-09-24T00:00:00Z", BookingStatus.COMPLETED, SlotStatus.BOOKED));

        assertEquals(List.of(id(10), id(15), id(11)), service.bookings(Optional.empty(), Optional.empty(),
                Optional.of(BookingStatus.COMPLETED)).stream()
                .map(TutorBookingView::bookingId).toList());
        assertEquals(List.of(id(10), id(15), id(11)), service.bookings(Optional.of(id(99)), Optional.empty(),
                Optional.of(BookingStatus.COMPLETED))
                .stream().map(TutorBookingView::bookingId).toList());
        assertEquals(TUTOR, reads.lastBookingTutor);
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

    private static TutorBooking booking(UUID bookingId, UUID owner, String start,
            BookingStatus bookingStatus, SlotStatus slotStatus) {
        Instant startTime = Instant.parse(start);
        var slot = new ConsultationSlot(id((int) bookingId.getLeastSignificantBits()), owner, id(99),
                startTime, startTime.plusSeconds(1800), slotStatus);
        var booking = new Booking(bookingId, id(3), slot.id(), startTime, bookingStatus);
        return new TutorBooking(booking, slot, "CS3227");
    }

    private static UUID id(int value) {
        return new UUID(0, value);
    }

    private static final class FakeReads implements TutorReadRepository {
        private Optional<TutorAccount> account = Optional.empty();
        private final List<TutorSlot> slots = new ArrayList<>();
        private final List<AssignedModule> modules = new ArrayList<>();
        private final List<TutorBooking> bookings = new ArrayList<>();
        private UUID lastSlotTutor;
        private UUID lastModuleTutor;
        private UUID lastBookingTutor;

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
        public List<TutorBooking> findBookingsByTutor(UUID tutorId) {
            lastBookingTutor = tutorId;
            return bookings;
        }
    }
}

package unichope.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.model.user.Role;

class TutorBookingReadServiceTest {
    private static final UUID TUTOR = id(1);

    @Test
    void bookingFiltersAndHistoryAreDelegatedWithTrustedTutorAndRequireAnActiveTutor() {
        var reads = new Reads();
        reads.account = Optional.of(new TutorAccount(TUTOR, Role.TUTOR, true));
        var service = new TutorReadService(() -> Optional.of(TUTOR), reads,
                Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC));
        LocalDate date = LocalDate.parse("2026-10-21");
        UUID module = id(2);
        assertEquals(List.of(), service.bookings(Optional.of(module), Optional.of(date), Optional.of(BookingStatus.ACTIVE)));
        assertEquals(TUTOR, reads.bookingTutor);
        assertEquals(module, reads.moduleFilter);
        assertEquals(date, reads.dateFilter);
        assertEquals(BookingStatus.ACTIVE, reads.bookingStatus);
        assertEquals(List.of(), service.slotHistory(SlotStatus.CANCELLED, Optional.of(date)));
        assertEquals(TUTOR, reads.historyTutor);
        assertEquals(SlotStatus.CANCELLED, reads.historyStatus);
        assertEquals(date, reads.historyDate);

        reads.account = Optional.of(new TutorAccount(TUTOR, Role.STUDENT, true));
        assertThrows(TutorAccessDeniedException.class,
                () -> service.bookings(Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private static UUID id(int n) { return new UUID(0, n); }

    private static final class Reads implements TutorReadRepository {
        private Optional<TutorAccount> account = Optional.empty();
        private UUID bookingTutor, moduleFilter, historyTutor;
        private LocalDate dateFilter, historyDate;
        private BookingStatus bookingStatus;
        private SlotStatus historyStatus;
        @Override public Optional<TutorAccount> findAccount(UUID actorId) { return account; }
        @Override public List<TutorSlot> findSlotsByTutor(UUID tutorId) { return List.of(); }
        @Override public List<AssignedModule> findAssignedModulesByTutor(UUID tutorId) { return List.of(); }
        @Override public List<TutorBooking> findBookingsByTutor(UUID tutorId, Optional<UUID> moduleId,
                Optional<LocalDate> date, Optional<BookingStatus> status) {
            bookingTutor=tutorId;moduleFilter=moduleId.orElse(null);dateFilter=date.orElse(null);bookingStatus=status.orElse(null);
            return List.of();
        }
        @Override public Optional<TutorBooking> findBookingByTutor(UUID tutorId, UUID bookingId) {
            return Optional.empty();
        }
        @Override public List<TutorSlot> findSlotHistoryByTutor(UUID tutorId, SlotStatus status, Optional<LocalDate> date) {
            historyTutor=tutorId;historyStatus=status;historyDate=date.orElse(null);return List.of();
        }
    }
}

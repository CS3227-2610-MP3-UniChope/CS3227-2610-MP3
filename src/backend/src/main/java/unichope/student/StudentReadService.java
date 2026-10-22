package unichope.student;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;

/** Student read operations. Every call checks the current actor before reading any resource. */
public final class StudentReadService {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private final StudentAccess access;
    private final StudentReadRepository reads;
    private final Clock clock;

    public StudentReadService(StudentAccess access, StudentReadRepository reads, Clock clock) {
        this.access = access;
        this.reads = reads;
        this.clock = clock;
    }

    /** Active modules (assigned or not), ordered by code then ID. */
    public List<ModuleOption> activeModules() {
        access.requireActiveStudent();
        return reads.findActiveModules();
    }

    /**
     * Bookable future slots, optionally filtered by module and by SGT start date. Slots that
     * overlap the student's own ACTIVE bookings stay listed (PRODUCT §10 A2).
     */
    public List<BookableSlot> bookableSlots(Optional<UUID> moduleId, Optional<LocalDate> sgtDate) {
        access.requireActiveStudent();
        return reads.findBookableSlots(clock.instant(), moduleId, sgtDay(sgtDate));
    }

    /**
     * The current student's own bookings in any status (or only {@code statuses} when not empty),
     * optionally on one SGT slot start date, ordered by slot start time then booking ID.
     */
    public List<StudentBooking> myBookings(Set<BookingStatus> statuses, Optional<LocalDate> sgtDate) {
        UUID student = access.requireActiveStudent();
        Instant now = clock.instant();
        return reads.findStudentBookings(student, Set.copyOf(statuses), sgtDay(sgtDate)).stream()
                .map(row -> project(row, now))
                .toList();
    }

    /**
     * The current student's booking with this ID in any status. Empty for a nonexistent ID and for
     * another student's booking alike (TECH §3, §4).
     */
    public Optional<StudentBooking> myBooking(UUID bookingId) {
        UUID student = access.requireActiveStudent();
        Instant now = clock.instant();
        return reads.findStudentBooking(student, bookingId).map(row -> project(row, now));
    }

    /** {@code cancellable} = ACTIVE and slot start strictly after the injected clock (TECH §3). */
    private static StudentBooking project(StudentReadRepository.BookingRow row, Instant now) {
        boolean cancellable = row.status() == BookingStatus.ACTIVE && row.startTime().isAfter(now);
        return new StudentBooking(row.id(), row.slotId(), row.moduleId(), row.moduleCode(), row.moduleName(),
                row.tutorName(), row.startTime(), row.endTime(), row.status(), cancellable);
    }

    /** SGT calendar date as the half-open UTC range {@code [date 00:00 SGT, next day 00:00 SGT)}. */
    private static Optional<StudentReadRepository.StartRange> sgtDay(Optional<LocalDate> sgtDate) {
        return sgtDate.map(date -> new StudentReadRepository.StartRange(
                date.atStartOfDay(SINGAPORE).toInstant(), date.plusDays(1).atStartOfDay(SINGAPORE).toInstant()));
    }
}

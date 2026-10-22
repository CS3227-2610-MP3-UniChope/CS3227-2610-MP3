package unichope.student;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;

/** Read-only boundary over core tables for the Student role (TECH §4, decision D3). */
public interface StudentReadRepository {
    /** Stored account state for the access check. */
    Optional<StudentAccount> findAccount(UUID actorId);

    /** Active modules ordered by code, then ID. */
    List<ModuleOption> findActiveModules();

    /**
     * Bookable slots: AVAILABLE, starting after {@code now}, active TUTOR owner, active module and
     * an existing tutor-module assignment; ordered by start time, then ID.
     *
     * @param moduleId optional module filter
     * @param startRange optional half-open start-time range {@code [from, to)}
     */
    List<BookableSlot> findBookableSlots(Instant now, Optional<UUID> moduleId, Optional<StartRange> startRange);

    /**
     * The student's own bookings in any status, ordered by slot start time, then booking ID. No
     * tutor/module eligibility condition applies (C3-ELIG-1).
     *
     * @param statuses status filter; empty means every status
     * @param startRange optional half-open slot start-time range {@code [from, to)}
     */
    List<BookingRow> findStudentBookings(UUID studentId, Set<BookingStatus> statuses, Optional<StartRange> startRange);

    /** One booking under the same owner condition; another student's booking is simply absent. */
    Optional<BookingRow> findStudentBooking(UUID studentId, UUID bookingId);

    /** Half-open start-time range {@code [from, to)}. */
    record StartRange(Instant from, Instant to) {}

    /** Stored booking projection; {@code cancellable} is decided by the service. */
    record BookingRow(
            UUID id,
            UUID slotId,
            UUID moduleId,
            String moduleCode,
            String moduleName,
            String tutorName,
            Instant startTime,
            Instant endTime,
            BookingStatus status) {}
}

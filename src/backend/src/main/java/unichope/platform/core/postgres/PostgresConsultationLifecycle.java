package unichope.platform.core.postgres;

import static unichope.platform.core.LifecycleFailure.Code.*;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.LifecycleFailure.Code;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.model.user.User;

/** Ordered PostgreSQL row locks protect empty ranges and all consultation transitions. */
public final class PostgresConsultationLifecycle implements ConsultationLifecycle {
    private static final Set<String> CONTENTION_STATES = Set.of("55P03", "40P01", "40001", "57014");
    private final JdbcClient jdbc;
    private final PostgresUserRepository users;
    private final PostgresModuleRepository modules;
    private final PostgresSlotRepository slots;
    private final PostgresBookingRepository bookings;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public PostgresConsultationLifecycle(DataSource source, Clock clock) {
        this.jdbc = JdbcClient.create(source);
        this.users = new PostgresUserRepository(jdbc);
        this.modules = new PostgresModuleRepository(jdbc);
        this.slots = new PostgresSlotRepository(jdbc);
        this.bookings = new PostgresBookingRepository(jdbc);
        this.clock = clock;
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        // Own the complete commit/rollback before returning a result or mapping SQL failures.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ConsultationSlot createSlot(UUID actorId, UUID moduleId, Instant start, Instant end) {
        required(actorId, moduleId);
        if (start == null || end == null) throw fail(INVALID_TIME);
        // PostgreSQL stores microseconds. Normalize before every time/overlap check,
        // rather than allowing JDBC rounding to change a validated interval later.
        Instant storedStart = start.truncatedTo(ChronoUnit.MICROS);
        Instant storedEnd = end.truncatedTo(ChronoUnit.MICROS);
        return mutate(() -> {
            requireActor(actorId, Role.TUTOR);
            lockUsers(actorId);
            lockModule(moduleId);
            requireActor(actorId, Role.TUTOR);
            requireEligibility(actorId, moduleId);
            if (!storedStart.isAfter(clock.instant()) || !storedEnd.isAfter(storedStart)) throw fail(INVALID_TIME);
            boolean overlaps = jdbc.sql("""
                    SELECT EXISTS (SELECT 1 FROM slots WHERE tutor_id=:tutor
                    AND status IN ('AVAILABLE','BOOKED') AND start_time<:end AND end_time>:start)
                    """).param("tutor", actorId).param("end", storedEnd.atOffset(ZoneOffset.UTC))
                    .param("start", storedStart.atOffset(ZoneOffset.UTC)).query(Boolean.class).single();
            if (overlaps) throw fail(SLOT_OVERLAP);
            ConsultationSlot slot = new ConsultationSlot(UUID.randomUUID(), actorId, moduleId, storedStart, storedEnd, SlotStatus.AVAILABLE);
            jdbc.sql("""
                    INSERT INTO slots (id,tutor_id,module_id,start_time,end_time,status)
                    VALUES (:id,:tutor,:module,:start,:end,'AVAILABLE')
                    """).param("id", slot.id()).param("tutor", actorId).param("module", moduleId)
                    .param("start", storedStart.atOffset(ZoneOffset.UTC)).param("end", storedEnd.atOffset(ZoneOffset.UTC)).update();
            return slot;
        });
    }

    @Override
    public ConsultationSlot cancelAvailableSlot(UUID actorId, UUID slotId) {
        required(actorId, slotId);
        return mutate(() -> {
            requireActor(actorId, Role.TUTOR);
            ConsultationSlot discovered = slot(slotId);
            if (!discovered.tutorId().equals(actorId)) throw fail(NOT_FOUND);
            lockUsers(actorId);
            lockModule(discovered.moduleId());
            lockSlot(slotId);
            requireActor(actorId, Role.TUTOR);
            ConsultationSlot current = slot(slotId);
            if (!current.tutorId().equals(actorId)) throw fail(NOT_FOUND);
            if (current.status() != SlotStatus.AVAILABLE) throw fail(INVALID_STATE);
            updateSlot(slotId, SlotStatus.CANCELLED);
            return current.withStatus(SlotStatus.CANCELLED);
        });
    }

    @Override
    public Booking bookAvailableSlot(UUID actorId, UUID slotId) {
        required(actorId, slotId);
        return mutate(() -> {
            requireActor(actorId, Role.STUDENT);
            ConsultationSlot discovered = slot(slotId);
            lockUsers(actorId, discovered.tutorId());
            lockModule(discovered.moduleId());
            lockSlot(slotId);
            requireActor(actorId, Role.STUDENT);
            ConsultationSlot current = slot(slotId);
            Instant now = clock.instant();
            if (current.status() != SlotStatus.AVAILABLE || !current.startTime().isAfter(now)) throw fail(INVALID_STATE);
            requireEligibility(current.tutorId(), current.moduleId());
            boolean activeBooking = jdbc.sql("SELECT EXISTS (SELECT 1 FROM bookings WHERE slot_id=:slot AND status='ACTIVE')")
                    .param("slot", slotId).query(Boolean.class).single();
            if (activeBooking) throw fail(INVALID_STATE);
            boolean overlaps = jdbc.sql("""
                    SELECT EXISTS (SELECT 1 FROM bookings b JOIN slots s ON s.id=b.slot_id
                    WHERE b.student_id=:student AND b.status='ACTIVE' AND s.start_time<:end AND s.end_time>:start)
                    """).param("student", actorId).param("end", current.endTime().atOffset(ZoneOffset.UTC))
                    .param("start", current.startTime().atOffset(ZoneOffset.UTC)).query(Boolean.class).single();
            if (overlaps) throw fail(BOOKING_OVERLAP);
            Instant createdAt = now.truncatedTo(ChronoUnit.MICROS);
            Booking booking = new Booking(UUID.randomUUID(), actorId, slotId, createdAt, BookingStatus.ACTIVE);
            jdbc.sql("""
                    INSERT INTO bookings (id,student_id,slot_id,created_at,status)
                    VALUES (:id,:student,:slot,:created,'ACTIVE')
                    """).param("id", booking.id()).param("student", actorId).param("slot", slotId)
                    .param("created", createdAt.atOffset(ZoneOffset.UTC)).update();
            updateSlot(slotId, SlotStatus.BOOKED);
            return booking;
        });
    }

    @Override
    public Booking cancelActiveBooking(UUID actorId, UUID bookingId) {
        required(actorId, bookingId);
        return changeBooking(actorId, bookingId, false);
    }

    @Override
    public Booking completeActiveBooking(UUID actorId, UUID bookingId) {
        required(actorId, bookingId);
        return changeBooking(actorId, bookingId, true);
    }

    private Booking changeBooking(UUID actorId, UUID bookingId, boolean complete) {
        Role role = complete ? Role.TUTOR : Role.STUDENT;
        return mutate(() -> {
            requireActor(actorId, role);
            Booking discoveredBooking = booking(bookingId);
            ConsultationSlot discoveredSlot = slot(discoveredBooking.slotId());
            UUID owner = complete ? discoveredSlot.tutorId() : discoveredBooking.studentId();
            if (!owner.equals(actorId)) throw fail(NOT_FOUND);
            lockUsers(discoveredSlot.tutorId(), discoveredBooking.studentId());
            lockModule(discoveredSlot.moduleId());
            lockSlot(discoveredSlot.id());
            lockBooking(bookingId);
            requireActor(actorId, role);
            Booking currentBooking = booking(bookingId);
            ConsultationSlot currentSlot = slot(currentBooking.slotId());
            UUID currentOwner = complete ? currentSlot.tutorId() : currentBooking.studentId();
            if (!currentOwner.equals(actorId)) throw fail(NOT_FOUND);
            if (currentBooking.status() != BookingStatus.ACTIVE || currentSlot.status() != SlotStatus.BOOKED
                    || (!complete && !currentSlot.startTime().isAfter(clock.instant()))) throw fail(INVALID_STATE);
            BookingStatus nextBooking = complete ? BookingStatus.COMPLETED : BookingStatus.CANCELLED;
            SlotStatus nextSlot = complete ? SlotStatus.COMPLETED : SlotStatus.AVAILABLE;
            jdbc.sql("UPDATE bookings SET status=:status WHERE id=:id").param("status", nextBooking.name()).param("id", bookingId).update();
            updateSlot(currentSlot.id(), nextSlot);
            return currentBooking.withStatus(nextBooking);
        });
    }

    private <T> T mutate(Supplier<T> work) {
        try {
            return transaction.execute(status -> {
                jdbc.sql("SET LOCAL lock_timeout='5s'").update();
                jdbc.sql("SET LOCAL statement_timeout='10s'").update();
                return work.get();
            });
        } catch (DataAccessException error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && CONTENTION_STATES.contains(sql.getSQLState())) {
                    throw fail(TEMPORARILY_UNAVAILABLE);
                }
            }
            throw error;
        }
    }

    private User requireActor(UUID actorId, Role role) {
        User actor = users.findById(actorId).orElseThrow(() -> fail(ACCESS_DENIED));
        if (!actor.isActive() || actor.role() != role) throw fail(ACCESS_DENIED);
        return actor;
    }

    private void requireEligibility(UUID tutorId, UUID moduleId) {
        User tutor = users.findById(tutorId).orElseThrow(() -> fail(MODULE_UNAVAILABLE));
        if (!tutor.isActive() || tutor.role() != Role.TUTOR
                || !modules.findById(moduleId).map(module -> module.isActive()).orElse(false)) throw fail(MODULE_UNAVAILABLE);
        boolean assigned = jdbc.sql("SELECT EXISTS (SELECT 1 FROM tutor_modules WHERE tutor_id=:tutor AND module_id=:module)")
                .param("tutor", tutorId).param("module", moduleId).query(Boolean.class).single();
        if (!assigned) throw fail(MODULE_UNAVAILABLE);
    }

    private void lockUsers(UUID... ids) {
        // PostgreSQL's UUID ordering is consistent across all callers and avoids signed Java UUID ordering differences.
        jdbc.sql("SELECT id FROM users WHERE id IN (:ids) ORDER BY id FOR UPDATE")
                .param("ids", Arrays.stream(ids).distinct().toList()).query(UUID.class).list();
    }
    private void lockModule(UUID id) { jdbc.sql("SELECT id FROM modules WHERE id=:id FOR UPDATE").param("id", id).query(UUID.class).optional(); }
    private void lockSlot(UUID id) { jdbc.sql("SELECT id FROM slots WHERE id=:id FOR UPDATE").param("id", id).query(UUID.class).optional(); }
    private void lockBooking(UUID id) { jdbc.sql("SELECT id FROM bookings WHERE id=:id FOR UPDATE").param("id", id).query(UUID.class).optional(); }
    private ConsultationSlot slot(UUID id) { return slots.findById(id).orElseThrow(() -> fail(NOT_FOUND)); }
    private Booking booking(UUID id) { return bookings.findById(id).orElseThrow(() -> fail(NOT_FOUND)); }
    private void updateSlot(UUID id, SlotStatus status) { jdbc.sql("UPDATE slots SET status=:status WHERE id=:id").param("status", status.name()).param("id", id).update(); }
    private static void required(UUID... ids) { if (Arrays.stream(ids).anyMatch(java.util.Objects::isNull)) throw fail(INVALID_INPUT); }
    private static LifecycleFailure fail(Code code) { return new LifecycleFailure(code); }
}

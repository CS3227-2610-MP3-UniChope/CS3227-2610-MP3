package unichope.tutor;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.repository.UserRepository;

/** Persistence adapter only; trusted session identity and authorization remain service responsibilities. */
public final class PostgresTutorReadRepository implements TutorReadRepository {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private final UserRepository users;
    private final JdbcClient jdbc;

    public PostgresTutorReadRepository(UserRepository users, JdbcClient jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TutorAccount> findAccount(UUID actorId) {
        return users.findById(actorId).map(user -> new TutorAccount(user.id(), user.role(), user.isActive()));
    }

    @Override
    public List<TutorSlot> findSlotsByTutor(UUID tutorId) {
        return jdbc.sql("""
                SELECT s.id, s.tutor_id, s.module_id, s.start_time, s.end_time, s.status, m.code
                FROM slots s JOIN modules m ON m.id=s.module_id
                WHERE s.tutor_id=:tutor ORDER BY s.start_time, s.id
                """).param("tutor", tutorId).query((row, index) -> new TutorSlot(
                        new ConsultationSlot(row.getObject("id", UUID.class), row.getObject("tutor_id", UUID.class),
                                row.getObject("module_id", UUID.class), row.getObject("start_time", OffsetDateTime.class).toInstant(),
                                row.getObject("end_time", OffsetDateTime.class).toInstant(), SlotStatus.valueOf(row.getString("status"))),
                        row.getString("code"))).list();
    }

    @Override
    public Optional<TutorSlot> findSlotByTutor(UUID tutorId, UUID slotId) {
        return jdbc.sql("""
                SELECT s.id, s.tutor_id, s.module_id, s.start_time, s.end_time, s.status, m.code
                FROM slots s JOIN modules m ON m.id=s.module_id
                WHERE s.tutor_id=:tutor AND s.id=:slot
                """).param("tutor", tutorId).param("slot", slotId).query((row, index) -> new TutorSlot(
                        new ConsultationSlot(row.getObject("id", UUID.class), row.getObject("tutor_id", UUID.class),
                                row.getObject("module_id", UUID.class), row.getObject("start_time", OffsetDateTime.class).toInstant(),
                                row.getObject("end_time", OffsetDateTime.class).toInstant(), SlotStatus.valueOf(row.getString("status"))),
                        row.getString("code"))).optional();
    }

    @Override
    public List<AssignedModule> findAssignedModulesByTutor(UUID tutorId) {
        return jdbc.sql("""
                SELECT m.id, m.code, m.name, m.active
                FROM tutor_modules tm JOIN modules m ON m.id=tm.module_id
                WHERE tm.tutor_id=:tutor ORDER BY m.code, m.id
                """).param("tutor", tutorId).query((row, index) -> new AssignedModule(row.getObject("id", UUID.class),
                        row.getString("code"), row.getString("name"), row.getBoolean("active"))).list();
    }

    @Override
    public List<TutorBooking> findBookingsByTutor(UUID tutorId, Optional<UUID> moduleId,
            Optional<LocalDate> date, Optional<BookingStatus> status) {
        StringBuilder query = new StringBuilder("""
                SELECT b.id, b.slot_id, s.module_id, m.code, u.name AS student_name,
                       s.start_time, s.end_time, b.status
                FROM bookings b
                JOIN slots s ON s.id=b.slot_id
                JOIN modules m ON m.id=s.module_id
                JOIN users u ON u.id=b.student_id
                WHERE s.tutor_id=:tutor
                """);
        // Build predicates from fixed clauses; all values remain bound parameters.
        if (moduleId.isPresent()) query.append(" AND s.module_id=:module");
        if (date.isPresent()) query.append(" AND s.start_time>=:day_start AND s.start_time<:day_end");
        if (status.isPresent()) query.append(" AND b.status=:status");
        query.append(" ORDER BY s.start_time, b.id");
        var spec = jdbc.sql(query.toString()).param("tutor", tutorId);
        if (moduleId.isPresent()) spec = spec.param("module", moduleId.get());
        if (date.isPresent()) {
            var start = date.get().atStartOfDay(SINGAPORE).toInstant().atOffset(ZoneOffset.UTC);
            var end = date.get().plusDays(1).atStartOfDay(SINGAPORE).toInstant().atOffset(ZoneOffset.UTC);
            spec = spec.param("day_start", start).param("day_end", end);
        }
        if (status.isPresent()) spec = spec.param("status", status.get().name());
        return spec.query((row, index) -> booking(row.getObject("id", UUID.class),
                row.getObject("slot_id", UUID.class), row.getObject("module_id", UUID.class), row.getString("code"),
                row.getString("student_name"), row.getObject("start_time", OffsetDateTime.class).toInstant(),
                row.getObject("end_time", OffsetDateTime.class).toInstant(),
                BookingStatus.valueOf(row.getString("status")))).list();
    }

    @Override
    public Optional<TutorBooking> findBookingByTutor(UUID tutorId, UUID bookingId) {
        return jdbc.sql("""
                SELECT b.id, b.slot_id, s.module_id, m.code, u.name AS student_name,
                       s.start_time, s.end_time, b.status
                FROM bookings b
                JOIN slots s ON s.id=b.slot_id
                JOIN modules m ON m.id=s.module_id
                JOIN users u ON u.id=b.student_id
                WHERE s.tutor_id=:tutor AND b.id=:booking
                """).param("tutor", tutorId).param("booking", bookingId)
                .query((row, index) -> booking(row.getObject("id", UUID.class),
                        row.getObject("slot_id", UUID.class), row.getObject("module_id", UUID.class), row.getString("code"),
                        row.getString("student_name"), row.getObject("start_time", OffsetDateTime.class).toInstant(),
                        row.getObject("end_time", OffsetDateTime.class).toInstant(),
                        BookingStatus.valueOf(row.getString("status")))).optional();
    }

    @Override
    public List<TutorSlot> findSlotHistoryByTutor(UUID tutorId, SlotStatus status, Optional<LocalDate> date) {
        String query = """
                SELECT s.id, s.tutor_id, s.module_id, s.start_time, s.end_time, s.status, m.code
                FROM slots s JOIN modules m ON m.id=s.module_id
                WHERE s.tutor_id=:tutor AND s.status=:status
                """ + (date.isPresent() ? " AND s.start_time>=:day_start AND s.start_time<:day_end" : "")
                + " ORDER BY s.start_time, s.id";
        var spec = jdbc.sql(query).param("tutor", tutorId).param("status", status.name());
        if (date.isPresent()) {
            var start = date.get().atStartOfDay(SINGAPORE).toInstant().atOffset(ZoneOffset.UTC);
            var end = date.get().plusDays(1).atStartOfDay(SINGAPORE).toInstant().atOffset(ZoneOffset.UTC);
            spec = spec.param("day_start", start).param("day_end", end);
        }
        return spec.query((row, index) -> new TutorSlot(
                new ConsultationSlot(row.getObject("id", UUID.class), row.getObject("tutor_id", UUID.class),
                        row.getObject("module_id", UUID.class), row.getObject("start_time", OffsetDateTime.class).toInstant(),
                        row.getObject("end_time", OffsetDateTime.class).toInstant(), SlotStatus.valueOf(row.getString("status"))),
                row.getString("code"))).list();
    }

    private static TutorBooking booking(UUID id, UUID slotId, UUID moduleId, String moduleCode,
            String studentName, java.time.Instant start, java.time.Instant end, BookingStatus status) {
        return new TutorBooking(id, slotId, moduleId, moduleCode, studentName, start, end, status);
    }
}

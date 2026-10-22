package unichope.student;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.user.Role;

/**
 * Parameterized, lock-free reads of core tables (TECH §4). Persistence only: the actor and
 * authorization are decided by {@link StudentAccess}. Never writes.
 */
public final class PostgresStudentReadRepository implements StudentReadRepository {
    private static final String BOOKABLE_SLOTS = """
            SELECT s.id, s.module_id, m.code, m.name, t.name AS tutor_name, s.start_time, s.end_time
            FROM slots s
            JOIN users t ON t.id = s.tutor_id
            JOIN modules m ON m.id = s.module_id
            WHERE s.status = 'AVAILABLE'
              AND s.start_time > :now
              AND t.active AND t.role = 'TUTOR'
              AND m.active
              AND EXISTS (SELECT 1 FROM tutor_modules tm WHERE tm.tutor_id = s.tutor_id AND tm.module_id = s.module_id)
            """;
    private static final String MODULE_FILTER = "  AND s.module_id = :moduleId\n";
    private static final String START_RANGE_FILTER = "  AND s.start_time >= :from AND s.start_time < :to\n";
    private static final String SLOT_ORDER = "ORDER BY s.start_time, s.id";
    // Owner condition only: no tutor/module eligibility filter (TECH §4, C3-ELIG-1).
    private static final String STUDENT_BOOKINGS = """
            SELECT b.id, b.slot_id, s.module_id, m.code, m.name, t.name AS tutor_name, s.start_time, s.end_time, b.status
            FROM bookings b
            JOIN slots s ON s.id = b.slot_id
            JOIN modules m ON m.id = s.module_id
            JOIN users t ON t.id = s.tutor_id
            WHERE b.student_id = :studentId
            """;
    private static final String BOOKING_ID_FILTER = "  AND b.id = :bookingId\n";
    private static final String STATUS_FILTER = "  AND b.status IN (:statuses)\n";
    private static final String BOOKING_ORDER = "ORDER BY s.start_time, b.id";

    private final JdbcClient jdbc;

    public PostgresStudentReadRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<StudentAccount> findAccount(UUID actorId) {
        return jdbc.sql("SELECT id, role, active FROM users WHERE id = :id")
                .param("id", actorId)
                .query((row, index) -> new StudentAccount(row.getObject("id", UUID.class),
                        Role.valueOf(row.getString("role")), row.getBoolean("active")))
                .optional();
    }

    @Override
    public List<ModuleOption> findActiveModules() {
        return jdbc.sql("SELECT id, code, name FROM modules WHERE active ORDER BY code, id")
                .query((row, index) -> new ModuleOption(row.getObject("id", UUID.class),
                        row.getString("code"), row.getString("name")))
                .list();
    }

    @Override
    public List<BookableSlot> findBookableSlots(Instant now, Optional<UUID> moduleId, Optional<StartRange> startRange) {
        // Only fixed SQL fragments are appended; every value is a bound parameter.
        StringBuilder sql = new StringBuilder(BOOKABLE_SLOTS);
        if (moduleId.isPresent()) sql.append(MODULE_FILTER);
        if (startRange.isPresent()) sql.append(START_RANGE_FILTER);
        sql.append(SLOT_ORDER);
        JdbcClient.StatementSpec statement = jdbc.sql(sql.toString()).param("now", utc(now));
        if (moduleId.isPresent()) statement = statement.param("moduleId", moduleId.get());
        if (startRange.isPresent()) {
            statement = statement.param("from", utc(startRange.get().from())).param("to", utc(startRange.get().to()));
        }
        return statement.query((row, index) -> new BookableSlot(
                row.getObject("id", UUID.class),
                row.getObject("module_id", UUID.class),
                row.getString("code"),
                row.getString("name"),
                row.getString("tutor_name"),
                row.getObject("start_time", OffsetDateTime.class).toInstant(),
                row.getObject("end_time", OffsetDateTime.class).toInstant())).list();
    }

    @Override
    public List<BookingRow> findStudentBookings(UUID studentId, Set<BookingStatus> statuses, Optional<StartRange> startRange) {
        // Only fixed SQL fragments are appended; every value is a bound parameter.
        StringBuilder sql = new StringBuilder(STUDENT_BOOKINGS);
        if (!statuses.isEmpty()) sql.append(STATUS_FILTER);
        if (startRange.isPresent()) sql.append(START_RANGE_FILTER);
        sql.append(BOOKING_ORDER);
        JdbcClient.StatementSpec statement = jdbc.sql(sql.toString()).param("studentId", studentId);
        if (!statuses.isEmpty()) statement = statement.param("statuses", statuses.stream().map(BookingStatus::name).sorted().toList());
        if (startRange.isPresent()) {
            statement = statement.param("from", utc(startRange.get().from())).param("to", utc(startRange.get().to()));
        }
        return statement.query((row, index) -> bookingRow(row)).list();
    }

    @Override
    public Optional<BookingRow> findStudentBooking(UUID studentId, UUID bookingId) {
        return jdbc.sql(STUDENT_BOOKINGS + BOOKING_ID_FILTER)
                .param("studentId", studentId)
                .param("bookingId", bookingId)
                .query((row, index) -> bookingRow(row))
                .optional();
    }

    private static BookingRow bookingRow(ResultSet row) throws SQLException {
        return new BookingRow(
                row.getObject("id", UUID.class),
                row.getObject("slot_id", UUID.class),
                row.getObject("module_id", UUID.class),
                row.getString("code"),
                row.getString("name"),
                row.getString("tutor_name"),
                row.getObject("start_time", OffsetDateTime.class).toInstant(),
                row.getObject("end_time", OffsetDateTime.class).toInstant(),
                BookingStatus.valueOf(row.getString("status")));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}

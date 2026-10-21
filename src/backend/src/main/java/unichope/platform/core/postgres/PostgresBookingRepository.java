package unichope.platform.core.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.repository.BookingRepository;

public final class PostgresBookingRepository implements BookingRepository {
    private static final String SELECT = "SELECT id, student_id, slot_id, created_at, status FROM bookings";
    private static final String ORDER = " ORDER BY created_at, id";
    private final JdbcClient jdbc;

    public PostgresBookingRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE id=:id").param("id", id).query(PostgresBookingRepository::map).optional();
    }

    @Override
    public List<Booking> findAll() {
        return jdbc.sql(SELECT + ORDER).query(PostgresBookingRepository::map).list();
    }

    @Override
    public List<Booking> findByStudentId(UUID studentId) {
        return jdbc.sql(SELECT + " WHERE student_id=:student" + ORDER).param("student", studentId).query(PostgresBookingRepository::map).list();
    }

    @Override
    public List<Booking> findBySlotId(UUID slotId) {
        return jdbc.sql(SELECT + " WHERE slot_id=:slot" + ORDER).param("slot", slotId).query(PostgresBookingRepository::map).list();
    }

    private static Booking map(ResultSet row, int index) throws SQLException {
        return new Booking(row.getObject("id", UUID.class), row.getObject("student_id", UUID.class),
                row.getObject("slot_id", UUID.class), row.getObject("created_at", OffsetDateTime.class).toInstant(),
                BookingStatus.valueOf(row.getString("status")));
    }
}

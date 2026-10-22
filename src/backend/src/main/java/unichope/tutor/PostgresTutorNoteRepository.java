package unichope.tutor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class PostgresTutorNoteRepository implements TutorNoteRepository {
    private final JdbcClient jdbc;

    public PostgresTutorNoteRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public Optional<TutorNote> findByBookingId(UUID bookingId) {
        return jdbc.sql("SELECT booking_id, content, version, updated_at FROM private_notes WHERE booking_id=:id")
                .param("id", bookingId).query(PostgresTutorNoteRepository::map).optional();
    }

    @Override public TutorNote insert(UUID bookingId, String content, long version) {
        return jdbc.sql("""
                INSERT INTO private_notes (booking_id, content, version, updated_at)
                VALUES (:id, :content, :version, CURRENT_TIMESTAMP)
                RETURNING booking_id, content, version, updated_at
                """).param("id", bookingId).param("content", content).param("version", version)
                .query(PostgresTutorNoteRepository::map).single();
    }

    @Override public TutorNote update(UUID bookingId, String content, long version) {
        return jdbc.sql("""
                UPDATE private_notes SET content=:content, version=:version, updated_at=CURRENT_TIMESTAMP
                WHERE booking_id=:id
                RETURNING booking_id, content, version, updated_at
                """).param("id", bookingId).param("content", content).param("version", version)
                .query(PostgresTutorNoteRepository::map).single();
    }

    private static TutorNote map(ResultSet row, int index) throws SQLException {
        return new TutorNote(row.getObject("booking_id", UUID.class), row.getString("content"),
                row.getLong("version"), row.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}

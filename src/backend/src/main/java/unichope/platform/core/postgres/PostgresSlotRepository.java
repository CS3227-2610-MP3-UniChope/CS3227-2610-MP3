package unichope.platform.core.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.repository.SlotRepository;

public final class PostgresSlotRepository implements SlotRepository {
    private static final String SELECT = "SELECT id, tutor_id, module_id, start_time, end_time, status FROM slots";
    private static final String ORDER = " ORDER BY start_time, id";
    private final JdbcClient jdbc;

    public PostgresSlotRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<ConsultationSlot> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE id=:id").param("id", id).query(PostgresSlotRepository::map).optional();
    }

    @Override
    public List<ConsultationSlot> findAll() {
        return jdbc.sql(SELECT + ORDER).query(PostgresSlotRepository::map).list();
    }

    @Override
    public List<ConsultationSlot> findByTutorId(UUID tutorId) {
        return jdbc.sql(SELECT + " WHERE tutor_id=:tutor" + ORDER).param("tutor", tutorId).query(PostgresSlotRepository::map).list();
    }

    @Override
    public List<ConsultationSlot> findByModuleId(UUID moduleId) {
        return jdbc.sql(SELECT + " WHERE module_id=:module" + ORDER).param("module", moduleId).query(PostgresSlotRepository::map).list();
    }

    private static ConsultationSlot map(ResultSet row, int index) throws SQLException {
        return new ConsultationSlot(row.getObject("id", UUID.class), row.getObject("tutor_id", UUID.class),
                row.getObject("module_id", UUID.class), row.getObject("start_time", OffsetDateTime.class).toInstant(),
                row.getObject("end_time", OffsetDateTime.class).toInstant(), SlotStatus.valueOf(row.getString("status")));
    }
}

package unichope.tutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.repository.UserRepository;

/** Persistence adapter only; trusted session identity and authorization remain service responsibilities. */
public final class PostgresTutorReadRepository implements TutorReadRepository {
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
}

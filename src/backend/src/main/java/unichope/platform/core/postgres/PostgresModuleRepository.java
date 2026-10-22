package unichope.platform.core.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import unichope.platform.core.model.module.Module;
import unichope.platform.core.model.module.TutorModule;
import unichope.platform.core.repository.ModuleRepository;

public final class PostgresModuleRepository implements ModuleRepository {
    private static final String SELECT = "SELECT id, code, name, active FROM modules";
    private final JdbcClient jdbc;

    public PostgresModuleRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Module> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE id=:id").param("id", id).query(PostgresModuleRepository::map).optional();
    }

    @Override
    public Optional<Module> findByCode(String code) {
        return jdbc.sql(SELECT + " WHERE lower(btrim(code))=:code")
                .param("code", code.strip().toLowerCase(Locale.ROOT)).query(PostgresModuleRepository::map).optional();
    }

    @Override
    public List<Module> findAll() {
        return jdbc.sql(SELECT + " ORDER BY code, id").query(PostgresModuleRepository::map).list();
    }

    @Override
    public List<TutorModule> findAssignmentsByTutor(UUID tutorId) {
        return jdbc.sql("SELECT tutor_id, module_id FROM tutor_modules WHERE tutor_id=:tutor ORDER BY module_id")
                .param("tutor", tutorId).query(PostgresModuleRepository::assignment).list();
    }

    @Override
    public List<TutorModule> findAssignments() {
        return jdbc.sql("SELECT tutor_id, module_id FROM tutor_modules ORDER BY tutor_id, module_id")
                .query(PostgresModuleRepository::assignment).list();
    }

    private static Module map(ResultSet row, int index) throws SQLException {
        return new Module(row.getObject("id", UUID.class), row.getString("code"), row.getString("name"), row.getBoolean("active"));
    }

    private static TutorModule assignment(ResultSet row, int index) throws SQLException {
        return new TutorModule(row.getObject("tutor_id", UUID.class), row.getObject("module_id", UUID.class));
    }
}

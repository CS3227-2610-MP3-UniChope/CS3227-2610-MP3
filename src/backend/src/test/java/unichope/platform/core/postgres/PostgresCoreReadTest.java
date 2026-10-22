package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.user.Role;

/** Explicit disposable PostgreSQL only; no default database and no skipped acceptance checks. */
@Tag("postgres")
class PostgresCoreReadTest {
    private static final UUID TUTOR = id(1), OTHER = id(2), STUDENT = id(3), MODULE = id(4), INACTIVE = id(5);
    private static final Instant NOW = Instant.parse("2026-10-20T00:00:00Z");
    private String url;
    private String user;
    private String password;
    private String schema;
    private JdbcClient jdbc;

    @BeforeEach
    void createIsolatedSchemaAndApplyReviewOnlyDdl() throws Exception {
        url = System.getProperty("core.test.jdbcUrl");
        assertNotNull(url, "Explicit disposable core.test.jdbcUrl is required for postgres tests");
        user = System.getProperty("core.test.user", "");
        password = System.getProperty("core.test.password", "");
        schema = "core_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.createStatement().execute("CREATE SCHEMA " + schema);
        }
        DriverManagerDataSource source = new DriverManagerDataSource(url, user, password);
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("currentSchema", schema);
        source.setConnectionProperties(properties);
        Flyway flyway = Flyway.configure().dataSource(source).defaultSchema(schema).schemas(schema)
                .locations("classpath:db/core-review").load();
        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        jdbc = JdbcClient.create(source);
        account(TUTOR, "TUTOR", true, "tutor@example.edu");
        account(OTHER, "TUTOR", true, "other@example.edu");
        account(STUDENT, "STUDENT", true, "student@example.edu");
        jdbc.sql("INSERT INTO modules VALUES (:id,'CS3227','Engineering',true)").param("id", MODULE).update();
        jdbc.sql("INSERT INTO modules VALUES (:id,'CS0000','Inactive',false)").param("id", INACTIVE).update();
        jdbc.sql("INSERT INTO tutor_modules VALUES (:tutor,:module)").param("tutor", TUTOR).param("module", MODULE).update();
        jdbc.sql("INSERT INTO tutor_modules VALUES (:tutor,:module)").param("tutor", TUTOR).param("module", INACTIVE).update();
    }

    @AfterEach
    void dropOnlyThisTestsSchema() throws Exception {
        if (schema != null) {
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                connection.createStatement().execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void coreRepositoriesPreserveIdentityRolesAssignmentsAndCaseInsensitiveLookups() {
        var users = new PostgresUserRepository(jdbc);
        assertEquals(Role.TUTOR, users.findById(TUTOR).orElseThrow().role());
        assertEquals(TUTOR, users.findByEmail("  TUTOR@EXAMPLE.EDU ").orElseThrow().id());
        assertTrue(users.findById(id(90)).isEmpty());
        var modules = new PostgresModuleRepository(jdbc);
        assertEquals(MODULE, modules.findByCode(" cs3227 ").orElseThrow().id());
        assertEquals(2, modules.findAssignmentsByTutor(TUTOR).size());
        assertEquals(2, modules.findAssignments().size());
        assertTrue(modules.findAssignmentsByTutor(OTHER).isEmpty());
        assertFalse(modules.findById(INACTIVE).orElseThrow().isActive());
        assertEquals(0L, jdbc.sql("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema=:schema AND table_name='users'
                AND column_name IN ('password_hash','must_change_password')
                """).param("schema", schema).query(Long.class).single());
        assertEquals(3, users.findAll().size());
        assertEquals(2, modules.findAll().size());
    }

    @Test
    void slotsAndBookingsRoundTripUtcAndScopeOwnersAndStudents() {
        slot(id(10), TUTOR, "2026-10-20T16:30:00Z", "BOOKED");
        slot(id(11), OTHER, "2026-10-20T17:00:00Z", "AVAILABLE");
        booking(id(20), STUDENT, id(10), "ACTIVE");
        var slots = new PostgresSlotRepository(jdbc);
        assertEquals(Instant.parse("2026-10-20T16:30:00Z"), slots.findById(id(10)).orElseThrow().startTime());
        assertEquals(List.of(id(10)), slots.findByTutorId(TUTOR).stream().map(s -> s.id()).toList());
        assertEquals(2, slots.findByModuleId(MODULE).size());
        assertEquals(2, slots.findAll().size());
        assertTrue(slots.findById(id(90)).isEmpty());
        var bookings = new PostgresBookingRepository(jdbc);
        assertEquals(BookingStatus.ACTIVE, bookings.findById(id(20)).orElseThrow().status());
        assertEquals(NOW, bookings.findById(id(20)).orElseThrow().createdAt());
        assertEquals(1, bookings.findByStudentId(STUDENT).size());
        assertEquals(1, bookings.findBySlotId(id(10)).size());
        assertEquals(1, bookings.findAll().size());
        assertTrue(bookings.findByStudentId(TUTOR).isEmpty());
        assertTrue(bookings.findById(id(90)).isEmpty());
    }

    @Test
    void ddlRejectsInvalidForeignKeysStatusTimesAndNormalizedDuplicates() {
        assertThrows(DataIntegrityViolationException.class, () -> account(id(90), "TUTOR", true, "TUTOR@EXAMPLE.EDU"));
        assertThrows(DataIntegrityViolationException.class, () -> account(id(90), "INVALID", true, "new@example.edu"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("INSERT INTO modules VALUES (:id,'cs3227','Duplicate',true)").param("id", id(90)).update());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("INSERT INTO modules VALUES (:id,' ','Blank',true)").param("id", id(90)).update());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("INSERT INTO tutor_modules VALUES (:id,:module)").param("id", id(90)).param("module", MODULE).update());
        assertThrows(DataIntegrityViolationException.class, () -> slot(id(90), id(91), NOW.toString(), "AVAILABLE"));
        assertThrows(DataIntegrityViolationException.class, () -> slot(id(90), TUTOR, NOW.toString(), "INVALID"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("INSERT INTO slots VALUES (:id,:tutor,:module,:time,:time,'AVAILABLE')").param("id", id(90)).param("tutor", TUTOR).param("module", MODULE).param("time", NOW.atOffset(ZoneOffset.UTC)).update());
        assertThrows(DataIntegrityViolationException.class, () -> booking(id(90), STUDENT, id(91), "ACTIVE"));
        slot(id(10), TUTOR, "2026-10-20T16:00:00Z", "BOOKED");
        assertThrows(DataIntegrityViolationException.class, () -> booking(id(90), id(91), id(10), "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class, () -> booking(id(90), STUDENT, id(10), "INVALID"));
    }

    @Test
    void oneActiveBookingPerSlotAllowsCancelledHistoryAndCannotDeleteReferencedUsers() {
        slot(id(10), TUTOR, "2026-10-20T16:00:00Z", "BOOKED");
        booking(id(20), STUDENT, id(10), "ACTIVE");
        booking(id(21), STUDENT, id(10), "CANCELLED");
        assertThrows(DataIntegrityViolationException.class, () -> booking(id(22), STUDENT, id(10), "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("DELETE FROM users WHERE id=:id").param("id", TUTOR).update());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("INSERT INTO tutor_modules VALUES (:tutor,:module)").param("tutor", TUTOR).param("module", MODULE).update());
    }

    private void account(UUID id, String role, boolean active, String email) {
        jdbc.sql("INSERT INTO users VALUES (:id,'Synthetic User',:email,:role,:active)").param("id", id).param("email", email).param("role", role).param("active", active).update();
    }

    private void slot(UUID id, UUID owner, String start, String status) {
        Instant time = Instant.parse(start);
        jdbc.sql("INSERT INTO slots VALUES (:id,:tutor,:module,:start,:end,:status)").param("id", id).param("tutor", owner).param("module", MODULE).param("start", time.atOffset(ZoneOffset.UTC)).param("end", time.plusSeconds(1800).atOffset(ZoneOffset.UTC)).param("status", status).update();
    }

    private void booking(UUID id, UUID student, UUID slot, String status) {
        jdbc.sql("INSERT INTO bookings VALUES (:id,:student,:slot,:created,:status)").param("id", id).param("student", student).param("slot", slot).param("created", NOW.atOffset(ZoneOffset.UTC)).param("status", status).update();
    }

    private static UUID id(int value) { return new UUID(0, value); }
}

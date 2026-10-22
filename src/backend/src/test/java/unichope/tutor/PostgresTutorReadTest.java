package unichope.tutor;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.core.postgres.PostgresUserRepository;

/** Explicit disposable PostgreSQL only; no default database and no skipped acceptance checks. */
@Tag("postgres")
class PostgresTutorReadTest {
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
    void tutorReadsUseRealAssignmentsOwnerScopeSgtDatesAndDeterministicOrder() {
        slot(id(13), TUTOR, "2026-10-20T16:30:00Z", "BOOKED");
        slot(id(12), TUTOR, "2026-10-20T16:30:00Z", "AVAILABLE");
        slot(id(14), TUTOR, "2026-10-20T15:59:59Z", "AVAILABLE");
        slot(id(15), OTHER, "2026-10-20T17:00:00Z", "AVAILABLE");
        slot(id(16), TUTOR, NOW.toString(), "AVAILABLE");
        slot(id(17), TUTOR, "2026-10-20T18:00:00Z", "CANCELLED");
        slot(id(18), TUTOR, "2026-10-20T19:00:00Z", "COMPLETED");
        var reads = tutorReads();
        assertEquals(6, reads.findSlotsByTutor(TUTOR).size());
        assertEquals("CS3227", reads.findSlotsByTutor(TUTOR).getFirst().moduleCode());
        var service = service(TUTOR);
        assertEquals(List.of(id(12), id(13)), service.upcomingSlots(LocalDate.parse("2026-10-21"))
                .stream().map(SlotView::id).toList());
        assertEquals(List.of(id(14)), service.upcomingSlots(LocalDate.parse("2026-10-20"))
                .stream().map(SlotView::id).toList());
        assertEquals(List.of(MODULE), service.activeAssignedModules().stream().map(m -> m.id()).toList());
        assertTrue(service(OTHER).activeAssignedModules().isEmpty());
        assertTrue(service.upcomingSlots(LocalDate.parse("2026-10-22")).isEmpty());
    }

    @Test
    void tutorServiceRejectsMissingWrongRoleAndFreshlyDeactivatedAccounts() {
        assertThrows(TutorAccessDeniedException.class, () -> service(STUDENT).upcomingSlots());
        assertThrows(TutorAccessDeniedException.class, () -> service(id(99)).activeAssignedModules());
        jdbc.sql("UPDATE users SET active=false WHERE id=:id").param("id", TUTOR).update();
        assertThrows(TutorAccessDeniedException.class, () -> service(TUTOR).upcomingSlots());
        assertFalse(tutorReads().findAccount(TUTOR).orElseThrow().active());
    }

    private PostgresTutorReadRepository tutorReads() {
        return new PostgresTutorReadRepository(new PostgresUserRepository(jdbc), jdbc);
    }

    private TutorReadService service(UUID actor) {
        return new TutorReadService(() -> Optional.of(actor), tutorReads(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void account(UUID id, String role, boolean active, String email) {
        jdbc.sql("INSERT INTO users VALUES (:id,'Synthetic User',:email,:role,:active)").param("id", id).param("email", email).param("role", role).param("active", active).update();
    }

    private void slot(UUID id, UUID owner, String start, String status) {
        Instant time = Instant.parse(start);
        jdbc.sql("INSERT INTO slots VALUES (:id,:tutor,:module,:start,:end,:status)").param("id", id).param("tutor", owner).param("module", MODULE).param("start", time.atOffset(ZoneOffset.UTC)).param("end", time.plusSeconds(1800).atOffset(ZoneOffset.UTC)).param("status", status).update();
    }

    private static UUID id(int value) { return new UUID(0, value); }
}

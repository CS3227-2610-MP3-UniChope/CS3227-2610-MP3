package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.UnichopeApplication;
import unichope.platform.web.ApiSecurityConfiguration;
import unichope.student.StudentTestActors;
import unichope.tutor.PostgresTutorReadRepository;

/**
 * stu-c1-browse-slots, tasks C1-T1..C1-T4: Student browse API through the real MVC and
 * Spring Security filter chain against disposable PostgreSQL (simulated authentication
 * only; no real login/session claim).
 *
 * <p>Student code is discovered by component scan of {@code unichope.student} with profile
 * {@code student-api} (as the Boot application scans {@code unichope}), and the actor seam
 * through {@link StudentTestActors}. No compile-time reference to unimplemented types exists,
 * so before implementation these tests fail on HTTP behavior rather than compilation.
 *
 * <p>Beans supplied to Student wiring: DataSource, Clock (fixture clock N=2026-10-21T00:00Z),
 * ConsultationLifecycle, core User/Module/Slot/Booking repositories, and the test-only
 * {@code unichope.platform.auth.CurrentActorProvider}. No JdbcClient bean is supplied.
 */
@Tag("postgres")
class StudentBrowsePgTest extends LifecyclePgFixture {
    private static final UUID T3 = id(6), M3 = id(12), M4 = id(13);
    private static final String MARKER = "client-request-marker";
    private static final String MODULES = "/api/student/modules", SLOTS = "/api/student/slots";
    private final JsonMapper json = JsonMapper.shared();
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void configureStudentWebContextOnRealPostgres() {
        db.sql("UPDATE users SET name=:n WHERE id=:id").param("n", "Tutor One Display").param("id", T1).update();
        db.sql("UPDATE users SET name=:n WHERE id=:id").param("n", "Tutor Two Display").param("id", T2).update();
        db.sql("UPDATE modules SET name=:n WHERE id=:id").param("n", "Software Design").param("id", M).update();
        db.sql("UPDATE modules SET name=:n WHERE id=:id").param("n", "Data Structures").param("id", M2).update();
        db.sql("INSERT INTO modules VALUES (:id,'CS12','Retired Module',false)").param("id", M3).update();
        db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t", T1).param("m", M3).update();
        context = studentContext(Set.of());
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void closeStudentWebContext() {
        if (context != null) context.close();
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- C1-T3 / C1-T4: data

    /** S-SLOT-1: only AVAILABLE, future, active-tutor, active-assigned-module slots; startTime then id. */
    @Test
    void sSlot1OnlyBookableFutureEligibleSlotsOrderedByStartThenId() throws Exception {
        Instant h1 = N.plusSeconds(3600), h2 = N.plusSeconds(7200), h3 = N.plusSeconds(10800);
        UUID later = slot(T1, M, h2, h3, "AVAILABLE");
        // Equal start: the higher id is on module CS10 and inserted first, so neither insertion
        // order nor module-code order can pass for id order.
        UUID tieHigh = slotWithId(new UUID(0, 0x200), T1, M, h1, h2, "AVAILABLE");
        UUID tieLow = slotWithId(new UUID(0, 0x100), T1, M2, h1, h2, "AVAILABLE");
        slot(T1, M, N, h1, "AVAILABLE");                          // started: start == now
        slot(T1, M, N.minusSeconds(3600), N, "AVAILABLE");        // past
        for (String status : List.of("BOOKED", "CANCELLED", "COMPLETED")) slot(T1, M, h1, h2, status);
        slot(T2, M, h1, h2, "AVAILABLE");                         // tutor becomes inactive
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", T2).update();
        slot(T1, M3, h1, h2, "AVAILABLE");                        // inactive module, still assigned
        db.sql("INSERT INTO users VALUES (:id,'Tutor Three',:e,'TUTOR',true)").param("id", T3).param("e", T3 + "@example.edu").update();
        db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t", T3).param("m", M).update();
        slot(T3, M, h1, h2, "AVAILABLE");                         // assignment removed below
        db.sql("DELETE FROM tutor_modules WHERE tutor_id=:t").param("t", T3).update();
        db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t", ADMIN).param("m", M).update();
        slot(ADMIN, M, h1, h2, "AVAILABLE");                      // owner's stored role is not TUTOR (TECH §4)

        JsonNode result = body(ok(get(SLOTS), session(S1), 200));
        assertEquals(List.of(tieLow, tieHigh, later), ids(result));
    }

    /** S-SLOT-2: SGT start-date filter (half-open SGT day), module filter, intersection, unmatched []. */
    @Test
    void sSlot2SgtDateBoundaryModuleFilterIntersectionAndUnmatched() throws Exception {
        UUID late = slot(T1, M, at("2026-10-21T15:30:00Z"), at("2026-10-21T16:00:00Z"), "AVAILABLE");       // 23:30 SGT 21st
        UUID early = slot(T1, M, at("2026-10-21T16:30:00Z"), at("2026-10-21T17:00:00Z"), "AVAILABLE");      // 00:30 SGT 22nd
        UUID other = slot(T1, M2, at("2026-10-21T17:00:00Z"), at("2026-10-21T17:30:00Z"), "AVAILABLE");     // 01:00 SGT 22nd
        UUID lastSecond = slot(T1, M, at("2026-10-22T15:59:59Z"), at("2026-10-22T16:30:00Z"), "AVAILABLE"); // 23:59:59 SGT 22nd
        UUID nextDay = slot(T1, M, at("2026-10-22T16:00:00Z"), at("2026-10-22T16:30:00Z"), "AVAILABLE");    // 00:00 SGT 23rd
        slot(T1, M3, at("2026-10-21T18:00:00Z"), at("2026-10-21T18:30:00Z"), "AVAILABLE");                  // inactive module

        assertEquals(List.of(early, other, lastSecond), ids(body(ok(get(SLOTS).param("date", "2026-10-22"), session(S1), 200))));
        assertEquals(List.of(late), ids(body(ok(get(SLOTS).param("date", "2026-10-21"), session(S1), 200))));
        assertEquals(List.of(nextDay), ids(body(ok(get(SLOTS).param("date", "2026-10-23"), session(S1), 200))));
        assertEquals(List.of(late, early, lastSecond, nextDay), ids(body(ok(get(SLOTS).param("moduleId", M.toString()), session(S1), 200))));
        assertEquals(List.of(early, lastSecond), ids(body(ok(get(SLOTS).param("moduleId", M.toString()).param("date", "2026-10-22"), session(S1), 200))));
        assertEquals(List.of(), ids(body(ok(get(SLOTS).param("moduleId", M2.toString()).param("date", "2026-10-21"), session(S1), 200))));
        assertEquals(List.of(), ids(body(ok(get(SLOTS).param("moduleId", UUID.randomUUID().toString()), session(S1), 200))));
        assertEquals(List.of(), ids(body(ok(get(SLOTS).param("moduleId", M3.toString()), session(S1), 200))));
        assertEquals(List.of(), ids(body(ok(get(SLOTS).param("date", "2026-10-25"), session(S1), 200))));
    }

    /** C1-OVL-1 (A2): a slot overlapping the student's own ACTIVE booking is still listed. */
    @Test
    void c1Ovl1SlotOverlappingOwnActiveBookingIsStillListed() throws Exception {
        UUID held = slot(T1, M, at("2026-10-22T02:00:00Z"), at("2026-10-22T03:00:00Z"), "BOOKED"); // 10:00-11:00 SGT
        booking(S1, held, "ACTIVE");
        UUID overlapping = slot(T2, M, at("2026-10-22T02:30:00Z"), at("2026-10-22T03:30:00Z"), "AVAILABLE"); // 10:30-11:30 SGT
        assertEquals(List.of(overlapping), ids(body(ok(get(SLOTS), session(S1), 200))));
        assertEquals(List.of(overlapping), ids(body(ok(get(SLOTS).param("date", "2026-10-22"), session(S1), 200))));
    }

    /** C1-OUT-1: exact BookableSlot fields, UTC Z instants, tutorName = users.name, no email in body. */
    @Test
    void c1Out1BookableSlotHasExactSafeFields() throws Exception {
        UUID id = slot(T1, M, START, END, "AVAILABLE");
        MvcResult response = ok(get(SLOTS), session(S1), 200);
        JsonNode result = body(response);
        assertEquals(1, result.size());
        JsonNode slot = result.get(0);
        assertEquals(Set.of("id", "moduleId", "moduleCode", "moduleName", "tutorName", "startTime", "endTime"),
                Set.copyOf(slot.propertyNames()));
        assertEquals(id.toString(), slot.get("id").asString());
        assertEquals(M.toString(), slot.get("moduleId").asString());
        assertEquals("CS10", slot.get("moduleCode").asString());
        assertEquals("Software Design", slot.get("moduleName").asString());
        assertEquals("Tutor One Display", slot.get("tutorName").asString());
        for (String field : List.of("startTime", "endTime")) assertTrue(slot.get(field).asString().endsWith("Z"), field);
        assertEquals(START, Instant.parse(slot.get("startTime").asString()));
        assertEquals(END, Instant.parse(slot.get("endTime").asString()));
        String text = response.getResponse().getContentAsString();
        assertFalse(text.contains("@"), "Student slot body exposes an email: " + text);
        assertFalse(text.contains(T1.toString()), "Student slot body exposes the tutor id");
    }

    /** C1-OUT-2: active modules only (assigned or not), exact { id, code, name }, code then id order, [] when none. */
    @Test
    void c1Out2ModulesAreActiveExactOrderedAndEmpty() throws Exception {
        db.sql("INSERT INTO modules VALUES (:id,'AA01','Algorithms Lab',true)").param("id", M4).update();
        JsonNode result = body(ok(get(MODULES), session(S1), 200));
        assertEquals(List.of(M4, M, M2), ids(result));
        for (JsonNode module : result) assertEquals(Set.of("id", "code", "name"), Set.copyOf(module.propertyNames()));
        assertEquals("CS10", result.get(1).get("code").asString());
        assertEquals("Software Design", result.get(1).get("name").asString());
        db.sql("UPDATE modules SET active=false").update();
        JsonNode empty = body(ok(get(MODULES), session(S1), 200));
        assertTrue(empty.isArray());
        assertEquals(0, empty.size());
    }

    /** C1-OUT-3: no-store everywhere; error envelope exact; requestId server-generated, never the client's. */
    @Test
    void c1Out3NoStoreAndServerGeneratedRequestIds() throws Exception {
        MvcResult success = ok(get(MODULES).header("X-Request-Id", MARKER), session(S1), 200);
        assertNotEquals(MARKER, success.getResponse().getHeader("X-Request-Id"));
        assertFalse(success.getResponse().getContentAsString().contains(MARKER));
        JsonNode a = error(perform(get(SLOTS).param("date", "2026-02-30").header("X-Request-Id", MARKER), session(S1)), 400, "VALIDATION_FAILED");
        JsonNode b = error(perform(get(SLOTS).param("date", "2026-02-30").header("X-Request-Id", MARKER), session(S1)), 400, "VALIDATION_FAILED");
        assertNotEquals(a.get("requestId"), b.get("requestId"));
        error(perform(get(MODULES).header("X-Request-Id", MARKER), null), 401, "UNAUTHENTICATED");
        error(perform(get(MODULES).header("X-Request-Id", MARKER), session(T1, "ROLE_STUDENT")), 403, "FORBIDDEN");
    }

    // ---------------------------------------------------------------- C1-T1: identity and access

    /** C1-ACC-1 / TECH §3.2 row 1: no principal (filter) or no provider actor (StudentAccess) → 401. */
    @Test
    void c1Acc1MissingPrincipalOrProviderActorIsUnauthenticated() throws Exception {
        for (String path : List.of(MODULES, SLOTS)) {
            error(perform(get(path), null), 401, "UNAUTHENTICATED");
            error(perform(get(path), session("not-a-provider-actor", "ROLE_STUDENT")), 401, "UNAUTHENTICATED");
        }
    }

    /** C1-ACC-2: principal not in users → 401, session invalidated, same session then 401. */
    @Test
    void c1Acc2DeletedActorIsUnauthenticatedAndSessionInvalidated() throws Exception {
        for (String path : List.of(MODULES, SLOTS)) {
            MockHttpSession original = session(id(999));
            error(perform(get(path), original), 401, "UNAUTHENTICATED");
            assertTrue(original.isInvalid(), path + " did not invalidate the deleted actor's session");
            error(perform(get(path), original), 401, "UNAUTHENTICATED");
        }
    }

    /** C1-ACC-3: student deactivated after sign-in → 403 ACCOUNT_INACTIVE, invalidated, then 401. */
    @Test
    void c1Acc3InactiveStudentIsDeniedAndSessionInvalidated() throws Exception {
        List<MockHttpSession> sessions = List.of(session(S2), session(S2));
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        List<String> paths = List.of(MODULES, SLOTS);
        for (int i = 0; i < paths.size(); i++) {
            MockHttpSession original = sessions.get(i);
            error(perform(get(paths.get(i)), original), 403, "ACCOUNT_INACTIVE");
            assertTrue(original.isInvalid(), paths.get(i) + " did not invalidate the inactive session");
            error(perform(get(paths.get(i)), original), 401, "UNAUTHENTICATED");
        }
    }

    /** C1-ACC-4: stored role wins over session authorities in both directions. */
    @Test
    void c1Acc4StoredRoleWinsOverSessionAuthorities() throws Exception {
        slot(T1, M, START, END, "AVAILABLE");
        for (String path : List.of(MODULES, SLOTS)) {
            for (UUID wrong : List.of(T1, ADMIN)) error(perform(get(path), session(wrong, "ROLE_STUDENT")), 403, "FORBIDDEN");
            ok(get(path), session(S1, "ROLE_TUTOR"), 200);
        }
    }

    /** C1-ACC-5: interleaved sessions are each resolved to their own actor on every request. */
    @Test
    void c1Acc5InterleavedSessionsResolveTheirOwnActor() throws Exception {
        slot(T1, M, START, END, "AVAILABLE");
        MockHttpSession a = session(S1), b = session(S2), tutor = session(T1, "ROLE_STUDENT");
        String first = ok(get(SLOTS), a, 200).getResponse().getContentAsString();
        ok(get(SLOTS), b, 200);
        error(perform(get(SLOTS), tutor), 403, "FORBIDDEN");
        assertEquals(first, ok(get(SLOTS), a, 200).getResponse().getContentAsString());
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        error(perform(get(SLOTS), b), 403, "ACCOUNT_INACTIVE");
        assertEquals(first, ok(get(SLOTS), a, 200).getResponse().getContentAsString());
        error(perform(get(SLOTS), tutor), 403, "FORBIDDEN");
        assertFalse(a.isInvalid());
    }

    // ---------------------------------------------------------------- C1-T4: input

    /** C1-IN-1: GET /modules accepts no query parameter at all. */
    @Test
    void c1In1ModulesRejectsAnyQueryParameter() throws Exception {
        List<MockHttpServletRequestBuilder> requests = List.of(
                get(MODULES).param("actorId", S2.toString()), get(MODULES).param("x", "1"),
                get(MODULES).param("moduleId", M.toString()), get(MODULES).param("date", "2026-10-22"),
                get(MODULES + "?flag"));
        for (var request : requests) error(perform(request, session(S1)), 400, "VALIDATION_FAILED");
    }

    /** C1-IN-2: GET /slots accepts only moduleId and date, each at most once, strictly formatted. */
    @Test
    void c1In2SlotsRejectsUnknownRepeatedAndMalformedFilters() throws Exception {
        List<MockHttpServletRequestBuilder> requests = List.of(
                get(SLOTS).param("actorId", S2.toString()),
                get(SLOTS).param("studentId", S1.toString()),
                get(SLOTS).param("date", "2026-10-22", "2026-10-22"),
                get(SLOTS).param("date", "2026-10-22", "2026-10-23"),
                get(SLOTS).param("moduleId", M.toString(), M.toString()),
                get(SLOTS).param("moduleId", "not-a-uuid"),
                get(SLOTS).param("moduleId", "1-1-1-1-1"),
                get(SLOTS).param("moduleId", ""),
                get(SLOTS).param("date", "2026-02-30"),
                get(SLOTS).param("date", "2026-1-01"),
                get(SLOTS).param("date", ""),
                get(SLOTS).param("date", "2026-10-22T00:00:00Z"));
        for (var request : requests) error(perform(request, session(S1)), 400, "VALIDATION_FAILED");
        ok(get(SLOTS).param("moduleId", M.toString()).param("date", "2026-10-22"), session(S1), 200);
    }

    /** C1-IN-3: framework errors on Student paths are safe JSON (405 with Allow, 406, 404). */
    @Test
    void c1In3FrameworkErrorsAreSafeJson() throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(put(MODULES), delete(MODULES))) {
            MvcResult result = perform(request.with(csrf().asHeader()), session(S1));
            error(result, 405, "METHOD_NOT_ALLOWED");
            String allow = result.getResponse().getHeader("Allow");
            assertNotNull(allow, "405 lacks an Allow header");
            assertTrue(allow.contains("GET"), "Allow header: " + allow);
        }
        error(perform(get(MODULES).accept("text/html"), session(S1)), 406, "NOT_ACCEPTABLE");
        for (String accept : List.of("application/json", "text/html")) {
            error(perform(get("/api/student/unknown").accept(accept), session(S1)), 404, "NOT_FOUND");
        }
    }

    /** C1-IN-4: controller errors keep their JSON code when Accept is text/html. */
    @Test
    void c1In4ControllerErrorsWithHtmlAcceptKeepJsonCodes() throws Exception {
        error(perform(get(MODULES).param("x", "1").accept("text/html"), session(S1)), 400, "VALIDATION_FAILED");
        error(perform(get(SLOTS).param("date", "2026-02-30").accept("text/html"), session(S1)), 400, "VALIDATION_FAILED");
        error(perform(get(MODULES).accept("text/html"), session(T1, "ROLE_STUDENT")), 403, "FORBIDDEN");
        MockHttpSession inactive = session(S2);
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        error(perform(get(SLOTS).accept("text/html"), inactive), 403, "ACCOUNT_INACTIVE");
        assertTrue(inactive.isInvalid());
        error(perform(get(SLOTS).accept("text/html"), inactive), 401, "UNAUTHENTICATED");
    }

    /** C1-PREC-1 (TECH §3.2 order): filter → syntax → actor; syntax errors never invalidate. */
    @Test
    void c1Prec1SyntaxErrorPrecedesActorCheckAndDoesNotInvalidate() throws Exception {
        error(perform(get(MODULES).param("x", "1"), null), 401, "UNAUTHENTICATED");
        MockHttpSession deleted = session(id(999));
        error(perform(get(MODULES).param("x", "1"), deleted), 400, "VALIDATION_FAILED");
        assertFalse(deleted.isInvalid(), "Syntax error invalidated the session");
        MockHttpSession inactive = session(S2);
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        error(perform(get(MODULES).param("x", "1"), inactive), 400, "VALIDATION_FAILED");
        error(perform(get(SLOTS).param("moduleId", "not-a-uuid"), inactive), 400, "VALIDATION_FAILED");
        assertFalse(inactive.isInvalid(), "Syntax error invalidated the session");
        error(perform(get(MODULES), inactive), 403, "ACCOUNT_INACTIVE");
        assertTrue(inactive.isInvalid());
    }

    // ---------------------------------------------------------------- C1-T2: activation and wiring

    /** C1-ACT-2 (TECH §2): student-api without provider, Clock or DataSource fails startup; with all, it serves. */
    @Test
    void c1Act2MissingRequiredBeanFailsStartupWithoutFallback() throws Exception {
        for (String omitted : List.of("provider", "clock", "dataSource")) {
            try (var incomplete = studentContext(Set.of(omitted))) {
                RuntimeException failure = assertThrows(RuntimeException.class, incomplete::refresh,
                        "student-api context started without " + omitted);
                if (omitted.equals("provider")) {
                    assertTrue(chain(failure).contains("CurrentActorProvider"), chain(failure));
                }
            }
        }
        ok(get(MODULES), session(S1), 200);
    }

    /** C1-ACT-2/3: the real Boot app with student-api and no test provider fails closed (no main-source actor). */
    @Test
    void c1Act3BootStudentProfileWithoutTestProviderFailsClosed() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            try (var boot = boot(Set.of("provider"), "student-api")) {
                assertNotNull(boot);
            }
        }, "Boot started student-api without a CurrentActorProvider: a main-source or fallback actor exists, or no Student wiring");
        assertTrue(chain(failure).contains("CurrentActorProvider"), chain(failure));
    }

    /** C1-ACT-4: both profiles together serve each role and keep unknown routes as 404 JSON. */
    @Test
    void c1Act4BothProfilesServeTutorAndStudentAndUnknownRoutesAre404Json() throws Exception {
        try (var boot = boot(Set.of(), "tutor-api", "student-api")) {
            MockMvc both = MockMvcBuilders.webAppContextSetup((WebApplicationContext) boot).apply(springSecurity()).build();
            assertEquals(200, both.perform(get("/api/tutor/modules").session(session(T1, "ROLE_TUTOR")).accept("application/json"))
                    .andReturn().getResponse().getStatus());
            MvcResult student = both.perform(get(MODULES).session(session(S1)).accept("application/json")).andReturn();
            assertEquals(200, student.getResponse().getStatus(), student.getResponse().getContentAsString());
            assertEquals("no-store", student.getResponse().getHeader("Cache-Control"));
            assertEquals(List.of(M, M2), ids(body(student)));
            for (String path : List.of("/api/tutor/unknown", "/api/student/unknown")) {
                error(both.perform(get(path).session(session(path.contains("tutor") ? T1 : S1, "ROLE_" + (path.contains("tutor") ? "TUTOR" : "STUDENT")))
                        .accept("application/json")).andReturn(), 404, "NOT_FOUND");
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** C1-ACT-4: student-api alone serves Student routes but never exposes Tutor routes. */
    @Test
    void c1Act4StudentOnlyProfileDoesNotExposeTutorRoutes() throws Exception {
        try (var boot = boot(Set.of(), "student-api")) {
            MockMvc only = MockMvcBuilders.webAppContextSetup((WebApplicationContext) boot).apply(springSecurity()).build();
            MvcResult tutor = only.perform(get("/api/tutor/modules").session(session(T1, "ROLE_TUTOR"))).andReturn();
            error(tutor, 403, "FORBIDDEN");
            assertFalse(tutor.getResponse().getContentAsString().contains("CS10"));
            MvcResult student = only.perform(get(MODULES).session(session(S1)).accept("application/json")).andReturn();
            assertEquals(200, student.getResponse().getStatus(), student.getResponse().getContentAsString());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ---------------------------------------------------------------- fixture support

    private AnnotationConfigWebApplicationContext studentContext(Set<String> omit) {
        var web = new AnnotationConfigWebApplicationContext();
        web.setServletContext(new MockServletContext());
        web.getEnvironment().setActiveProfiles("student-api");
        web.addBeanFactoryPostProcessor(factory -> registerStudentDependencies(factory, omit));
        web.register(DelegatingWebMvcConfiguration.class, ApiSecurityConfiguration.class);
        web.scan("unichope.student");
        return web;
    }

    private ConfigurableApplicationContext boot(Set<String> omit, String... profiles) {
        boolean tutor = Arrays.asList(profiles).contains("tutor-api");
        return new SpringApplicationBuilder(UnichopeApplication.class).web(WebApplicationType.SERVLET).profiles(profiles)
                .properties("server.address=127.0.0.1", "server.port=0")
                .initializers(application -> {
                    registerStudentDependencies(application.getBeanFactory(), omit);
                    if (tutor) registerTutorDependencies(application.getBeanFactory());
                }).run();
    }

    private void registerStudentDependencies(ConfigurableListableBeanFactory factory, Set<String> omit) {
        if (!omit.contains("dataSource")) factory.registerSingleton("dataSource", source);
        if (!omit.contains("clock")) factory.registerSingleton("clock", clock);
        factory.registerSingleton("consultationLifecycle", lifecycle);
        factory.registerSingleton("userReads", new PostgresUserRepository(db));
        factory.registerSingleton("moduleReads", new PostgresModuleRepository(db));
        factory.registerSingleton("slotReads", new PostgresSlotRepository(db));
        factory.registerSingleton("bookingReads", new PostgresBookingRepository(db));
        if (!omit.contains("provider")) StudentTestActors.registerSessionProvider(factory);
    }

    private void registerTutorDependencies(ConfigurableListableBeanFactory factory) {
        factory.registerSingleton("tutorReads", new PostgresTutorReadRepository(new PostgresUserRepository(db), db));
        factory.registerSingleton("currentActorProvider", (unichope.tutor.CurrentActorProvider) StudentTestActors::currentPrincipal);
    }

    private UUID slotWithId(UUID id, UUID owner, UUID module, Instant start, Instant end, String status) {
        db.sql("INSERT INTO slots VALUES (:id,:t,:m,:s,:e,:status)").param("id", id).param("t", owner).param("m", module)
                .param("s", start.atOffset(ZoneOffset.UTC)).param("e", end.atOffset(ZoneOffset.UTC)).param("status", status).update();
        return id;
    }

    private static Instant at(String instant) { return Instant.parse(instant); }

    private static String chain(Throwable failure) {
        var text = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) text.append(t).append(" | ");
        return text.toString();
    }

    private MockHttpSession session(UUID id) { return session(id, "ROLE_STUDENT"); }

    private MockHttpSession session(Object principal, String authority) {
        var session = new MockHttpSession();
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, "synthetic",
                List.of(new SimpleGrantedAuthority(authority))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        return session;
    }

    private MvcResult perform(MockHttpServletRequestBuilder request, MockHttpSession session) throws Exception {
        if (session != null) request.session(session);
        return mvc.perform(request).andReturn();
    }

    private MvcResult ok(MockHttpServletRequestBuilder request, MockHttpSession session, int status) throws Exception {
        MvcResult result = perform(request, session);
        assertEquals(status, result.getResponse().getStatus(),
                result.getResponse().getContentAsString() + " resolved=" + result.getResolvedException());
        assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));
        return result;
    }

    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }

    private static List<UUID> ids(JsonNode array) {
        assertTrue(array.isArray(), "Expected a JSON array: " + array);
        List<UUID> ids = new ArrayList<>();
        for (JsonNode item : array) ids.add(UUID.fromString(item.get("id").asString()));
        return ids;
    }

    private JsonNode error(MvcResult result, int status, String code) throws Exception {
        var response = result.getResponse();
        assertEquals(status, response.getStatus(), response.getContentAsString() + " resolved=" + result.getResolvedException());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertNotNull(response.getContentType(), "Expected safe JSON content type");
        assertTrue(response.getContentType().startsWith("application/json"), response.getContentType());
        JsonNode body = body(result);
        assertEquals(Set.of("code", "message", "requestId"), Set.copyOf(body.propertyNames()));
        assertEquals(code, body.get("code").asString());
        UUID.fromString(body.get("requestId").asString());
        String text = body.toString();
        assertFalse(text.contains(MARKER), "Client X-Request-Id echoed");
        assertFalse(text.contains("@"), "Error exposes an email");
        assertFalse(text.contains("SELECT"), "Error exposes SQL");
        return body;
    }
}

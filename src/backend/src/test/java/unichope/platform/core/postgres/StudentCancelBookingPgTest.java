package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.core.*;
import unichope.platform.core.model.consultation.*;
import unichope.platform.web.ApiSecurityConfiguration;
import unichope.student.StudentTestActors;

/** C4 seam tests: simulated UUID authentication, real PostgreSQL lifecycle and row-lock races.
 * Actual shared SessionPrincipal authentication is separately covered by StudentAuthIntegrationPgTest. */
@Tag("postgres")
class StudentCancelBookingPgTest extends LifecyclePgFixture {
    private static final String ROOT = "/api/student/bookings";
    private static final Set<String> FIELDS = Set.of("id", "slotId", "moduleId", "moduleCode", "moduleName",
            "tutorName", "startTime", "endTime", "status", "cancellable");
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    ScriptedLifecycle writes;
    FailingReads reads;

    @BeforeEach void web() {
        writes = new ScriptedLifecycle(lifecycle);
        reads = new FailingReads(source);
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles("student-api");
        context.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("dataSource", reads);
            factory.registerSingleton("clock", clock);
            factory.registerSingleton("consultationLifecycle", writes);
            StudentTestActors.registerSessionProvider(factory);
        });
        context.register(DelegatingWebMvcConfiguration.class, ApiSecurityConfiguration.class);
        context.scan("unichope.student");
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void closeWeb() { if (context != null) context.close(); SecurityContextHolder.clearContext(); }

    @Test void sCanc1CancelsAtomicallyReturnsExactProjectionReleasesSlotAndRepeatConflicts() throws Exception {
        UUID slot = slot(T1, START, END, "BOOKED"), booking = booking(S1, slot, "ACTIVE");
        JsonNode result = body(ok(cancel(booking), session(S1), 200));
        assertEquals(FIELDS, Set.copyOf(result.propertyNames()));
        assertEquals(booking.toString(), result.get("id").asString());
        assertEquals(slot.toString(), result.get("slotId").asString());
        assertEquals("CANCELLED", result.get("status").asString());
        assertFalse(result.get("cancellable").booleanValue());
        assertEquals("CANCELLED", bookingStatus(booking));
        assertEquals("AVAILABLE", slotStatus(slot));
        assertEquals(result, body(ok(get(ROOT + "/" + booking), session(S1), 200)));
        assertEquals(slot.toString(), body(ok(get("/api/student/slots"), session(S1), 200)).get(0).get("id").asString());
        String before = snapshot();
        error(perform(cancel(booking), session(S1)), 409, "INVALID_STATE");
        assertEquals(before, snapshot());
        assertEquals(2, writes.calls.get());
    }

    @Test void sCanc2ForeignMissingStartedHistoryAndUnbookedSlotNeverChangeRows() throws Exception {
        UUID foreign = booking(S2, slot(T1, START, END, "BOOKED"), "ACTIVE");
        String before = snapshot();
        JsonNode missing = error(perform(cancel(UUID.randomUUID()), session(S1)), 404, "NOT_FOUND");
        JsonNode hidden = error(perform(cancel(foreign), session(S1)), 404, "NOT_FOUND");
        assertEquals(missing.get("message"), hidden.get("message"));
        assertFalse(hidden.toString().contains(foreign.toString()));
        assertEquals(before, snapshot());
        for (String status : List.of("CANCELLED", "COMPLETED", "ACTIVE")) {
            String slotState = status.equals("ACTIVE") ? "AVAILABLE" : status.equals("COMPLETED") ? "COMPLETED" : "AVAILABLE";
            UUID id = booking(S1, slot(T1, START, END, slotState), status);
            before = snapshot();
            error(perform(cancel(id), session(S1)), 409, "INVALID_STATE");
            assertEquals(before, snapshot());
        }
        for (Instant start : List.of(N, N.minusSeconds(1))) {
            UUID id = booking(S1, slot(T1, start, END, "BOOKED"), "ACTIVE");
            before = snapshot();
            error(perform(cancel(id), session(S1)), 409, "INVALID_STATE");
            assertEquals(before, snapshot());
        }
    }

    @Test void c4Bnd1OneSecondBeforeStartIsAllowedExactStartIsConflict() throws Exception {
        UUID a = booking(S1, slot(T1, START, END, "BOOKED"), "ACTIVE");
        clock.now.set(START.minusSeconds(1));
        ok(cancel(a), session(S1), 200);
        UUID b = booking(S1, slot(T2, START, END, "BOOKED"), "ACTIVE");
        clock.now.set(START);
        String before = snapshot();
        error(perform(cancel(b), session(S1)), 409, "INVALID_STATE");
        assertEquals(before, snapshot());
    }

    @Test void c4Elig1CancellationIgnoresTutorModuleAndAssignmentIneligibility() throws Exception {
        for (String cause : List.of("tutor", "module", "assignment")) {
            UUID slot = slot(T1, START, END, "BOOKED"), id = booking(S1, slot, "ACTIVE");
            if (cause.equals("tutor")) db.sql("UPDATE users SET active=false WHERE id=:id").param("id", T1).update();
            if (cause.equals("module")) db.sql("UPDATE modules SET active=false WHERE id=:id").param("id", M).update();
            if (cause.equals("assignment")) db.sql("DELETE FROM tutor_modules WHERE tutor_id=:t AND module_id=:m").param("t", T1).param("m", M).update();
            ok(cancel(id), session(S1), 200);
            assertEquals("AVAILABLE", slotStatus(slot));
            assertEquals(0, body(ok(get("/api/student/slots"), session(S1), 200)).size());
            clearConsultations();
            db.sql("UPDATE users SET active=true WHERE id=:id").param("id", T1).update();
            db.sql("UPDATE modules SET active=true WHERE id=:id").param("id", M).update();
        }
    }

    @Test void sCanc3CompletionAndHttpCancellationRaceKeepConsistentStates() throws Exception {
        requireWorkingCancellation();
        UUID slot = slot(T1, START, END, "BOOKED"), id = booking(S1, slot, "ACTIVE");
        var outcomes = race(T1, () -> lifecycle.completeActiveBooking(T1, id), () -> perform(cancel(id), session(S1)));
        MvcResult student = (MvcResult) outcomes.get(1);
        if (student.getResponse().getStatus() == 200) {
            assertInstanceOf(LifecycleFailure.class, outcomes.get(0));
            assertEquals(LifecycleFailure.Code.INVALID_STATE, ((LifecycleFailure) outcomes.get(0)).code());
            assertEquals("CANCELLED", bookingStatus(id)); assertEquals("AVAILABLE", slotStatus(slot));
        } else {
            error(student, 409, "INVALID_STATE"); assertInstanceOf(Booking.class, outcomes.get(0));
            assertEquals("COMPLETED", bookingStatus(id)); assertEquals("COMPLETED", slotStatus(slot));
        }
        assertEquals(1, count("bookings"));
    }

    @Test void sCanc3DoubleHttpCancelHasOneSuccessAndOneConflict() throws Exception {
        requireWorkingCancellation();
        UUID slot = slot(T1, START, END, "BOOKED"), id = booking(S1, slot, "ACTIVE");
        var outcomes = race(T1, () -> perform(cancel(id), session(S1)), () -> perform(cancel(id), session(S1)));
        assertEquals(List.of(200, 409), outcomes.stream().map(MvcResult.class::cast).map(r -> r.getResponse().getStatus()).sorted().toList());
        for (Object outcome : outcomes) if (((MvcResult) outcome).getResponse().getStatus() == 409) error((MvcResult) outcome, 409, "INVALID_STATE");
        assertEquals("CANCELLED", bookingStatus(id)); assertEquals("AVAILABLE", slotStatus(slot));
    }

    @Test void c4In1ExactEmptyObjectCanonicalPathNoQueriesAndStreamedSize() throws Exception {
        UUID id = booking(S1, slot(T1, START, END, "BOOKED"), "ACTIVE");
        String before = snapshot();
        for (String bad : List.of("", " ", "null", "[]", "42", "\"x\"", "{", "{\"reason\":\"x\"}",
                "{\"studentId\":\"" + S2 + "\"}", "{\"x\":1,\"x\":2}", "{} {}", "{}x"))
            error(perform(cancelAt(id.toString(), bad), session(S1)), 400, "VALIDATION_FAILED");
        for (String bad : List.of("not-a-uuid", "1-1-1-1-1", id.toString().replace("-", "")))
            error(perform(cancelAt(bad, "{}"), session(S1)), 400, "VALIDATION_FAILED");
        for (String query : List.of("x=1", "studentId=" + S2, "flag"))
            error(perform(cancel(id).queryParam(query.contains("=") ? query.split("=")[0] : query, "x"), session(S1)), 400, "VALIDATION_FAILED");
        error(perform(post(ROOT + "/" + id + "/cancel").content("{}").with(csrf().asHeader()), session(S1)), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(perform(cancel(id).contentType("text/plain"), session(S1)), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(perform(cancel(id).content("{}" + " ".repeat(65535)).header("Content-Length", "2"), session(S1)), 413, "PAYLOAD_TOO_LARGE");
        assertEquals(before, snapshot()); assertEquals(0, writes.calls.get());
        // Isolated syntax precedes StudentAccess, while shared-auth filter ordering has its own C5 tests.
        MockHttpSession deleted = session(id(999));
        error(perform(cancelAt("not-a-uuid", "{}"), deleted), 400, "VALIDATION_FAILED"); assertFalse(deleted.isInvalid());
        error(perform(cancelAt(id.toString(), "[]"), session(T1)), 400, "VALIDATION_FAILED");
        ok(cancel(id).content("{}" + " ".repeat(65534)), session(S1), 200);
    }

    @Test void c4Acc1CsrfAndStoredActorMatrixRejectWithoutLifecycleCalls() throws Exception {
        UUID id = booking(S1, slot(T1, START, END, "BOOKED"), "ACTIVE");
        String before = snapshot();
        error(perform(post(ROOT + "/" + id + "/cancel").contentType("application/json").content("{}"), session(S1)), 403, "CSRF_INVALID");
        error(perform(post(ROOT + "/" + id + "/cancel").contentType("application/json").content("{}").with(csrf().asHeader().useInvalidToken()), session(S1)), 403, "CSRF_INVALID");
        error(perform(cancel(id), null), 401, "UNAUTHENTICATED");
        error(perform(cancel(id), session("untrusted")), 401, "UNAUTHENTICATED");
        for (UUID actor : List.of(T1, ADMIN)) { MockHttpSession s = session(actor); error(perform(cancel(id), s), 403, "FORBIDDEN"); assertFalse(s.isInvalid()); }
        MockHttpSession deleted = session(id(999)); error(perform(cancel(id), deleted), 401, "UNAUTHENTICATED"); assertTrue(deleted.isInvalid());
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        MockHttpSession inactive = session(S2); error(perform(cancel(id), inactive), 403, "ACCOUNT_INACTIVE"); assertTrue(inactive.isInvalid());
        assertEquals(before, snapshot()); assertEquals(0, writes.calls.get());
        ok(cancel(id).header("X-Actor-Id", S2.toString()), session(S1), 200);
    }

    @Test void c4Err1SafeLifecycleMappingsAndNoMutation() throws Exception {
        UUID id = booking(S1, slot(T1, START, END, "BOOKED"), "ACTIVE");
        record Case(RuntimeException failure, int status, String code) {}
        List<Case> cases = List.of(
                new Case(new LifecycleFailure(LifecycleFailure.Code.INVALID_INPUT), 400, "VALIDATION_FAILED"),
                new Case(new LifecycleFailure(LifecycleFailure.Code.TEMPORARILY_UNAVAILABLE), 503, "TEMPORARILY_UNAVAILABLE"),
                new Case(new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND), 404, "NOT_FOUND"),
                new Case(new LifecycleFailure(LifecycleFailure.Code.INVALID_STATE), 409, "INVALID_STATE"),
                new Case(new LifecycleFailure(LifecycleFailure.Code.BOOKING_OVERLAP), 500, "INTERNAL_ERROR"),
                new Case(new LifecycleFailure(LifecycleFailure.Code.MODULE_UNAVAILABLE), 500, "INTERNAL_ERROR"),
                new Case(new IllegalStateException("SELECT private-marker " + id), 500, "INTERNAL_ERROR"));
        String before = snapshot();
        for (Case c : cases) { writes.failure = c.failure(); error(perform(cancel(id), session(S1)), c.status(), c.code()); assertEquals(before, snapshot()); }
        assertEquals(cases.size(), writes.calls.get());
    }

    @Test void c4Err1AccessDeniedRechecksInactiveDeletedChangedRoleAndStillValidActor() throws Exception {
        UUID id = booking(S1, slot(T1, START, END, "BOOKED"), "ACTIVE");
        String before = snapshot(); writes.failure = new LifecycleFailure(LifecycleFailure.Code.ACCESS_DENIED);
        MockHttpSession valid = session(S1); error(perform(cancel(id), valid), 403, "FORBIDDEN"); assertFalse(valid.isInvalid());
        ok(get(ROOT), valid, 200);
        writes.before = () -> db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S1).update();
        MockHttpSession inactive = session(S1); error(perform(cancel(id), inactive), 403, "ACCOUNT_INACTIVE"); assertTrue(inactive.isInvalid());
        error(perform(cancel(id), inactive), 401, "UNAUTHENTICATED");
        writes.before = () -> db.sql("UPDATE users SET role='TUTOR' WHERE id=:id").param("id", S2).update();
        MockHttpSession changed = session(S2); error(perform(cancel(id), changed), 403, "FORBIDDEN"); assertFalse(changed.isInvalid());
        UUID other = id(998);
        db.sql("INSERT INTO users VALUES (:id,'Synthetic','other@example.edu','STUDENT',true)").param("id", other).update();
        writes.before = () -> db.sql("DELETE FROM users WHERE id=:id").param("id", other).update();
        MockHttpSession deleted = session(other); error(perform(cancel(id), deleted), 401, "UNAUTHENTICATED"); assertTrue(deleted.isInvalid());
        assertEquals(before, snapshot()); assertEquals(4, writes.calls.get());
    }

    @Test void c4Err1ProjectionFailureAfterCommitKeepsCancellationAndDoesNotRetry() throws Exception {
        UUID slot = slot(T1, START, END, "BOOKED"), id = booking(S1, slot, "ACTIVE");
        writes.after = () -> reads.fail = true;
        MvcResult result = perform(cancel(id), session(S1)); reads.fail = false;
        error(result, 500, "INTERNAL_ERROR");
        assertEquals(1, writes.calls.get()); assertEquals(1, count("bookings"));
        assertEquals("CANCELLED", bookingStatus(id)); assertEquals("AVAILABLE", slotStatus(slot));
        assertEquals("CANCELLED", body(ok(get(ROOT + "/" + id), session(S1), 200)).get("status").asString());
    }

    private void requireWorkingCancellation() throws Exception {
        UUID control = booking(S1, slot(T2, START, END, "BOOKED"), "ACTIVE");
        ok(cancel(control), session(S1), 200);
        clearConsultations();
    }
    private MockHttpServletRequestBuilder cancel(UUID id) { return cancelAt(id.toString(), "{}"); }
    private MockHttpServletRequestBuilder cancelAt(String id, String body) { return post(ROOT + "/" + id + "/cancel").contentType("application/json").content(body).with(csrf().asHeader()); }
    private MockHttpSession session(Object principal) {
        var session = new MockHttpSession(); var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, "synthetic", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security); return session;
    }
    private MvcResult perform(MockHttpServletRequestBuilder request, MockHttpSession session) throws Exception {
        if (session != null) request.session(session); return mvc.perform(request.accept("application/json")).andReturn();
    }
    private MvcResult ok(MockHttpServletRequestBuilder request, MockHttpSession session, int status) throws Exception {
        MvcResult r = perform(request, session); assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertEquals("no-store", r.getResponse().getHeader("Cache-Control")); return r;
    }
    private JsonNode body(MvcResult result) throws Exception { return JsonMapper.shared().readTree(result.getResponse().getContentAsString()); }
    private JsonNode error(MvcResult result, int status, String code) throws Exception {
        assertEquals(status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));
        assertTrue(result.getResponse().getContentType().startsWith("application/json"));
        JsonNode b = body(result); assertEquals(Set.of("code", "message", "requestId"), Set.copyOf(b.propertyNames()));
        assertEquals(code, b.get("code").asString()); UUID.fromString(b.get("requestId").asString());
        assertFalse(b.toString().contains("SELECT") || b.toString().contains("private-marker") || b.toString().contains("@")); return b;
    }
    private static final class FailingReads extends DelegatingDataSource {
        volatile boolean fail; FailingReads(DataSource source) { super(source); }
        private void check() throws SQLException { if (fail) throw new SQLException("SELECT private-marker projection unavailable"); }
        @Override public Connection getConnection() throws SQLException { check(); return super.getConnection(); }
        @Override public Connection getConnection(String u, String p) throws SQLException { check(); return super.getConnection(u, p); }
    }
    private static final class ScriptedLifecycle implements ConsultationLifecycle {
        final ConsultationLifecycle delegate; final AtomicInteger calls = new AtomicInteger();
        volatile RuntimeException failure; volatile Runnable before = () -> {}; volatile Runnable after = () -> {};
        ScriptedLifecycle(ConsultationLifecycle delegate) { this.delegate = delegate; }
        @Override public Booking cancelActiveBooking(UUID actor, UUID id) { calls.incrementAndGet(); if (failure != null) { before.run(); throw failure; } Booking b = delegate.cancelActiveBooking(actor, id); after.run(); return b; }
        @Override public Booking bookAvailableSlot(UUID a, UUID s) { return delegate.bookAvailableSlot(a,s); }
        @Override public Booking completeActiveBooking(UUID a, UUID b) { return delegate.completeActiveBooking(a,b); }
        @Override public ConsultationSlot createSlot(UUID a, UUID m, Instant s, Instant e) { return delegate.createSlot(a,m,s,e); }
        @Override public ConsultationSlot cancelAvailableSlot(UUID a, UUID s) { return delegate.cancelAvailableSlot(a,s); }
        @Override public LockedCompletedBooking lockForPrivateNote(UUID a, UUID b) { return delegate.lockForPrivateNote(a,b); }
    }
}

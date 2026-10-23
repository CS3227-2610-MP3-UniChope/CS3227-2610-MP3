package unichope.platform.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.UnichopeApplication;
import unichope.platform.ai.AiGateway;
import unichope.platform.auth.PasswordHasher;
import unichope.platform.auth.SessionPrincipal;

/** C5: actual app-api/local composition, Flyway runtime migrations, AuthController login,
 * SessionPrincipal and server-issued CSRF. No Student actor or CSRF test double. */
@Tag("postgres")
class StudentAuthIntegrationPgTest {
    static final String PASSWORD = "Synthetic-pass-2026";
    static final String ROOT = "/api/student/bookings";
    static final Set<String> BOOKING_FIELDS = Set.of("id", "slotId", "moduleId", "moduleCode", "moduleName",
            "tutorName", "startTime", "endTime", "status", "cancellable");
    ConfigurableApplicationContext context;
    DriverManagerDataSource base;
    JdbcClient db;
    String schema;
    MockMvc mvc;
    UUID student, second, tutor, admin, module, slot;
    java.util.concurrent.atomic.AtomicInteger draftCalls;
    java.util.concurrent.atomic.AtomicReference<AiGateway.Request> draftRequest;
    java.util.concurrent.atomic.AtomicReference<Runnable> afterDraft;
    record Browser(MockHttpSession session, String header, String token) {}

    @BeforeEach void setup() throws Exception {
        String url = System.getProperty("core.test.jdbcUrl"); assertNotNull(url, "Disposable PostgreSQL required");
        String user = System.getProperty("core.test.user", ""), password = System.getProperty("core.test.password", "");
        base = new DriverManagerDataSource(url, user, password);
        schema = "student_auth_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = base.getConnection(); var statement = c.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
        draftCalls = new java.util.concurrent.atomic.AtomicInteger();
        draftRequest = new java.util.concurrent.atomic.AtomicReference<>();
        afterDraft = new java.util.concurrent.atomic.AtomicReference<>(() -> {});
        AiGateway tutorFake = request -> {
            draftCalls.incrementAndGet(); draftRequest.set(request); afterDraft.get().run();
            return new AiGateway.Result("{\"summary\":\"Reviewed recursion\",\"topicsDiscussed\":\"Base cases\",\"followUp\":\"Practise examples\"}", AiGateway.ProviderMode.FAKE);
        };
        context = new SpringApplicationBuilder(UnichopeApplication.class).web(WebApplicationType.SERVLET)
                .profiles("app-api", "local").properties(Map.of("server.port", "0", "server.address", "127.0.0.1",
                        "unichope.jdbc.url", url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                        "unichope.jdbc.user", user, "unichope.jdbc.password", password, "unichope.ai.mode", "fake",
                        "unichope.ai.tutor.note-draft.max-output-tokens", "256"))
                .initializers(application -> application.getBeanFactory().registerSingleton("tutorAiTestGateway", tutorFake)).run();
        mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).apply(springSecurity()).build();
        db = JdbcClient.create(context.getBean(javax.sql.DataSource.class));
        String hash = context.getBean(PasswordHasher.class).hash(PASSWORD);
        tutor = seed("tutor", "TUTOR", hash); admin = seed("admin", "ADMIN", hash);
        register("student"); register("second");
        student = accountId("student"); second = accountId("second");
        module = UUID.randomUUID(); slot = UUID.randomUUID();
        db.sql("INSERT INTO modules VALUES(:id,'CS3227','Software Design',true)").param("id", module).update();
        db.sql("INSERT INTO tutor_modules VALUES(:t,:m)").param("t", tutor).param("m", module).update();
        Instant start = Instant.now().plusSeconds(86400);
        db.sql("INSERT INTO slots VALUES(:id,:t,:m,:s,:e,'AVAILABLE')").param("id", slot).param("t", tutor).param("m", module)
                .param("s", start.atOffset(java.time.ZoneOffset.UTC)).param("e", start.plusSeconds(3600).atOffset(java.time.ZoneOffset.UTC)).update();
    }

    @AfterEach void cleanup() throws Exception {
        SecurityContextHolder.clearContext();
        if (context != null) context.close();
        if (schema != null) try (Connection c = base.getConnection(); var statement = c.createStatement()) { statement.execute("DROP SCHEMA " + schema + " CASCADE"); }
    }

    @Test void c5Run1AppApiIncludesStudentWithRealRuntimeMigrationsAndSharedActor() throws Exception {
        assertTrue(Arrays.asList(context.getEnvironment().getActiveProfiles()).containsAll(List.of("auth-api", "tutor-api", "admin-api", "student-api")));
        // V1-V4 core/auth/notes/bootstrap, V5-V6 AI admission, V7 Tutor password change, V8 module seed.
        assertEquals(8, db.sql("SELECT count(*) FROM flyway_schema_history WHERE success").query(Integer.class).single());
        Browser b = login("student");
        assertInstanceOf(SessionPrincipal.class, authentication(b.session()).getPrincipal());
        JsonNode modules = body(ok(get("/api/student/modules"), b, 200));
        assertEquals(Set.of("id", "code", "name"), Set.copyOf(modules.get(0).propertyNames()));
        JsonNode slots = body(ok(get("/api/student/slots"), b, 200));
        assertEquals(Set.of("id", "moduleId", "moduleCode", "moduleName", "tutorName", "startTime", "endTime"), Set.copyOf(slots.get(0).propertyNames()));
        assertEquals(slot.toString(), slots.get(0).get("id").asString());
        assertEquals(0, body(ok(get(ROOT), b, 200)).size());
        error(request(get(ROOT + "/" + UUID.randomUUID()), b), 404, "NOT_FOUND");

        Browser tutorBrowser = login("tutor");
        assertInstanceOf(SessionPrincipal.class, authentication(tutorBrowser.session()).getPrincipal());
        JsonNode tutorModules = body(ok(get("/api/tutor/modules"), tutorBrowser, 200));
        assertEquals(1, tutorModules.size());
        assertEquals(module.toString(), tutorModules.get(0).get("id").asString());
        error(request(get("/api/tutor/modules"), b), 403, "FORBIDDEN");
        error(request(get("/api/student/modules"), tutorBrowser), 403, "FORBIDDEN");
    }

    @Test void p4TutorCompletionUsesRealSessionCsrfAndSharedLifecycle() throws Exception {
        JsonNode created = body(ok(book(), login("student"), 201));
        UUID booking = UUID.fromString(created.get("id").asString());
        Browser anonymous = token(null);
        error(request(complete(booking), anonymous), 401, "UNAUTHENTICATED");
        Browser studentBrowser = login("student");
        error(request(complete(booking), studentBrowser), 403, "FORBIDDEN");
        assertEquals("ACTIVE", db.sql("SELECT status FROM bookings WHERE id=:id").param("id", booking).query(String.class).single());
        assertEquals("BOOKED", db.sql("SELECT status FROM slots WHERE id=:id").param("id", slot).query(String.class).single());

        JsonNode completed = body(ok(complete(booking), login("tutor"), 200));
        assertEquals(Set.of("id", "slotId", "moduleId", "moduleCode", "studentName", "startTime", "endTime", "status"),
                Set.copyOf(completed.propertyNames()));
        assertEquals(booking.toString(), completed.get("id").asString());
        assertEquals(slot.toString(), completed.get("slotId").asString());
        assertEquals(module.toString(), completed.get("moduleId").asString());
        assertEquals("CS3227", completed.get("moduleCode").asString());
        assertEquals("student", completed.get("studentName").asString());
        assertEquals("COMPLETED", completed.get("status").asString());
        assertFalse(completed.toString().contains("@example.edu"));
        assertEquals("COMPLETED", db.sql("SELECT status FROM bookings WHERE id=:id").param("id", booking).query(String.class).single());
        assertEquals("COMPLETED", db.sql("SELECT status FROM slots WHERE id=:id").param("id", slot).query(String.class).single());
        error(request(complete(booking), login("tutor")), 409, "INVALID_STATE");
    }

    @Test void c5Auth1RegistrationLoginRotationOldCsrfFailsFreshCsrfBooksReadsAndCancels() throws Exception {
        Browser anonymous = token(null);
        String beforeId = anonymous.session().getId();
        Browser b = login("student", anonymous);
        assertNotEquals(beforeId, b.session().getId());
        assertInstanceOf(SessionPrincipal.class, authentication(b.session()).getPrincipal());
        assertEquals(student, ((SessionPrincipal) authentication(b.session()).getPrincipal()).userId());
        error(request(book().header(anonymous.header(), anonymous.token()), new Browser(b.session(), "unused", "unused")), 403, "CSRF_INVALID");
        assertEquals(0, bookingCount());
        MvcResult bookingResponse = ok(book(), b, 201);
        JsonNode created = body(bookingResponse);
        assertEquals(BOOKING_FIELDS, Set.copyOf(created.propertyNames()));
        UUID id = UUID.fromString(created.get("id").asString());
        assertEquals(ROOT + "/" + id, bookingResponse.getResponse().getHeader("Location"));
        assertEquals("ACTIVE", created.get("status").asString()); assertTrue(created.get("cancellable").booleanValue());
        assertEquals(student, db.sql("SELECT student_id FROM bookings WHERE id=:id").param("id", id).query(UUID.class).single());
        assertEquals(created, body(ok(get(ROOT + "/" + id), b, 200)));
        assertEquals(created, body(ok(get(ROOT), b, 200)).get(0));
        JsonNode cancelled = body(ok(cancel(id), b, 200));
        assertEquals(BOOKING_FIELDS, Set.copyOf(cancelled.propertyNames())); assertEquals("CANCELLED", cancelled.get("status").asString()); assertFalse(cancelled.get("cancellable").booleanValue());
        assertEquals("AVAILABLE", db.sql("SELECT status FROM slots WHERE id=:id").param("id", slot).query(String.class).single());
    }

    @Test void c5Auth2AnonymousWrongRoleCachedAuthoritiesAndUntrustedPrincipalsCannotChooseActor() throws Exception {
        error(request(get(ROOT), null), 401, "UNAUTHENTICATED");
        error(request(book(), null), 403, "CSRF_INVALID");
        Browser anon = token(null); error(request(book(), anon), 401, "UNAUTHENTICATED");
        for (String name : List.of("tutor", "admin")) {
            Browser b = login(name);
            var auth = authentication(b.session());
            var security = SecurityContextHolder.createEmptyContext();
            security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            b.session().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
            error(request(get(ROOT).header("X-Actor-Id", student), b), 403, "FORBIDDEN");
            error(request(book().header("X-Actor-Id", student), b), 403, "FORBIDDEN");
            error(request(cancel(UUID.randomUUID()), b), 403, "FORBIDDEN");
            error(request(get(ROOT).param("status", "bad"), b), 400, "VALIDATION_FAILED");
            error(request(post(ROOT).contentType("application/json").content("{}"), b), 400, "VALIDATION_FAILED");
            assertFalse(b.session().isInvalid());
        }
        for (Object principal : List.of(student, student.toString())) {
            MockHttpSession session = new MockHttpSession(); var security = SecurityContextHolder.createEmptyContext();
            security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
            error(request(get(ROOT).param("status", "bad"), new Browser(session, "unused", "unused")), 401, "UNAUTHENTICATED");
            assertTrue(session.isInvalid());
        }
        assertEquals(0, bookingCount());
    }

    @Test void c5Rev1TwoSessionsAdminDeactivationAndReactivationNeverReviveOldGeneration() throws Exception {
        Browser first = login("student"), secondSession = login("student"), administrator = login("admin");
        // Valid Student read control ensures this test cannot pass while Student is absent from app-api.
        ok(get(ROOT), first, 200);
        ok(put("/api/admin/users/" + student + "/active").contentType("application/json").content("{\"active\":false}"), administrator, 200);
        error(request(post(ROOT).contentType("application/json").content("bad"), new Browser(first.session(), "unused", "unused")), 403, "ACCOUNT_INACTIVE");
        assertTrue(first.session().isInvalid()); error(request(get(ROOT), first), 401, "UNAUTHENTICATED");
        ok(put("/api/admin/users/" + student + "/active").contentType("application/json").content("{\"active\":true}"), administrator, 200);
        error(request(get(ROOT).param("status", "bad"), secondSession), 401, "UNAUTHENTICATED"); assertTrue(secondSession.session().isInvalid());
        Browser fresh = login("student"); ok(get(ROOT), fresh, 200);
        assertEquals(0, bookingCount());
    }

    @Test void c5Auth2MissingAccountWinsMalformedSyntaxAndInvalidatesSession() throws Exception {
        Browser b = login("second"); ok(get(ROOT), b, 200);
        db.sql("DELETE FROM users WHERE id=:id").param("id", second).update();
        error(request(get(ROOT).param("status", "bad"), b), 401, "UNAUTHENTICATED"); assertTrue(b.session().isInvalid());
        error(request(get(ROOT), b), 401, "UNAUTHENTICATED"); assertEquals(0, bookingCount());
    }

    @Test void p6TutorDraftUsesRealSessionAndRechecksGenerationAfterProviderReturns() throws Exception {
        UUID booking = completedBooking();
        error(request(draft(booking), token(null)), 401, "UNAUTHENTICATED");
        error(request(draft(booking), login("student")), 403, "FORBIDDEN");
        assertEquals(0, draftCalls.get());
        Browser browser = login("tutor");
        var response = ok(draft(booking), browser, 200);
        JsonNode result = body(response);
        assertEquals(Set.of("bookingId", "draft", "requiresReview", "providerMode"), Set.copyOf(result.propertyNames()));
        assertEquals(booking.toString(), result.get("bookingId").asString());
        assertTrue(result.get("draft").asString().contains("Reviewed recursion"));
        assertTrue(result.get("requiresReview").booleanValue()); assertEquals("fake", result.get("providerMode").asString());
        assertEquals(1, draftCalls.get()); assertEquals(tutor, draftRequest.get().actorId());
        assertEquals("tutor.note-draft.v1", draftRequest.get().task());
        assertEquals("Reviewed recursion", draftRequest.get().input()); assertEquals(256, draftRequest.get().maxOutputTokens());
        assertEquals(0, db.sql("SELECT count(*) FROM private_notes").query(Long.class).single());

        afterDraft.set(() -> db.sql("UPDATE users SET session_generation=session_generation+1 WHERE id=:id").param("id", tutor).update());
        MvcResult revoked = request(draft(booking), browser);
        error(revoked, 401, "UNAUTHENTICATED");
        assertTrue(browser.session().isInvalid());
        assertFalse(revoked.getResponse().getContentAsString().contains("Reviewed recursion"));
        assertEquals(2, draftCalls.get());
        assertEquals(0, db.sql("SELECT count(*) FROM private_notes").query(Long.class).single());
    }

    @Test void p6TutorDraftDiscardsOutputAndInvalidatesSessionWhenAccountIsDeactivatedDuringCall() throws Exception {
        UUID booking = completedBooking();
        Browser browser = login("tutor");
        afterDraft.set(() -> db.sql("UPDATE users SET active=false,session_generation=session_generation+1 WHERE id=:id").param("id", tutor).update());

        MvcResult response = request(draft(booking), browser);

        error(response, 403, "ACCOUNT_INACTIVE"); assertTrue(browser.session().isInvalid());
        assertFalse(response.getResponse().getContentAsString().contains("Reviewed recursion"));
        assertEquals(1, draftCalls.get());
        assertEquals(0, db.sql("SELECT count(*) FROM private_notes").query(Long.class).single());
    }

    @Test void c5Csrf1MissingInvalidTokensLogoutCookieRepeatAndFreshLogin() throws Exception {
        Browser b = login("student");
        JsonNode created = body(ok(book(), b, 201)); UUID id = UUID.fromString(created.get("id").asString());
        String before = snapshot();
        Browser missing = new Browser(b.session(), "unused", "unused");
        error(request(book(), missing), 403, "CSRF_INVALID"); error(request(cancel(id), missing), 403, "CSRF_INVALID");
        Browser invalid = new Browser(b.session(), b.header(), "invalid");
        error(request(book(), invalid), 403, "CSRF_INVALID"); error(request(cancel(id), invalid), 403, "CSRF_INVALID");
        assertEquals(before, snapshot());
        MvcResult logout = ok(post("/api/auth/logout"), b, 204); assertTrue(b.session().isInvalid());
        var cookie = logout.getResponse().getCookie("JSESSIONID"); assertNotNull(cookie); assertEquals(0, cookie.getMaxAge()); assertTrue(cookie.isHttpOnly());
        error(request(cancel(id), b), 403, "CSRF_INVALID"); assertEquals(before, snapshot());
        Browser anon = token(null); ok(post("/api/auth/logout"), anon, 204);
        Browser newAnon = token(null), fresh = login("student", newAnon);
        error(request(cancel(id).header(newAnon.header(), newAnon.token()), new Browser(fresh.session(), "unused", "unused")), 403, "CSRF_INVALID");
        ok(cancel(id), fresh, 200);
    }

    @Test void c5State1LogoutThenDifferentLoginRetainsNoActorOrBookingState() throws Exception {
        Browser first = login("student"); UUID id = UUID.fromString(body(ok(book().header("X-Actor-Id", second), first, 201)).get("id").asString());
        ok(post("/api/auth/logout"), first, 204);
        Browser other = login("second");
        assertEquals(0, body(ok(get(ROOT).header("X-Actor-Id", student), other, 200)).size());
        JsonNode missing = error(request(get(ROOT + "/" + UUID.randomUUID()), other), 404, "NOT_FOUND");
        JsonNode foreign = error(request(get(ROOT + "/" + id).header("X-Actor-Id", student), other), 404, "NOT_FOUND");
        assertEquals(missing.get("message"), foreign.get("message"));
        JsonNode cancelForeign = error(request(cancel(id).header("X-Actor-Id", student), other), 404, "NOT_FOUND");
        JsonNode cancelMissing = error(request(cancel(UUID.randomUUID()), other), 404, "NOT_FOUND");
        assertEquals(cancelMissing.get("message"), cancelForeign.get("message"));
        assertEquals("ACTIVE", db.sql("SELECT status FROM bookings WHERE id=:id").param("id", id).query(String.class).single());
        assertEquals(1, body(ok(get(ROOT), login("student"), 200)).size());
    }

    private UUID seed(String name, String role, String hash) {
        UUID id = UUID.randomUUID(); db.sql("INSERT INTO users(id,name,email,role,active,password_hash) VALUES(:id,:n,:e,:r,true,:h)")
                .param("id", id).param("n", name).param("e", name + "@example.edu").param("r", role).param("h", hash).update(); return id;
    }
    private UUID accountId(String name) { return db.sql("SELECT id FROM users WHERE email=:e").param("e", name + "@example.edu").query(UUID.class).single(); }
    private void register(String name) throws Exception {
        Browser b = token(null); ok(post("/api/auth/register").contentType("application/json").content("{\"name\":\"" + name + "\",\"email\":\"" + name + "@example.edu\",\"password\":\"" + PASSWORD + "\",\"confirmation\":\"" + PASSWORD + "\"}"), b, 201);
        error(request(get("/api/auth/me"), b), 401, "UNAUTHENTICATED");
    }
    private Browser token(MockHttpSession session) throws Exception {
        MvcResult result = request(get("/api/auth/csrf"), session == null ? null : new Browser(session, "unused", "unused"));
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        JsonNode b = body(result); return new Browser((MockHttpSession) result.getRequest().getSession(false), b.get("headerName").asString(), b.get("token").asString());
    }
    private Browser login(String name) throws Exception { return login(name, token(null)); }
    private Browser login(String name, Browser b) throws Exception {
        MvcResult result = ok(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + name + "@example.edu\",\"password\":\"" + PASSWORD + "\"}"), b, 200);
        assertEquals(Set.of("id", "name", "role", "mustChangePassword"), Set.copyOf(body(result).propertyNames())); return token((MockHttpSession) result.getRequest().getSession(false));
    }
    private org.springframework.security.core.Authentication authentication(MockHttpSession session) {
        return ((org.springframework.security.core.context.SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).getAuthentication();
    }
    private MockHttpServletRequestBuilder book() { return post(ROOT).contentType("application/json").content("{\"slotId\":\"" + slot + "\"}"); }
    private MockHttpServletRequestBuilder draft(UUID booking) {
        return post("/api/tutor/bookings/" + booking + "/note-draft").contentType("application/json").content("{\"roughNotes\":\"Reviewed recursion\"}");
    }
    private MockHttpServletRequestBuilder complete(UUID booking) {
        return post("/api/tutor/bookings/" + booking + "/complete").contentType("application/json").content("{}");
    }
    private MockHttpServletRequestBuilder cancel(UUID id) { return post(ROOT + "/" + id + "/cancel").contentType("application/json").content("{}"); }
    private UUID completedBooking() {
        db.sql("UPDATE slots SET status='COMPLETED' WHERE id=:id").param("id", slot).update();
        UUID id = UUID.randomUUID();
        db.sql("INSERT INTO bookings(id,student_id,slot_id,created_at,status) VALUES(:id,:student,:slot,:created,'COMPLETED')")
                .param("id", id).param("student", student).param("slot", slot)
                .param("created", Instant.now().minusSeconds(7200).atOffset(java.time.ZoneOffset.UTC)).update();
        return id;
    }
    private MvcResult request(MockHttpServletRequestBuilder request, Browser browser) throws Exception {
        if (browser != null && !browser.session().isInvalid()) { request.session(browser.session()); if (!browser.header().equals("unused")) request.header(browser.header(), browser.token()); }
        else if (browser != null && !browser.header().equals("unused")) request.header(browser.header(), browser.token());
        return mvc.perform(request.accept("application/json")).andReturn();
    }
    private MvcResult ok(MockHttpServletRequestBuilder request, Browser browser, int status) throws Exception {
        MvcResult r = request(request, browser); assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString()); assertEquals("no-store", r.getResponse().getHeader("Cache-Control")); return r;
    }
    private JsonNode body(MvcResult r) throws Exception { return JsonMapper.shared().readTree(r.getResponse().getContentAsString()); }
    private JsonNode error(MvcResult r, int status, String code) throws Exception {
        assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString()); assertEquals("no-store", r.getResponse().getHeader("Cache-Control"));
        JsonNode b = body(r); assertEquals(Set.of("code", "message", "requestId"), Set.copyOf(b.propertyNames())); assertEquals(code, b.get("code").asString()); UUID.fromString(b.get("requestId").asString());
        for (String leak : List.of("@", "SELECT", "password", "Software Design")) assertFalse(b.toString().contains(leak), b.toString());
        for (UUID id : Arrays.asList(student, tutor, slot)) if (id != null) assertFalse(b.toString().contains(id.toString()), b.toString()); return b;
    }
    private long bookingCount() { return db.sql("SELECT count(*) FROM bookings").query(Long.class).single(); }
    private String snapshot() { return db.sql("SELECT row_to_json(b)::text FROM bookings b ORDER BY id").query(String.class).list().toString() + db.sql("SELECT row_to_json(s)::text FROM slots s ORDER BY id").query(String.class).list(); }
}

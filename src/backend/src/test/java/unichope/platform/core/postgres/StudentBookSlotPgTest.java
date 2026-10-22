package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DelegatingDataSource;
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
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.UnichopeApplication;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.LockedCompletedBooking;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.web.ApiSecurityConfiguration;
import unichope.student.StudentTestActors;

/**
 * stu-c2-book-slot, task C2-T1: {@code POST /api/student/bookings} through the real MVC and Spring
 * Security filter chain against disposable PostgreSQL (simulated authentication only; no real
 * login/session claim). Also covers the stu-c1 carry-forward R1-05 / note N3: with {@code student-api}
 * active, startup without a {@code ConsultationLifecycle} bean fails (TECH §2).
 *
 * <p>Same discovery contract as stu-c1 and stu-c3: Student code is found by component scan of
 * {@code unichope.student} with profile {@code student-api}, and the actor seam through
 * {@link StudentTestActors}. There is no compile-time reference to any Student product type, so before
 * implementation these tests fail on HTTP behavior or startup outcome, not on compilation.
 *
 * <p>Beans supplied to Student wiring are those of stu-c1/stu-c3, with two test-only wrappers:
 * {@code dataSource} is a {@link SwitchableDataSource} over the fixture source (it can fail new
 * connections after a successful booking, for C2-ERR-3), and {@code consultationLifecycle} is a
 * {@link ScriptedLifecycle} that delegates to the real {@code PostgresConsultationLifecycle} unless a
 * test scripts a failure (C2-ERR-1/2). The real lifecycle uses the fixture source directly, so the
 * fixture {@code race}/{@code awaitBlocked} helpers observe its row locks (S-BOOK-4). Core
 * User/Module/Slot/Booking repositories read through the same switchable source.
 */
@Tag("postgres")
class StudentBookSlotPgTest extends LifecyclePgFixture {
    private static final UUID S3 = id(7), M3 = id(12), M4 = id(13);
    private static final String BOOKINGS = "/api/student/bookings";
    private static final String UNAVAILABLE_MESSAGE = "This slot is no longer available. Refresh the list and choose another.";
    private static final String OVERLAP_MESSAGE = "This slot overlaps one of your active bookings.";
    private static final String STUDENT_ONE_NAME = "Student Alpha Name";
    private static final String MARKER = "private-marker";
    private static final String CLIENT_MARKER = "client-sensitive-marker";
    private static final int MAX_BODY = 65536;
    private static final Set<String> BOOKING_FIELDS = Set.of("id", "slotId", "moduleId", "moduleCode", "moduleName",
            "tutorName", "startTime", "endTime", "status", "cancellable");
    private final JsonMapper json = JsonMapper.shared();
    private ScriptedLifecycle writes;
    private SwitchableDataSource reads;
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void configureStudentWebContextOnRealPostgres() {
        db.sql("UPDATE users SET name=:n WHERE id=:id").param("n", "Tutor One Display").param("id", T1).update();
        db.sql("UPDATE users SET name=:n WHERE id=:id").param("n", "Tutor Two Display").param("id", T2).update();
        db.sql("UPDATE users SET name=:n WHERE id=:id").param("n", STUDENT_ONE_NAME).param("id", S1).update();
        db.sql("UPDATE users SET name=:n WHERE id=:id").param("n", "Student Beta Name").param("id", S2).update();
        db.sql("UPDATE modules SET name=:n WHERE id=:id").param("n", "Software Design").param("id", M).update();
        db.sql("UPDATE modules SET name=:n WHERE id=:id").param("n", "Data Structures").param("id", M2).update();
        writes = new ScriptedLifecycle(lifecycle);
        reads = new SwitchableDataSource(source);
        context = studentContext(Set.of());
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void closeStudentWebContext() {
        if (context != null) context.close();
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- success, rejection, overlap, rebooking

    /**
     * S-BOOK-1 (and A1, no enrolment: fixture students have no module relationship): 201, a followable
     * Location, an exact StudentBooking (ACTIVE, cancellable), one ACTIVE booking for the session student and
     * the slot BOOKED; the booking appears in My bookings and stays hidden from another student.
     */
    @Test
    void sBook1BooksEligibleSlotWithLocationExactProjectionAndAtomicRows() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        UUID untouched = slot(T2, M2, END, END.plusSeconds(1800), "AVAILABLE");

        MvcResult created = ok(book(slot), session(S1), 201);
        JsonNode booking = body(created);
        exactBooking(booking);
        UUID id = UUID.fromString(booking.get("id").asString());
        String location = created.getResponse().getHeader("Location");
        assertEquals(BOOKINGS + "/" + id, location);
        assertEquals(slot.toString(), booking.get("slotId").asString());
        assertEquals(M.toString(), booking.get("moduleId").asString());
        assertEquals("CS10", booking.get("moduleCode").asString());
        assertEquals("Software Design", booking.get("moduleName").asString());
        assertEquals("Tutor One Display", booking.get("tutorName").asString());
        assertEquals(START, Instant.parse(booking.get("startTime").asString()));
        assertEquals(END, Instant.parse(booking.get("endTime").asString()));
        assertEquals("ACTIVE", booking.get("status").asString());
        assertTrue(booking.get("cancellable").booleanValue());
        String text = created.getResponse().getContentAsString();
        assertFalse(text.contains("@"), "Booking response exposes an email: " + text);
        assertFalse(text.contains(STUDENT_ONE_NAME), "Booking response exposes the student name");
        assertFalse(text.contains(S1.toString()), "Booking response exposes the student id");
        assertFalse(text.contains(T1.toString()), "Booking response exposes the tutor id");

        assertEquals(booking, body(ok(get(location), session(S1), 200)), "Location does not show the created booking");
        assertEquals(List.of(id), ids(body(ok(get(BOOKINGS), session(S1), 200))), "New booking missing from My bookings");
        error(perform(get(location), session(S2)), 404, "NOT_FOUND");

        assertEquals(1, count("bookings"));
        assertEquals(S1, db.sql("SELECT student_id FROM bookings WHERE id=:id").param("id", id).query(UUID.class).single());
        assertEquals(slot, db.sql("SELECT slot_id FROM bookings WHERE id=:id").param("id", id).query(UUID.class).single());
        assertEquals("ACTIVE", bookingStatus(id));
        assertEquals("BOOKED", slotStatus(slot));
        assertEquals("AVAILABLE", slotStatus(untouched));
        assertEquals(1, writes.bookCalls.get(), "Expected exactly one lifecycle booking call");
    }

    /**
     * S-BOOK-2 (A4): BOOKED, CANCELLED, COMPLETED, started (at now and earlier), nonexistent, inactive tutor,
     * inactive module and removed assignment all give 409 SLOT_UNAVAILABLE with one message and change nothing.
     */
    @Test
    void sBook2EveryUnavailableCauseSharesOneConflictAndChangesNothing() throws Exception {
        db.sql("INSERT INTO modules VALUES (:id,'CS12','Inactive Module',false)").param("id", M3).update();
        db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t", T1).param("m", M3).update();
        db.sql("INSERT INTO modules VALUES (:id,'CS13','Unassigned Module',true)").param("id", M4).update();
        Map<String, UUID> causes = new LinkedHashMap<>();
        UUID booked = slot(T1, M, START, END, "BOOKED");
        booking(S2, booked, "ACTIVE");
        causes.put("BOOKED by another student", booked);
        causes.put("CANCELLED", slot(T1, M, END, END.plusSeconds(1800), "CANCELLED"));
        causes.put("COMPLETED", slot(T1, M, N.minusSeconds(7200), N.minusSeconds(3600), "COMPLETED"));
        causes.put("started exactly now", slot(T1, M, N, N.plusSeconds(1800), "AVAILABLE"));
        causes.put("started earlier", slot(T1, M, N.minusSeconds(1800), N.plusSeconds(1800), "AVAILABLE"));
        causes.put("nonexistent", UUID.randomUUID());
        causes.put("inactive tutor", slot(T2, M, START, END, "AVAILABLE"));
        causes.put("inactive module", slot(T1, M3, START, END, "AVAILABLE"));
        causes.put("removed assignment", slot(T1, M4, START, END, "AVAILABLE"));
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", T2).update();

        String before = snapshot();
        MockHttpSession student = session(S1);
        Set<String> messages = new HashSet<>();
        for (Map.Entry<String, UUID> cause : causes.entrySet()) {
            MvcResult result = perform(book(cause.getValue()), student);
            JsonNode failure = error(result, 409, "SLOT_UNAVAILABLE");
            messages.add(failure.get("message").asString());
            assertEquals(UNAVAILABLE_MESSAGE, failure.get("message").asString(), cause.getKey());
            assertFalse(result.getResponse().getContentAsString().contains(cause.getValue().toString()),
                    cause.getKey() + ": conflict body echoes the slot id");
            assertEquals(before, snapshot(), cause.getKey() + ": a rejected booking changed rows");
        }
        assertEquals(1, messages.size(), "Unavailable causes must share one message: " + messages);
        assertFalse(student.isInvalid(), "A booking conflict must not invalidate the session");

        UUID eligible = slot(T1, M2, END.plusSeconds(3600), END.plusSeconds(7200), "AVAILABLE");
        ok(book(eligible), student, 201);
        assertEquals("BOOKED", slotStatus(eligible));
    }

    /**
     * S-BOOK-3: an ACTIVE 10:00-11:00 SGT booking blocks 10:30-11:30, a contained and an enclosing slot of
     * another tutor (409 BOOKING_OVERLAP, no change); the adjacent 11:00-12:00 and 09:00-10:00 slots book
     * (half-open). Only the student's own ACTIVE bookings count.
     */
    @Test
    void sBook3OverlapWithOwnActiveBookingIsRejectedAndAdjacentIsAllowed() throws Exception {
        UUID held = slot(T1, M, sgt(10, 0), sgt(11, 0), "BOOKED");
        booking(S1, held, "ACTIVE");
        UUID overlapping = slot(T2, M2, sgt(10, 30), sgt(11, 30), "AVAILABLE");
        UUID inside = slot(T2, M, sgt(10, 15), sgt(10, 45), "AVAILABLE");
        UUID enclosing = slot(T2, M2, sgt(9, 30), sgt(11, 30), "AVAILABLE");
        String before = snapshot();
        for (UUID candidate : List.of(overlapping, inside, enclosing)) {
            JsonNode failure = error(perform(book(candidate), session(S1)), 409, "BOOKING_OVERLAP");
            assertEquals(OVERLAP_MESSAGE, failure.get("message").asString());
            assertEquals(before, snapshot(), "An overlapping booking attempt changed rows");
        }

        UUID after = slot(T2, M2, sgt(11, 0), sgt(12, 0), "AVAILABLE");
        UUID beforeSlot = slot(T2, M, sgt(9, 0), sgt(10, 0), "AVAILABLE");
        ok(book(after), session(S1), 201);
        ok(book(beforeSlot), session(S1), 201);

        // A CANCELLED booking of the student and another student's ACTIVE booking never block.
        booking(S1, slot(T1, M, sgt(14, 0), sgt(15, 0), "AVAILABLE"), "CANCELLED");
        booking(S2, slot(T1, M2, sgt(16, 0), sgt(17, 0), "BOOKED"), "ACTIVE");
        UUID overCancelled = slot(T2, M, sgt(14, 30), sgt(15, 30), "AVAILABLE");
        UUID overOthers = slot(T2, M2, sgt(16, 30), sgt(17, 30), "AVAILABLE");
        ok(book(overCancelled), session(S1), 201);
        ok(book(overOthers), session(S1), 201);

        assertEquals(Set.of(held, after, beforeSlot, overCancelled, overOthers), activeSlotsOf(S1));
        for (UUID rejected : List.of(overlapping, inside, enclosing)) assertEquals("AVAILABLE", slotStatus(rejected));
    }

    /**
     * C2-PREC-1 (F2-08): a slot that is both no longer available and overlapping gives 409 SLOT_UNAVAILABLE,
     * not BOOKING_OVERLAP. The free overlapping control shows that overlap is detected for this interval.
     */
    @Test
    void c2Prec1UnavailableWinsOverOverlap() throws Exception {
        db.sql("INSERT INTO modules VALUES (:id,'CS12','Inactive Module',false)").param("id", M3).update();
        db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t", T2).param("m", M3).update();
        UUID held = slot(T1, M, sgt(10, 0), sgt(11, 0), "BOOKED");
        booking(S1, held, "ACTIVE");
        UUID takenOverlapping = slot(T2, M2, sgt(10, 30), sgt(11, 30), "BOOKED");
        booking(S2, takenOverlapping, "ACTIVE");
        UUID inactiveModuleOverlapping = slot(T2, M3, sgt(10, 30), sgt(11, 30), "AVAILABLE");
        UUID freeOverlapping = slot(T2, M, sgt(10, 45), sgt(11, 15), "AVAILABLE");
        String before = snapshot();

        for (UUID candidate : List.of(takenOverlapping, inactiveModuleOverlapping)) {
            JsonNode failure = error(perform(book(candidate), session(S1)), 409, "SLOT_UNAVAILABLE");
            assertEquals(UNAVAILABLE_MESSAGE, failure.get("message").asString());
            assertEquals(before, snapshot());
        }
        error(perform(book(freeOverlapping), session(S1)), 409, "BOOKING_OVERLAP");
        assertEquals(before, snapshot());
    }

    /**
     * S-BOOK-4 (same slot): S1 and S2 book one T1 slot while T1's user row is held, so both requests block
     * inside the lifecycle on real PostgreSQL row locks; after release exactly one 201 and one 409
     * SLOT_UNAVAILABLE, one ACTIVE booking owned by the winner, and the slot BOOKED.
     */
    @Test
    void sBook4SameSlotRaceHasExactlyOneWinner() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        MockHttpSession first = session(S1), second = session(S2);
        List<Object> outcomes = race(T1, () -> perform(book(slot), first), () -> perform(book(slot), second));
        MvcResult a = (MvcResult) outcomes.get(0), b = (MvcResult) outcomes.get(1);
        assertEquals(Set.of(201, 409), Set.of(a.getResponse().getStatus(), b.getResponse().getStatus()),
                "Expected exactly one 201 and one 409: " + a.getResponse().getContentAsString() + " / " + b.getResponse().getContentAsString());
        MvcResult winner = a.getResponse().getStatus() == 201 ? a : b;
        MvcResult loser = winner == a ? b : a;
        UUID winningStudent = winner == a ? S1 : S2;
        JsonNode created = body(winner);
        exactBooking(created);
        assertEquals(UNAVAILABLE_MESSAGE, error(loser, 409, "SLOT_UNAVAILABLE").get("message").asString());

        assertEquals(1, count("bookings"));
        assertEquals(1, db.sql("SELECT count(*) FROM bookings WHERE slot_id=:s AND status='ACTIVE'").param("s", slot).query(Long.class).single());
        assertEquals(winningStudent, db.sql("SELECT student_id FROM bookings WHERE slot_id=:s").param("s", slot).query(UUID.class).single());
        assertEquals(created.get("id").asString(), db.sql("SELECT id FROM bookings WHERE slot_id=:s").param("s", slot).query(UUID.class).single().toString());
        assertEquals("BOOKED", slotStatus(slot));
        assertEquals(2, writes.bookCalls.get(), "Both racing requests must reach the lifecycle");
    }

    /**
     * S-BOOK-4 (overlap): S1 books two overlapping slots of T1 and T2 while S1's user row is held; after
     * release exactly one 201 and one 409 BOOKING_OVERLAP, one ACTIVE booking, the other slot AVAILABLE.
     */
    @Test
    void sBook4OverlappingSlotsRaceForOneStudentHasExactlyOneWinner() throws Exception {
        UUID slotA = slot(T1, M, sgt(10, 0), sgt(11, 0), "AVAILABLE");
        UUID slotB = slot(T2, M2, sgt(10, 30), sgt(11, 30), "AVAILABLE");
        MockHttpSession first = session(S1), second = session(S1);
        List<Object> outcomes = race(S1, () -> perform(book(slotA), first), () -> perform(book(slotB), second));
        MvcResult a = (MvcResult) outcomes.get(0), b = (MvcResult) outcomes.get(1);
        assertEquals(Set.of(201, 409), Set.of(a.getResponse().getStatus(), b.getResponse().getStatus()),
                "Expected exactly one 201 and one 409: " + a.getResponse().getContentAsString() + " / " + b.getResponse().getContentAsString());
        boolean aWon = a.getResponse().getStatus() == 201;
        exactBooking(body(aWon ? a : b));
        assertEquals(OVERLAP_MESSAGE, error(aWon ? b : a, 409, "BOOKING_OVERLAP").get("message").asString());

        assertEquals(Set.of(aWon ? slotA : slotB), activeSlotsOf(S1));
        assertEquals(1, count("bookings"));
        assertEquals("BOOKED", slotStatus(aWon ? slotA : slotB));
        assertEquals("AVAILABLE", slotStatus(aWon ? slotB : slotA));
        assertEquals(2, writes.bookCalls.get(), "Both racing requests must reach the lifecycle");
    }

    /**
     * S-BOOK-5: after the student's booking is cancelled (core lifecycle) and the slot is AVAILABLE again, the
     * student books it again: 201 with a new ID, the CANCELLED booking remains and both show in history. Another
     * student can later book it too, keeping all history.
     */
    @Test
    void sBook5RebookingAfterCancellationCreatesNewBookingAndKeepsHistory() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        UUID first = UUID.fromString(body(ok(book(slot), session(S1), 201)).get("id").asString());
        lifecycle.cancelActiveBooking(S1, first);
        assertEquals("AVAILABLE", slotStatus(slot));

        JsonNode again = body(ok(book(slot), session(S1), 201));
        exactBooking(again);
        UUID second = UUID.fromString(again.get("id").asString());
        assertNotEquals(first, second, "Rebooking must create a new booking");
        assertEquals("ACTIVE", again.get("status").asString());
        assertEquals("CANCELLED", bookingStatus(first));
        assertEquals("ACTIVE", bookingStatus(second));
        assertEquals("BOOKED", slotStatus(slot));
        assertEquals(2, count("bookings"));
        assertEquals(Set.of(first, second), Set.copyOf(ids(body(ok(get(BOOKINGS), session(S1), 200)))));

        lifecycle.cancelActiveBooking(S1, second);
        UUID third = UUID.fromString(body(ok(book(slot), session(S2), 201)).get("id").asString());
        assertEquals(3, count("bookings"));
        assertEquals("ACTIVE", bookingStatus(third));
        assertEquals(Set.of(first, second), Set.copyOf(ids(body(ok(get(BOOKINGS), session(S1), 200)))));
    }

    // ---------------------------------------------------------------- input, identity and access

    /**
     * C2-IN-1: non-object, empty, malformed, wrongly typed or non-canonical slotId, extra fields (studentId,
     * status, role, actorId), renamed fields, duplicate keys, trailing tokens, and any query parameter give 400
     * VALIDATION_FAILED with no lifecycle call and no change. Each body names a bookable slot, so ignoring
     * extra fields would book. TECH §3.2 order: filter denials first, then syntax, then the actor check.
     */
    @Test
    void c2In1RejectsEveryNonExactBodyAndAnyQueryBeforeTheActorCheck() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        String valid = bodyFor(slot);
        String id = slot.toString();
        List<String> bad = List.of("null", "[]", "{}", "\"" + id + "\"", "", " ", "42",
                "{\"slotId\":123}", "{\"slotId\":null}", "{\"slotId\":true}", "{\"slotId\":[\"" + id + "\"]}",
                "{\"slotId\":{\"id\":\"" + id + "\"}}", "{\"slotId\":\"not-a-uuid\"}", "{\"slotId\":\"1-1-1-1-1\"}",
                "{\"slotId\":\"" + id.replace("-", "") + "\"}", "{\"slotId\":\"\"}", "{\"slotId\":\" " + id + "\"}",
                "{\"slotId\":\"" + id + "\",\"studentId\":\"" + S2 + "\"}",
                "{\"slotId\":\"" + id + "\",\"status\":\"ACTIVE\"}",
                "{\"slotId\":\"" + id + "\",\"role\":\"STUDENT\"}",
                "{\"slotId\":\"" + id + "\",\"actorId\":\"" + S1 + "\"}",
                "{\"SlotId\":\"" + id + "\"}", "{\"slot_id\":\"" + id + "\"}",
                "{\"slotId\":\"" + id + "\",\"slotId\":\"" + id + "\"}",
                "{\"slotId\":\"" + UUID.randomUUID() + "\",\"slotId\":\"" + id + "\"}",
                valid + " {}", valid + "x", valid + ",", "{\"slotId\":\"" + id + "\"");
        String before = snapshot();
        MockHttpSession student = session(S1);
        for (String content : bad) {
            error(perform(book(content), student), 400, "VALIDATION_FAILED");
            assertEquals(before, snapshot(), "Rejected body changed rows: " + content);
        }
        for (String query : List.of("x=1", "studentId=" + S2, "slotId=" + id, "actorId=" + S1, "flag")) {
            error(perform(bookAt(BOOKINGS + "?" + query, valid), student), 400, "VALIDATION_FAILED");
        }
        assertEquals(before, snapshot());
        assertFalse(student.isInvalid());

        // Evaluation order (TECH §3.2, F2-06).
        error(perform(book("{}"), null), 401, "UNAUTHENTICATED");
        error(perform(post(BOOKINGS).contentType("application/json").content("{}"), student), 403, "CSRF_INVALID");
        MockHttpSession deleted = session(id(999));
        error(perform(book("{}"), deleted), 400, "VALIDATION_FAILED");
        assertFalse(deleted.isInvalid(), "A syntax error must not invalidate the session");
        error(perform(bookAt(BOOKINGS + "?x=1", valid), session(T1, "ROLE_STUDENT")), 400, "VALIDATION_FAILED");
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        MockHttpSession inactive = session(S2);
        error(perform(book("{\"slotId\":\"" + id + "\",\"studentId\":\"" + S2 + "\"}"), inactive), 400, "VALIDATION_FAILED");
        assertFalse(inactive.isInvalid(), "A syntax error must not invalidate the session");
        error(perform(book(valid), inactive), 403, "ACCOUNT_INACTIVE");
        assertTrue(inactive.isInvalid(), "The inactive account's session must be invalidated");
        assertEquals(before, snapshot());
        assertEquals(0, writes.bookCalls.get(), "A rejected request reached the lifecycle");

        ok(book(slot), student, 201);
    }

    /**
     * C2-IN-2: a non-JSON or missing content type gives 415; more than 65,536 body bytes give 413 even with a
     * misleading Content-Length: 2; exactly 65,536 bytes and a JSON type with a UTF-8 charset are accepted.
     * Syntax errors precede the actor check and do not invalidate (TECH §3.2).
     */
    @Test
    void c2In2MediaTypeAndStreamedBodySizeLimits() throws Exception {
        UUID exact = slot(T1, M, START, END, "AVAILABLE");
        UUID charset = slot(T2, M2, END, END.plusSeconds(1800), "AVAILABLE");
        String valid = bodyFor(exact);
        String before = snapshot();
        MockHttpSession student = session(S1);
        for (String type : List.of("text/plain", "application/xml")) {
            error(perform(post(BOOKINGS).contentType(type).content(valid).with(csrf().asHeader()), student), 415, "UNSUPPORTED_MEDIA_TYPE");
        }
        error(perform(post(BOOKINGS).content(valid).with(csrf().asHeader()), student), 415, "UNSUPPORTED_MEDIA_TYPE");
        String oversized = padTo(valid, MAX_BODY + 1);
        error(perform(book(oversized).header("Content-Length", "2"), student), 413, "PAYLOAD_TOO_LARGE");
        error(perform(book(oversized), student), 413, "PAYLOAD_TOO_LARGE");
        MockHttpSession deleted = session(id(999));
        error(perform(post(BOOKINGS).contentType("text/plain").content(valid).with(csrf().asHeader()), deleted), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(perform(book(oversized), deleted), 413, "PAYLOAD_TOO_LARGE");
        assertFalse(deleted.isInvalid(), "A syntax error must not invalidate the session");
        assertEquals(before, snapshot());
        assertEquals(0, writes.bookCalls.get(), "A rejected request reached the lifecycle");

        String boundary = padTo(valid, MAX_BODY);
        assertEquals(MAX_BODY, boundary.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        ok(book(boundary), student, 201);
        ok(post(BOOKINGS).contentType("application/json;charset=UTF-8").content(bodyFor(charset)).with(csrf().asHeader()), student, 201);
        assertEquals("BOOKED", slotStatus(exact));
        assertEquals("BOOKED", slotStatus(charset));
    }

    /** C2-IN-3: an X-Actor-Id header never selects the actor; the booking belongs to the session student. */
    @Test
    void c2In3ActorHeaderIsIgnored() throws Exception {
        UUID first = slot(T1, M, START, END, "AVAILABLE");
        UUID second = slot(T2, M2, END, END.plusSeconds(1800), "AVAILABLE");
        UUID asS1 = UUID.fromString(body(ok(book(first).header("X-Actor-Id", S2.toString()), session(S1), 201)).get("id").asString());
        UUID asS2 = UUID.fromString(body(ok(book(second).header("X-Actor-Id", S1.toString()), session(S2), 201)).get("id").asString());
        assertEquals(S1, db.sql("SELECT student_id FROM bookings WHERE id=:id").param("id", asS1).query(UUID.class).single());
        assertEquals(S2, db.sql("SELECT student_id FROM bookings WHERE id=:id").param("id", asS2).query(UUID.class).single());
        assertEquals(List.of(asS1), ids(body(ok(get(BOOKINGS), session(S1), 200))));
        UUID third = slot(T1, M2, END.plusSeconds(3600), END.plusSeconds(7200), "AVAILABLE");
        error(perform(book(third).header("X-Actor-Id", S1.toString()), null), 401, "UNAUTHENTICATED");
        assertEquals("AVAILABLE", slotStatus(third));
    }

    /**
     * C2-ACC-1: with a valid body, missing and invalid CSRF give 403 CSRF_INVALID; no principal and a principal
     * without a provider actor give 401; a deleted actor 401 with invalidation; tutor/admin (even with
     * ROLE_STUDENT) 403 FORBIDDEN without invalidation (R3-02); an inactive student 403 ACCOUNT_INACTIVE with
     * invalidation. No case reaches the lifecycle or changes rows. The stored role wins in both directions.
     */
    @Test
    void c2Acc1AccessMatrixAndCsrfDenyWithoutAnyChange() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        String valid = bodyFor(slot);
        List<String> leaks = List.of(slot.toString(), "CS10", "Tutor One Display");
        String before = snapshot();

        denied(perform(post(BOOKINGS).contentType("application/json").content(valid), session(S1)), 403, "CSRF_INVALID", leaks);
        denied(perform(post(BOOKINGS).contentType("application/json").content(valid).with(csrf().asHeader().useInvalidToken()), session(S1)),
                403, "CSRF_INVALID", leaks);
        denied(perform(book(slot), null), 401, "UNAUTHENTICATED", leaks);
        denied(perform(book(slot), session("no-provider-actor", "ROLE_STUDENT")), 401, "UNAUTHENTICATED", leaks);

        MockHttpSession deleted = session(id(999));
        denied(perform(book(slot), deleted), 401, "UNAUTHENTICATED", leaks);
        assertTrue(deleted.isInvalid(), "The deleted actor's session must be invalidated");
        denied(perform(book(slot), deleted), 401, "UNAUTHENTICATED", leaks);

        for (UUID wrong : List.of(T1, ADMIN)) {
            MockHttpSession wrongRole = session(wrong, "ROLE_STUDENT");
            denied(perform(book(slot), wrongRole), 403, "FORBIDDEN", leaks);
            assertFalse(wrongRole.isInvalid(), "FORBIDDEN must not invalidate the session (TECH §3.2)");
            denied(perform(book(slot), wrongRole), 403, "FORBIDDEN", leaks);
        }

        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        MockHttpSession inactive = session(S2);
        denied(perform(book(slot), inactive), 403, "ACCOUNT_INACTIVE", leaks);
        assertTrue(inactive.isInvalid(), "The inactive account's session must be invalidated");
        denied(perform(book(slot), inactive), 401, "UNAUTHENTICATED", leaks);

        assertEquals(before, snapshot(), "A denied request changed rows");
        assertEquals(0, writes.bookCalls.get(), "A denied request reached the lifecycle");

        UUID created = UUID.fromString(body(ok(book(slot), session(S1, "ROLE_TUTOR"), 201)).get("id").asString());
        assertEquals(S1, db.sql("SELECT student_id FROM bookings WHERE id=:id").param("id", created).query(UUID.class).single());
    }

    // ---------------------------------------------------------------- lifecycle failure mapping and post-commit failure

    /**
     * C2-ERR-1 (F2-07, F2-10): lifecycle double outcomes. INVALID_INPUT 400; TEMPORARILY_UNAVAILABLE 503;
     * NOT_FOUND/INVALID_STATE/MODULE_UNAVAILABLE 409 SLOT_UNAVAILABLE; BOOKING_OVERLAP 409 BOOKING_OVERLAP; a
     * DataIntegrityViolationException naming {@code bookings_one_active_per_slot} in its message or cause chain
     * 409 SLOT_UNAVAILABLE; any other integrity/data-access failure, a lifecycle code outside the TECH §3.3
     * table, and an unexpected exception 500 INTERNAL_ERROR without SQL, constraint, table, IDs or core code.
     */
    @Test
    void c2Err1LifecycleOutcomesMapToSafeStudentErrors() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        record Case(String name, RuntimeException failure, int status, String code, String message) {}
        String unique = "duplicate key value violates unique constraint \"bookings_one_active_per_slot\"";
        List<Case> cases = List.of(
                new Case("INVALID_INPUT", new LifecycleFailure(LifecycleFailure.Code.INVALID_INPUT), 400, "VALIDATION_FAILED", null),
                new Case("TEMPORARILY_UNAVAILABLE", new LifecycleFailure(LifecycleFailure.Code.TEMPORARILY_UNAVAILABLE), 503, "TEMPORARILY_UNAVAILABLE", null),
                new Case("NOT_FOUND", new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND), 409, "SLOT_UNAVAILABLE", UNAVAILABLE_MESSAGE),
                new Case("INVALID_STATE", new LifecycleFailure(LifecycleFailure.Code.INVALID_STATE), 409, "SLOT_UNAVAILABLE", UNAVAILABLE_MESSAGE),
                new Case("MODULE_UNAVAILABLE", new LifecycleFailure(LifecycleFailure.Code.MODULE_UNAVAILABLE), 409, "SLOT_UNAVAILABLE", UNAVAILABLE_MESSAGE),
                new Case("BOOKING_OVERLAP", new LifecycleFailure(LifecycleFailure.Code.BOOKING_OVERLAP), 409, "BOOKING_OVERLAP", OVERLAP_MESSAGE),
                new Case("unique index (message)", new DuplicateKeyException(unique), 409, "SLOT_UNAVAILABLE", UNAVAILABLE_MESSAGE),
                new Case("unique index (cause chain)", new DataIntegrityViolationException("could not insert booking " + MARKER,
                        new SQLException("ERROR: " + unique, "23505")), 409, "SLOT_UNAVAILABLE", UNAVAILABLE_MESSAGE),
                new Case("foreign key", new DataIntegrityViolationException("insert violates foreign key constraint \"bookings_slot_id_fkey\" " + MARKER),
                        500, "INTERNAL_ERROR", null),
                new Case("other unique index", new DuplicateKeyException("duplicate key value violates unique constraint \"bookings_pkey\" " + MARKER),
                        500, "INTERNAL_ERROR", null),
                new Case("data access", new DataAccessResourceFailureException("SELECT id FROM slots WHERE id='" + slot + "' " + MARKER),
                        500, "INTERNAL_ERROR", null),
                new Case("SLOT_OVERLAP", new LifecycleFailure(LifecycleFailure.Code.SLOT_OVERLAP), 500, "INTERNAL_ERROR", null),
                new Case("INVALID_TIME", new LifecycleFailure(LifecycleFailure.Code.INVALID_TIME), 500, "INTERNAL_ERROR", null),
                new Case("unexpected", new IllegalStateException("SELECT * FROM bookings WHERE student_id='" + S1 + "' " + MARKER),
                        500, "INTERNAL_ERROR", null));
        String before = snapshot();
        MockHttpSession student = session(S1);
        for (Case c : cases) {
            writes.failure = c.failure();
            MvcResult result = perform(book(slot).header("X-Request-Id", CLIENT_MARKER), student);
            JsonNode failure = error(result, c.status(), c.code());
            if (c.message() != null) assertEquals(c.message(), failure.get("message").asString(), c.name());
            String text = result.getResponse().getContentAsString();
            for (String leak : List.of("constraint", "bookings_one_active_per_slot", "bookings_slot_id_fkey", "bookings_pkey",
                    "SELECT", MARKER, CLIENT_MARKER, slot.toString(), S1.toString(), "Exception")) {
                assertFalse(text.contains(leak), c.name() + ": error body discloses '" + leak + "': " + text);
            }
            if (c.status() == 500) {
                for (String leak : List.of("SLOT_OVERLAP", "INVALID_TIME", "bookings", "slots", "users")) {
                    assertFalse(text.contains(leak), c.name() + ": 500 body discloses '" + leak + "': " + text);
                }
            }
            assertEquals(before, snapshot(), c.name() + ": a failed booking changed rows");
            assertFalse(student.isInvalid(), c.name() + ": a lifecycle failure must not invalidate the session");
        }
        assertEquals(cases.size(), writes.bookCalls.get(), "Each scripted case must reach the lifecycle exactly once");
        writes.failure = null;
        ok(book(slot), student, 201);
    }

    /**
     * C2-ERR-2 (F2-09): the lifecycle reports ACCESS_DENIED. (a) S1 deactivated meanwhile: 403 ACCOUNT_INACTIVE,
     * session invalidated, then 401. (b) S1 still an active student: 403 FORBIDDEN, session kept, a following
     * GET /bookings succeeds. (c) the account deleted meanwhile: 401 with invalidation. (d) the stored role
     * changed meanwhile: 403 FORBIDDEN, session kept. Nothing changes.
     */
    @Test
    void c2Err2LifecycleAccessDeniedRerunsTheAccessCheck() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        String before = snapshot();
        writes.failure = new LifecycleFailure(LifecycleFailure.Code.ACCESS_DENIED);

        writes.beforeFailure = () -> db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S1).update();
        MockHttpSession deactivated = session(S1);
        error(perform(book(slot), deactivated), 403, "ACCOUNT_INACTIVE");
        assertTrue(deactivated.isInvalid(), "(a) the session must be invalidated");
        error(perform(book(slot), deactivated), 401, "UNAUTHENTICATED");
        db.sql("UPDATE users SET active=true WHERE id=:id").param("id", S1).update();

        writes.beforeFailure = () -> {};
        MockHttpSession stillValid = session(S1);
        error(perform(book(slot), stillValid), 403, "FORBIDDEN");
        assertFalse(stillValid.isInvalid(), "(b) a generic lifecycle ACCESS_DENIED must not invalidate the session");
        ok(get(BOOKINGS), stillValid, 200);

        db.sql("INSERT INTO users VALUES (:id,'Student Gamma Name',:e,'STUDENT',true)").param("id", S3).param("e", S3 + "@example.edu").update();
        writes.beforeFailure = () -> db.sql("DELETE FROM users WHERE id=:id").param("id", S3).update();
        MockHttpSession removed = session(S3);
        error(perform(book(slot), removed), 401, "UNAUTHENTICATED");
        assertTrue(removed.isInvalid(), "(c) the deleted account's session must be invalidated");

        writes.beforeFailure = () -> db.sql("UPDATE users SET role='TUTOR' WHERE id=:id").param("id", S2).update();
        MockHttpSession roleChanged = session(S2);
        error(perform(book(slot), roleChanged), 403, "FORBIDDEN");
        assertFalse(roleChanged.isInvalid(), "(d) FORBIDDEN must not invalidate the session");

        assertEquals(before, snapshot());
        assertEquals(4, writes.bookCalls.get(), "Each case must reach the lifecycle once; the (a) retry must not");
    }

    /**
     * C2-ERR-3 (TECH §3.3 post-commit paragraph): the booking commits, then building the response fails (every
     * new read connection fails). Response 500 without SQL text; the booking stays committed exactly once (no
     * retry, no rollback) and a later GET shows it; booking the slot again is a conflict.
     */
    @Test
    void c2Err3ProjectionFailureAfterCommitIs500AndKeepsTheBookingOnce() throws Exception {
        UUID slot = slot(T1, M, START, END, "AVAILABLE");
        writes.afterBook = () -> reads.fail = true;
        MockHttpSession student = session(S1);
        MvcResult failed = perform(book(slot), student);
        reads.fail = false;
        writes.afterBook = () -> {};
        error(failed, 500, "INTERNAL_ERROR");
        String text = failed.getResponse().getContentAsString();
        assertFalse(text.contains(MARKER) || text.contains("SELECT") || text.contains(slot.toString()), text);

        assertEquals(1, writes.bookCalls.get(), "The committed booking must not be retried");
        assertEquals(1, count("bookings"));
        UUID id = db.sql("SELECT id FROM bookings WHERE slot_id=:s").param("s", slot).query(UUID.class).single();
        assertEquals("ACTIVE", bookingStatus(id));
        assertEquals(S1, db.sql("SELECT student_id FROM bookings WHERE id=:id").param("id", id).query(UUID.class).single());
        assertEquals("BOOKED", slotStatus(slot));
        JsonNode mine = body(ok(get(BOOKINGS), student, 200));
        assertEquals(List.of(id), ids(mine));
        assertEquals("ACTIVE", mine.get(0).get("status").asString());
        assertEquals(UNAVAILABLE_MESSAGE, error(perform(book(slot), student), 409, "SLOT_UNAVAILABLE").get("message").asString());
        assertEquals(1, count("bookings"));
    }

    // ---------------------------------------------------------------- activation (stu-c1 R1-05 / N3)

    /**
     * TECH §2 activation (stu-c1 C1-ACT-2 lifecycle part, carried forward as R1-05 / N3): with student-api
     * active, a context without a ConsultationLifecycle bean fails startup, both in the Student web context and
     * in the real Boot application. The complete context serves the booking route.
     */
    @Test
    void c2ActStudentApiWithoutConsultationLifecycleFailsStartup() throws Exception {
        try (var incomplete = studentContext(Set.of("lifecycle"))) {
            RuntimeException failure = assertThrows(RuntimeException.class, incomplete::refresh,
                    "student-api context started without a ConsultationLifecycle bean");
            assertTrue(chain(failure).contains("ConsultationLifecycle"), chain(failure));
        }
        RuntimeException bootFailure = assertThrows(RuntimeException.class, () -> {
            try (var boot = boot(Set.of("lifecycle"))) {
                assertNotNull(boot);
            }
        }, "Boot started student-api without a ConsultationLifecycle bean");
        assertTrue(chain(bootFailure).contains("ConsultationLifecycle"), chain(bootFailure));

        ok(book(slot(T1, M, START, END, "AVAILABLE")), session(S1), 201);
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

    private ConfigurableApplicationContext boot(Set<String> omit) {
        return new SpringApplicationBuilder(UnichopeApplication.class).web(WebApplicationType.SERVLET).profiles("student-api")
                .properties("server.address=127.0.0.1", "server.port=0")
                .initializers(application -> registerStudentDependencies(application.getBeanFactory(), omit)).run();
    }

    private void registerStudentDependencies(ConfigurableListableBeanFactory factory, Set<String> omit) {
        factory.registerSingleton("dataSource", reads);
        factory.registerSingleton("clock", clock);
        if (!omit.contains("lifecycle")) factory.registerSingleton("consultationLifecycle", writes);
        JdbcClient jdbc = JdbcClient.create(reads);
        factory.registerSingleton("userReads", new PostgresUserRepository(jdbc));
        factory.registerSingleton("moduleReads", new PostgresModuleRepository(jdbc));
        factory.registerSingleton("slotReads", new PostgresSlotRepository(jdbc));
        factory.registerSingleton("bookingReads", new PostgresBookingRepository(jdbc));
        StudentTestActors.registerSessionProvider(factory);
    }

    /** SGT wall-clock time on the fixture day (N = 2026-10-21T00:00Z = 08:00 SGT). */
    private static Instant sgt(int hour, int minute) { return N.plusSeconds((hour - 8) * 3600L + minute * 60L); }

    private static String bodyFor(UUID slot) { return "{\"slotId\":\"" + slot + "\"}"; }

    private static String padTo(String json, int bytes) { return json + " ".repeat(bytes - json.length()); }

    private MockHttpServletRequestBuilder book(UUID slot) { return book(bodyFor(slot)); }

    private MockHttpServletRequestBuilder book(String content) { return bookAt(BOOKINGS, content); }

    private MockHttpServletRequestBuilder bookAt(String uri, String content) {
        return post(uri).contentType("application/json").content(content).with(csrf().asHeader());
    }

    private Set<UUID> activeSlotsOf(UUID student) {
        return Set.copyOf(db.sql("SELECT slot_id FROM bookings WHERE student_id=:s AND status='ACTIVE'").param("s", student)
                .query(UUID.class).list());
    }

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
        return mvc.perform(request.accept("application/json")).andReturn();
    }

    private MvcResult ok(MockHttpServletRequestBuilder request, MockHttpSession session, int status) throws Exception {
        MvcResult result = perform(request, session);
        assertEquals(status, result.getResponse().getStatus(),
                result.getResponse().getContentAsString() + " resolved=" + result.getResolvedException());
        assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));
        return result;
    }

    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }

    private void exactBooking(JsonNode booking) {
        assertTrue(booking.isObject(), "Expected a StudentBooking object: " + booking);
        assertEquals(BOOKING_FIELDS, Set.copyOf(booking.propertyNames()));
        UUID.fromString(booking.get("id").asString());
        for (String field : List.of("startTime", "endTime")) assertTrue(booking.get(field).asString().endsWith("Z"), field);
        assertEquals("ACTIVE", booking.get("status").asString());
        assertTrue(booking.get("cancellable").isBoolean(), "cancellable must be a JSON boolean");
        assertTrue(booking.get("cancellable").booleanValue(), "A new future booking is cancellable");
    }

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
        assertFalse(text.contains("@"), "Error exposes an email");
        assertFalse(text.contains("SELECT"), "Error exposes SQL");
        assertFalse(text.contains(MARKER), "Error exposes exception text");
        return body;
    }

    private void denied(MvcResult result, int status, String code, List<String> leaks) throws Exception {
        error(result, status, code);
        String text = result.getResponse().getContentAsString();
        for (String leak : leaks) assertFalse(text.contains(leak), "Denial body discloses booking data (" + leak + "): " + text);
    }

    /**
     * Test-only lifecycle double: delegates to the real PostgreSQL lifecycle, or throws a scripted failure
     * (after an optional side effect) before touching any row; counts booking calls; can run a hook after a
     * successful (committed) booking.
     */
    private static final class ScriptedLifecycle implements ConsultationLifecycle {
        private final ConsultationLifecycle delegate;
        final AtomicInteger bookCalls = new AtomicInteger();
        volatile RuntimeException failure;
        volatile Runnable beforeFailure = () -> {};
        volatile Runnable afterBook = () -> {};

        ScriptedLifecycle(ConsultationLifecycle delegate) { this.delegate = delegate; }

        @Override public Booking bookAvailableSlot(UUID actor, UUID slot) {
            bookCalls.incrementAndGet();
            RuntimeException scripted = failure;
            if (scripted != null) {
                beforeFailure.run();
                throw scripted;
            }
            Booking booking = delegate.bookAvailableSlot(actor, slot);
            afterBook.run();
            return booking;
        }
        @Override public ConsultationSlot createSlot(UUID actor, UUID module, Instant start, Instant end) { return delegate.createSlot(actor, module, start, end); }
        @Override public ConsultationSlot cancelAvailableSlot(UUID actor, UUID slot) { return delegate.cancelAvailableSlot(actor, slot); }
        @Override public Booking cancelActiveBooking(UUID actor, UUID booking) { return delegate.cancelActiveBooking(actor, booking); }
        @Override public Booking completeActiveBooking(UUID actor, UUID booking) { return delegate.completeActiveBooking(actor, booking); }
        @Override public LockedCompletedBooking lockForPrivateNote(UUID tutor, UUID booking) { return delegate.lockForPrivateNote(tutor, booking); }
    }

    /** Test-only DataSource over the fixture source whose new connections can be made to fail (C2-ERR-3). */
    private static final class SwitchableDataSource extends DelegatingDataSource {
        volatile boolean fail;

        SwitchableDataSource(DataSource target) { super(target); }

        @Override public Connection getConnection() throws SQLException { check(); return super.getConnection(); }
        @Override public Connection getConnection(String user, String password) throws SQLException { check(); return super.getConnection(user, password); }

        private void check() throws SQLException {
            if (fail) throw new SQLException("SELECT private-marker: read connection refused after commit");
        }
    }
}

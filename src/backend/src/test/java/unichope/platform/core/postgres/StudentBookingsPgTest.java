package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
import unichope.platform.web.ApiSecurityConfiguration;
import unichope.student.StudentTestActors;

/**
 * stu-c3-my-bookings, tasks C3-T1 and C3-T2: {@code GET /api/student/bookings} and
 * {@code GET /api/student/bookings/{id}} through the real MVC and Spring Security filter chain
 * against disposable PostgreSQL (simulated authentication only; no real login/session claim).
 *
 * <p>Same discovery contract as stu-c1 ({@code StudentBrowsePgTest}): Student code is found by
 * component scan of {@code unichope.student} with profile {@code student-api}, and the actor seam
 * through {@link StudentTestActors}. There is no compile-time reference to the C3 projection or
 * query, so before implementation these tests fail on HTTP behavior (the routes answer 404), not
 * on compilation.
 *
 * <p>Beans supplied to Student wiring are exactly those of stu-c1: DataSource, Clock (fixture
 * {@code MovingClock}, N = 2026-10-21T00:00Z), ConsultationLifecycle, core User/Module/Slot/Booking
 * repositories, and the test-only {@code unichope.platform.auth.CurrentActorProvider}.
 *
 * <p>Fixture convention: an ACTIVE booking sits on a BOOKED slot, a COMPLETED booking on a
 * COMPLETED slot, and a CANCELLED booking on a slot that is AVAILABLE or re-booked by someone else,
 * as the core lifecycle would leave them.
 */
@Tag("postgres")
class StudentBookingsPgTest extends LifecyclePgFixture {
    private static final UUID S3 = id(7), M4 = id(13);
    private static final String BOOKINGS = "/api/student/bookings";
    private static final String STUDENT_ONE_NAME = "Student Alpha Name";
    private static final Set<String> BOOKING_FIELDS = Set.of("id", "slotId", "moduleId", "moduleCode", "moduleName",
            "tutorName", "startTime", "endTime", "status", "cancellable");
    private final JsonMapper json = JsonMapper.shared();
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
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles("student-api");
        context.addBeanFactoryPostProcessor(this::registerStudentDependencies);
        context.register(DelegatingWebMvcConfiguration.class, ApiSecurityConfiguration.class);
        context.scan("unichope.student");
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void closeStudentWebContext() {
        if (context != null) context.close();
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- C3-T1: projection, filters, cancellable

    /**
     * S-LIST-1: only the session student's bookings in every status, ordered by slot startTime then
     * booking id (equal-start tie), with server-computed cancellable; another student sees only their
     * own; a student without bookings gets [].
     */
    @Test
    void sList1OnlyOwnBookingsAllStatusesOrderedBySlotStartThenBookingId() throws Exception {
        Instant h1 = N.plusSeconds(3600), h2 = N.plusSeconds(7200), h3 = N.plusSeconds(10800), h4 = N.plusSeconds(14400);
        // S1: COMPLETED in the past, ACTIVE already started, ACTIVE and CANCELLED with equal start, a CANCELLED
        // booking on a slot S2 re-booked, and an ACTIVE future booking.
        UUID s1Completed = booking(S1, slot(T1, M, N.minusSeconds(2 * 86400), N.minusSeconds(2 * 86400 - 1800), "COMPLETED"), "COMPLETED");
        UUID s1Started = booking(S1, slot(T1, M, N.minusSeconds(1800), N.plusSeconds(1800), "BOOKED"), "ACTIVE");
        // Equal start h1: slot A has the lower slot id, module CS10 and tutor "Tutor One"; its booking is inserted
        // first but has the HIGHER booking id. Slot-id, module, tutor-name and insertion order all put A first; only
        // booking-id order puts B first.
        UUID slotA = slotWithId(new UUID(0, 0x100), T1, M, h1, h2, "BOOKED");
        UUID slotB = slotWithId(new UUID(0, 0x200), T2, M2, h1, h2, "AVAILABLE");
        UUID tieHigh = bookingWithId(new UUID(0, 0xB00), S1, slotA, "ACTIVE");
        UUID tieLow = bookingWithId(new UUID(0, 0xA00), S1, slotB, "CANCELLED");
        UUID rebooked = slot(T1, M2, h2, h3, "BOOKED");
        UUID s1CancelledOnRebooked = booking(S1, rebooked, "CANCELLED");
        UUID s1Future = booking(S1, slot(T2, M, h3, h4, "BOOKED"), "ACTIVE");
        // S2: interleaved in time with S1's bookings.
        UUID s2Completed = booking(S2, slot(T2, M2, N.minusSeconds(86400), N.minusSeconds(86400 - 1800), "COMPLETED"), "COMPLETED");
        UUID s2Cancelled = booking(S2, slot(T2, M, h1.plusSeconds(900), h2, "AVAILABLE"), "CANCELLED");
        UUID s2Active = booking(S2, rebooked, "ACTIVE");

        JsonNode mine = body(ok(get(BOOKINGS), session(S1), 200));
        assertEquals(List.of(s1Completed, s1Started, tieLow, tieHigh, s1CancelledOnRebooked, s1Future), ids(mine));
        Map<UUID, Boolean> cancellable = cancellableById(mine);
        assertEquals(Map.of(s1Completed, false, s1Started, false, tieLow, false, tieHigh, true,
                s1CancelledOnRebooked, false, s1Future, true), cancellable);
        Map<UUID, String> statuses = statusById(mine);
        assertEquals(Map.of(s1Completed, "COMPLETED", s1Started, "ACTIVE", tieLow, "CANCELLED", tieHigh, "ACTIVE",
                s1CancelledOnRebooked, "CANCELLED", s1Future, "ACTIVE"), statuses);

        assertEquals(List.of(s2Completed, s2Cancelled, s2Active), ids(body(ok(get(BOOKINGS), session(S2), 200))));
        // Interleaving does not leak: S1 again gets the identical list.
        assertEquals(List.of(s1Completed, s1Started, tieLow, tieHigh, s1CancelledOnRebooked, s1Future),
                ids(body(ok(get(BOOKINGS), session(S1), 200))));

        db.sql("INSERT INTO users VALUES (:id,'Student Gamma Name',:e,'STUDENT',true)").param("id", S3).param("e", S3 + "@example.edu").update();
        JsonNode none = body(ok(get(BOOKINGS), session(S3), 200));
        assertTrue(none.isArray(), "Expected [] for a student without bookings: " + none);
        assertEquals(0, none.size());
    }

    /**
     * C3-OUT-1: StudentBooking has exactly the ten specified fields with correct values on both list
     * and detail; instants are UTC strings ending in Z; no email, student name, student id or tutor id.
     */
    @Test
    void c3Out1StudentBookingHasExactSafeFields() throws Exception {
        UUID slot = slot(T1, M, START, END, "BOOKED");
        UUID active = booking(S1, slot, "ACTIVE");
        UUID cancelledSlot = slot(T2, M2, END, END.plusSeconds(1800), "AVAILABLE");
        UUID cancelled = booking(S1, cancelledSlot, "CANCELLED");

        MvcResult listResponse = ok(get(BOOKINGS), session(S1), 200);
        JsonNode list = body(listResponse);
        assertEquals(List.of(active, cancelled), ids(list));
        MvcResult detailResponse = ok(get(BOOKINGS + "/" + active), session(S1), 200);
        JsonNode detail = body(detailResponse);
        assertTrue(detail.isObject(), "Detail must be a single StudentBooking object: " + detail);
        assertEquals(list.get(0), detail, "Detail and list projections of the same booking differ");

        for (JsonNode booking : List.of(list.get(0), detail)) {
            assertEquals(BOOKING_FIELDS, Set.copyOf(booking.propertyNames()));
            assertEquals(active.toString(), booking.get("id").asString());
            assertEquals(slot.toString(), booking.get("slotId").asString());
            assertEquals(M.toString(), booking.get("moduleId").asString());
            assertEquals("CS10", booking.get("moduleCode").asString());
            assertEquals("Software Design", booking.get("moduleName").asString());
            assertEquals("Tutor One Display", booking.get("tutorName").asString());
            for (String field : List.of("startTime", "endTime")) assertTrue(booking.get(field).asString().endsWith("Z"), field);
            assertEquals(START, Instant.parse(booking.get("startTime").asString()));
            assertEquals(END, Instant.parse(booking.get("endTime").asString()));
            assertEquals("ACTIVE", booking.get("status").asString());
            assertTrue(booking.get("cancellable").isBoolean(), "cancellable must be a JSON boolean");
            assertTrue(booking.get("cancellable").booleanValue());
        }
        JsonNode other = list.get(1);
        assertEquals(BOOKING_FIELDS, Set.copyOf(other.propertyNames()));
        assertEquals(cancelledSlot.toString(), other.get("slotId").asString());
        assertEquals(M2.toString(), other.get("moduleId").asString());
        assertEquals("CS11", other.get("moduleCode").asString());
        assertEquals("Data Structures", other.get("moduleName").asString());
        assertEquals("Tutor Two Display", other.get("tutorName").asString());
        assertEquals("CANCELLED", other.get("status").asString());
        assertTrue(other.get("cancellable").isBoolean(), "cancellable must be a JSON boolean");
        assertFalse(other.get("cancellable").booleanValue());

        for (MvcResult response : List.of(listResponse, detailResponse)) {
            String text = response.getResponse().getContentAsString();
            assertFalse(text.contains("@"), "Student booking body exposes an email: " + text);
            assertFalse(text.contains(STUDENT_ONE_NAME), "Student booking body exposes the student name");
            assertFalse(text.contains(S1.toString()), "Student booking body exposes the student id");
            assertFalse(text.contains(T1.toString()) || text.contains(T2.toString()), "Student booking body exposes a tutor id");
        }
    }

    /**
     * C3-FLT-1: status filter. ACTIVE only; CANCELLED+COMPLETED history; a repeated equal value equals the
     * single value; no status returns all; each single history status; all three equal no filter.
     */
    @Test
    void c3Flt1StatusFilterSelectsUpcomingHistoryAndAll() throws Exception {
        Instant h1 = N.plusSeconds(3600), h2 = N.plusSeconds(7200), h3 = N.plusSeconds(10800);
        UUID completed = booking(S1, slot(T1, M, N.minusSeconds(86400), N.minusSeconds(86400 - 1800), "COMPLETED"), "COMPLETED");
        UUID cancelled = booking(S1, slot(T1, M, h1, h2, "AVAILABLE"), "CANCELLED");
        UUID active = booking(S1, slot(T1, M, h2, h3, "BOOKED"), "ACTIVE");
        UUID laterActive = booking(S1, slot(T2, M2, h3, h3.plusSeconds(1800), "BOOKED"), "ACTIVE");
        booking(S2, slot(T2, M, h1, h2, "BOOKED"), "ACTIVE");
        booking(S2, slot(T2, M, N.minusSeconds(7200), N.minusSeconds(5400), "COMPLETED"), "COMPLETED");

        String upcoming = ok(get(BOOKINGS).param("status", "ACTIVE"), session(S1), 200).getResponse().getContentAsString();
        assertEquals(List.of(active, laterActive), ids(json.readTree(upcoming)));
        assertEquals(List.of(completed, cancelled),
                ids(body(ok(get(BOOKINGS).param("status", "CANCELLED", "COMPLETED"), session(S1), 200))));
        assertEquals(List.of(completed, cancelled),
                ids(body(ok(get(BOOKINGS).param("status", "COMPLETED", "CANCELLED"), session(S1), 200))));
        assertEquals(upcoming, ok(get(BOOKINGS).param("status", "ACTIVE", "ACTIVE"), session(S1), 200).getResponse().getContentAsString(),
                "status=ACTIVE&status=ACTIVE must equal status=ACTIVE");
        assertEquals(List.of(completed), ids(body(ok(get(BOOKINGS).param("status", "COMPLETED"), session(S1), 200))));
        assertEquals(List.of(cancelled), ids(body(ok(get(BOOKINGS).param("status", "CANCELLED"), session(S1), 200))));
        List<UUID> all = List.of(completed, cancelled, active, laterActive);
        assertEquals(all, ids(body(ok(get(BOOKINGS), session(S1), 200))));
        assertEquals(all, ids(body(ok(get(BOOKINGS).param("status", "ACTIVE", "CANCELLED", "COMPLETED"), session(S1), 200))));
        for (JsonNode booking : body(ok(get(BOOKINGS).param("status", "ACTIVE"), session(S1), 200))) {
            assertEquals("ACTIVE", booking.get("status").asString());
        }
    }

    /**
     * C3-FLT-2: date filters by the slot's SGT start date across the UTC/SGT boundary (half-open SGT day),
     * includes past dates, intersects with status, and returns [] when nothing matches.
     */
    @Test
    void c3Flt2SgtStartDateFilterAcrossBoundaryIntersectsStatus() throws Exception {
        UUID late = booking(S1, slot(T1, M, at("2026-10-21T15:30:00Z"), at("2026-10-21T16:00:00Z"), "BOOKED"), "ACTIVE");          // 23:30 SGT 21st
        UUID early = booking(S1, slot(T1, M, at("2026-10-21T16:30:00Z"), at("2026-10-21T17:00:00Z"), "BOOKED"), "ACTIVE");         // 00:30 SGT 22nd
        UUID lastSecond = booking(S1, slot(T2, M2, at("2026-10-22T15:59:59Z"), at("2026-10-22T16:30:00Z"), "AVAILABLE"), "CANCELLED"); // 23:59:59 SGT 22nd
        UUID nextDay = booking(S1, slot(T1, M, at("2026-10-22T16:00:00Z"), at("2026-10-22T16:30:00Z"), "BOOKED"), "ACTIVE");        // 00:00 SGT 23rd
        UUID past = booking(S1, slot(T1, M2, at("2026-10-20T02:00:00Z"), at("2026-10-20T03:00:00Z"), "COMPLETED"), "COMPLETED");    // 10:00 SGT 20th
        booking(S2, slot(T2, M, at("2026-10-21T17:00:00Z"), at("2026-10-21T17:30:00Z"), "BOOKED"), "ACTIVE");                        // S2, 01:00 SGT 22nd

        assertEquals(List.of(early, lastSecond), ids(body(ok(get(BOOKINGS).param("date", "2026-10-22"), session(S1), 200))));
        assertEquals(List.of(late), ids(body(ok(get(BOOKINGS).param("date", "2026-10-21"), session(S1), 200))));
        assertEquals(List.of(nextDay), ids(body(ok(get(BOOKINGS).param("date", "2026-10-23"), session(S1), 200))));
        assertEquals(List.of(past), ids(body(ok(get(BOOKINGS).param("date", "2026-10-20"), session(S1), 200))));
        assertEquals(List.of(early), ids(body(ok(get(BOOKINGS).param("date", "2026-10-22").param("status", "ACTIVE"), session(S1), 200))));
        assertEquals(List.of(lastSecond),
                ids(body(ok(get(BOOKINGS).param("status", "CANCELLED", "COMPLETED").param("date", "2026-10-22"), session(S1), 200))));
        assertEquals(List.of(), ids(body(ok(get(BOOKINGS).param("date", "2026-10-22").param("status", "COMPLETED"), session(S1), 200))));
        assertEquals(List.of(), ids(body(ok(get(BOOKINGS).param("date", "2026-10-25"), session(S1), 200))));
    }

    /**
     * C3-FLT-3: cancellable is true only for ACTIVE bookings whose slot starts strictly after the injected
     * clock, on list and detail; it turns false when the clock reaches the start instant.
     */
    @Test
    void c3Flt3CancellableFollowsInjectedClockStrictlyBeforeStart() throws Exception {
        UUID active = booking(S1, slot(T1, M, START, END, "BOOKED"), "ACTIVE");
        UUID started = booking(S1, slot(T1, M2, N.minusSeconds(600), N.plusSeconds(1200), "BOOKED"), "ACTIVE");
        UUID cancelled = booking(S1, slot(T2, M, END, END.plusSeconds(1800), "AVAILABLE"), "CANCELLED");
        UUID completed = booking(S1, slot(T2, M2, END.plusSeconds(1800), END.plusSeconds(3600), "COMPLETED"), "COMPLETED");
        String detail = BOOKINGS + "/" + active;

        assertEquals(Map.of(active, true, started, false, cancelled, false, completed, false),
                cancellableById(body(ok(get(BOOKINGS), session(S1), 200))));
        assertTrue(body(ok(get(detail), session(S1), 200)).get("cancellable").booleanValue());

        clock.now.set(START.minusMillis(1));
        assertTrue(cancellableById(body(ok(get(BOOKINGS), session(S1), 200))).get(active), "one millisecond before start");
        assertTrue(body(ok(get(detail), session(S1), 200)).get("cancellable").booleanValue());

        clock.now.set(START);
        assertEquals(Map.of(active, false, started, false, cancelled, false, completed, false),
                cancellableById(body(ok(get(BOOKINGS), session(S1), 200))), "start == now is not cancellable");
        JsonNode atStart = body(ok(get(detail), session(S1), 200));
        assertFalse(atStart.get("cancellable").booleanValue());
        assertEquals("ACTIVE", atStart.get("status").asString(), "status is stored state, not clock-derived");

        clock.now.set(START.plusSeconds(1));
        assertFalse(body(ok(get(detail), session(S1), 200)).get("cancellable").booleanValue());
    }

    /**
     * C3-ELIG-1: a student's own ACTIVE future bookings stay listed, retrievable and cancellable after the
     * slot's tutor is deactivated, the module is deactivated, or the tutor-module assignment is removed.
     */
    @Test
    void c3Elig1OwnBookingsStayVisibleAfterTutorOrModuleIneligibility() throws Exception {
        db.sql("INSERT INTO modules VALUES (:id,'CS13','Retiring Module',true)").param("id", M4).update();
        db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t", T1).param("m", M4).update();
        UUID inactiveTutor = booking(S1, slot(T2, M2, START, END, "BOOKED"), "ACTIVE");
        UUID inactiveModule = booking(S1, slot(T1, M4, END, END.plusSeconds(1800), "BOOKED"), "ACTIVE");
        UUID unassigned = booking(S1, slot(T1, M, END.plusSeconds(1800), END.plusSeconds(3600), "BOOKED"), "ACTIVE");
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", T2).update();
        db.sql("UPDATE modules SET active=false WHERE id=:id").param("id", M4).update();
        db.sql("DELETE FROM tutor_modules WHERE tutor_id=:t AND module_id=:m").param("t", T1).param("m", M).update();

        JsonNode list = body(ok(get(BOOKINGS), session(S1), 200));
        assertEquals(List.of(inactiveTutor, inactiveModule, unassigned), ids(list));
        assertEquals(Map.of(inactiveTutor, true, inactiveModule, true, unassigned, true), cancellableById(list));
        assertEquals(Map.of(inactiveTutor, "ACTIVE", inactiveModule, "ACTIVE", unassigned, "ACTIVE"), statusById(list));
        assertEquals(List.of(inactiveTutor, inactiveModule, unassigned),
                ids(body(ok(get(BOOKINGS).param("status", "ACTIVE"), session(S1), 200))));
        for (UUID booking : List.of(inactiveTutor, inactiveModule, unassigned)) {
            JsonNode detail = body(ok(get(BOOKINGS + "/" + booking), session(S1), 200));
            assertEquals(booking.toString(), detail.get("id").asString());
            assertEquals("ACTIVE", detail.get("status").asString());
            assertTrue(detail.get("cancellable").booleanValue(), booking + " must stay cancellable");
        }
    }

    // ---------------------------------------------------------------- C3-T2: detail ownership, input, access

    /**
     * C3-OWN-1: the owner retrieves their booking in any status; another student's booking id and a random
     * UUID both return 404 NOT_FOUND with bodies identical apart from requestId.
     */
    @Test
    void c3Own1DetailReturnsOwnBookingInAnyStatusAndHidesOthersAsNotFound() throws Exception {
        UUID active = booking(S1, slot(T1, M, START, END, "BOOKED"), "ACTIVE");
        UUID cancelled = booking(S1, slot(T1, M, END, END.plusSeconds(1800), "AVAILABLE"), "CANCELLED");
        UUID completed = booking(S1, slot(T1, M, N.minusSeconds(7200), N.minusSeconds(5400), "COMPLETED"), "COMPLETED");
        UUID foreign = booking(S2, slot(T2, M2, START, END, "BOOKED"), "ACTIVE");
        UUID foreignHistory = booking(S2, slot(T2, M2, N.minusSeconds(7200), N.minusSeconds(5400), "COMPLETED"), "COMPLETED");

        JsonNode list = body(ok(get(BOOKINGS), session(S1), 200));
        Map<String, String> expectedStatus = Map.of(active.toString(), "ACTIVE", cancelled.toString(), "CANCELLED",
                completed.toString(), "COMPLETED");
        for (JsonNode listed : list) {
            String id = listed.get("id").asString();
            JsonNode detail = body(ok(get(BOOKINGS + "/" + id), session(S1), 200));
            assertEquals(expectedStatus.get(id), detail.get("status").asString());
            assertEquals(listed, detail, "Detail differs from the list projection for " + id);
        }
        // The foreign booking is real: its owner can read it.
        assertEquals(foreign.toString(), body(ok(get(BOOKINGS + "/" + foreign), session(S2), 200)).get("id").asString());

        MockHttpSession s1 = session(S1);
        JsonNode random = error(perform(get(BOOKINGS + "/" + UUID.randomUUID()), s1), 404, "NOT_FOUND");
        for (UUID other : List.of(foreign, foreignHistory)) {
            MvcResult result = perform(get(BOOKINGS + "/" + other), s1);
            JsonNode hidden = error(result, 404, "NOT_FOUND");
            assertEquals(random.get("code"), hidden.get("code"));
            assertEquals(random.get("message"), hidden.get("message"), "Foreign and nonexistent bookings must be indistinguishable");
            String text = result.getResponse().getContentAsString();
            assertFalse(text.contains(other.toString()) || text.contains("CS11") || text.contains("Tutor Two"),
                    "404 body discloses foreign booking data: " + text);
        }
        assertFalse(s1.isInvalid(), "A not-found lookup must not end the owner's session");
        assertEquals(List.of(active, cancelled, completed).size(), body(ok(get(BOOKINGS), s1, 200)).size());
    }

    /**
     * C3-IN-1: unknown or spoofing query parameters, invalid status values, repeated or malformed date,
     * a non-UUID path id and any query on the detail route are 400 VALIDATION_FAILED. TECH §3.2 order:
     * filter denial first (anonymous 401), then syntax (400) before the actor check, with no invalidation.
     */
    @Test
    void c3In1BookingsRejectsUnknownInvalidRepeatedAndMalformedInput() throws Exception {
        UUID own = booking(S1, slot(T1, M, START, END, "BOOKED"), "ACTIVE");
        String detail = BOOKINGS + "/" + own;
        List<MockHttpServletRequestBuilder> invalid = List.of(
                get(BOOKINGS).param("x", "1"),
                get(BOOKINGS).param("actorId", S2.toString()),
                get(BOOKINGS).param("studentId", S2.toString()),
                get(BOOKINGS).param("status", "ACTIVE").param("studentId", S2.toString()),
                get(BOOKINGS).param("status", "active"),
                get(BOOKINGS).param("status", "PENDING"),
                get(BOOKINGS).param("status", "Active"),
                get(BOOKINGS).param("status", ""),
                get(BOOKINGS).param("status", "ACTIVE", "PENDING"),
                get(BOOKINGS).param("status", "ACTIVE,CANCELLED"),
                get(BOOKINGS).param("date", "2026-10-22", "2026-10-22"),
                get(BOOKINGS).param("date", "2026-10-22", "2026-10-23"),
                get(BOOKINGS).param("date", "2026-02-30"),
                get(BOOKINGS).param("date", "2026-1-01"),
                get(BOOKINGS).param("date", ""),
                get(BOOKINGS).param("date", "2026-10-22T00:00:00Z"),
                get(BOOKINGS + "?flag"),
                get(BOOKINGS + "/not-a-uuid"),
                get(BOOKINGS + "/1-1-1-1-1"),
                get(BOOKINGS + "/" + own.toString().replace("-", "")),
                get(detail).param("x", "1"),
                get(detail).param("status", "ACTIVE"),
                get(detail).param("actorId", S1.toString()),
                get(detail + "?flag"));
        for (MockHttpServletRequestBuilder request : invalid) {
            MvcResult result = perform(request, session(S1));
            error(result, 400, "VALIDATION_FAILED");
            assertFalse(result.getResponse().getContentAsString().contains(own.toString()), "400 body echoes booking data");
        }
        // Valid controls.
        ok(get(BOOKINGS).param("status", "ACTIVE", "CANCELLED").param("date", "2026-10-21"), session(S1), 200);
        ok(get(detail), session(S1), 200);

        // Evaluation order (TECH §3.2): anonymous with bad input is a filter 401.
        error(perform(get(BOOKINGS).param("status", "PENDING"), null), 401, "UNAUTHENTICATED");
        error(perform(get(BOOKINGS + "/not-a-uuid"), null), 401, "UNAUTHENTICATED");
        // Syntax precedes the actor check and never invalidates, even for an inactive or deleted actor.
        MockHttpSession deleted = session(id(999));
        error(perform(get(BOOKINGS).param("x", "1"), deleted), 400, "VALIDATION_FAILED");
        assertFalse(deleted.isInvalid(), "Syntax error invalidated the session");
        UUID s2Own = booking(S2, slot(T2, M2, START, END, "BOOKED"), "ACTIVE");
        MockHttpSession inactive = session(S2);
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        error(perform(get(BOOKINGS).param("status", "PENDING"), inactive), 400, "VALIDATION_FAILED");
        error(perform(get(BOOKINGS + "/not-a-uuid"), inactive), 400, "VALIDATION_FAILED");
        error(perform(get(BOOKINGS + "/" + s2Own).param("x", "1"), inactive), 400, "VALIDATION_FAILED");
        assertFalse(inactive.isInvalid(), "Syntax error invalidated the session");
        error(perform(get(BOOKINGS), inactive), 403, "ACCOUNT_INACTIVE");
        assertTrue(inactive.isInvalid());
    }

    /**
     * C3-ACC-1: TECH §3.2 access matrix on both endpoints with valid input. No principal 401; provider
     * without actor 401; principal not in users 401 + invalidated, then 401; inactive student 403
     * ACCOUNT_INACTIVE + invalidated, then 401; active tutor/admin even with ROLE_STUDENT 403 FORBIDDEN;
     * a student carrying ROLE_TUTOR still succeeds (stored role wins). No denial body carries booking data.
     */
    @Test
    void c3Acc1AccessMatrixOnListAndDetail() throws Exception {
        UUID s1Slot = slot(T1, M, START, END, "BOOKED");
        UUID s1Booking = booking(S1, s1Slot, "ACTIVE");
        UUID s2Booking = booking(S2, slot(T1, M2, END, END.plusSeconds(1800), "BOOKED"), "ACTIVE");
        List<String> s1Paths = List.of(BOOKINGS, BOOKINGS + "/" + s1Booking);
        List<String> s2Paths = List.of(BOOKINGS, BOOKINGS + "/" + s2Booking);
        List<String> leaks = List.of(s1Booking.toString(), s2Booking.toString(), s1Slot.toString(), "CS10", "CS11", "Tutor One");

        for (String path : s1Paths) {
            denied(perform(get(path), null), 401, "UNAUTHENTICATED", leaks);
            denied(perform(get(path), session("not-a-provider-actor", "ROLE_STUDENT")), 401, "UNAUTHENTICATED", leaks);

            MockHttpSession deleted = session(id(999));
            denied(perform(get(path), deleted), 401, "UNAUTHENTICATED", leaks);
            assertTrue(deleted.isInvalid(), path + " did not invalidate the deleted actor's session");
            denied(perform(get(path), deleted), 401, "UNAUTHENTICATED", leaks);

            for (UUID wrong : List.of(T1, ADMIN)) {
                MockHttpSession wrongRole = session(wrong, "ROLE_STUDENT");
                denied(perform(get(path), wrongRole), 403, "FORBIDDEN", leaks);
            }
            ok(get(path), session(S1, "ROLE_TUTOR"), 200);
        }

        List<MockHttpSession> inactiveSessions = List.of(session(S2), session(S2));
        assertEquals(1, body(ok(get(BOOKINGS), session(S2), 200)).size(), "S2 can read before deactivation");
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id", S2).update();
        for (int i = 0; i < s2Paths.size(); i++) {
            MockHttpSession inactive = inactiveSessions.get(i);
            denied(perform(get(s2Paths.get(i)), inactive), 403, "ACCOUNT_INACTIVE", leaks);
            assertTrue(inactive.isInvalid(), s2Paths.get(i) + " did not invalidate the inactive session");
            denied(perform(get(s2Paths.get(i)), inactive), 401, "UNAUTHENTICATED", leaks);
        }
    }

    // ---------------------------------------------------------------- fixture support

    private void registerStudentDependencies(ConfigurableListableBeanFactory factory) {
        factory.registerSingleton("dataSource", source);
        factory.registerSingleton("clock", clock);
        factory.registerSingleton("consultationLifecycle", lifecycle);
        factory.registerSingleton("userReads", new PostgresUserRepository(db));
        factory.registerSingleton("moduleReads", new PostgresModuleRepository(db));
        factory.registerSingleton("slotReads", new PostgresSlotRepository(db));
        factory.registerSingleton("bookingReads", new PostgresBookingRepository(db));
        StudentTestActors.registerSessionProvider(factory);
    }

    private UUID slotWithId(UUID id, UUID owner, UUID module, Instant start, Instant end, String status) {
        db.sql("INSERT INTO slots VALUES (:id,:t,:m,:s,:e,:status)").param("id", id).param("t", owner).param("m", module)
                .param("s", start.atOffset(ZoneOffset.UTC)).param("e", end.atOffset(ZoneOffset.UTC)).param("status", status).update();
        return id;
    }

    private UUID bookingWithId(UUID id, UUID student, UUID slot, String status) {
        db.sql("INSERT INTO bookings VALUES (:id,:s,:slot,:at,:status)").param("id", id).param("s", student).param("slot", slot)
                .param("at", N.atOffset(ZoneOffset.UTC)).param("status", status).update();
        return id;
    }

    private static Instant at(String instant) { return Instant.parse(instant); }

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

    private static List<UUID> ids(JsonNode array) {
        assertTrue(array.isArray(), "Expected a JSON array: " + array);
        List<UUID> ids = new ArrayList<>();
        for (JsonNode item : array) ids.add(UUID.fromString(item.get("id").asString()));
        return ids;
    }

    private static Map<UUID, Boolean> cancellableById(JsonNode array) {
        assertTrue(array.isArray(), "Expected a JSON array: " + array);
        Map<UUID, Boolean> result = new HashMap<>();
        for (JsonNode item : array) {
            assertTrue(item.get("cancellable") != null && item.get("cancellable").isBoolean(), "cancellable must be a JSON boolean: " + item);
            result.put(UUID.fromString(item.get("id").asString()), item.get("cancellable").booleanValue());
        }
        return result;
    }

    private static Map<UUID, String> statusById(JsonNode array) {
        Map<UUID, String> result = new HashMap<>();
        for (JsonNode item : array) result.put(UUID.fromString(item.get("id").asString()), item.get("status").asString());
        return result;
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
        return body;
    }

    private void denied(MvcResult result, int status, String code, List<String> leaks) throws Exception {
        error(result, status, code);
        String text = result.getResponse().getContentAsString();
        for (String leak : leaks) assertFalse(text.contains(leak), "Denial body discloses booking data (" + leak + "): " + text);
    }
}

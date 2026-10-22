package unichope.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unichope.platform.ai.AiGateway;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.auth.AuthFailure;
import unichope.platform.auth.SessionPrincipal;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.model.user.Tutor;
import unichope.platform.core.repository.BookingRepository;
import unichope.platform.core.repository.SlotRepository;
import unichope.platform.web.ApiFailure;

class TutorNoteDraftServiceTest {
    private static final UUID TUTOR_ID = id(1);
    private static final UUID BOOKING_ID = id(2);
    private static final UUID SLOT_ID = id(3);
    private static final SessionPrincipal PRINCIPAL = new SessionPrincipal(TUTOR_ID, 7);
    private static final String OUTPUT = "{\"summary\":\"Summary\",\"topicsDiscussed\":\"Topics\",\"followUp\":\"Next\"}";

    private final TutorAccess access = mock(TutorAccess.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final SlotRepository slots = mock(SlotRepository.class);
    private final AuthenticationService authentication = mock(AuthenticationService.class);
    private int gatewayCalls;
    private TutorNoteDraftService service;

    @BeforeEach
    void setUp() {
        when(access.requireActiveTutor()).thenReturn(TUTOR_ID);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(new Booking(
                BOOKING_ID, id(4), SLOT_ID, Instant.parse("2026-10-20T00:00:00Z"), BookingStatus.COMPLETED)));
        when(slots.findById(SLOT_ID)).thenReturn(Optional.of(new ConsultationSlot(
                SLOT_ID, TUTOR_ID, id(5), Instant.parse("2026-10-20T01:00:00Z"),
                Instant.parse("2026-10-20T02:00:00Z"), SlotStatus.COMPLETED)));
        when(authentication.current(PRINCIPAL)).thenReturn(new Tutor(TUTOR_ID, "Tutor", "tutor@example.edu", true));
        var gateway = new TutorNoteDraftGateway(request -> {
            gatewayCalls++;
            return new AiGateway.Result(OUTPUT, AiGateway.ProviderMode.FAKE);
        }, 256);
        service = new TutorNoteDraftService(access, bookings, slots, authentication, gateway);
    }

    @Test
    void authorizesCompletedOwnedBookingThenRevalidatesSessionAndReturnsUnpersistedDraft() {
        var result = service.generate(PRINCIPAL, BOOKING_ID, "Rough notes");

        assertEquals(BOOKING_ID, result.bookingId());
        assertTrue(result.requiresReview());
        assertEquals(AiGateway.ProviderMode.FAKE, result.providerMode());
        assertEquals("Summary\nSummary\n\nTopics discussed\nTopics\n\nFollow-up\nNext", result.draft());
        assertEquals(1, gatewayCalls);
        verify(authentication, times(2)).current(PRINCIPAL);
        verify(access).requireActiveTutor();
    }

    @Test
    void hidesOtherTutorsBookingAndDoesNotCallGateway() {
        when(slots.findById(SLOT_ID)).thenReturn(Optional.of(new ConsultationSlot(
                SLOT_ID, id(9), id(5), Instant.parse("2026-10-20T01:00:00Z"),
                Instant.parse("2026-10-20T02:00:00Z"), SlotStatus.COMPLETED)));

        var failure = assertThrows(LifecycleFailure.class,
                () -> service.generate(PRINCIPAL, BOOKING_ID, "Rough notes"));

        assertEquals(LifecycleFailure.Code.NOT_FOUND, failure.code());
        assertEquals(0, gatewayCalls);
        verify(authentication, times(1)).current(PRINCIPAL);
    }

    @Test
    void rejectsIncompleteConsultationBeforeCallingGateway() {
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(new Booking(
                BOOKING_ID, id(4), SLOT_ID, Instant.parse("2026-10-20T00:00:00Z"), BookingStatus.ACTIVE)));

        var failure = assertThrows(LifecycleFailure.class,
                () -> service.generate(PRINCIPAL, BOOKING_ID, "Rough notes"));

        assertEquals(LifecycleFailure.Code.INVALID_STATE, failure.code());
        assertEquals(0, gatewayCalls);
    }

    @Test
    void rejectsWrongRoleBeforeCheckingBookingOrCallingGateway() {
        when(authentication.current(PRINCIPAL)).thenReturn(
                new unichope.platform.core.model.user.Student(TUTOR_ID, "Student", "student@example.edu", true));

        var failure = assertThrows(ApiFailure.class,
                () -> service.generate(PRINCIPAL, BOOKING_ID, "Rough notes"));

        assertEquals(403, failure.status());
        assertEquals("FORBIDDEN", failure.code());
        assertEquals(0, gatewayCalls);
        verifyNoInteractions(bookings, slots, access);
    }

    @Test
    void discardsGeneratedDraftWhenSessionIsRevokedDuringGeneration() {
        when(authentication.current(PRINCIPAL))
                .thenReturn(new Tutor(TUTOR_ID, "Tutor", "tutor@example.edu", true))
                .thenThrow(new AuthFailure(401, "UNAUTHENTICATED"));

        var failure = assertThrows(ApiFailure.class,
                () -> service.generate(PRINCIPAL, BOOKING_ID, "Rough notes"));

        assertEquals(401, failure.status());
        assertEquals("UNAUTHENTICATED", failure.code());
        assertEquals(1, gatewayCalls);
    }

    @Test
    void blocksInactiveAccountBeforeGenerationAndDiscardsOutputIfDeactivatedDuringCall() {
        when(authentication.current(PRINCIPAL)).thenThrow(new AuthFailure(403, "ACCOUNT_INACTIVE"));

        var before = assertThrows(ApiFailure.class,
                () -> service.generate(PRINCIPAL, BOOKING_ID, "Rough notes"));

        assertEquals(403, before.status());
        assertEquals("ACCOUNT_INACTIVE", before.code());
        assertEquals(0, gatewayCalls);
        verifyNoInteractions(access, bookings, slots);

        reset(authentication);
        when(authentication.current(PRINCIPAL))
                .thenReturn(new Tutor(TUTOR_ID, "Tutor", "tutor@example.edu", true))
                .thenThrow(new AuthFailure(403, "ACCOUNT_INACTIVE"));

        var after = assertThrows(ApiFailure.class,
                () -> service.generate(PRINCIPAL, BOOKING_ID, "Rough notes"));

        assertEquals(403, after.status());
        assertEquals("ACCOUNT_INACTIVE", after.code());
        assertEquals(1, gatewayCalls);
    }

    private static UUID id(long value) {
        return new UUID(0, value);
    }
}

package unichope.tutor;

import java.util.UUID;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.auth.AuthFailure;
import unichope.platform.auth.SessionPrincipal;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.AccountFailure;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.repository.BookingRepository;
import unichope.platform.core.repository.SlotRepository;
import unichope.platform.web.ApiFailure;

/** Authorizes a completed Tutor booking around a no-write AI draft request. */
public final class TutorNoteDraftService {
    private final TutorAccess access;
    private final BookingRepository bookings;
    private final SlotRepository slots;
    private final AuthenticationService authentication;
    private final TutorNoteDraftGateway drafts;

    public TutorNoteDraftService(TutorAccess access, BookingRepository bookings, SlotRepository slots,
            AuthenticationService authentication, TutorNoteDraftGateway drafts) {
        this.access = access;
        this.bookings = bookings;
        this.slots = slots;
        this.authentication = authentication;
        this.drafts = drafts;
    }

    public Result generate(SessionPrincipal principal, UUID bookingId, String roughNotes) {
        requireTutor(principal);
        UUID tutorId = access.requireActiveTutor();
        if (!tutorId.equals(principal.userId())) throw unauthenticated();

        var booking = bookings.findById(bookingId)
                .orElseThrow(() -> new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND));
        var slot = slots.findById(booking.slotId())
                .orElseThrow(() -> new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND));
        if (!slot.tutorId().equals(tutorId)) throw new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND);
        if (booking.status() != BookingStatus.COMPLETED || slot.status() != SlotStatus.COMPLETED) {
            throw new LifecycleFailure(LifecycleFailure.Code.INVALID_STATE);
        }

        TutorNoteDraftGateway.Draft generated = drafts.generate(tutorId, roughNotes);
        requireTutor(principal);
        return new Result(bookingId, generated.text(), generated.providerMode());
    }

    private void requireTutor(SessionPrincipal principal) {
        try {
            var user = authentication.current(principal);
            if (user.role() != Role.TUTOR) throw new ApiFailure(403, "FORBIDDEN", "Access is forbidden");
        } catch (AuthFailure failure) {
            String message = failure.code().equals("ACCOUNT_INACTIVE")
                    ? "Account is inactive" : "Authentication is no longer valid";
            throw new ApiFailure(failure.status(), failure.code(), message);
        } catch (AccountFailure failure) {
            throw new ApiFailure(503, "TEMPORARILY_UNAVAILABLE", "Authentication is temporarily unavailable");
        }
    }

    private static ApiFailure unauthenticated() {
        return new ApiFailure(401, "UNAUTHENTICATED", "Authentication is no longer valid");
    }

    public record Result(UUID bookingId, String draft, unichope.platform.ai.AiGateway.ProviderMode providerMode) {
        public boolean requiresReview() { return true; }
    }
}

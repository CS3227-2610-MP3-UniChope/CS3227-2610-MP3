package unichope.student.http;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.web.ApiFailure;
import unichope.student.StudentAccess;
import unichope.student.StudentAccessDeniedException;
import unichope.student.StudentBooking;
import unichope.student.StudentReadService;

/** Student mutations use the shared atomic lifecycle, followed by an owned read projection. */
public final class StudentHttpService {
    private final StudentAccess access;
    private final ConsultationLifecycle lifecycle;
    private final StudentReadService reads;

    public StudentHttpService(StudentAccess access, ConsultationLifecycle lifecycle, StudentReadService reads) {
        this.access = access;
        this.lifecycle = lifecycle;
        this.reads = reads;
    }

    public StudentBooking book(UUID slotId) {
        UUID actor = access.requireActiveStudent();
        Booking booking = mutate(() -> lifecycle.bookAvailableSlot(actor, slotId), true);
        return project(booking);
    }

    public StudentBooking cancel(UUID bookingId) {
        UUID actor = access.requireActiveStudent();
        Booking booking = mutate(() -> lifecycle.cancelActiveBooking(actor, bookingId), false);
        return project(booking);
    }

    private Booking mutate(Supplier<Booking> operation, boolean booking) {
        try {
            return operation.get();
        } catch (LifecycleFailure failure) {
            if (failure.code() == LifecycleFailure.Code.ACCESS_DENIED) {
                access.requireActiveStudent();
                throw new StudentAccessDeniedException(StudentAccessDeniedException.Reason.FORBIDDEN);
            }
            throw switch (failure.code()) {
                case INVALID_INPUT -> new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed");
                case TEMPORARILY_UNAVAILABLE -> new ApiFailure(503, "TEMPORARILY_UNAVAILABLE", "The consultation operation is temporarily unavailable");
                case NOT_FOUND -> booking ? unavailable() : new ApiFailure(404, "NOT_FOUND", "Resource not found");
                case INVALID_STATE -> booking ? unavailable() : new ApiFailure(409, "INVALID_STATE", "The consultation transition is no longer available");
                case MODULE_UNAVAILABLE -> booking ? unavailable() : internal();
                case BOOKING_OVERLAP -> booking
                        ? new ApiFailure(409, "BOOKING_OVERLAP", "This slot overlaps one of your active bookings.") : internal();
                default -> internal();
            };
        } catch (DataIntegrityViolationException failure) {
            if (booking) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    if (cause.getMessage() != null && cause.getMessage().contains("bookings_one_active_per_slot"))
                        throw unavailable();
                }
            }
            throw failure;
        }
    }

    private StudentBooking project(Booking booking) {
        // The lifecycle has committed; a failed response read must never retry the mutation.
        return reads.myBooking(booking.id()).orElseThrow(IllegalStateException::new);
    }

    private static ApiFailure unavailable() {
        return new ApiFailure(409, "SLOT_UNAVAILABLE", "This slot is no longer available. Refresh the list and choose another.");
    }

    private static ApiFailure internal() {
        return new ApiFailure(500, "INTERNAL_ERROR", "The request could not be completed");
    }
}

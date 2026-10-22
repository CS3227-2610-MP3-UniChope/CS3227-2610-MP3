package unichope.student.http;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.web.ApiFailure;

/** Adapter-local strict query parsing (TECH §3.1); every violation is 400 VALIDATION_FAILED. */
public final class StudentApiInput {
    private static final Pattern UUID_TEXT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern DATE_TEXT = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final Set<String> SLOT_PARAMETERS = Set.of("moduleId", "date");
    private static final Set<String> BOOKING_PARAMETERS = Set.of("status", "date");
    private static final Map<String, BookingStatus> BOOKING_STATUSES = Map.of(
            "ACTIVE", BookingStatus.ACTIVE, "CANCELLED", BookingStatus.CANCELLED, "COMPLETED", BookingStatus.COMPLETED);

    private StudentApiInput() {}

    /** Parsed {@code GET /slots} filters. */
    public record SlotFilter(Optional<UUID> moduleId, Optional<LocalDate> date) {}

    /** Parsed {@code GET /bookings} filters; an empty status set means every status. */
    public record BookingFilter(Set<BookingStatus> statuses, Optional<LocalDate> date) {}

    /** Rejects any query parameter. */
    public static void noQuery(HttpServletRequest request) {
        if (!request.getParameterMap().isEmpty()) throw invalid();
    }

    /** {@code GET /slots}: only {@code moduleId} and {@code date}, each at most once. */
    public static SlotFilter slotFilter(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        if (!SLOT_PARAMETERS.containsAll(parameters.keySet())) throw invalid();
        return new SlotFilter(single(parameters, "moduleId").map(StudentApiInput::id),
                single(parameters, "date").map(StudentApiInput::date));
    }

    /**
     * {@code GET /bookings}: only {@code status} (zero or more times, each exactly ACTIVE, CANCELLED
     * or COMPLETED; repeated equal values are harmless) and {@code date} at most once.
     */
    public static BookingFilter bookingFilter(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        if (!BOOKING_PARAMETERS.containsAll(parameters.keySet())) throw invalid();
        Set<BookingStatus> statuses = EnumSet.noneOf(BookingStatus.class);
        String[] values = parameters.get("status");
        if (values != null) {
            for (String value : values) {
                BookingStatus status = value == null ? null : BOOKING_STATUSES.get(value);
                if (status == null) throw invalid();
                statuses.add(status);
            }
        }
        return new BookingFilter(Set.copyOf(statuses), single(parameters, "date").map(StudentApiInput::date));
    }

    /** Canonical 8-4-4-4-12 hexadecimal UUID text. */
    public static UUID id(String value) {
        if (value == null || !UUID_TEXT.matcher(value).matches()) throw invalid();
        return UUID.fromString(value);
    }

    /** Exactly {@code YYYY-MM-DD} and a real calendar date. */
    public static LocalDate date(String value) {
        if (value == null || !DATE_TEXT.matcher(value).matches()) throw invalid();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException error) {
            throw invalid();
        }
    }

    private static Optional<String> single(Map<String, String[]> parameters, String name) {
        String[] values = parameters.get(name);
        if (values == null) return Optional.empty();
        if (values.length != 1) throw invalid();
        return Optional.of(values[0]);
    }

    private static ApiFailure invalid() {
        return new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed");
    }
}

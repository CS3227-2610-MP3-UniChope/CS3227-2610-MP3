package unichope.platform.core;

/** Safe business outcome; database diagnostics and identifiers are never public messages. */
public final class LifecycleFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Code {
        INVALID_INPUT, ACCESS_DENIED, NOT_FOUND, INVALID_TIME, MODULE_UNAVAILABLE,
        SLOT_OVERLAP, BOOKING_OVERLAP, INVALID_STATE, TEMPORARILY_UNAVAILABLE
    }

    private final Code code;

    public LifecycleFailure(Code code) {
        super(switch (code) {
            case INVALID_INPUT -> "Required input is missing";
            case ACCESS_DENIED -> "An active account with the required role is needed";
            case NOT_FOUND -> "Consultation resource not found";
            case INVALID_TIME -> "A valid future consultation interval is required";
            case MODULE_UNAVAILABLE -> "Consultation eligibility is unavailable";
            case SLOT_OVERLAP -> "The consultation interval overlaps an existing slot";
            case BOOKING_OVERLAP -> "The consultation interval overlaps an active booking";
            case INVALID_STATE -> "The consultation transition is no longer available";
            case TEMPORARILY_UNAVAILABLE -> "The consultation operation is temporarily unavailable";
        });
        this.code = code;
    }

    public Code code() { return code; }
}

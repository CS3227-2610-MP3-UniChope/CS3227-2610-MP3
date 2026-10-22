package unichope.student;

/** Student access check failure; the HTTP adapter maps each reason per TECH §3.2. */
public final class StudentAccessDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        /** The provider returned no actor: 401, session left as is. */
        NO_ACTOR,
        /** The actor ID is not in {@code users}: 401 and session invalidated. */
        UNKNOWN_ACCOUNT,
        /** The stored account is inactive: 403 ACCOUNT_INACTIVE and session invalidated. */
        INACTIVE,
        /** Active account whose stored role is not STUDENT: 403 FORBIDDEN. */
        FORBIDDEN
    }

    private final Reason reason;

    public StudentAccessDeniedException(Reason reason) {
        super("An active student account is required");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}

package unichope.tutor;

public class TutorAccessDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Reason { UNAUTHENTICATED, INACTIVE, FORBIDDEN }
    private final Reason reason;
    public TutorAccessDeniedException() { this(Reason.FORBIDDEN); }
    public TutorAccessDeniedException(Reason reason) {
        super("An active tutor account is required");
        this.reason=reason;
    }
    public Reason reason() { return reason; }
}

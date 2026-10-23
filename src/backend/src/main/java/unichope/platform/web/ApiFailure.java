package unichope.platform.web;

/** Adapter validation failure with fixed public diagnostics. */
public final class ApiFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String code;
    private final Long retryAfterSeconds;
    public ApiFailure(int status, String code, String message) { this(status, code, message, null); }
    public ApiFailure(int status, String code, String message, Long retryAfterSeconds) {
        super(message); this.status=status; this.code=code;
        if (retryAfterSeconds != null && retryAfterSeconds < 1) throw new IllegalArgumentException("Retry seconds must be positive");
        this.retryAfterSeconds=retryAfterSeconds;
    }
    public int status() { return status; }
    public String code() { return code; }
    public Long retryAfterSeconds() { return retryAfterSeconds; }
}

package unichope.platform.web;

/** Adapter validation failure with fixed public diagnostics. */
public final class ApiFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String code;
    public ApiFailure(int status, String code, String message) { super(message); this.status=status; this.code=code; }
    public int status() { return status; }
    public String code() { return code; }
}

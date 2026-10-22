package unichope.platform.core;
/** Safe account outcome. SQL exceptions and their sensitive detail are not retained. */
public final class AccountFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Code { INVALID_INPUT, ACCESS_DENIED, NOT_FOUND, INVALID_STATE, ACCOUNT_UNAVAILABLE, LOGIN_REJECTED, TEMPORARILY_UNAVAILABLE, STORAGE_FAILURE }
    private final Code code;
    public AccountFailure(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}

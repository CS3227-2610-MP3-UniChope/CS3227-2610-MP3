package unichope.platform.auth;

public final class AuthFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final int status;
  private final String code;
  private final int retryAfter;

  public AuthFailure(int status, String code) {
    this(status, code, 0);
  }

  public AuthFailure(int status, String code, int retryAfter) {
    super(code);
    this.status = status;
    this.code = code;
    this.retryAfter = retryAfter;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public int retryAfter() {
    return retryAfter;
  }
}

package unichope.platform.auth;

import java.time.*;
import java.util.Locale;
import java.util.concurrent.*;
import unichope.platform.core.*;
import unichope.platform.core.model.user.*;

/** Password work is bounded; core rechecks the verified snapshot under its account lock. */
public final class AuthenticationService implements AutoCloseable {
  private final AccountStorage accounts;
  private final PasswordHasher hasher;
  private final LoginLimiter limiter;
  private final Clock clock;
  private final String dummy;
  private final ThreadPoolExecutor workers =
      new ThreadPoolExecutor(
          4,
          4,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(16),
          r -> {
            Thread t = new Thread(r, "password-verification");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());

  public AuthenticationService(
      AccountStorage accounts, PasswordHasher hasher, LoginLimiter limiter, Clock clock) {
    this.accounts = accounts;
    this.hasher = hasher;
    this.limiter = limiter;
    this.clock = clock;
    this.dummy = hasher.hash("dummy-only-not-a-real-account");
  }

  public record LoginResult(SessionPrincipal principal, User user, long deadlineNanos) {
    public void requireUnexpired() {
      if (System.nanoTime() - deadlineNanos >= 0)
        throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
    }
  }

  public SessionPrincipal login(String email, String password) {
    return verifyLogin(email, password).principal();
  }

  public LoginResult verifyLogin(String email, String password) {
    long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    String key = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    var reservation = limiter.reserve(key);
    boolean success = false;
    try {
      AccountState confirmed =
          work(
              () -> {
                var snapshot =
                    validEmail(key)
                        ? accounts.findCredentialByEmail(key)
                        : java.util.Optional.<AccountCredentialSnapshot>empty();
                String candidate =
                    password != null && password.codePointCount(0, password.length()) <= 128
                        ? password
                        : "";
                boolean matches =
                    hasher.matches(
                        candidate,
                        snapshot.map(AccountCredentialSnapshot::encodedHash).orElse(dummy));
                if (!matches
                    || !validEmail(key)
                    || snapshot.isEmpty()
                    || !snapshot.get().user().isActive()
                    || !validPassword(password)) throw new AuthFailure(401, "INVALID_CREDENTIALS");
                if (!limiter.valid(reservation))
                  throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
                var observed = snapshot.get();
                try {
                  return accounts.confirmVerifiedLogin(
                      observed.user().id(),
                      observed.user().role(),
                      observed.encodedHash(),
                      observed.sessionGeneration());
                } catch (AccountFailure failure) {
                  if (failure.code() == AccountFailure.Code.LOGIN_REJECTED)
                    throw new AuthFailure(401, "INVALID_CREDENTIALS");
                  throw failure;
                }
              },
              reservation.expires());
      if (!limiter.valid(reservation)) throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
      var result =
          new LoginResult(
              new SessionPrincipal(confirmed.user().id(), confirmed.sessionGeneration()),
              confirmed.user(),
              deadlineNanos);
      result.requireUnexpired();
      success = true;
      return result;
    } finally {
      limiter.finish(reservation, success);
    }
  }

  public User current(SessionPrincipal principal) {
    if (principal == null) throw new AuthFailure(401, "UNAUTHENTICATED");
    AccountState state =
        accounts
            .findAccountState(principal.userId())
            .orElseThrow(() -> new AuthFailure(401, "UNAUTHENTICATED"));
    if (!state.user().isActive()) throw new AuthFailure(403, "ACCOUNT_INACTIVE");
    if (state.sessionGeneration() != principal.generation())
      throw new AuthFailure(401, "UNAUTHENTICATED");
    return state.user();
  }

  public User register(String name, String email, String password, String confirmation) {
    validate(name, email, password);
    if (!password.equals(confirmation)) throw new AuthFailure(400, "VALIDATION_FAILED");
    return accounts.registerStudent(
        name.strip(), email.strip().toLowerCase(Locale.ROOT), hash(password));
  }

  public User provision(AdminContext actor, Role role, String name, String email, String password) {
    validate(name, email, password);
    if (role != Role.STUDENT && role != Role.TUTOR) throw new AuthFailure(400, "VALIDATION_FAILED");
    return accounts.provisionAccount(
        actor, role, name.strip(), email.strip().toLowerCase(Locale.ROOT), hash(password));
  }

  private String hash(String password) {
    return work(() -> hasher.hash(password), clock.instant().plusSeconds(10));
  }

  private <T> T work(Callable<T> task, Instant deadline) {
    Future<T> future;
    try {
      future = workers.submit(task);
    } catch (RejectedExecutionException failure) {
      throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
    }
    try {
      return future.get(
          Math.max(1, Duration.between(clock.instant(), deadline).toMillis()),
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
    } catch (ExecutionException failure) {
      if (failure.getCause() instanceof AuthFailure auth) throw auth;
      if (failure.getCause() instanceof AccountFailure account) throw account;
      throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
    } catch (TimeoutException failure) {
      future.cancel(true);
      throw new AuthFailure(503, "TEMPORARILY_UNAVAILABLE");
    }
  }

  private static void validate(String name, String email, String password) {
    if (name == null
        || name.strip().isEmpty()
        || name.strip().codePointCount(0, name.strip().length()) > 100
        || email == null
        || !validEmail(email.strip().toLowerCase(Locale.ROOT))
        || !validPassword(password)) throw new AuthFailure(400, "VALIDATION_FAILED");
  }

  private static boolean validPassword(String p) {
    return p != null
        && p.codePointCount(0, p.length()) >= 8
        && p.codePointCount(0, p.length()) <= 128;
  }

  private static boolean validEmail(String e) {
    return e.codePointCount(0, e.length()) <= 254
        && e.indexOf('@') > 0
        && e.indexOf('@') == e.lastIndexOf('@')
        && e.indexOf('@') < e.length() - 1
        && e.codePoints()
            .noneMatch(
                c ->
                    Character.isWhitespace(c)
                        || Character.isSpaceChar(c)
                        || Character.isISOControl(c));
  }

  public void close() {
    workers.shutdownNow();
  }
}

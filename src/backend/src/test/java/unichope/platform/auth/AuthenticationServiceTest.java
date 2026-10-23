package unichope.platform.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import unichope.platform.core.*;
import unichope.platform.core.model.user.*;

class AuthenticationServiceTest {
  private final AccountStorage accounts = mock(AccountStorage.class);
  private final PasswordHasher hasher =
      new PasswordHasher() {
        public String hash(String p) {
          return "hash:" + p;
        }

        public boolean matches(String p, String h) {
          return h.equals(hash(p));
        }
      };
  private final Clock clock = Clock.systemUTC();
  private final User user = new Student(UUID.randomUUID(), "Student", "s@example.test", true);

  private AuthenticationService service() {
    return new AuthenticationService(accounts, hasher, new LoginLimiter(clock), clock);
  }

  @Test
  void successfulLoginConfirmsExactCredentialSnapshot() {
    when(accounts.findCredentialByEmail(user.email()))
        .thenReturn(Optional.of(new AccountCredentialSnapshot(user, "hash:password", 3)));
    when(accounts.confirmVerifiedLogin(user.id(), Role.STUDENT, "hash:password", 3))
        .thenReturn(new AccountState(user, 3));
    try (var s = service()) {
      assertEquals(new SessionPrincipal(user.id(), 3), s.login(" S@EXAMPLE.TEST ", "password"));
    }
  }

  @Test
  void staleSnapshotNeverAuthenticates() {
    when(accounts.findCredentialByEmail(user.email()))
        .thenReturn(Optional.of(new AccountCredentialSnapshot(user, "hash:password", 3)));
    when(accounts.confirmVerifiedLogin(any(), any(), any(), anyLong()))
        .thenThrow(new AccountFailure(AccountFailure.Code.LOGIN_REJECTED));
    try (var s = service()) {
      assertEquals(
          "INVALID_CREDENTIALS",
          assertThrows(AuthFailure.class, () -> s.login(user.email(), "password")).code());
    }
  }

  @Test
  void invalidEmailUsesGenericFailureWithoutStorageLookup() {
    try (var s = service()) {
      assertEquals(401, assertThrows(AuthFailure.class, () -> s.login("bad", "password")).status());
      verifyNoInteractions(accounts);
    }
  }

  @Test
  void deactivationAndGenerationRevocationAreDistinct() {
    try (var s = service()) {
      when(accounts.findAccountState(user.id()))
          .thenReturn(Optional.of(new AccountState(user.withActive(false), 4)));
      assertEquals(
          "ACCOUNT_INACTIVE",
          assertThrows(AuthFailure.class, () -> s.current(new SessionPrincipal(user.id(), 3)))
              .code());
      when(accounts.findAccountState(user.id())).thenReturn(Optional.of(new AccountState(user, 4)));
      assertEquals(
          "UNAUTHENTICATED",
          assertThrows(AuthFailure.class, () -> s.current(new SessionPrincipal(user.id(), 3)))
              .code());
    }
  }

  @Test
  void registrationNormalizesIdentityButPreservesPassword() {
    try (var s = service()) {
      s.register(" Student ", " S@EXAMPLE.TEST ", " password ", " password ");
      verify(accounts).registerStudent("Student", "s@example.test", "hash: password ");
    }
  }

  @Test
  void invalidRegistrationNeverWrites() {
    try (var s = service()) {
      assertThrows(
          AuthFailure.class, () -> s.register("Student", user.email(), "password", "different"));
      verifyNoInteractions(accounts);
    }
  }

  @Test
  void forcedPasswordChangeRequiresMatchingPolicyAndRevokesThroughStorage() {
    User tutor = new Tutor(UUID.randomUUID(), "Tutor", "tutor@example.test", true);
    var principal = new SessionPrincipal(tutor.id(), 4);
    when(accounts.findAccountState(tutor.id())).thenReturn(Optional.of(new AccountState(tutor, 4, true)));
    when(accounts.changeTutorPassword(tutor.id(), 4, "hash:New-password-2026"))
        .thenReturn(new AccountState(tutor, 5, false));
    try (var s = service()) {
      s.changeTutorPassword(principal, "New-password-2026", "New-password-2026");
      verify(accounts).changeTutorPassword(tutor.id(), 4, "hash:New-password-2026");
    }
  }

  @Test
  void passwordChangeRejectsMismatchWeakPasswordAndNonForcedAccount() {
    User tutor = new Tutor(UUID.randomUUID(), "Tutor", "tutor@example.test", true);
    when(accounts.findAccountState(tutor.id())).thenReturn(Optional.of(new AccountState(tutor, 4, true)));
    try (var s = service()) {
      assertEquals("VALIDATION_FAILED", assertThrows(AuthFailure.class,
          () -> s.changeTutorPassword(new SessionPrincipal(tutor.id(), 4), "short", "different")).code());
      assertEquals("VALIDATION_FAILED", assertThrows(AuthFailure.class,
          () -> s.changeTutorPassword(new SessionPrincipal(tutor.id(), 4), "short", "short")).code());
      verify(accounts, never()).changeTutorPassword(any(), anyLong(), anyString());
    }
  }

  @Test
  void verificationFinishingAfterDeadlineCannotIssueSession() {
    var mutable = new LoginLimiterExpiryTest.MutableClock();
    var slowHasher =
        new PasswordHasher() {
          public String hash(String password) {
            return "hash:" + password;
          }

          public boolean matches(String password, String hash) {
            mutable.now = mutable.now.plusSeconds(11);
            return true;
          }
        };
    when(accounts.findCredentialByEmail(user.email()))
        .thenReturn(Optional.of(new AccountCredentialSnapshot(user, "hash:password", 3)));
    when(accounts.confirmVerifiedLogin(user.id(), Role.STUDENT, "hash:password", 3))
        .thenReturn(new AccountState(user, 3));
    try (var service =
        new AuthenticationService(accounts, slowHasher, new LoginLimiter(mutable), mutable)) {
      assertEquals(
          "TEMPORARILY_UNAVAILABLE",
          assertThrows(AuthFailure.class, () -> service.login(user.email(), "password")).code());
    }
  }

  @Test
  void slowStorageIsBoundedByLoginDeadline() {
    when(accounts.findCredentialByEmail(user.email()))
        .thenAnswer(
            invocation -> {
              new java.util.concurrent.CountDownLatch(1).await();
              return Optional.empty();
            });
    var shortLimiter =
        new LoginLimiter(clock, 5, 100, Duration.ofMinutes(15), Duration.ofMillis(50));
    try (var service = new AuthenticationService(accounts, hasher, shortLimiter, clock)) {
      assertTimeoutPreemptively(
          Duration.ofSeconds(2),
          () -> {
            assertEquals(
                "TEMPORARILY_UNAVAILABLE",
                assertThrows(AuthFailure.class, () -> service.login(user.email(), "password"))
                    .code());
          });
      verify(accounts, never()).confirmVerifiedLogin(any(), any(), any(), anyLong());
    }
  }

  @Test
  void incorrectPasswordStillReturnsUnauthorizedFromWorker() {
    when(accounts.findCredentialByEmail(user.email()))
        .thenReturn(Optional.of(new AccountCredentialSnapshot(user, "hash:password", 3)));
    try (var service = service()) {
      assertEquals(
          401,
          assertThrows(AuthFailure.class, () -> service.login(user.email(), "incorrect")).status());
    }
  }

  @Test
  void realArgon2RoundTrip() {
    var h = new Argon2PasswordHasher();
    String encoded = h.hash("password123");
    assertTrue(encoded.startsWith("$argon2id$"));
    assertTrue(h.matches("password123", encoded));
    assertFalse(h.matches("wrong", encoded));
  }
}

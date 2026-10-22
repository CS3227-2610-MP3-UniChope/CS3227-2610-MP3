package unichope.platform.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import unichope.platform.web.ApiFailure;

class AdmittedAiGatewayTest {
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000007");
  private static final String TASK = "tutor.note-draft.v1";
  private static final AiGateway.Request REQUEST =
      new AiGateway.Request(ACTOR, TASK, "fixed instruction", "synthetic input", 128);

  @Test
  void routesByServerTaskPolicyAdmitsMetadataOnlyAndCompletesReservation() {
    var admission = new RecordingAdmission();
    var calls = new AtomicInteger();
    var selectedKey = new AtomicInteger(-1);
    AiGateway provider =
        request -> {
          calls.incrementAndGet();
          return new AiGateway.Result("draft", AiGateway.ProviderMode.FAKE);
        };

    try (var gateway = gateway(admission, key -> {
      selectedKey.set(key.ordinal());
      return provider;
    }, policy(Duration.ofSeconds(1)))) {
      assertEquals("draft", gateway.generate(REQUEST).text());
    }

    assertEquals(1, calls.get());
    assertEquals(AiProviderKey.TUTOR.ordinal(), selectedKey.get());
    assertEquals(1, admission.admitted.get());
    assertEquals(1, admission.completed.get());
    assertEquals(ACTOR, admission.attempt.actorId());
    assertEquals(TASK, admission.attempt.task());
    assertEquals(128, admission.attempt.reservedOutputTokens());
    assertFalse(admission.attempt.toString().contains("synthetic input"));
  }

  @Test
  void admissionRejectionMakesZeroProviderCallsAndPreservesRetryDelay() {
    var admission = new RecordingAdmission();
    admission.failure = new AiGateway.Failure(AiGateway.FailureCode.RATE_LIMITED, Duration.ofSeconds(27));
    var calls = new AtomicInteger();

    try (var gateway = gateway(admission, ignored -> request -> {
      calls.incrementAndGet();
      return new AiGateway.Result("draft", AiGateway.ProviderMode.FAKE);
    }, policy(Duration.ofSeconds(1)))) {
      var failure = assertThrows(AiGateway.Failure.class, () -> gateway.generate(REQUEST));
      assertEquals(AiGateway.FailureCode.RATE_LIMITED, failure.code());
      assertEquals(Duration.ofSeconds(27), failure.retryAfter().orElseThrow());
    }

    assertEquals(0, calls.get());
    assertEquals(0, admission.completed.get());
  }

  @Test
  void admissionStoreFailureFailsClosedBeforeProviderCall() {
    var admission = new RecordingAdmission();
    admission.throwUnexpected = true;
    var calls = new AtomicInteger();

    try (var gateway = gateway(admission, ignored -> request -> {
      calls.incrementAndGet();
      return new AiGateway.Result("draft", AiGateway.ProviderMode.FAKE);
    }, policy(Duration.ofSeconds(1)))) {
      var failure = assertThrows(AiGateway.Failure.class, () -> gateway.generate(REQUEST));
      assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
      assertNull(failure.getCause());
    }

    assertEquals(0, calls.get());
  }

  @Test
  void inputAndTokenBoundsRejectBeforeAdmission() {
    var admission = new RecordingAdmission();
    var calls = new AtomicInteger();
    try (var gateway = gateway(admission, ignored -> request -> {
      calls.incrementAndGet();
      return new AiGateway.Result("draft", AiGateway.ProviderMode.FAKE);
    }, policy(Duration.ofSeconds(1)))) {
      assertEquals(
          "VALIDATION_FAILED",
          assertThrows(
                  ApiFailure.class,
                  () -> gateway.generate(new AiGateway.Request(ACTOR, TASK, "i", "😀".repeat(17), 128)))
              .code());
      assertEquals(
          "VALIDATION_FAILED",
          assertThrows(
                  ApiFailure.class,
                  () -> gateway.generate(new AiGateway.Request(ACTOR, TASK, "i", "x", 257)))
              .code());
    }
    assertEquals(0, admission.admitted.get());
    assertEquals(0, calls.get());
  }

  @Test
  void oversizedProviderResultIsRejectedAndAdmissionIsReleased() {
    var admission = new RecordingAdmission();
    AiGateway provider = request -> new AiGateway.Result("😀😀😀", AiGateway.ProviderMode.FAKE);
    try (var gateway = gateway(admission, ignored -> provider, policy(Duration.ofSeconds(1)))) {
      var failure = assertThrows(AiGateway.Failure.class, () -> gateway.generate(REQUEST));
      assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
    }
    assertEquals(1, admission.completed.get());
  }

  @Test
  void timeoutDoesNotRetryOrReleaseUncertainReservation() throws Exception {
    var admission = new RecordingAdmission();
    var calls = new AtomicInteger();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    AiGateway provider =
        request -> {
          calls.incrementAndGet();
          entered.countDown();
          boolean done = false;
          while (!done) {
            try {
              release.await();
              done = true;
            } catch (InterruptedException ignored) {
              // Keep the fake blocked to model an uncertain provider transport.
            }
          }
          return new AiGateway.Result("draft", AiGateway.ProviderMode.FAKE);
        };

    try (var gateway = gateway(admission, ignored -> provider, policy(Duration.ofMillis(40)))) {
      var failure = assertThrows(AiGateway.Failure.class, () -> gateway.generate(REQUEST));
      assertEquals(AiGateway.FailureCode.TIMEOUT, failure.code());
      assertTrue(entered.await(1, TimeUnit.SECONDS));
      assertEquals(1, calls.get());
      assertEquals(0, admission.completed.get());
    } finally {
      release.countDown();
    }
  }

  @Test
  void unknownTaskIsUnavailableWithoutAdmission() {
    var admission = new RecordingAdmission();
    var request = new AiGateway.Request(ACTOR, "unregistered.task", "instruction", "input", 10);
    try (var gateway = gateway(admission, ignored -> request2 -> {
      fail("provider must not be called for an unregistered task");
      return null;
    }, policy(Duration.ofSeconds(1)))) {
      var failure = assertThrows(AiGateway.Failure.class, () -> gateway.generate(request));
      assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
    }
    assertEquals(0, admission.admitted.get());
  }

  private static AdmittedAiGateway gateway(
      RecordingAdmission admission, AiProviderRouter router, AiTaskPolicy policy) {
    return new AdmittedAiGateway(admission, router, Map.of(policy.task(), policy));
  }

  private static AiTaskPolicy policy(Duration deadline) {
    return new AiTaskPolicy(
        TASK, AiProviderKey.TUTOR, 3, Duration.ofMinutes(5), 1, 16, 256, 10, deadline);
  }

  private static final class RecordingAdmission implements AiAdmissionService {
    private final AtomicInteger admitted = new AtomicInteger();
    private final AtomicInteger completed = new AtomicInteger();
    private Attempt attempt;
    private AiGateway.Failure failure;
    private boolean throwUnexpected;

    @Override
    public Reservation admit(Attempt attempt, AiTaskPolicy policy) {
      if (throwUnexpected) throw new IllegalStateException("private db detail");
      if (failure != null) throw failure;
      this.attempt = attempt;
      admitted.incrementAndGet();
      return new Reservation(UUID.randomUUID());
    }

    @Override
    public void complete(Reservation reservation) {
      completed.incrementAndGet();
    }
  }
}

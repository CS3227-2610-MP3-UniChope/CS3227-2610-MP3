package unichope.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import unichope.platform.ai.AiGateway;
import unichope.platform.core.AdminContext;

class ActivityReportServiceTest {
  AdminContext actor = new AdminContext(UUID.randomUUID(), 0);
  AdminRepository repo = mock(AdminRepository.class);
  AiGateway gateway = mock(AiGateway.class);
  AdminActorProvider actors = mock(AdminActorProvider.class);

  AdminViews.Statistics snapshot(long n) {
    return new AdminViews.Statistics(
        Instant.EPOCH,
        n,
        0,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        List.of(new AdminViews.Group(UUID.randomUUID(), "SECRET Tutor", n)),
        List.of());
  }

  @Test
  void emptyAndDeniedMakeNoProviderCalls() {
    when(actors.currentAdmin()).thenReturn(actor);
    when(repo.statistics()).thenReturn(snapshot(0));
    var report = new ActivityReportService(actors, repo, gateway).generate();
    assertEquals("none", report.providerMode());
    verifyNoInteractions(gateway);
    when(actors.currentAdmin()).thenThrow(new IllegalStateException());
    assertThrows(
        IllegalStateException.class,
        () -> new ActivityReportService(actors, repo, gateway).generate());
    verifyNoInteractions(gateway);
  }

  @Test
  void sendsOnlyTotalsAndRechecksIdentity() {
    when(actors.currentAdmin()).thenReturn(actor);
    when(repo.statistics()).thenReturn(snapshot(4));
    when(gateway.generate(any()))
        .thenReturn(
            new AiGateway.Result(
                "{\"summary\":\"Four bookings.\",\"caveats\":[]}", AiGateway.ProviderMode.FAKE));
    var report = new ActivityReportService(actors, repo, gateway).generate();
    assertEquals("Four bookings.", report.draft());
    verify(gateway)
        .generate(
            argThat(
                r ->
                    r.actorId().equals(actor.actorId())
                        && !r.input().contains("SECRET")
                        && !r.instructions().contains(actor.actorId().toString())
                        && r.input().contains("\"total\":4")));
    verify(actors, times(2)).currentAdmin();
    verify(repo).statistics();
  }

  @Test
  void rejectsUnsafeAndMalformedOutput() {
    for (String raw :
        List.of(
            "{}",
            "{\"summary\":\"<script>x</script>\",\"caveats\":[]}",
            "{\"summary\":\"ok\",\"caveats\":[],\"action\":\"delete\"}",
            "{\"summary\":\"ok\",\"summary\":\"other\",\"caveats\":[]}",
            "{\"summary\":\"ok\",\"caveats\":[3]}",
            "{\"summary\":\"ok\",\"caveats\":[]} {}")) {
      assertEquals(
          AiGateway.FailureCode.INVALID_OUTPUT,
          assertThrows(AiGateway.Failure.class, () -> ActivityReportService.parse(raw)).code());
    }
  }

  @Test
  void revokedSessionCannotReceiveGeneratedReport() {
    when(actors.currentAdmin()).thenReturn(actor, new AdminContext(actor.actorId(), 1));
    when(repo.statistics()).thenReturn(snapshot(1));
    when(gateway.generate(any()))
        .thenReturn(
            new AiGateway.Result(
                "{\"summary\":\"One booking.\",\"caveats\":[]}", AiGateway.ProviderMode.FAKE));
    assertThrows(
        unichope.platform.web.ApiFailure.class,
        () -> new ActivityReportService(actors, repo, gateway).generate());
  }

  @Test
  void emptyReportRechecksCurrentAccountWithoutProviderCall() {
    when(actors.currentAdmin()).thenReturn(actor, new AdminContext(actor.actorId(), 1));
    when(repo.statistics()).thenReturn(snapshot(0));
    assertThrows(
        unichope.platform.web.ApiFailure.class,
        () -> new ActivityReportService(actors, repo, gateway).generate());
    verifyNoInteractions(gateway);
  }

  @Test
  void concurrentGenerationForSameAccountIsRejectedWithoutSecondProviderCall() throws Exception {
    when(actors.currentAdmin()).thenReturn(actor);
    when(repo.statistics()).thenReturn(snapshot(1));
    var entered = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    AiGateway blocking =
        request -> {
          entered.countDown();
          try {
            release.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return new AiGateway.Result(
              "{\"summary\":\"One booking.\",\"caveats\":[]}", AiGateway.ProviderMode.FAKE);
        };
    try (var service = new ActivityReportService(actors, repo, blocking);
        var caller = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var first = caller.submit(service::generate);
      try {
        assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
        assertThrows(ReportBusyFailure.class, service::generate);
      } finally {
        release.countDown();
      }
      assertEquals("fake", first.get(2, java.util.concurrent.TimeUnit.SECONDS).providerMode());
    }
  }

  @Test
  void timeoutReturnsPromptlyAndRetainsUncertainLeaseWithoutRetry() throws Exception {
    when(actors.currentAdmin()).thenReturn(actor);
    when(repo.statistics()).thenReturn(snapshot(1));
    var entered = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    AiGateway hanging =
        request -> {
          calls.incrementAndGet();
          entered.countDown();
          boolean done = false;
          while (!done) {
            try {
              release.await();
              done = true;
            } catch (InterruptedException ignored) {
              /* Simulate a provider that ignores cancellation. */
            }
          }
          return new AiGateway.Result(
              "{\"summary\":\"One booking.\",\"caveats\":[]}", AiGateway.ProviderMode.FAKE);
        };
    var time = new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.now());
    java.time.Clock clock =
        new java.time.Clock() {
          public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
          }

          public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          public java.time.Instant instant() {
            return time.get();
          }
        };
    try (var service =
            new ActivityReportService(
                actors, repo, hanging, clock, java.time.Duration.ofMillis(100));
        var caller = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var first = caller.submit(service::generate);
      try {
        assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
        var failed =
            assertThrows(
                java.util.concurrent.ExecutionException.class,
                () -> first.get(2, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(AiGateway.FailureCode.TIMEOUT, ((AiGateway.Failure) failed.getCause()).code());
        assertThrows(ReportBusyFailure.class, service::generate);
        time.set(time.get().plusSeconds(61));
        assertThrows(ReportBusyFailure.class, service::generate);
        assertEquals(1, calls.get());
      } finally {
        release.countDown();
      }
    }
  }
}

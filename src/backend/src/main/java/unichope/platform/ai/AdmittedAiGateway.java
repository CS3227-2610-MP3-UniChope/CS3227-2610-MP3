package unichope.platform.ai;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import unichope.platform.web.ApiFailure;

/** Shared policy boundary. The injected admission implementation must be durable in production. */
public final class AdmittedAiGateway implements AiGateway, AutoCloseable {
  private final AiAdmissionService admissions;
  private final AiProviderRouter providers;
  private final Map<String, AiTaskPolicy> policies;
  private final ThreadPoolExecutor workers;

  public AdmittedAiGateway(
      AiAdmissionService admissions,
      AiProviderRouter providers,
      Map<String, AiTaskPolicy> policies) {
    this.admissions = Objects.requireNonNull(admissions);
    this.providers = Objects.requireNonNull(providers);
    this.policies = Map.copyOf(policies);
    if (this.policies.isEmpty()
        || this.policies.entrySet().stream().anyMatch(e -> !e.getKey().equals(e.getValue().task()))) {
      throw new IllegalArgumentException("AI task policy keys must match registered task names.");
    }
    ThreadFactory threads =
        action -> {
          Thread thread = new Thread(action, "admitted-ai-provider");
          thread.setDaemon(true);
          return thread;
        };
    workers =
        new ThreadPoolExecutor(
            4,
            4,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16),
            threads,
            new ThreadPoolExecutor.AbortPolicy());
  }

  @Override
  public Result generate(Request request) {
    Objects.requireNonNull(request);
    AiTaskPolicy policy = policies.get(request.task());
    if (policy == null) throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
    if (request.maxOutputTokens() > policy.maxOutputTokens()
        || request.input().codePointCount(0, request.input().length()) > policy.maxInputCodePoints()) {
      throw new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed");
    }

    AiGateway provider;
    try {
      provider = Objects.requireNonNull(providers.providerFor(policy.providerKey()));
    } catch (AiGateway.Failure unavailable) {
      throw unavailable;
    } catch (RuntimeException unavailable) {
      throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
    }

    var attempt =
        new AiAdmissionService.Attempt(
            request.actorId(), request.task(), policy.providerKey(), request.maxOutputTokens());
    AiAdmissionService.Reservation reservation;
    try {
      reservation = admissions.admit(attempt, policy);
    } catch (AiGateway.Failure known) {
      throw known;
    } catch (RuntimeException unavailable) {
      throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
    }
    boolean retainLeaseUntilExpiry = false;
    try {
      Future<Result> future;
      try {
        future = workers.submit(() -> provider.generate(request));
      } catch (RejectedExecutionException rejected) {
        throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
      }

      Result result;
      try {
        result = future.get(policy.deadline().toNanos(), TimeUnit.NANOSECONDS);
      } catch (TimeoutException expired) {
        retainLeaseUntilExpiry = true;
        future.cancel(true);
        throw new AiGateway.Failure(FailureCode.TIMEOUT);
      } catch (InterruptedException interrupted) {
        retainLeaseUntilExpiry = true;
        future.cancel(true);
        Thread.currentThread().interrupt();
        throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
      } catch (ExecutionException failed) {
        if (failed.getCause() instanceof AiGateway.Failure known) throw known;
        throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
      }

      if (result.text().getBytes(StandardCharsets.UTF_8).length > policy.maxResponseBytes()) {
        throw new AiGateway.Failure(FailureCode.INVALID_OUTPUT);
      }
      return result;
    } catch (AiGateway.Failure known) {
      throw known;
    } catch (RuntimeException unexpected) {
      throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
    } finally {
      if (!retainLeaseUntilExpiry) {
        try {
          admissions.complete(reservation);
        } catch (RuntimeException unavailable) {
          throw new AiGateway.Failure(FailureCode.UNAVAILABLE);
        }
      }
    }
  }

  @Override
  public void close() {
    workers.shutdownNow();
  }
}

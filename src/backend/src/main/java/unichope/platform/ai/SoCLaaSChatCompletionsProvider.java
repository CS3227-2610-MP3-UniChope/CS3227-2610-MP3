package unichope.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import unichope.platform.ai.AiGateway.Failure;
import unichope.platform.ai.AiGateway.FailureCode;

/** Server-side SoCLaaS adapter using its published OpenAI-compatible chat endpoint. */
public final class SoCLaaSChatCompletionsProvider implements AiGateway {
  public static final URI ENDPOINT =
      URI.create("https://soclaas-api.comp.nus.edu.sg/v1/chat/completions");
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(18);
  private static final int MAX_RESPONSE_BYTES = 65_536;

  private final HttpClient client;
  private final ObjectMapper json;
  private final URI endpoint;
  private final String apiKey;
  private final String model;
  private final int maxResponseBytes;
  private final Clock clock;
  private final Duration requestTimeout;

  public SoCLaaSChatCompletionsProvider(
      String apiKey, String model, int maxResponseBytes) {
    this(
        createHttpClient(),
        new ObjectMapper(),
        ENDPOINT,
        apiKey,
        model,
        maxResponseBytes,
        Clock.systemUTC(),
        REQUEST_TIMEOUT);
  }

  static HttpClient createHttpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  SoCLaaSChatCompletionsProvider(
      HttpClient client,
      ObjectMapper json,
      URI endpoint,
      String apiKey,
      String model,
      int maxResponseBytes,
      Clock clock,
      Duration requestTimeout) {
    this.client = Objects.requireNonNull(client);
    this.json = Objects.requireNonNull(json);
    this.endpoint = Objects.requireNonNull(endpoint);
    this.apiKey = required(apiKey, "SoCLaaS API key");
    this.model = required(model, "SoCLaaS model");
    this.clock = Objects.requireNonNull(clock);
    this.requestTimeout = Objects.requireNonNull(requestTimeout);
    if (maxResponseBytes <= 0 || maxResponseBytes > MAX_RESPONSE_BYTES) {
      throw new IllegalArgumentException("Invalid SoCLaaS response byte limit.");
    }
    if (requestTimeout.isZero() || requestTimeout.isNegative()) {
      throw new IllegalArgumentException("Invalid SoCLaaS request timeout.");
    }
    this.maxResponseBytes = maxResponseBytes;
  }

  @Override
  public Result generate(Request request) {
    Objects.requireNonNull(request);
    byte[] requestBody;
    try {
      requestBody =
          json.writeValueAsBytes(
              Map.of(
                  "model",
                  model,
                  "messages",
                  List.of(
                      Map.of("role", "system", "content", request.instructions()),
                      Map.of("role", "user", "content", request.input())),
                  "max_tokens",
                  request.maxOutputTokens(),
                  "stream",
                  false));
    } catch (IOException serializationFailure) {
      throw new Failure(FailureCode.UNAVAILABLE);
    }

    HttpRequest httpRequest =
        HttpRequest.newBuilder(endpoint)
            .timeout(requestTimeout)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
            .build();

    HttpResponse<CapturedBody> response;
    try {
      response =
          client.send(
              httpRequest,
              ignored -> new BoundedBodySubscriber(maxResponseBytes));
    } catch (java.net.http.HttpTimeoutException timeout) {
      throw new Failure(FailureCode.TIMEOUT);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new Failure(FailureCode.UNAVAILABLE);
    } catch (IOException | RuntimeException transportFailure) {
      throw new Failure(FailureCode.UNAVAILABLE);
    }

    if (response.statusCode() == 429) {
      throw new Failure(
          FailureCode.RATE_LIMITED,
          retryAfter(response.headers().firstValue("Retry-After").orElse(null), clock.instant()));
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new Failure(FailureCode.UNAVAILABLE);
    }

    CapturedBody body = response.body();
    if (body.oversized()) throw new Failure(FailureCode.INVALID_OUTPUT);
    try {
      JsonNode root = json.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .readTree(body.bytes());
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (!root.isObject() || !content.isTextual() || content.textValue().isBlank()) {
        throw new Failure(FailureCode.INVALID_OUTPUT);
      }
      return new Result(content.textValue(), ProviderMode.SOC);
    } catch (IOException | RuntimeException invalidResponse) {
      if (invalidResponse instanceof Failure failure) throw failure;
      throw new Failure(FailureCode.INVALID_OUTPUT);
    }
  }

  private static Duration retryAfter(String value, Instant now) {
    if (value == null || value.isBlank()) return null;
    try {
      long seconds = Long.parseLong(value.trim());
      return seconds > 0 ? Duration.ofSeconds(seconds) : null;
    } catch (NumberFormatException notSeconds) {
      try {
        Duration delay = Duration.between(now, ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
        return delay.isPositive() ? delay : null;
      } catch (java.time.DateTimeException invalidDate) {
        return null;
      }
    }
  }

  private static String required(String value, String name) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required.");
    return value;
  }

  @Override
  public String toString() {
    return "SoCLaaSChatCompletionsProvider[configuration=redacted]";
  }

  private record CapturedBody(byte[] bytes, boolean oversized) {}

  private static final class BoundedBodySubscriber
      implements HttpResponse.BodySubscriber<CapturedBody> {
    private final int limit;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final CompletableFuture<CapturedBody> body = new CompletableFuture<>();
    private Flow.Subscription subscription;
    private boolean oversized;

    private BoundedBodySubscriber(int limit) {
      this.limit = limit;
    }

    @Override
    public java.util.concurrent.CompletionStage<CapturedBody> getBody() {
      return body;
    }

    @Override
    public void onSubscribe(Flow.Subscription value) {
      subscription = value;
      value.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
      for (ByteBuffer buffer : buffers) {
        int remaining = buffer.remaining();
        if (remaining > limit - output.size()) {
          oversized = true;
          subscription.cancel();
          body.complete(new CapturedBody(output.toByteArray(), true));
          return;
        }
        byte[] chunk = new byte[remaining];
        buffer.get(chunk);
        output.writeBytes(chunk);
      }
      subscription.request(1);
    }

    @Override
    public void onError(Throwable failure) {
      body.completeExceptionally(failure);
    }

    @Override
    public void onComplete() {
      if (!oversized) body.complete(new CapturedBody(output.toByteArray(), false));
    }
  }
}

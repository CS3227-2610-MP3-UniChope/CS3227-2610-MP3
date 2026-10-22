package unichope.platform.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoCLaaSChatCompletionsProviderTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final AiGateway.Request REQUEST =
      new AiGateway.Request(
          UUID.fromString("00000000-0000-4000-8000-000000000007"),
          "tutor.note-draft.v1",
          "Use only the supplied notes.",
          "Discussed recursion.",
          128);

  private final AtomicReference<ExchangeAction> action = new AtomicReference<>();
  private HttpServer server;
  private ExecutorService executor;
  private URI endpoint;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    executor = Executors.newCachedThreadPool(task -> {
      Thread thread = new Thread(task, "soclaas-provider-test");
      thread.setDaemon(true);
      return thread;
    });
    server.setExecutor(executor);
    server.createContext("/v1/chat/completions", exchange -> action.get().run(exchange));
    server.start();
    endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
    executor.shutdownNow();
  }

  @Test
  void sendsOnlyFixedInstructionAndRoughNotesAndExtractsText() throws Exception {
    AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    AtomicReference<String> contentType = new AtomicReference<>();
    AtomicReference<String> accept = new AtomicReference<>();
    AtomicReference<String> method = new AtomicReference<>();
    AtomicReference<String> path = new AtomicReference<>();
    action.set(exchange -> {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
      accept.set(exchange.getRequestHeaders().getFirst("Accept"));
      method.set(exchange.getRequestMethod());
      path.set(exchange.getRequestURI().getPath());
      requestBody.set(JSON.readTree(exchange.getRequestBody().readAllBytes()));
      respond(exchange, 200, """
          {"choices":[{"message":{"content":"{\\"summary\\":\\"Reviewed recursion\\",\\"topicsDiscussed\\":\\"Base cases\\",\\"followUp\\":\\"Practice examples\\"}"}}]}
          """);
    });

    var result = provider(4096, Duration.ofSeconds(2)).generate(REQUEST);

    assertEquals(AiGateway.ProviderMode.SOC, result.providerMode());
    assertTrue(result.text().contains("Reviewed recursion"));
    assertEquals("Bearer synthetic-secret", authorization.get());
    assertEquals("application/json", contentType.get());
    assertEquals("application/json", accept.get());
    assertEquals("POST", method.get());
    assertEquals("/v1/chat/completions", path.get());
    JsonNode body = requestBody.get();
    assertEquals("synthetic-model", body.get("model").asText());
    assertEquals(128, body.get("max_tokens").asInt());
    assertFalse(body.get("stream").asBoolean());
    assertEquals(2, body.get("messages").size());
    assertEquals("system", body.at("/messages/0/role").asText());
    assertEquals(REQUEST.instructions(), body.at("/messages/0/content").asText());
    assertEquals("user", body.at("/messages/1/role").asText());
    assertEquals(REQUEST.input(), body.at("/messages/1/content").asText());
    assertFalse(body.toString().contains(REQUEST.actorId().toString()));
    assertFalse(body.toString().contains(REQUEST.task()));
  }

  @Test
  void malformedOrNonTextResponsesAreRejectedWithoutUpstreamDetails() {
    for (String response : new String[] {
      "not-json",
      "{}",
      "{\"choices\":[]}",
      "{\"choices\":[{}]}",
      "{\"choices\":[{\"message\":{}}]}",
      "{\"choices\":[{\"message\":{\"content\":9}}]}",
      "{} {}"
    }) {
      action.set(exchange -> respond(exchange, 200, response));
      var failure = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
      assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
      assertFalse(failure.getMessage().contains(response));
    }
  }

  @Test
  void invalidUtf8ResponseIsRejected() {
    action.set(exchange -> respondBytes(exchange, 200, new byte[] {(byte) 0xc3, (byte) 0x28}));
    var failure = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
    assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
  }

  @Test
  void rawBodyIsCappedBeforeJsonParsing() {
    action.set(exchange -> respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"" + "x".repeat(500) + "\"}}]}"));
    var failure = assertThrows(AiGateway.Failure.class, () -> provider(128, Duration.ofSeconds(2)).generate(REQUEST));
    assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
    assertFalse(failure.getMessage().contains("xxxx"));
  }

  @Test
  void responseBodyAtByteCapIsAcceptedAndOneByteOverIsRejected() {
    String prefix = "{\"choices\":[{\"message\":{\"content\":\"";
    String suffix = "\"}}]}";
    int limit = 128;
    String exact = prefix + "x".repeat(limit - prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
        - suffix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length) + suffix;
    action.set(exchange -> respond(exchange, 200, exact));
    assertEquals(AiGateway.ProviderMode.SOC, provider(limit, Duration.ofSeconds(2)).generate(REQUEST).providerMode());

    action.set(exchange -> respond(exchange, 200, exact + " "));
    var failure = assertThrows(AiGateway.Failure.class, () -> provider(limit, Duration.ofSeconds(2)).generate(REQUEST));
    assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
  }

  @Test
  void providerRateLimitPreservesRetryAfterWithoutRetrying() {
    AtomicInteger requests = new AtomicInteger();
    action.set(exchange -> {
      requests.incrementAndGet();
      exchange.getResponseHeaders().add("Retry-After", "7");
      respond(exchange, 429, "private upstream error");
    });

    var failure = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));

    assertEquals(AiGateway.FailureCode.RATE_LIMITED, failure.code());
    assertEquals(Duration.ofSeconds(7), failure.retryAfter().orElseThrow());
    assertFalse(failure.getMessage().contains("private upstream error"));
    assertEquals(1, requests.get());
  }

  @Test
  void invalidRetryAfterIsOmittedAndOtherStatusesAreUnavailable() {
    for (String retryAfter : new String[] {"0", "not-a-date"}) {
      action.set(exchange -> {
        exchange.getResponseHeaders().add("Retry-After", retryAfter);
        respond(exchange, 429, "sensitive details");
      });
      var limited = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
      assertEquals(AiGateway.FailureCode.RATE_LIMITED, limited.code());
      assertTrue(limited.retryAfter().isEmpty());
    }
    action.set(exchange -> respond(exchange, 429, "sensitive details"));
    var missing = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
    assertTrue(missing.retryAfter().isEmpty());

    action.set(exchange -> {
      exchange.getResponseHeaders().add("Retry-After", "Fri, 23 Oct 2026 00:00:10 GMT");
      respond(exchange, 429, "sensitive details");
    });
    var future = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
    assertEquals(Duration.ofSeconds(10), future.retryAfter().orElseThrow());

    action.set(exchange -> {
      exchange.getResponseHeaders().add("Retry-After", "Thu, 22 Oct 2026 00:00:10 GMT");
      respond(exchange, 429, "sensitive details");
    });
    var past = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
    assertTrue(past.retryAfter().isEmpty());

    for (int status : new int[] {400, 503}) {
      action.set(exchange -> respond(exchange, status, "sensitive details"));
      var unavailable = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
      assertEquals(AiGateway.FailureCode.UNAVAILABLE, unavailable.code());
      assertFalse(unavailable.getMessage().contains("sensitive details"));
    }
  }

  @Test
  void requestTimeoutIsMappedWithoutRetry() {
    AtomicInteger requests = new AtomicInteger();
    action.set(exchange -> {
      requests.incrementAndGet();
      try {
        Thread.sleep(250);
        respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"late\"}}]}");
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
    });
    var failure = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofMillis(50)).generate(REQUEST));
    assertEquals(AiGateway.FailureCode.TIMEOUT, failure.code());
    assertEquals(1, requests.get());
  }

  @Test
  void redirectIsNotFollowed() {
    AtomicInteger redirected = new AtomicInteger();
    server.createContext("/redirected", exchange -> {
      redirected.incrementAndGet();
      respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"unexpected\"}}]}");
    });
    action.set(exchange -> {
      exchange.getResponseHeaders().add("Location", "/redirected");
      respond(exchange, 302, "redirect");
    });
    var failure = assertThrows(AiGateway.Failure.class, () -> provider(4096, Duration.ofSeconds(2)).generate(REQUEST));
    assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
    assertEquals(0, redirected.get());
  }

  @Test
  void providerConfigurationAndRequestRenderingAreRedacted() {
    var provider = provider(4096, Duration.ofSeconds(2));
    assertFalse(provider.toString().contains("synthetic-secret"));
    assertFalse(provider.toString().contains("synthetic-model"));
    assertFalse(REQUEST.toString().contains(REQUEST.input()));
    assertEquals(
        URI.create("https://soclaas-api.comp.nus.edu.sg/v1/chat/completions"),
        SoCLaaSChatCompletionsProvider.ENDPOINT);
  }

  @Test
  void productionConfigurationRequiresKeyModelAndBoundedResponseLimit() {
    assertThrows(IllegalArgumentException.class, () -> new SoCLaaSChatCompletionsProvider(" ", "model", 1024));
    assertThrows(IllegalArgumentException.class, () -> new SoCLaaSChatCompletionsProvider("key", " ", 1024));
    assertThrows(IllegalArgumentException.class, () -> new SoCLaaSChatCompletionsProvider("key", "model", 65_537));
  }

  private SoCLaaSChatCompletionsProvider provider(int responseBytes, Duration timeout) {
    return new SoCLaaSChatCompletionsProvider(
        SoCLaaSChatCompletionsProvider.createHttpClient(),
        JSON,
        endpoint,
        "synthetic-secret",
        "synthetic-model",
        responseBytes,
        Clock.fixed(Instant.parse("2026-10-23T00:00:00Z"), ZoneOffset.UTC),
        timeout);
  }

  private static void respond(HttpExchange exchange, int status, String response) throws IOException {
    byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    respondBytes(exchange, status, bytes);
  }

  private static void respondBytes(HttpExchange exchange, int status, byte[] bytes) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  @FunctionalInterface
  private interface ExchangeAction {
    void run(HttpExchange exchange) throws IOException;
  }
}

package unichope.platform.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.web.ApiFailure;

/** Transport tests against a local stand-in server; no real key or network access. */
class SocLlmProviderTest {
  private HttpServer server;
  private final AtomicReference<String> auth = new AtomicReference<>();
  private final AtomicReference<String> body = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String reply = "";
  private volatile long delayMillis = 0;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/chat/completions", exchange -> {
      auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
      body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      try { Thread.sleep(delayMillis); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
      byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
      if (bytes.length > 0) exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
  }

  @AfterEach
  void stop() { server.stop(0); }

  private SocLlmProvider provider() {
    return new SocLlmProvider("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
  }

  private static String completion(String content, String usage) {
    return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
        + JsonMapper.shared().writeValueAsString(content) + "}}]" + usage + "}";
  }

  @Test
  void sendsBearerKeyModelSeparatedMessagesAndOutputCap() {
    reply = completion("{\"summary\":\"ok\"}", ",\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":7}");
    var response = provider().complete("tutor-key", "qwen3.5:9b", "Instructions", "Untrusted <input>", 300, Duration.ofSeconds(5));
    assertEquals("Bearer tutor-key", auth.get());
    JsonNode sent = JsonMapper.shared().readTree(body.get());
    assertEquals("qwen3.5:9b", sent.get("model").stringValue());
    assertEquals(300, sent.get("max_tokens").intValue());
    assertEquals("none", sent.get("reasoning_effort").stringValue());
    assertEquals("system", sent.get("messages").get(0).get("role").stringValue());
    assertEquals("Instructions", sent.get("messages").get(0).get("content").stringValue());
    assertEquals("user", sent.get("messages").get(1).get("role").stringValue());
    assertEquals("Untrusted <input>", sent.get("messages").get(1).get("content").stringValue());
    assertEquals("{\"summary\":\"ok\"}", response.text());
    assertEquals(12L, response.inputTokens());
    assertEquals(7L, response.outputTokens());
  }

  @Test
  void stripsLeadingThinkingBlockAndToleratesMissingUsage() {
    reply = completion("<think>reasoning here</think>\n{\"a\":1}", "");
    var response = provider().complete("k", "qwen3.5:9b", "i", "x", 10, Duration.ofSeconds(5));
    assertEquals("{\"a\":1}", response.text());
    assertNull(response.inputTokens());
  }

  @Test
  void upstreamErrorsMapToSafeFailuresWithoutUpstreamBody() {
    reply = "{\"error\":\"secret-upstream-detail\"}";
    status = 429;
    var limited = assertThrows(ApiFailure.class, () -> provider().complete("k", "qwen3.5:9b", "i", "x", 10, Duration.ofSeconds(5)));
    assertEquals("AI_RATE_LIMITED", limited.code());
    assertFalse(limited.getMessage().contains("secret"));
    for (int code : new int[] {401, 403, 500, 502}) {
      status = code;
      var failure = assertThrows(AiGateway.Failure.class, () -> provider().complete("k", "qwen3.5:9b", "i", "x", 10, Duration.ofSeconds(5)));
      assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
      assertFalse(failure.getMessage().contains("secret"));
    }
  }

  @Test
  void malformedEmptyAndOversizedResponsesAreInvalidOutput() {
    for (String bad : new String[] {"not json", "{}", completion("   ", ""), completion("<think>never closed", ""),
        completion("x".repeat(70_000), "")}) {
      reply = bad;
      var failure = assertThrows(AiGateway.Failure.class, () -> provider().complete("k", "qwen3.5:9b", "i", "x", 10, Duration.ofSeconds(5)));
      assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
    }
  }

  @Test
  void slowProviderTimesOut() {
    reply = completion("late", "");
    delayMillis = 2000;
    var failure = assertThrows(AiGateway.Failure.class, () -> provider().complete("k", "qwen3.5:9b", "i", "x", 10, Duration.ofMillis(300)));
    assertEquals(AiGateway.FailureCode.TIMEOUT, failure.code());
  }
}

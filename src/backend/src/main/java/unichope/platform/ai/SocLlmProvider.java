package unichope.platform.ai;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import unichope.platform.web.ApiFailure;

/**
 * SoCLaaS client for its OpenAI-compatible POST /v1/chat/completions endpoint. Instructions go in the system
 * message and untrusted task input in the user message. Responses over 64 KiB, malformed bodies
 * and empty content are rejected; upstream bodies are never forwarded or logged.
 */
public final class SocLlmProvider implements AiProvider {
  static final int MAX_RESPONSE_BYTES = 65536;
  private static final JsonMapper JSON = JsonMapper.shared();
  private final URI endpoint;
  private final HttpClient http;

  /** baseUrl includes the version path, for example https://host/v1. */
  public SocLlmProvider(String baseUrl) {
    Objects.requireNonNull(baseUrl);
    String trimmed = baseUrl.strip().replaceAll("/+$", "");
    this.endpoint = URI.create(trimmed + "/chat/completions");
    if (!"https".equals(endpoint.getScheme()) && !"http".equals(endpoint.getScheme()))
      throw new IllegalArgumentException("SoC LLM URL must be http or https");
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @Override
  public Response complete(
      String apiKey,
      String model,
      String instructions,
      String input,
      int maxOutputTokens,
      Duration timeout) {
    ObjectNode body = JSON.createObjectNode();
    body.put("model", Objects.requireNonNull(model));
    body.put("max_tokens", maxOutputTokens);
    body.put("temperature", 0.2);
    body.put("stream", false);
    // SoCLaaS Qwen models otherwise spend the whole output budget in a separate reasoning field
    // and return empty content; verified against SoCLaaS, accepted by llama, gemma and qwen.
    body.put("reasoning_effort", "none");
    var messages = body.putArray("messages");
    messages.addObject().put("role", "system").put("content", instructions);
    messages.addObject().put("role", "user").put("content", input);
    HttpRequest request =
        HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
            .build();
    HttpResponse<InputStream> response;
    byte[] raw;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream stream = response.body()) {
        raw = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
      }
    } catch (HttpTimeoutException e) {
      throw new AiGateway.Failure(AiGateway.FailureCode.TIMEOUT);
    } catch (IOException e) {
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    }
    int status = response.statusCode();
    if (status == 429)
      throw new ApiFailure(429, "AI_RATE_LIMITED", "The AI service is busy. Try again shortly.");
    if (status == 408 || status == 504) throw new AiGateway.Failure(AiGateway.FailureCode.TIMEOUT);
    if (status < 200 || status >= 300)
      throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
    if (raw.length > MAX_RESPONSE_BYTES)
      throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
    return parse(raw);
  }

  static Response parse(byte[] raw) {
    try {
      JsonNode root = JSON.readTree(new String(raw, StandardCharsets.UTF_8));
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (!content.isString()) throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
      String text = stripThinking(content.stringValue()).strip();
      if (text.isEmpty()) throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
      JsonNode usage = root.path("usage");
      Long in = usage.path("prompt_tokens").isIntegralNumber() ? usage.path("prompt_tokens").longValue() : null;
      Long out =
          usage.path("completion_tokens").isIntegralNumber()
              ? usage.path("completion_tokens").longValue()
              : null;
      return new Response(text, in, out);
    } catch (JacksonException e) {
      throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
    }
  }

  /** Some Qwen3-family models prepend a reasoning block; only the answer after it is used. */
  static String stripThinking(String text) {
    String trimmed = text.stripLeading();
    if (!trimmed.startsWith("<think>")) return text;
    int end = trimmed.indexOf("</think>");
    if (end < 0) throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
    return trimmed.substring(end + "</think>".length());
  }

  @Override
  public String toString() {
    return "SocLlmProvider[endpoint=" + endpoint + "]";
  }
}

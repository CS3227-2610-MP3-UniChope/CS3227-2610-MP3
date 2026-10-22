package unichope.platform.ai;

import java.time.Duration;

/** Transport to an LLM service. Implementations never log or return keys or upstream bodies. */
public interface AiProvider {
  Response complete(
      String apiKey,
      String model,
      String instructions,
      String input,
      int maxOutputTokens,
      Duration timeout);

  /** Token counts are null when the provider does not report usage. */
  record Response(String text, Long inputTokens, Long outputTokens) {
    @Override
    public String toString() {
      return "AiProvider.Response[text=redacted, inputTokens="
          + inputTokens
          + ", outputTokens="
          + outputTokens
          + "]";
    }
  }
}

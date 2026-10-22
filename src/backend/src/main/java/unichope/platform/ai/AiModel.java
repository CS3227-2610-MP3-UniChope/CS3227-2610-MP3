package unichope.platform.ai;

/** A SoCLaaS chat model and its price in microdollars per one million tokens. */
public record AiModel(
    String id,
    int contextLength,
    long inputMicrosPerMillionTokens,
    long outputMicrosPerMillionTokens,
    String recommendedFor) {

  /** Upper-bound reservation: every prompt byte counted as a token, full output budget used. */
  public long reserveMicros(long promptBytes, int maxOutputTokens) {
    return cost(promptBytes, maxOutputTokens);
  }

  public long cost(long inputTokens, long outputTokens) {
    long micros =
        Math.ceilDiv(Math.multiplyExact(inputTokens, inputMicrosPerMillionTokens), 1_000_000L)
            + Math.ceilDiv(Math.multiplyExact(outputTokens, outputMicrosPerMillionTokens), 1_000_000L);
    return Math.max(1, micros);
  }
}

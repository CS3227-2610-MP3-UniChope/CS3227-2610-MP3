package unichope.platform.ai;

import java.util.List;
import java.util.Optional;

/**
 * Chat models an Admin may select, with SoCLaaS prices (microdollars per 1M tokens). Only listed
 * models can be chosen, so every request has a known price for budget accounting. Vision and
 * embedding/transcription models are omitted because no feature uses them.
 */
public final class AiModelCatalog {
  public static final String DEFAULT_ID = "qwen3.5:9b";

  private static final List<AiModel> MODELS =
      List.of(
          new AiModel("llama3.1:8b", 65536, 20_000, 30_000, "Capable small-model experimentation"),
          new AiModel("qwen3.5:9b", 65536, 100_000, 150_000, "Higher-quality general-purpose and multimodal work"),
          new AiModel("gemma4:26b", 131072, 60_000, 300_000, "Higher-quality general-purpose and multimodal work"),
          new AiModel("qwen3-coder-next", 196608, 110_000, 800_000, "Coding agents and software-engineering workflows"),
          new AiModel("qwen3.6:35b", 262144, 140_000, 900_000, "Efficient agentic workflows and advanced coding"),
          new AiModel("qwen3.8:27b", 262144, 195_000, 900_000, "High-quality general reasoning and multimodal tasks"),
          new AiModel("ornith1.5:35b", 262144, 260_000, 900_000, "Agentic software engineering and coding workflows"));

  private AiModelCatalog() {}

  public static List<AiModel> models() {
    return MODELS;
  }

  public static Optional<AiModel> find(String id) {
    return MODELS.stream().filter(model -> model.id().equals(id)).findFirst();
  }

  public static AiModel defaultModel() {
    return find(DEFAULT_ID).orElseThrow();
  }
}

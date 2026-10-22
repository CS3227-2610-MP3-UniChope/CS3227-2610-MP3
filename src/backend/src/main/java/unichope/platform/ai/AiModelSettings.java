package unichope.platform.ai;

import java.time.Instant;
import java.util.UUID;

/** The model used for all AI requests. Admin-selected, durable, defaulting to qwen3.5:9b. */
public interface AiModelSettings {
  Setting current();

  /** Selects a catalog model; unknown ids are rejected with 400 VALIDATION_FAILED. */
  Setting select(String modelId, UUID adminId);

  /** updatedAt is null while the default has never been changed. */
  record Setting(AiModel model, Instant updatedAt) {}
}

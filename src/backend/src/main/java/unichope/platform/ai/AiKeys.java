package unichope.platform.ai;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Server-only API keys by slot. Values are never exposed through toString, logs or errors. */
public final class AiKeys {
  private final Map<AiKeySlot, String> keys = new EnumMap<>(AiKeySlot.class);

  public AiKeys(Map<AiKeySlot, String> configured) {
    configured.forEach(
        (slot, key) -> {
          if (key != null && !key.isBlank()) keys.put(slot, key.strip());
        });
  }

  public Optional<String> key(AiKeySlot slot) {
    return Optional.ofNullable(keys.get(slot));
  }

  public boolean configured(AiKeySlot slot) {
    return keys.containsKey(slot);
  }

  @Override
  public String toString() {
    return "AiKeys[configured=" + keys.keySet() + ", values=redacted]";
  }
}

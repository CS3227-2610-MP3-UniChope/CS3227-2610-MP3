package unichope.platform.ai;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import unichope.platform.core.model.user.Role;

/**
 * Shared SoC gateway. The task id selects the key slot; the actor's current stored role must match
 * that slot. The Admin-selected model and its price are read for every request. A missing key,
 * unknown task or role mismatch makes no admission and no provider call. Failures in one slot
 * never fall back to another slot's key.
 */
public final class RoutedAiGateway implements AiGateway {
  private final AiKeys keys;
  private final Function<UUID, Optional<Role>> roles;
  private final AiAdmission admission;
  private final AiProvider provider;
  private final AiModelSettings models;
  private final Duration providerTimeout;

  public RoutedAiGateway(
      AiKeys keys,
      Function<UUID, Optional<Role>> roles,
      AiAdmission admission,
      AiProvider provider,
      AiModelSettings models,
      Duration providerTimeout) {
    this.keys = keys;
    this.roles = roles;
    this.admission = admission;
    this.provider = provider;
    this.models = models;
    this.providerTimeout = providerTimeout;
  }

  @Override
  public Result generate(Request request) {
    AiTask task =
        AiTask.fromId(request.task()).orElseThrow(() -> new Failure(FailureCode.UNAVAILABLE));
    AiKeySlot slot = task.slot();
    if (roles.apply(request.actorId()).filter(role -> role == slot.role()).isEmpty())
      throw new Failure(FailureCode.UNAVAILABLE);
    String key = keys.key(slot).orElseThrow(() -> new Failure(FailureCode.UNAVAILABLE));
    AiModel model = models.current().model();

    long promptBytes =
        request.instructions().getBytes(StandardCharsets.UTF_8).length
            + request.input().getBytes(StandardCharsets.UTF_8).length;
    long reserved = model.reserveMicros(promptBytes, request.maxOutputTokens());
    UUID attempt = admission.admit(slot, request.actorId(), task, reserved);
    Long actual = null;
    try {
      AiProvider.Response response =
          provider.complete(
              key,
              model.id(),
              request.instructions(),
              request.input(),
              request.maxOutputTokens(),
              providerTimeout);
      if (response.inputTokens() != null && response.outputTokens() != null)
        actual = model.cost(response.inputTokens(), response.outputTokens());
      return new Result(response.text(), ProviderMode.SOC);
    } finally {
      admission.complete(attempt, actual);
    }
  }

  @Override
  public String toString() {
    return "RoutedAiGateway[" + keys + "]";
  }
}

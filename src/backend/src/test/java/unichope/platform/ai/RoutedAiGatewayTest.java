package unichope.platform.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import unichope.platform.core.model.user.Role;
import unichope.platform.web.ApiFailure;

class RoutedAiGatewayTest {
  private static final UUID STUDENT = new UUID(0, 1), TUTOR = new UUID(0, 2), ADMIN = new UUID(0, 3);
  private final Map<UUID, Role> roles = Map.of(STUDENT, Role.STUDENT, TUTOR, Role.TUTOR, ADMIN, Role.ADMIN);
  private final RecordingProvider provider = new RecordingProvider();
  private final RecordingAdmission admission = new RecordingAdmission();
  private AiModel selected = AiModelCatalog.defaultModel();
  private final AiModelSettings settings = new AiModelSettings() {
    @Override public Setting current() { return new Setting(selected, null); }
    @Override public Setting select(String id, UUID admin) { throw new UnsupportedOperationException(); }
  };

  private RoutedAiGateway gateway(Map<AiKeySlot, String> keys) {
    return new RoutedAiGateway(
        new AiKeys(keys),
        id -> Optional.ofNullable(roles.get(id)),
        admission,
        provider,
        settings,
        Duration.ofSeconds(25));
  }

  private static Map<AiKeySlot, String> allKeys() {
    Map<AiKeySlot, String> keys = new EnumMap<>(AiKeySlot.class);
    keys.put(AiKeySlot.STUDENT, "student-key");
    keys.put(AiKeySlot.TUTOR, "tutor-key");
    keys.put(AiKeySlot.ADMIN, "admin-key");
    return keys;
  }

  private static AiGateway.Request request(UUID actor, String task) {
    return new AiGateway.Request(actor, task, "Return JSON only.", "untrusted input", 512);
  }

  @Test
  void eachTaskUsesOnlyItsOwnRoleKey() {
    var gateway = gateway(allKeys());
    assertEquals(AiGateway.ProviderMode.SOC, gateway.generate(request(STUDENT, "STU-AGENDA-1")).providerMode());
    gateway.generate(request(TUTOR, "TUT-NOTE-1"));
    gateway.generate(request(ADMIN, "ADM-REPORT-1"));
    assertEquals(List.of("student-key", "tutor-key", "admin-key"), provider.keys);
    assertEquals(List.of(AiKeySlot.STUDENT, AiKeySlot.TUTOR, AiKeySlot.ADMIN), admission.slots);
    assertEquals(3, admission.completed.size());
  }

  @Test
  void unknownTaskRoleMismatchAndUnknownActorMakeNoAdmissionOrCall() {
    var gateway = gateway(allKeys());
    for (var bad : List.of(request(STUDENT, "FREE-CHAT"), request(STUDENT, "ADM-REPORT-1"),
        request(TUTOR, "STU-AGENDA-1"), request(new UUID(9, 9), "STU-AGENDA-1"))) {
      var failure = assertThrows(AiGateway.Failure.class, () -> gateway.generate(bad));
      assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
    }
    assertTrue(provider.keys.isEmpty());
    assertTrue(admission.slots.isEmpty());
  }

  @Test
  void missingKeyDisablesOnlyThatRoleAndNeverBorrowsAnotherKey() {
    var keys = allKeys();
    keys.remove(AiKeySlot.STUDENT);
    var gateway = gateway(keys);
    assertThrows(AiGateway.Failure.class, () -> gateway.generate(request(STUDENT, "STU-AGENDA-1")));
    assertTrue(provider.keys.isEmpty());
    assertTrue(admission.slots.isEmpty());
    gateway.generate(request(ADMIN, "ADM-REPORT-1"));
    assertEquals(List.of("admin-key"), provider.keys);
  }

  @Test
  void providerFailureStillEndsTheLeaseAndKeepsFullReservation() {
    provider.failure = new ApiFailure(429, "AI_RATE_LIMITED", "busy");
    var gateway = gateway(allKeys());
    var thrown = assertThrows(ApiFailure.class, () -> gateway.generate(request(STUDENT, "STU-AGENDA-1")));
    assertEquals("AI_RATE_LIMITED", thrown.code());
    assertEquals(1, admission.completed.size());
    assertNull(admission.actual.get(0), "unknown usage keeps the reservation charged");
    assertEquals(List.of("student-key"), provider.keys, "no retry and no fallback key");
  }

  @Test
  void reportedUsageReplacesReservationWithActualCost() {
    provider.inputTokens = 20L;
    provider.outputTokens = 300L;
    gateway(allKeys()).generate(request(STUDENT, "STU-AGENDA-1"));
    long expected = AiModelCatalog.defaultModel().cost(20, 300);
    assertEquals(expected, admission.actual.get(0));
    assertTrue(admission.reserved.get(0) >= expected);
  }

  @Test
  void usesTheCurrentlySelectedModelAndItsPriceOnEachRequest() {
    var gateway = gateway(allKeys());
    gateway.generate(request(STUDENT, "STU-AGENDA-1"));
    selected = AiModelCatalog.find("gemma4:26b").orElseThrow();
    gateway.generate(request(STUDENT, "STU-AGENDA-1"));
    assertEquals(List.of("qwen3.5:9b", "gemma4:26b"), provider.models);
    long promptBytes = "Return JSON only.".length() + "untrusted input".length();
    assertEquals(AiModelCatalog.defaultModel().reserveMicros(promptBytes, 512), admission.reserved.get(0));
    assertEquals(selected.reserveMicros(promptBytes, 512), admission.reserved.get(1));
  }

  @Test
  void keysNeverAppearInTextForms() {
    var gateway = gateway(allKeys());
    String text = gateway + " " + new AiKeys(allKeys());
    for (String key : allKeys().values()) assertFalse(text.contains(key));
  }

  private static final class RecordingProvider implements AiProvider {
    final List<String> keys = new ArrayList<>();
    final List<String> models = new ArrayList<>();
    RuntimeException failure;
    Long inputTokens, outputTokens;

    @Override
    public Response complete(String apiKey, String model, String instructions, String input, int max, Duration timeout) {
      keys.add(apiKey);
      models.add(model);
      if (failure != null) throw failure;
      return new Response("{\"ok\":true}", inputTokens, outputTokens);
    }
  }

  private static final class RecordingAdmission implements AiAdmission {
    final List<AiKeySlot> slots = new ArrayList<>();
    final List<Long> reserved = new ArrayList<>();
    final List<UUID> completed = new ArrayList<>();
    final List<Long> actual = new ArrayList<>();

    @Override
    public UUID admit(AiKeySlot slot, UUID actorId, AiTask task, long reservedMicros) {
      slots.add(slot);
      reserved.add(reservedMicros);
      return UUID.randomUUID();
    }

    @Override
    public void complete(UUID attemptId, Long actualMicros) {
      completed.add(attemptId);
      actual.add(actualMicros);
    }
  }
}

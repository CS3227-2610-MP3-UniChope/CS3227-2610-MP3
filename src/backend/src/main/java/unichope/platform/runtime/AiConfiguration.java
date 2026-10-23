package unichope.platform.runtime;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import unichope.platform.ai.*;
import unichope.platform.ai.postgres.PostgresAiAdmissionService;

/**
 * Shared AI wiring, registered only with unichope.ai.mode=shared. Every role call goes through
 * {@link AdmittedAiGateway}: task policy, durable PostgreSQL admission, then the SoCLaaS adapter
 * for that role's key. A missing role key makes only that role's AI unavailable.
 *
 * <pre>
 * SOC_LLM_API_KEY_STUDENT    clsk_... key for Student AI tasks
 * SOC_LLM_API_KEY_TUTOR      clsk_... key for Tutor AI tasks
 * SOC_LLM_API_KEY_ADMIN      clsk_... key for Admin AI tasks
 * SOC_LLM_MODEL              chat model id (default qwen3.5:9b)
 * SOC_LLM_RPM                per-key requests per minute (default 80, under SoCLaaS's 90)
 * SOC_LLM_TPM                per-key reserved output tokens per minute (default 160000)
 * </pre>
 *
 * The SoCLaaS endpoint is fixed in {@link SoCLaaSChatCompletionsProvider}; it is never configured.
 */
@Configuration
@Profile("app-api")
public class AiConfiguration {
  static final String DEFAULT_MODEL = "qwen3.5:9b";
  static final Duration ACTOR_WINDOW = Duration.ofMinutes(5);
  static final int ACTOR_REQUESTS = 3;
  static final int KEY_IN_FLIGHT = 8;
  static final int MAX_RESPONSE_BYTES = 65_536;
  static final Duration DEADLINE = Duration.ofSeconds(25);

  @Bean(destroyMethod = "close")
  @DependsOn("applicationMigrations")
  @Conditional(SharedMode.class)
  AdmittedAiGateway sharedAiGateway(Environment env, DataSource source, Clock clock) {
    var budget =
        new AiProviderBudget(
            number(env, "SOC_LLM_RPM", 80),
            number(env, "SOC_LLM_TPM", 160_000),
            Duration.ofMinutes(1),
            KEY_IN_FLIGHT);
    Map<AiProviderKey, AiProviderBudget> budgets = new EnumMap<>(AiProviderKey.class);
    for (AiProviderKey key : AiProviderKey.values()) budgets.put(key, budget);

    String model = env.getProperty("SOC_LLM_MODEL", DEFAULT_MODEL).strip();
    Map<AiProviderKey, AiGateway> providers = new EnumMap<>(AiProviderKey.class);
    for (AiProviderKey key : AiProviderKey.values()) {
      String apiKey = env.getProperty("SOC_LLM_API_KEY_" + key.name());
      if (apiKey != null && !apiKey.isBlank())
        providers.put(
            key, new SoCLaaSChatCompletionsProvider(apiKey.strip(), model, MAX_RESPONSE_BYTES));
    }
    AiProviderRouter router =
        key -> {
          AiGateway provider = providers.get(key);
          if (provider == null) throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
          return provider;
        };

    int tutorTokens = env.getProperty("unichope.ai.tutor.note-draft.max-output-tokens", Integer.class, 2048);
    Map<String, AiTaskPolicy> policies =
        Stream.of(
                // Agenda input is JSON-escaped questions (4,000 code points) plus booking context.
                policy("STU-AGENDA-1", AiProviderKey.STUDENT, 25_000, 2048),
                policy("tutor.note-draft.v1", AiProviderKey.TUTOR, 8_000, tutorTokens),
                policy("ADM-REPORT-1", AiProviderKey.ADMIN, 4_000, 2048))
            .collect(Collectors.toMap(AiTaskPolicy::task, p -> p));

    return new AdmittedAiGateway(new PostgresAiAdmissionService(source, clock, budgets), router, policies);
  }

  private static AiTaskPolicy policy(String task, AiProviderKey key, int maxInput, int maxOutputTokens) {
    return new AiTaskPolicy(
        task, key, ACTOR_REQUESTS, ACTOR_WINDOW, 1, maxInput, maxOutputTokens, MAX_RESPONSE_BYTES, DEADLINE);
  }

  private static int number(Environment env, String name, int fallback) {
    String raw = env.getProperty(name);
    if (raw == null || raw.isBlank()) return fallback;
    try {
      return Integer.parseInt(raw.strip());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(name + " must be a whole number");
    }
  }

  static final class SharedMode implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return "shared".equals(context.getEnvironment().getProperty("unichope.ai.mode"));
    }
  }
}

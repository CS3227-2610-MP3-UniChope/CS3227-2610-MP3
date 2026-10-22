package unichope.platform.runtime;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import unichope.platform.ai.*;
import unichope.platform.core.model.user.User;
import unichope.platform.core.repository.UserRepository;

/**
 * AI wiring. The Admin model setting is always available with app-api; the SoCLaaS gateway is
 * registered only with unichope.ai.mode=shared. Without a URL every call is unavailable; a
 * missing key disables only that role's AI.
 *
 * <pre>
 * SOC_LLM_API_URL            SoCLaaS base URL including /v1
 * SOC_LLM_API_KEY_STUDENT    clsk_... key for Student AI tasks
 * SOC_LLM_API_KEY_TUTOR      clsk_... key for Tutor AI tasks
 * SOC_LLM_API_KEY_ADMIN      clsk_... key for Admin AI tasks
 * SOC_LLM_RPM / SOC_LLM_DAILY_BUDGET / SOC_LLM_MONTHLY_BUDGET   per-key limits (microdollars)
 * </pre>
 *
 * The model is chosen by an Admin (GET/PUT /api/admin/ai/model) and defaults to qwen3.5:9b.
 */
@Configuration
@Profile("app-api")
public class AiConfiguration {
  static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(25);

  @Bean
  @DependsOn("applicationMigrations")
  AiModelSettings aiModelSettings(DataSource source, Clock clock) {
    return new PostgresAiModelSettings(source, clock);
  }

  @Bean
  @DependsOn("applicationMigrations")
  @Conditional(SharedMode.class)
  AiGateway sharedAiGateway(
      Environment env, DataSource source, Clock clock, UserRepository users, AiModelSettings models) {
    AiQuotaPolicy defaults = AiQuotaPolicy.defaults();
    AiQuotaPolicy policy =
        new AiQuotaPolicy(
            Math.toIntExact(number(env, "SOC_LLM_RPM", defaults.requestsPerMinute())),
            number(env, "SOC_LLM_DAILY_BUDGET", defaults.dailyBudgetMicros()),
            number(env, "SOC_LLM_MONTHLY_BUDGET", defaults.monthlyBudgetMicros()),
            defaults.perActorRequests(),
            defaults.perActorWindow(),
            defaults.inFlightLease());
    Map<AiKeySlot, String> configured = new EnumMap<>(AiKeySlot.class);
    for (AiKeySlot slot : AiKeySlot.values())
      configured.put(slot, env.getProperty("SOC_LLM_API_KEY_" + slot.name()));

    String url = env.getProperty("SOC_LLM_API_URL");
    if (url == null || url.isBlank())
      return request -> {
        throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
      };
    return new RoutedAiGateway(
        new AiKeys(configured),
        id -> users.findById(id).filter(User::isActive).map(User::role),
        new PostgresAiAdmission(source, clock, policy),
        new SocLlmProvider(url),
        models,
        PROVIDER_TIMEOUT);
  }

  private static long number(Environment env, String name, long fallback) {
    String raw = env.getProperty(name);
    if (raw == null || raw.isBlank()) return fallback;
    try {
      return Long.parseLong(raw.strip());
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

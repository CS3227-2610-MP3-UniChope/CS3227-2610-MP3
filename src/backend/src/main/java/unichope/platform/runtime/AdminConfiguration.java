package unichope.platform.runtime;

import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import unichope.admin.*;
import unichope.platform.ai.*;
import unichope.platform.auth.*;
import unichope.platform.core.*;

@Configuration
@Profile("app-api")
public class AdminConfiguration {
  @Bean
  AdminActorProvider adminActor(AuthenticationService auth) {
    return () -> {
      SessionPrincipal principal = AuthController.principal();
      var user = auth.current(principal);
      if (user.role() != unichope.platform.core.model.user.Role.ADMIN)
        throw new AuthFailure(403, "FORBIDDEN");
      return new AdminContext(user.id(), principal.generation());
    };
  }

  @Bean
  @DependsOn("applicationMigrations")
  AdminRepository adminRepository(DataSource source, Clock clock) {
    return new PostgresAdminRepository(source, clock);
  }

  @Bean
  AdminService adminService(
      AccountStorage accounts,
      AdminRepository repository,
      AuthenticationService auth,
      AdminActorProvider actors) {
    return new AdminService(accounts, repository, auth, actors);
  }

  @Bean
  ActivityReportService activityReportService(
      AdminActorProvider actors,
      AdminRepository repository,
      Environment env,
      Clock clock,
      ObjectProvider<AiGateway> sharedGateway) {
    String mode = env.getProperty("unichope.ai.mode", "unavailable");
    AiGateway gateway;
    if (mode.equals("fake")) {
      if (!env.matchesProfiles("local"))
        throw new IllegalStateException("Fake AI is only available with the local profile");
      gateway = new DevelopmentReportGateway(clock);
    } else if (mode.equals("shared")) {
      AiGateway shared = sharedGateway.getIfAvailable();
      if (shared == null) throw new IllegalStateException("Shared AI gateway is not configured");
      gateway =
          request -> {
            var result = shared.generate(request);
            if (result.providerMode() == AiGateway.ProviderMode.FAKE
                && !env.matchesProfiles("local"))
              throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
            return result;
          };
    } else if (mode.equals("unavailable"))
      gateway =
          request -> {
            throw new AiGateway.Failure(AiGateway.FailureCode.UNAVAILABLE);
          };
    else
      throw new IllegalStateException(
          "Select unavailable, local fake, or a configured shared gateway");
    return new ActivityReportService(actors, repository, gateway);
  }
}

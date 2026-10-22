package unichope.platform.runtime;

import java.time.Clock;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.auth.*;
import unichope.platform.core.*;
import unichope.platform.core.postgres.*;
import unichope.platform.core.repository.*;
import unichope.tutor.*;
import unichope.tutor.CurrentActorProvider;

/** Explicit application wiring; default profile remains closed and database-free. */
@Configuration
@Profile("app-api")
public class ApplicationConfiguration {
  @Bean
  Clock applicationClock() {
    return Clock.systemUTC();
  }

  @Bean
  DataSource applicationDataSource(Environment env) {
    String url = env.getRequiredProperty("unichope.jdbc.url");
    if (!url.startsWith("jdbc:postgresql:"))
      throw new IllegalStateException("PostgreSQL configuration is required");
    var source =
        new DriverManagerDataSource(
            url,
            env.getRequiredProperty("unichope.jdbc.user"),
            env.getRequiredProperty("unichope.jdbc.password"));
    var properties = new java.util.Properties();
    properties.setProperty("connectTimeout", "5");
    properties.setProperty("socketTimeout", "10");
    source.setConnectionProperties(properties);
    return source;
  }

  @Bean(initMethod = "migrate")
  Flyway applicationMigrations(DataSource source) {
    return Flyway.configure().dataSource(source).locations("classpath:db/migration").load();
  }

  @Bean
  @DependsOn("applicationMigrations")
  AccountStorage accountStorage(DataSource source, Clock clock) {
    return new PostgresAccountStorage(source, clock);
  }

  @Bean
  @DependsOn("applicationMigrations")
  UserRepository users(DataSource source) {
    return new PostgresUserRepository(JdbcClient.create(source));
  }

  @Bean
  @DependsOn("applicationMigrations")
  ModuleRepository modules(DataSource source) {
    return new PostgresModuleRepository(JdbcClient.create(source));
  }

  @Bean
  @DependsOn("applicationMigrations")
  SlotRepository slots(DataSource source) {
    return new PostgresSlotRepository(JdbcClient.create(source));
  }

  @Bean
  @DependsOn("applicationMigrations")
  BookingRepository bookings(DataSource source) {
    return new PostgresBookingRepository(JdbcClient.create(source));
  }

  @Bean
  @DependsOn("applicationMigrations")
  ConsultationLifecycle consultationLifecycle(DataSource source, Clock clock) {
    return new PostgresConsultationLifecycle(source, clock);
  }

  @Bean
  @DependsOn("applicationMigrations")
  TutorReadRepository tutorReads(DataSource source, UserRepository users) {
    return new PostgresTutorReadRepository(users, JdbcClient.create(source));
  }

  @Bean
  CurrentActorProvider tutorActor() {
    return () ->
        java.util.Optional.ofNullable(AuthController.principal()).map(SessionPrincipal::userId);
  }

  @Bean
  PasswordHasher passwordHasher() {
    return new Argon2PasswordHasher();
  }

  @Bean
  LoginLimiter loginLimiter(Clock clock) {
    return new LoginLimiter(clock);
  }

  @Bean(destroyMethod = "close")
  AuthenticationService authenticationService(
      AccountStorage accounts, PasswordHasher hasher, LoginLimiter limiter, Clock clock) {
    return new AuthenticationService(accounts, hasher, limiter, clock);
  }
}

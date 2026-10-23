package unichope.platform.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.*;
import unichope.platform.auth.AuthenticationService;

/** Default remains closed; explicit profiles activate application APIs. */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {
  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      Environment environment,
      CsrfTokenRepository tokens,
      ObjectProvider<AuthenticationService> authentication)
      throws Exception {
    boolean tutor = environment.matchesProfiles("tutor-api");
    boolean student = environment.matchesProfiles("student-api");
    boolean auth = environment.matchesProfiles("auth-api");
    boolean admin = environment.matchesProfiles("admin-api");
    boolean frontend = environment.matchesProfiles("app-api");
    http.authorizeHttpRequests(
        authorize -> {
          if (frontend)
            authorize.requestMatchers(SpaController::isPublicFrontendRequest).permitAll();
          if (auth) {
            authorize
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/csrf")
                .permitAll();
            authorize
                .requestMatchers(
                    org.springframework.http.HttpMethod.POST,
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/logout")
                .permitAll();
            authorize.requestMatchers("/api/auth/me").authenticated();
          }
          if (admin) authorize.requestMatchers("/api/admin/**").hasRole("ADMIN");
          if (tutor) {
            if (auth) authorize.requestMatchers("/api/tutor/**").hasRole("TUTOR");
            else authorize.requestMatchers("/api/tutor/**").authenticated();
          }
          if (student) authorize.requestMatchers("/api/student/**").authenticated();
          authorize.anyRequest().denyAll();
        });
    http.csrf(csrf -> csrf.csrfTokenRepository(tokens));
    http.formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable());
    http.exceptionHandling(
        errors ->
            errors
                .authenticationEntryPoint(
                    (request, response, error) ->
                        ApiErrors.write(
                            request,
                            response,
                            401,
                            "UNAUTHENTICATED",
                            "Authentication is required"))
                .accessDeniedHandler(
                    (request, response, error) ->
                        ApiErrors.write(
                            request,
                            response,
                            403,
                            error instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN",
                            error instanceof CsrfException
                                ? "A valid CSRF token is required"
                                : "Access is forbidden")));
    http.addFilterBefore(new ApiResponseFilter(), SecurityContextHolderFilter.class);
    if (auth)
      http.addFilterAfter(
          new CurrentAccountFilter(authentication.getObject()), SecurityContextHolderFilter.class);
    return http.build();
  }
}

package unichope.platform.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/** Closed by default. Real principal/session provisioning remains authentication-owned. */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,Environment environment) throws Exception {
        boolean enabled=environment.matchesProfiles("tutor-api");
        http.authorizeHttpRequests(authorize->{
            if(enabled) authorize.requestMatchers("/api/tutor/**").authenticated();
            authorize.anyRequest().denyAll();
        });
        http.csrf(csrf->csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()));
        http.formLogin(form->form.disable()).httpBasic(basic->basic.disable()).logout(logout->logout.disable());
        http.exceptionHandling(errors->errors
                .authenticationEntryPoint((request,response,error)->ApiErrors.write(request,response,401,"UNAUTHENTICATED","Authentication is required"))
                .accessDeniedHandler((request,response,error)->ApiErrors.write(request,response,403,
                        error instanceof CsrfException?"CSRF_INVALID":"FORBIDDEN",
                        error instanceof CsrfException?"A valid CSRF token is required":"Access is forbidden")));
        http.addFilterBefore(new ApiResponseFilter(),SecurityContextHolderFilter.class);
        return http.build();
    }
}

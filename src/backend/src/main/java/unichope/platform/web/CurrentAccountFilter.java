package unichope.platform.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import unichope.platform.auth.*;
import unichope.platform.core.AccountFailure;

/** Current stored eligibility is checked before authorization, not just at sign-in. */
final class CurrentAccountFilter extends OncePerRequestFilter {
  private final AuthenticationService accounts;

  CurrentAccountFilter(AuthenticationService accounts) {
    this.accounts = accounts;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      try {
        if (!(authentication.getPrincipal() instanceof SessionPrincipal principal))
          throw new AuthFailure(401, "UNAUTHENTICATED");
        var user = accounts.current(principal);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
        SecurityContextHolder.setContext(context);
      } catch (AuthFailure failure) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        ApiErrors.write(
            request,
            response,
            failure.status(),
            failure.code(),
            "Authentication is no longer valid");
        return;
      } catch (AccountFailure failure) {
        ApiErrors.write(
            request,
            response,
            503,
            "TEMPORARILY_UNAVAILABLE",
            "Authentication storage is unavailable");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}

package unichope.student.http;

import java.util.Objects;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.auth.AuthFailure;
import unichope.platform.auth.SessionPrincipal;
import unichope.platform.core.AccountFailure;
import unichope.platform.web.ApiFailure;
import unichope.student.StudentAccessDeniedException;

/** Captures a live agenda request session; a thread's cached principal alone cannot revive it. */
final class StudentAgendaSession {
    private final AuthenticationService authentication;

    StudentAgendaSession(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    Runnable capture() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) throw unauthenticated();
        var request = attributes.getRequest();
        var session = request.getSession(false);
        var current = SecurityContextHolder.getContext().getAuthentication();
        if (session == null || current == null || !current.isAuthenticated()) throw unauthenticated();
        final String sessionId;
        try { sessionId = session.getId(); }
        catch (IllegalStateException invalidated) { throw unauthenticated(); }
        Object principal = current.getPrincipal();
        return () -> {
            try {
                if (request.getSession(false) != session || !sessionId.equals(session.getId())
                        || !(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) instanceof SecurityContext saved)
                        || saved.getAuthentication() == null || !saved.getAuthentication().isAuthenticated()
                        || !Objects.equals(saved.getAuthentication().getPrincipal(), principal)
                        || SecurityContextHolder.getContext().getAuthentication() == null
                        || !Objects.equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), principal)) throw unauthenticated();
                // Isolated Student tests use their established actor seam without auth-api.
                // app-api supplies this existing service: stored generation remains authoritative.
                if (authentication != null) {
                    if (!(principal instanceof SessionPrincipal identity)) throw unauthenticated();
                    authentication.current(identity);
                }
            } catch (IllegalStateException invalidated) {
                throw unauthenticated();
            } catch (AuthFailure denied) {
                if (denied.code().equals("ACCOUNT_INACTIVE"))
                    throw new StudentAccessDeniedException(StudentAccessDeniedException.Reason.INACTIVE);
                throw unauthenticated();
            } catch (AccountFailure unavailable) {
                throw new ApiFailure(503, "TEMPORARILY_UNAVAILABLE", "Authentication storage is unavailable");
            }
        };
    }

    private static StudentAccessDeniedException unauthenticated() {
        // Uses the existing session-invalidating safe 401 mapping without disclosing its cause.
        return new StudentAccessDeniedException(StudentAccessDeniedException.Reason.UNKNOWN_ACCOUNT);
    }
}

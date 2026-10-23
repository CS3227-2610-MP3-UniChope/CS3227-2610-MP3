package unichope.student.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unichope.platform.web.ApiErrors;
import unichope.platform.web.ApiFailure;
import unichope.student.StudentAccessDeniedException;

/**
 * Maps Student controller failures to the shared error envelope (TECH §3.2, §3.3). Errors are
 * written as JSON even when {@code Accept} excludes JSON; messages are fixed safe text.
 */
@RestControllerAdvice(assignableTypes = StudentController.class)
@Profile("student-api")
public class StudentExceptionHandler {
    @ExceptionHandler(Exception.class)
    public void handle(Exception failure, HttpServletRequest request, HttpServletResponse response) throws Exception {
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = "The request could not be completed";
        if (failure instanceof ApiFailure api) {
            status = api.status();
            code = api.code();
            message = api.getMessage();
            if (api.retryAfterSeconds() != null) response.setHeader("Retry-After", Long.toString(api.retryAfterSeconds()));
        } else if (failure instanceof StudentAccessDeniedException access) {
            switch (access.reason()) {
                case NO_ACTOR -> { status = 401; code = "UNAUTHENTICATED"; message = "Authentication is required"; }
                case UNKNOWN_ACCOUNT -> { status = 401; code = "UNAUTHENTICATED"; message = "Authentication is required"; invalidate(request); }
                case INACTIVE -> { status = 403; code = "ACCOUNT_INACTIVE"; message = "Account is inactive"; invalidate(request); }
                case FORBIDDEN -> { status = 403; code = "FORBIDDEN"; message = "Access is forbidden"; }
            }
        }
        // A committed transport response cannot be replaced with a new safe status and body.
        if (response.isCommitted()) throw failure;
        ApiErrors.write(request, response, status, code, message);
    }

    private static void invalidate(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}

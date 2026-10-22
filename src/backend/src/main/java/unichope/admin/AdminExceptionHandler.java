package unichope.admin;

import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import unichope.platform.ai.AiGateway;
import unichope.platform.core.AccountFailure;
import unichope.platform.web.*;

@RestControllerAdvice(assignableTypes = AdminController.class)
@org.springframework.context.annotation.Profile("admin-api")
public final class AdminExceptionHandler {
  @ExceptionHandler(Exception.class)
  public void handle(Exception e, HttpServletRequest request, HttpServletResponse response)
      throws java.io.IOException {
    int status = 500;
    String code = "INTERNAL_ERROR";
    if (e instanceof ReportBusyFailure) {
      status = 409;
      code = "AI_BUSY";
      response.setHeader("Retry-After", "60");
    } else if (e instanceof unichope.platform.auth.AuthFailure f) {
      status = f.status();
      code = f.code();
      if (f.retryAfter() > 0) response.setHeader("Retry-After", Integer.toString(f.retryAfter()));
    } else if (e instanceof ApiFailure f) {
      status = f.status();
      code = f.code();
    } else if (e instanceof AccountFailure f) {
      switch (f.code()) {
        case INVALID_INPUT -> {
          status = 400;
          code = "VALIDATION_FAILED";
        }
        case ACCESS_DENIED -> {
          status = 403;
          code = "FORBIDDEN";
        }
        case NOT_FOUND -> {
          status = 404;
          code = "NOT_FOUND";
        }
        case INVALID_STATE -> {
          status = 409;
          code = "INVALID_STATE";
        }
        case ACCOUNT_UNAVAILABLE -> {
          status = 409;
          code = "DUPLICATE_RECORD";
        }
        case TEMPORARILY_UNAVAILABLE -> {
          status = 503;
          code = "TEMPORARILY_UNAVAILABLE";
        }
        default -> {}
      }
    } else if (e instanceof AiGateway.Failure f) {
      switch (f.code()) {
        case INVALID_OUTPUT -> {
          status = 502;
          code = "AI_INVALID_OUTPUT";
        }
        case TIMEOUT -> {
          status = 504;
          code = "AI_TIMEOUT";
        }
        case UNAVAILABLE -> {
          status = 503;
          code = "AI_UNAVAILABLE";
        }
      }
    }
    if (e instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
      status = 405;
      code = "METHOD_NOT_ALLOWED";
    }
    if (e instanceof org.springframework.web.HttpMediaTypeNotSupportedException) {
      status = 415;
      code = "UNSUPPORTED_MEDIA_TYPE";
    }
    if (code.equals("UNAUTHENTICATED") || code.equals("ACCOUNT_INACTIVE")) {
      var session = request.getSession(false);
      if (session != null) session.invalidate();
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    if (status == 429 && response.getHeader("Retry-After") == null)
      response.setHeader("Retry-After", "300");
    ApiErrors.write(request, response, status, code, "The request could not be completed");
  }
}

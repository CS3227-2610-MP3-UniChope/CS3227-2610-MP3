package unichope.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unichope.platform.core.AccountFailure;
import unichope.platform.web.*;

@RestControllerAdvice(basePackageClasses = AuthController.class)
@Profile("auth-api")
public class AuthExceptionHandler {
  @ExceptionHandler(AuthFailure.class)
  ResponseEntity<ApiErrors.Error> auth(AuthFailure f, HttpServletRequest r) {
    var b = ResponseEntity.status(f.status()).header("Cache-Control", "no-store");
    if (f.retryAfter() > 0) b.header("Retry-After", Integer.toString(f.retryAfter()));
    return b.body(ApiErrors.error(r, f.code(), message(f.code())));
  }

  @ExceptionHandler(ApiFailure.class)
  ResponseEntity<ApiErrors.Error> api(ApiFailure f, HttpServletRequest r) {
    return ResponseEntity.status(f.status()).body(ApiErrors.error(r, f.code(), f.getMessage()));
  }

  @ExceptionHandler(AccountFailure.class)
  ResponseEntity<ApiErrors.Error> account(AccountFailure f, HttpServletRequest r) {
    int status =
        switch (f.code()) {
          case INVALID_INPUT -> 400;
          case ACCOUNT_UNAVAILABLE -> 409;
          case ACCESS_DENIED -> 403;
          case NOT_FOUND -> 404;
          case INVALID_STATE -> 409;
          case LOGIN_REJECTED -> 401;
          default -> 503;
        };
    String code =
        switch (f.code()) {
          case INVALID_INPUT -> "VALIDATION_FAILED";
          case LOGIN_REJECTED -> "INVALID_CREDENTIALS";
          case ACCESS_DENIED -> "FORBIDDEN";
          case STORAGE_FAILURE, TEMPORARILY_UNAVAILABLE -> "TEMPORARILY_UNAVAILABLE";
          default -> f.code().name();
        };
    return ResponseEntity.status(status).body(ApiErrors.error(r, code, message(code)));
  }

  private static String message(String code) {
    return switch (code) {
      case "INVALID_CREDENTIALS" -> "Invalid credentials";
      case "ACCOUNT_INACTIVE" -> "Account is inactive";
      case "UNAUTHENTICATED" -> "Authentication is required";
      case "AUTH_RATE_LIMITED" -> "Too many login attempts";
      case "VALIDATION_FAILED" -> "Request validation failed";
      case "ACCOUNT_UNAVAILABLE" -> "Account is unavailable";
      default -> "Request could not be completed";
    };
  }
}

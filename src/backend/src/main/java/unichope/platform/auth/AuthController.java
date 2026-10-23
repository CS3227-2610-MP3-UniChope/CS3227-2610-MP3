package unichope.platform.auth;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.core.model.user.*;
import unichope.platform.web.ApiFailure;

@RestController
@Profile("auth-api")
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthenticationService service;
  private final CsrfTokenRepository csrf;
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  public AuthController(AuthenticationService service, CsrfTokenRepository csrf) {
    this.service = service;
    this.csrf = csrf;
  }

  public record Actor(UUID id, String name, Role role, boolean mustChangePassword) {
    static Actor of(User u, boolean mustChangePassword) {
      return new Actor(u.id(), u.name(), u.role(), mustChangePassword);
    }
  }

  public record Token(String headerName, String token) {}

  @GetMapping("/csrf")
  public Token csrf(HttpServletRequest request) {
    noQuery(request);
    CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return new Token(token.getHeaderName(), token.getToken());
  }

  @PostMapping("/login")
  public Actor login(HttpServletRequest request, HttpServletResponse response) throws IOException {
    var body = input(request, Set.of("email", "password"));
    var verified =
        service.verifyLogin(body.get("email").stringValue(), body.get("password").stringValue());
    var principal = verified.principal();
    User user = verified.user();
    request.getSession();
    request.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    verified.requireUnexpired();
    SecurityContextHolder.setContext(context);
    request
        .getSession()
        .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    csrf.saveToken(null, request, response);
    return Actor.of(user, verified.mustChangePassword());
  }

  @GetMapping("/me")
  public Actor me(HttpServletRequest request) {
    noQuery(request);
    var state = service.currentState(principal());
    return Actor.of(state.user(), state.mustChangePassword());
  }

  @PostMapping("/change-password")
  public ResponseEntity<Void> changePassword(HttpServletRequest request) throws IOException {
    var body = input(request, Set.of("password", "confirmation"));
    service.changeTutorPassword(
        principal(), body.get("password").stringValue(), body.get("confirmation").stringValue());
    return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
  }

  public static SessionPrincipal principal() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof SessionPrincipal p ? p : null;
  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, String>> register(HttpServletRequest request)
      throws IOException {
    var b = input(request, Set.of("name", "email", "password", "confirmation"));
    service.register(
        b.get("name").stringValue(),
        b.get("email").stringValue(),
        b.get("password").stringValue(),
        b.get("confirmation").stringValue());
    return ResponseEntity.status(201).body(Map.of("message", "Registration completed"));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    noQuery(request);
    byte[] body = request.getInputStream().readNBytes(65537);
    if (body.length > 65536)
      throw new ApiFailure(413, "PAYLOAD_TOO_LARGE", "Request body is too large");
    if (body.length > 0) {
      try {
        var parsed = JSON.readTree(body);
        if (parsed == null || !parsed.isObject() || !parsed.isEmpty()) throw invalid();
      } catch (tools.jackson.core.JacksonException failure) {
        throw invalid();
      }
    }
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
    csrf.saveToken(null, request, response);
    Cookie cookie = new Cookie("JSESSIONID", "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    cookie.setSecure(request.isSecure());
    cookie.setAttribute("SameSite", "Lax");
    response.addCookie(cookie);
    return ResponseEntity.noContent().build();
  }

  private static void noQuery(HttpServletRequest r) {
    if (!r.getParameterMap().isEmpty()) throw invalid();
  }

  private static JsonNode input(HttpServletRequest r, Set<String> fields) throws IOException {
    noQuery(r);
    String type = r.getContentType();
    if (type == null || !type.split(";", 2)[0].strip().equalsIgnoreCase("application/json"))
      throw new ApiFailure(415, "UNSUPPORTED_MEDIA_TYPE", "JSON content type is required");
    byte[] bytes = r.getInputStream().readNBytes(65537);
    if (bytes.length > 65536)
      throw new ApiFailure(413, "PAYLOAD_TOO_LARGE", "Request body is too large");
    try {
      var b = JSON.readTree(bytes);
      if (b == null || !b.isObject() || !Set.copyOf(b.propertyNames()).equals(fields))
        throw invalid();
      for (String f : fields) if (!b.get(f).isString()) throw invalid();
      return b;
    } catch (tools.jackson.core.JacksonException failure) {
      throw invalid();
    }
  }

  private static ApiFailure invalid() {
    return new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed");
  }
}

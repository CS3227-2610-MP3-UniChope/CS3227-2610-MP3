package unichope.admin;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.web.ApiFailure;

@RestController
@RequestMapping("/api/admin")
@org.springframework.context.annotation.Profile("admin-api")
public final class AdminController {
  private final AdminService service;
  private final ActivityReportService reports;

  public AdminController(AdminService service, ActivityReportService reports) {
    this.service = service;
    this.reports = reports;
  }

  @GetMapping("/users")
  public Object users(HttpServletRequest r) {
    query(r, "role", "active");
    return service.users(r.getParameter("role"), bool(r.getParameter("active")));
  }

  @PostMapping("/users")
  public ResponseEntity<?> createUser(HttpServletRequest r) throws IOException {
    query(r);
    var b = body(r, "role", "name", "email", "password");
    return ResponseEntity.status(201)
        .body(
            service.provision(
                string(b, "role"), string(b, "name"), string(b, "email"), string(b, "password")));
  }

  @PutMapping("/users/{id}/active")
  public Object active(@PathVariable("id") String id, HttpServletRequest r) throws IOException {
    query(r);
    var b = body(r, "active");
    if (!b.get("active").isBoolean()) throw invalid();
    return service.active(id(id), b.get("active").booleanValue());
  }

  @GetMapping("/modules")
  public Object modules(HttpServletRequest r) {
    query(r, "active");
    return service.modules(bool(r.getParameter("active")));
  }

  @PostMapping("/modules")
  public ResponseEntity<?> createModule(HttpServletRequest r) throws IOException {
    query(r);
    var b = body(r, "code", "name");
    return ResponseEntity.status(201)
        .body(service.saveModule(null, string(b, "code"), string(b, "name")));
  }

  @PutMapping("/modules/{id}")
  public Object edit(@PathVariable("id") String id, HttpServletRequest r) throws IOException {
    query(r);
    var b = body(r, "code", "name");
    return service.saveModule(id(id), string(b, "code"), string(b, "name"));
  }

  @PostMapping("/modules/{id}/deactivate")
  public Object deactivate(@PathVariable("id") String id, HttpServletRequest r) throws IOException {
    query(r);
    body(r);
    return service.deactivate(id(id));
  }

  @GetMapping("/assignments")
  public Object assignments(HttpServletRequest r) {
    query(r, "tutorId", "moduleId");
    return service.assignments(optionalId(r, "tutorId"), optionalId(r, "moduleId"));
  }

  @PutMapping("/assignments/{tutorId}/{moduleId}")
  public ResponseEntity<Void> assign(
      @PathVariable("tutorId") String tutor,
      @PathVariable("moduleId") String module,
      HttpServletRequest r)
      throws IOException {
    query(r);
    body(r);
    service.assign(id(tutor), id(module), true);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/assignments/{tutorId}/{moduleId}")
  public ResponseEntity<Void> remove(
      @PathVariable("tutorId") String tutor,
      @PathVariable("moduleId") String module,
      HttpServletRequest r)
      throws IOException {
    query(r);
    if (r.getInputStream().read() != -1) throw invalid();
    service.assign(id(tutor), id(module), false);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/bookings")
  public Object bookings(HttpServletRequest r) {
    query(r, "moduleId", "tutorId", "date", "status");
    LocalDate date = null;
    if (r.getParameter("date") != null) {
      try {
        String raw = r.getParameter("date");
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) throw invalid();
        date = LocalDate.parse(raw);
      } catch (java.time.DateTimeException e) {
        throw invalid();
      }
    }
    return service.bookings(
        optionalId(r, "moduleId"), optionalId(r, "tutorId"), date, r.getParameter("status"));
  }

  @GetMapping("/statistics")
  public Object statistics(HttpServletRequest r) {
    query(r);
    return service.statistics();
  }

  @PostMapping("/activity-report")
  public Object report(HttpServletRequest r) throws IOException {
    query(r);
    body(r);
    return reports.generate();
  }

  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  private static JsonNode body(HttpServletRequest r, String... fields) throws IOException {
    try {
      var type = MediaType.parseMediaType(Objects.requireNonNullElse(r.getContentType(), ""));
      if (!type.getType().equals("application") || !type.getSubtype().equals("json"))
        throw new ApiFailure(415, "UNSUPPORTED_MEDIA_TYPE", "JSON content type is required");
    } catch (org.springframework.http.InvalidMediaTypeException e) {
      throw new ApiFailure(415, "UNSUPPORTED_MEDIA_TYPE", "JSON content type is required");
    }
    byte[] bytes = r.getInputStream().readNBytes(65537);
    if (bytes.length > 65536)
      throw new ApiFailure(413, "PAYLOAD_TOO_LARGE", "Request body is too large");
    try {
      var b = JSON.readTree(bytes);
      if (b == null || !b.isObject() || !Set.copyOf(b.propertyNames()).equals(Set.of(fields)))
        throw invalid();
      return b;
    } catch (tools.jackson.core.JacksonException e) {
      throw invalid();
    }
  }

  private static String string(JsonNode b, String key) {
    if (!b.get(key).isString()) throw invalid();
    return b.get(key).stringValue();
  }

  private static void query(HttpServletRequest r, String... allowed) {
    var names = Set.of(allowed);
    for (var entry : r.getParameterMap().entrySet())
      if (!names.contains(entry.getKey()) || entry.getValue().length != 1) throw invalid();
  }

  private static Boolean bool(String s) {
    if (s == null) return null;
    if (s.equals("true")) return true;
    if (s.equals("false")) return false;
    throw invalid();
  }

  private static UUID optionalId(HttpServletRequest r, String key) {
    return r.getParameter(key) == null ? null : id(r.getParameter(key));
  }

  private static UUID id(String s) {
    if (!s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
      throw invalid();
    return UUID.fromString(s);
  }

  private static ApiFailure invalid() {
    return new ApiFailure(400, "VALIDATION_FAILED", "Request validation failed");
  }
}

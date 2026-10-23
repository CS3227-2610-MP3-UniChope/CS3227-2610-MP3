package unichope.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the React shell only for routes implemented by the frontend. */
@Controller
@Profile("app-api")
public class SpaController {
  private static final Set<String> PAGES =
      Set.of(
          "/", "/login", "/register", "/tutor", "/student/slots", "/student/bookings",
          "/admin", "/admin/overview", "/admin/users", "/admin/modules", "/admin/assignments",
          "/admin/bookings", "/admin/reports");
  private static final Pattern AGENDA =
      Pattern.compile(
          "/student/bookings/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/agenda",
          Pattern.CASE_INSENSITIVE);

  static boolean isPublicFrontendRequest(HttpServletRequest request) {
    if (!request.getMethod().equals("GET") && !request.getMethod().equals("HEAD")) return false;
    String path = request.getServletPath();
    if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
    return PAGES.contains(path)
        || AGENDA.matcher(path).matches()
        || path.equals("/index.html")
        || path.equals("/theme.js")
        || path.startsWith("/assets/");
  }

  @GetMapping({
    "/", "/login", "/register", "/tutor", "/student/slots", "/student/bookings",
    "/student/bookings/{bookingId}/agenda", "/admin", "/admin/overview", "/admin/users",
    "/admin/modules", "/admin/assignments", "/admin/bookings", "/admin/reports"
  })
  String frontend() {
    return "forward:/index.html";
  }
}

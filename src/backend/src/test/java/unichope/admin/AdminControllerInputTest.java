package unichope.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import unichope.platform.web.ApiFailure;

class AdminControllerInputTest {
  final AdminService service = mock(AdminService.class);
  final AdminController controller =
      new AdminController(service, mock(ActivityReportService.class));

  MockHttpServletRequest json(String raw) {
    var r = new MockHttpServletRequest();
    r.setContentType("application/json");
    r.setContent(raw.getBytes(StandardCharsets.UTF_8));
    return r;
  }

  @Test
  void rejectsActorOverridesUnknownDuplicateAndTrailingJson() throws Exception {
    for (String raw :
        new String[] {
          "{\"code\":\"CS\",\"name\":\"Name\",\"actorId\":\"x\"}",
          "{\"code\":\"CS\",\"code\":\"ZZ\",\"name\":\"Name\"}",
          "{\"code\":\"CS\",\"name\":\"Name\"} {}",
          "{\"code\":true,\"name\":\"Name\"}"
        }) {
      assertEquals(
          400, assertThrows(ApiFailure.class, () -> controller.createModule(json(raw))).status());
    }
    verifyNoInteractions(service);
  }

  @Test
  void boundedActualBytesAndStrictContentType() {
    var huge = json(" ".repeat(65537));
    assertEquals(413, assertThrows(ApiFailure.class, () -> controller.createModule(huge)).status());
    var text = json("{}");
    text.setContentType("text/plain");
    assertEquals(415, assertThrows(ApiFailure.class, () -> controller.createModule(text)).status());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsUnknownRepeatedAndMalformedQuery() {
    var unknown = new MockHttpServletRequest();
    unknown.addParameter("actorId", "x");
    assertThrows(ApiFailure.class, () -> controller.users(unknown));
    var repeated = new MockHttpServletRequest();
    repeated.addParameter("role", "TUTOR", "STUDENT");
    assertThrows(ApiFailure.class, () -> controller.users(repeated));
    var bool = new MockHttpServletRequest();
    bool.addParameter("active", "yes");
    assertThrows(ApiFailure.class, () -> controller.users(bool));
    var date = new MockHttpServletRequest();
    date.addParameter("date", "2026-02-30");
    assertThrows(ApiFailure.class, () -> controller.bookings(date));
    verifyNoInteractions(service);
  }
}

package unichope.platform.auth;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerInputTest {
  @Test
  void strictFieldsAndBodyBoundsRejectBeforeService() throws Exception {
    var service = mock(AuthenticationService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AuthController(service, new HttpSessionCsrfTokenRepository()))
            .setControllerAdvice(new AuthExceptionHandler())
            .build();
    for (String body :
        new String[] {
          "{\"email\":\"x\",\"password\":\"password\",\"role\":\"ADMIN\"}",
          "{\"email\":42,\"password\":\"password\"}",
          "{\"email\":\"x\",\"email\":\"y\",\"password\":\"password\"}",
          "{} {}"
        })
      mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    mvc.perform(post("/api/auth/login").contentType("application/json").content("x".repeat(65537)))
        .andExpect(status().is(413));
    mvc.perform(post("/api/auth/login").contentType("text/plain").content("{}"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(service);
  }
}

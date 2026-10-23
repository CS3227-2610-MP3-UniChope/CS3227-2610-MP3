package unichope.platform.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import unichope.UnichopeApplication;
import unichope.platform.auth.PasswordHasher;
import unichope.platform.core.*;

@Tag("postgres")
class ApplicationIntegrationTest {
  ConfigurableApplicationContext context;
  DriverManagerDataSource base;
  JdbcClient db;
  String schema;
  MockMvc mvc;
  UUID admin, tutor, student;
  static final String PASSWORD = "Synthetic-pass-2026";

  @BeforeEach
  void setup() throws Exception {
    String url = System.getProperty("core.test.jdbcUrl");
    assertNotNull(url, "Explicit disposable PostgreSQL required");
    String user = System.getProperty("core.test.user", "");
    String password = System.getProperty("core.test.password", "");
    base = new DriverManagerDataSource(url, user, password);
    schema = "runtime_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection c = base.getConnection();
        var statement = c.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
    }
    String schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
    context =
        new SpringApplicationBuilder(UnichopeApplication.class)
            .web(WebApplicationType.SERVLET)
            .profiles("app-api", "local")
            .properties(
                Map.of(
                    "server.port",
                    "0",
                    "unichope.jdbc.url",
                    schemaUrl,
                    "unichope.jdbc.user",
                    user,
                    "unichope.jdbc.password",
                    password,
                    "unichope.ai.mode",
                    "fake"))
            .run();
    mvc =
        MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
            .apply(springSecurity())
            .build();
    db = JdbcClient.create(context.getBean(javax.sql.DataSource.class));
    String hash = context.getBean(PasswordHasher.class).hash(PASSWORD);
    admin = seed("ADMIN", "admin", hash);
    tutor = seed("TUTOR", "tutor", hash);
    student = seed("STUDENT", "student", hash);
  }

  UUID seed(String role, String name, String hash) {
    UUID id = UUID.randomUUID();
    db.sql(
            "INSERT INTO users(id,name,email,role,active,password_hash)"
                + " VALUES(:id,:name,:email,:role,true,:hash)")
        .param("id", id)
        .param("name", name)
        .param("email", name + "@example.edu")
        .param("role", role)
        .param("hash", hash)
        .update();
    return id;
  }

  @AfterEach
  void cleanup() throws Exception {
    if (context != null) context.close();
    if (schema != null)
      try (Connection c = base.getConnection();
          var statement = c.createStatement()) {
        statement.execute("DROP SCHEMA " + schema + " CASCADE");
      }
  }

  record Browser(MockHttpSession session, String token, String header) {}

  Browser token(MockHttpSession session) throws Exception {
    var request = get("/api/auth/csrf");
    if (session != null) request.session(session);
    var result = mvc.perform(request).andReturn();
    assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
    var body = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
    return new Browser(
        (MockHttpSession) result.getRequest().getSession(false),
        body.get("token").stringValue(),
        body.get("headerName").stringValue());
  }

  Browser login(String name) throws Exception {
    Browser browser = token(null);
    String previous = browser.session().getId();
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .session(browser.session())
                    .header(browser.header(), browser.token())
                    .contentType("application/json")
                    .content(
                        "{\"email\":\""
                            + name
                            + "@example.edu\",\"password\":\""
                            + PASSWORD
                            + "\"}"))
            .andReturn();
    assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
    assertNotEquals(previous, result.getRequest().getSession(false).getId());
    var body = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
    assertEquals(Set.of("id", "name", "role", "mustChangePassword"), Set.copyOf(body.propertyNames()));
    return token((MockHttpSession) result.getRequest().getSession(false));
  }

  @Test
  void loginRoleGatesCsrfAndLogoutWorkTogether() throws Exception {
    assertEquals(401, mvc.perform(get("/api/auth/me")).andReturn().getResponse().getStatus());
    Browser b = login("admin");
    assertEquals(
        200,
        mvc.perform(get("/api/auth/me").session(b.session()))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        403,
        mvc.perform(get("/api/tutor/modules").session(b.session()))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        403,
        mvc.perform(
                post("/api/admin/modules")
                    .session(b.session())
                    .contentType("application/json")
                    .content("{\"code\":\"CS1\",\"name\":\"Module\"}"))
            .andReturn()
            .getResponse()
            .getStatus());
    var created =
        mvc.perform(
                post("/api/admin/modules")
                    .session(b.session())
                    .header(b.header(), b.token())
                    .contentType("application/json")
                    .content("{\"code\":\"CS1\",\"name\":\"Module\"}"))
            .andReturn();
    assertEquals(
        201, created.getResponse().getStatus(), created.getResponse().getContentAsString());
    var logout =
        mvc.perform(post("/api/auth/logout").session(b.session()).header(b.header(), b.token()))
            .andReturn();
    assertEquals(204, logout.getResponse().getStatus());
    assertTrue(b.session().isInvalid());
    assertEquals(401, mvc.perform(get("/api/admin/users")).andReturn().getResponse().getStatus());
  }

  @Test
  void deactivationInvalidatesSessionsAndReactivationCannotReviveThem() throws Exception {
    Browser b = login("student");
    var storage = context.getBean(AccountStorage.class);
    storage.setAccountActive(new AdminContext(admin, 0), student, false);
    var denied = mvc.perform(get("/api/auth/me").session(b.session())).andReturn();
    assertEquals(403, denied.getResponse().getStatus());
    assertTrue(denied.getResponse().getContentAsString().contains("ACCOUNT_INACTIVE"));
    assertTrue(b.session().isInvalid());
    storage.setAccountActive(new AdminContext(admin, 0), student, true);
    assertEquals(401, mvc.perform(get("/api/auth/me")).andReturn().getResponse().getStatus());
    Browser old = login("student");
    storage.setAccountActive(new AdminContext(admin, 0), student, false);
    storage.setAccountActive(new AdminContext(admin, 0), student, true);
    assertEquals(
        401,
        mvc.perform(get("/api/auth/me").session(old.session()))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void tutorUsesRealSessionAndRegistrationCannotChooseRole() throws Exception {
    Browser b = login("tutor");
    var list = mvc.perform(get("/api/tutor/modules").session(b.session())).andReturn();
    assertEquals(200, list.getResponse().getStatus(), list.getResponse().getContentAsString());
    assertEquals(
        403,
        mvc.perform(get("/api/admin/users").session(b.session()))
            .andReturn()
            .getResponse()
            .getStatus());
    Browser anon = token(null);
    String fields =
        "\"name\":\"New\",\"email\":\"New@example.edu\",\"password\":\""
            + PASSWORD
            + "\",\"confirmation\":\""
            + PASSWORD
            + "\"";
    var spoof =
        mvc.perform(
                post("/api/auth/register")
                    .session(anon.session())
                    .header(anon.header(), anon.token())
                    .contentType("application/json")
                    .content("{" + fields + ",\"role\":\"ADMIN\"}"))
            .andReturn();
    assertEquals(400, spoof.getResponse().getStatus());
    var created =
        mvc.perform(
                post("/api/auth/register")
                    .session(anon.session())
                    .header(anon.header(), anon.token())
                    .contentType("application/json")
                    .content("{" + fields + "}"))
            .andReturn();
    assertEquals(
        201, created.getResponse().getStatus(), created.getResponse().getContentAsString());
    assertEquals(
        "STUDENT",
        db.sql("SELECT role FROM users WHERE email='new@example.edu'")
            .query(String.class)
            .single());
    assertEquals(
        401,
        mvc.perform(get("/api/auth/me").session(anon.session()))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void provisioningAndEmptyReportAreSafe() throws Exception {
    Browser b = login("admin");
    var created =
        mvc.perform(
                post("/api/admin/users")
                    .session(b.session())
                    .header(b.header(), b.token())
                    .contentType("application/json")
                    .content(
                        "{\"role\":\"TUTOR\",\"name\":\"Created\",\"email\":\"created@example.edu\",\"password\":\""
                            + PASSWORD
                            + "\"}"))
            .andReturn();
    assertEquals(
        201, created.getResponse().getStatus(), created.getResponse().getContentAsString());
    assertFalse(created.getResponse().getContentAsString().contains("hash"));
    assertFalse(created.getResponse().getContentAsString().contains(PASSWORD));
    UUID createdId = UUID.fromString(JsonMapper.shared().readTree(created.getResponse().getContentAsString()).get("id").stringValue());
    assertTrue(db.sql("SELECT must_change_password FROM users WHERE id=:id").param("id", createdId).query(Boolean.class).single());
    Browser initial = login("created");
    var me = mvc.perform(get("/api/auth/me").session(initial.session())).andReturn();
    assertTrue(me.getResponse().getContentAsString().contains("\"mustChangePassword\":true"));
    assertEquals(403, mvc.perform(get("/api/tutor/modules").session(initial.session())).andReturn().getResponse().getStatus());
    assertTrue(mvc.perform(get("/api/tutor/modules").session(initial.session())).andReturn().getResponse().getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
    var mismatch = mvc.perform(post("/api/auth/change-password").session(initial.session()).header(initial.header(), initial.token())
        .contentType("application/json").content("{\"password\":\"New-password-2026\",\"confirmation\":\"Different-password-2026\"}")).andReturn();
    assertEquals(400, mismatch.getResponse().getStatus());
    assertTrue(db.sql("SELECT must_change_password FROM users WHERE id=:id").param("id", createdId).query(Boolean.class).single());
    var missingCsrf = mvc.perform(post("/api/auth/change-password").session(initial.session())
        .contentType("application/json").content("{\"password\":\"New-password-2026\",\"confirmation\":\"New-password-2026\"}")).andReturn();
    assertEquals(403, missingCsrf.getResponse().getStatus());
    var changed = mvc.perform(post("/api/auth/change-password").session(initial.session()).header(initial.header(), initial.token())
        .contentType("application/json").content("{\"password\":\"New-password-2026\",\"confirmation\":\"New-password-2026\"}")).andReturn();
    assertEquals(204, changed.getResponse().getStatus(), changed.getResponse().getContentAsString());
    assertEquals(401, mvc.perform(get("/api/auth/me").session(initial.session())).andReturn().getResponse().getStatus());
    var newLoginToken = token(null);
    var newLogin = mvc.perform(post("/api/auth/login").session(newLoginToken.session()).header(newLoginToken.header(), newLoginToken.token())
        .contentType("application/json").content("{\"email\":\"created@example.edu\",\"password\":\"New-password-2026\"}")).andReturn();
    assertEquals(200, newLogin.getResponse().getStatus(), newLogin.getResponse().getContentAsString());
    assertTrue(newLogin.getResponse().getContentAsString().contains("\"mustChangePassword\":false"));
    assertEquals(200, mvc.perform(get("/api/auth/me").session((MockHttpSession) newLogin.getRequest().getSession(false)))
        .andReturn().getResponse().getStatus());
    var report =
        mvc.perform(
                post("/api/admin/activity-report")
                    .session(b.session())
                    .header(b.header(), b.token())
                    .contentType("application/json")
                    .content("{}"))
            .andReturn();
    assertEquals(200, report.getResponse().getStatus(), report.getResponse().getContentAsString());
    assertTrue(report.getResponse().getContentAsString().contains("\"providerMode\":\"none\""));
  }

  @Test
  void nonemptyFakeReportsAreLabelledAndRateLimited() throws Exception {
    UUID module = UUID.randomUUID(), slot = UUID.randomUUID();
    db.sql("INSERT INTO modules VALUES(:id,'CS3227','Private label not for provider',true)")
        .param("id", module)
        .update();
    db.sql(
            "INSERT INTO slots VALUES(:id,:tutor,:module,CURRENT_TIMESTAMP-INTERVAL '2"
                + " hours',CURRENT_TIMESTAMP-INTERVAL '1 hour','COMPLETED')")
        .param("id", slot)
        .param("tutor", tutor)
        .param("module", module)
        .update();
    db.sql("INSERT INTO bookings VALUES(:id,:student,:slot,CURRENT_TIMESTAMP,'COMPLETED')")
        .param("id", UUID.randomUUID())
        .param("student", student)
        .param("slot", slot)
        .update();
    Browser b = login("admin");
    for (int i = 0; i < 3; i++) {
      var result =
          mvc.perform(
                  post("/api/admin/activity-report")
                      .session(b.session())
                      .header(b.header(), b.token())
                      .contentType("application/json")
                      .content("{}"))
              .andReturn();
      assertEquals(
          200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
      var json = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
      assertEquals("fake", json.get("providerMode").stringValue());
      assertTrue(json.get("requiresReview").booleanValue());
      assertFalse(result.getResponse().getContentAsString().contains("password_hash"));
    }
    var limited =
        mvc.perform(
                post("/api/admin/activity-report")
                    .session(b.session())
                    .header(b.header(), b.token())
                    .contentType("application/json")
                    .content("{}"))
            .andReturn();
    assertEquals(429, limited.getResponse().getStatus());
    assertNotNull(limited.getResponse().getHeader("Retry-After"));
    assertEquals(1, db.sql("SELECT count(*) FROM bookings").query(Long.class).single());
    assertEquals(
        200,
        mvc.perform(get("/api/admin/statistics").session(b.session()))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void initialAdminSetupIsAtomicAndOnlyOneCallerSucceeds() throws Exception {
    db.sql("DELETE FROM users").update();
    var source = context.getBean(javax.sql.DataSource.class);
    String hash = context.getBean(PasswordHasher.class).hash(PASSWORD);
    var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var tasks =
          java.util.List.<java.util.concurrent.Callable<Boolean>>of(
              () -> {
                try {
                  BootstrapAdmin.create(source, "First", "first@example.edu", hash);
                  return true;
                } catch (IllegalStateException expected) {
                  return false;
                }
              },
              () -> {
                try {
                  BootstrapAdmin.create(source, "Second", "second@example.edu", hash);
                  return true;
                } catch (IllegalStateException expected) {
                  return false;
                }
              });
      var results = workers.invokeAll(tasks);
      int successes = 0;
      for (var result : results) if (result.get()) successes++;
      assertEquals(1, successes);
      assertEquals(
          1, db.sql("SELECT count(*) FROM users WHERE role='ADMIN'").query(Long.class).single());
      assertEquals(1, db.sql("SELECT count(*) FROM bootstrap_state").query(Long.class).single());
      assertThrows(
          IllegalStateException.class,
          () -> BootstrapAdmin.create(source, "Third", "third@example.edu", hash));
    } finally {
      workers.shutdownNow();
    }
  }
}

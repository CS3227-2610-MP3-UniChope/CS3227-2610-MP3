package unichope.platform.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import unichope.platform.auth.AuthFailure;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.auth.SessionPrincipal;

/** Exercises the real servlet security chain without requiring a database. */
class SpaServingTest {
  @Configuration
  @EnableWebMvc
  @Import({ApiSecurityConfiguration.class, SpaController.class})
  static class WebConfiguration implements WebMvcConfigurer {
    @Bean
    AuthenticationService authenticationService() {
      return mock(AuthenticationService.class);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resources) {
      resources.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
  }

  private AnnotationConfigWebApplicationContext context(String... profiles) {
    var context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.getEnvironment().setActiveProfiles(profiles);
    context.register(WebConfiguration.class);
    context.refresh();
    return context;
  }

  private MockMvc mvc(AnnotationConfigWebApplicationContext context) {
    return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void applicationPagesCanBeOpenedAndRefreshedWithoutAuthentication() throws Exception {
    try (var context = context("app-api", "auth-api", "admin-api", "student-api", "tutor-api")) {
      var mvc = mvc(context);
      for (String path :
          new String[] {
            "/",
            "/login",
            "/register",
            "/admin/modules",
            "/admin/reports",
            "/tutor",
            "/student/slots",
            "/student/bookings/12345678-1234-1234-1234-123456789abc/agenda"
          }) {
        var response =
            mvc.perform(
                    get(path)
                        .with(
                            request -> {
                              request.setServletPath(path);
                              return request;
                            }))
                .andReturn()
                .getResponse();
        assertEquals(200, response.getStatus(), path);
        assertEquals("/index.html", response.getForwardedUrl(), path);
      }
      var head =
          mvc.perform(head("/admin/modules").servletPath("/admin/modules"))
              .andReturn()
              .getResponse();
      assertEquals(200, head.getStatus());
      assertEquals("/index.html", head.getForwardedUrl());
    }
  }

  @Test
  void staticAssetsArePublicButApisAndUnknownPathsNeverReturnTheShell() throws Exception {
    try (var context = context("app-api", "auth-api", "admin-api")) {
      var mvc = mvc(context);
      var asset =
          mvc.perform(
                  get("/assets/spa-security-probe.js").servletPath("/assets/spa-security-probe.js"))
              .andReturn()
              .getResponse();
      assertEquals(200, asset.getStatus());
      assertTrue(asset.getContentAsString().contains("spa-security-probe"));
      for (String path :
          new String[] {
            "/api/admin/statistics",
            "/api/unknown",
            "/unknown-page",
            "/admin/unknown",
            "/student/bookings/not-a-uuid/agenda",
            "/.env",
            "/application.properties"
          }) {
        var response = mvc.perform(get(path).servletPath(path)).andReturn().getResponse();
        assertEquals(401, response.getStatus(), path);
        assertNull(response.getForwardedUrl(), path);
        assertTrue(response.getContentType().startsWith("application/json"), path);
        assertTrue(response.getContentAsString().contains("UNAUTHENTICATED"), path);
      }
      var missingAsset =
          mvc.perform(get("/assets/missing.js").servletPath("/assets/missing.js"))
              .andReturn()
              .getResponse();
      assertEquals(404, missingAsset.getStatus());
      assertNull(missingAsset.getForwardedUrl());
    }
  }

  @Test
  void frontendPermissionDoesNotPermitWritesOrDisableCsrf() throws Exception {
    try (var context = context("app-api", "auth-api")) {
      var mvc = mvc(context);
      for (String path :
          new String[] {"/admin/modules", "/index.html", "/assets/spa-security-probe.js"}) {
        var response = mvc.perform(post(path).servletPath(path)).andReturn().getResponse();
        assertEquals(403, response.getStatus(), path);
        assertNull(response.getForwardedUrl(), path);
        assertTrue(response.getContentAsString().contains("CSRF_INVALID"), path);
      }
    }
  }

  @Test
  void staleSessionCanLoadLoginPageButCannotAccessApi() throws Exception {
    try (var context = context("app-api", "auth-api", "admin-api")) {
      var principal = new SessionPrincipal(java.util.UUID.randomUUID(), 1);
      when(context.getBean(AuthenticationService.class).current(principal))
          .thenThrow(new AuthFailure(401, "UNAUTHENTICATED"));
      var authentication =
          UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of());
      var mvc = mvc(context);
      var page =
          mvc.perform(get("/login").servletPath("/login").with(authentication(authentication)))
              .andReturn()
              .getResponse();
      assertEquals(200, page.getStatus());
      assertEquals("/index.html", page.getForwardedUrl());
      var api =
          mvc.perform(
                  get("/api/admin/statistics")
                      .servletPath("/api/admin/statistics")
                      .with(authentication(authentication)))
              .andReturn()
              .getResponse();
      assertEquals(401, api.getStatus());
      assertNull(api.getForwardedUrl());
    }
  }

  @Test
  void frontendStaysClosedWithoutApplicationProfile() throws Exception {
    try (var context = context()) {
      var mvc = mvc(context);
      for (String path :
          new String[] {"/", "/login", "/index.html", "/assets/spa-security-probe.js"}) {
        var response =
            mvc.perform(
                    get(path)
                        .with(
                            request -> {
                              request.setServletPath(path);
                              return request;
                            }))
                .andReturn()
                .getResponse();
        assertEquals(401, response.getStatus(), path);
        assertNull(response.getForwardedUrl(), path);
      }
    }
  }
}

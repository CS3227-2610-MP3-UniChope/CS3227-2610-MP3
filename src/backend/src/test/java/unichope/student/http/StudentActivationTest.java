package unichope.student.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.UnichopeApplication;
import unichope.student.StudentTestActors;

/** stu-c1 C1-ACT-1 and C1-ACT-3 checks that need no database (unit `test` task). */
class StudentActivationTest {
    private static final String STUDENT_PACKAGE = "unichope.student.";
    private final JsonMapper json = JsonMapper.shared();

    /** C1-ACT-1: default startup registers no Student handler and serves no Student data. */
    @Test
    void c1Act1DefaultStartupHasNoStudentHandlersAndDeniesStudentRoutes() throws Exception {
        try (var context = new SpringApplicationBuilder(UnichopeApplication.class).web(WebApplicationType.SERVLET)
                .properties("server.address=127.0.0.1", "server.port=0").run()) {
            assertTrue(context.getBeansWithAnnotation(Controller.class).values().stream()
                    .noneMatch(bean -> ClassUtils.getUserClass(bean).getName().startsWith(STUDENT_PACKAGE)),
                    "Default startup registered a Student controller bean");
            var mapping = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
            assertTrue(mapping.getHandlerMethods().values().stream()
                    .noneMatch(method -> method.getBeanType().getName().startsWith(STUDENT_PACKAGE)),
                    "Default startup mapped a Student handler method");
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).apply(springSecurity()).build();
            for (String path : List.of("/api/student/modules", "/api/student/slots")) {
                error(mvc.perform(get(path)).andReturn(), 401, "UNAUTHENTICATED");
                error(mvc.perform(get(path).session(session(new UUID(0, 3)))).andReturn(), 403, "FORBIDDEN");
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** C1-ACT-3 (TECH §2): the actor seam is the shared platform/auth interface, Optional&lt;UUID&gt; currentActorId(). */
    @Test
    void c1Act3ActorSeamIsSharedPlatformAuthInterface() throws Exception {
        Optional<Class<?>> found = StudentTestActors.providerType();
        assertTrue(found.isPresent(), StudentTestActors.PROVIDER_TYPE + " does not exist");
        Class<?> type = found.get();
        assertTrue(type.isInterface(), "CurrentActorProvider must be an interface");
        Method method = type.getMethod("currentActorId");
        assertEquals(0, method.getParameterCount());
        assertTrue(Modifier.isAbstract(method.getModifiers()), "currentActorId must be implemented by auth, not defaulted");
        assertEquals("java.util.Optional<java.util.UUID>", method.getGenericReturnType().getTypeName());
    }

    private static MockHttpSession session(Object principal) {
        var session = new MockHttpSession();
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, "synthetic",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        return session;
    }

    private void error(MvcResult result, int status, String code) throws Exception {
        var response = result.getResponse();
        assertEquals(status, response.getStatus(), response.getContentAsString());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = json.readTree(response.getContentAsString());
        assertEquals(Set.of("code", "message", "requestId"), Set.copyOf(body.propertyNames()));
        assertEquals(code, body.get("code").asString());
        UUID.fromString(body.get("requestId").asString());
    }
}

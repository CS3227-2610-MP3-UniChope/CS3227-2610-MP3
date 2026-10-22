package unichope.tutor.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import unichope.UnichopeApplication;
import unichope.platform.web.ApiSecurityConfiguration;

class TutorActivationTest {
    @Test
    void rt20DefaultHasNoTutorHandlersOrGeneratedLogin() throws Exception {
        try(var context=new SpringApplicationBuilder(UnichopeApplication.class).web(WebApplicationType.SERVLET)
                .properties("server.port=0").run()) {
            assertTrue(context.getBeansOfType(UserDetailsService.class).isEmpty());
            assertTrue(context.getBeansOfType(TutorController.class).isEmpty());
            var mapping=context.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class);
            assertTrue(mapping.getHandlerMethods().values().stream().noneMatch(method->method.getBeanType()==TutorController.class));
            var mvc=MockMvcBuilders.webAppContextSetup((org.springframework.web.context.WebApplicationContext)context).apply(springSecurity()).build();
            for(String path:new String[]{"/api/tutor/modules","/login"}) {
                var response=mvc.perform(get(path)).andReturn().getResponse();
                assertEquals(401,response.getStatus());assertEquals("no-store",response.getHeader("Cache-Control"));
                assertFalse(response.getContentAsString().contains("<form"));
            }
        }
    }

    @Test
    void rt20ActivatedMissingIdentityAndDatabaseFailsWithoutFallback() {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.getEnvironment().setActiveProfiles("tutor-api");
            context.register(MvcConfig.class,TutorApiConfiguration.class,ApiSecurityConfiguration.class);
            var error=assertThrows(org.springframework.beans.BeansException.class,context::refresh);
            assertTrue(error.toString().contains("CurrentActorProvider")||error.toString().contains("TutorReadRepository"));
        }
    }
    @Configuration @EnableWebMvc static class MvcConfig {}
}

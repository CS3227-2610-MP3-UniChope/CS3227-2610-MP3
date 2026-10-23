package unichope.platform.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.UnichopeApplication;
import unichope.platform.ai.AiGateway;
import unichope.platform.auth.PasswordHasher;

/** AG-R3: actual login/session/filter composition, runtime migrations and real PostgreSQL.
 * Provider latches isolate a completed logout/revocation before the original call resumes. */
@Tag("postgres")
class AgendaSessionRacePgTest {
    static final String PASSWORD="Synthetic-pass-2026";
    static final String AGENDA="{\"goal\":\"Synthetic private result\",\"items\":[{\"topic\":\"Recursion\",\"question\":\"Clarify base cases\",\"minutes\":1}],\"prepare\":[]}";
    final CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
    final AtomicInteger providerCalls=new AtomicInteger();
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    ConfigurableApplicationContext context;
    DriverManagerDataSource base;
    JdbcClient db;
    MockMvc mvc;
    String schema;
    UUID student, booking;
    record Browser(MockHttpSession session,String header,String token) {}

    @BeforeEach void setup() throws Exception {
        String url=System.getProperty("core.test.jdbcUrl"); assertNotNull(url,"Explicit disposable PostgreSQL required");
        String user=System.getProperty("core.test.user",""), password=System.getProperty("core.test.password","");
        base=new DriverManagerDataSource(url,user,password); schema="agenda_session_test_"+UUID.randomUUID().toString().replace("-","");
        try(Connection c=base.getConnection();var s=c.createStatement()){s.execute("CREATE SCHEMA "+schema);}
        context=new SpringApplicationBuilder(UnichopeApplication.class).web(WebApplicationType.SERVLET)
                .profiles("app-api","local").initializers(application->{
                    var definition=new RootBeanDefinition(AiGateway.class,()->(AiGateway) request->{
                        providerCalls.incrementAndGet(); entered.countDown();
                        try { if(!release.await(15,TimeUnit.SECONDS)) throw new AssertionError("Provider release timed out"); }
                        catch(InterruptedException failure){Thread.currentThread().interrupt();throw new AssertionError(failure);}
                        return new AiGateway.Result(AGENDA,AiGateway.ProviderMode.FAKE);
                    });
                    definition.setPrimary(true); ((BeanDefinitionRegistry)application).registerBeanDefinition("agendaSessionRaceGateway",definition);
                }).properties(Map.of("server.port","0","server.address","127.0.0.1",
                        "unichope.jdbc.url",url+(url.contains("?")?"&":"?")+"currentSchema="+schema,
                        "unichope.jdbc.user",user,"unichope.jdbc.password",password,"unichope.ai.mode","fake")).run();
        mvc=MockMvcBuilders.webAppContextSetup((WebApplicationContext)context).apply(springSecurity()).build();
        db=JdbcClient.create(context.getBean(javax.sql.DataSource.class));
        String hash=context.getBean(PasswordHasher.class).hash(PASSWORD);
        student=seed("student","STUDENT",hash); UUID admin=seed("admin","ADMIN",hash),tutor=seed("tutor","TUTOR",hash);
        assertNotNull(admin);
        UUID module=UUID.randomUUID(),slot=UUID.randomUUID(); booking=UUID.randomUUID();
        db.sql("INSERT INTO modules VALUES(:id,'CS3227','Synthetic module',true)").param("id",module).update();
        db.sql("INSERT INTO tutor_modules VALUES(:t,:m)").param("t",tutor).param("m",module).update();
        Instant start=Instant.now().plusSeconds(86400);
        db.sql("INSERT INTO slots VALUES(:id,:t,:m,:s,:e,'BOOKED')").param("id",slot).param("t",tutor).param("m",module)
                .param("s",start.atOffset(ZoneOffset.UTC)).param("e",start.plusSeconds(3600).atOffset(ZoneOffset.UTC)).update();
        db.sql("INSERT INTO bookings VALUES(:id,:s,:slot,:at,'ACTIVE')").param("id",booking).param("s",student).param("slot",slot).param("at",Instant.now().atOffset(ZoneOffset.UTC)).update();
    }
    UUID seed(String name,String role,String hash) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO users(id,name,email,role,active,password_hash) VALUES(:id,:n,:e,:r,true,:h)")
                .param("id",id).param("n","Synthetic "+name).param("e",name+"@example.edu").param("r",role).param("h",hash).update(); return id;
    }
    Browser token(MockHttpSession session) throws Exception {
        var r=get("/api/auth/csrf");if(session!=null&&!session.isInvalid())r.session(session);
        var result=mvc.perform(r).andReturn(); assertEquals(200,result.getResponse().getStatus()); var body=body(result);
        return new Browser((MockHttpSession)result.getRequest().getSession(false),body.get("headerName").asString(),body.get("token").asString());
    }
    Browser login(String name) throws Exception {
        Browser anonymous=token(null);
        var r=request(post("/api/auth/login").contentType("application/json").content(JsonMapper.shared().writeValueAsString(Map.of("email",name+"@example.edu","password",PASSWORD))),anonymous);
        assertEquals(200,r.getResponse().getStatus(),r.getResponse().getContentAsString()); return token((MockHttpSession)r.getRequest().getSession(false));
    }
    MvcResult request(MockHttpServletRequestBuilder request,Browser browser) throws Exception {
        return mvc.perform(request.session(browser.session()).header(browser.header(),browser.token()).accept("application/json")).andReturn();
    }
    JsonNode body(MvcResult result) throws Exception{return JsonMapper.shared().readTree(result.getResponse().getContentAsString());}
    String snapshot(){return db.sql("SELECT row_to_json(b)::text FROM bookings b ORDER BY id").query(String.class).list().toString()+db.sql("SELECT row_to_json(s)::text FROM slots s ORDER BY id").query(String.class).list();}
    Future<MvcResult> pending(Browser browser) throws Exception {
        var response=worker.submit(()->request(post("/api/student/bookings/"+booking+"/agenda").contentType("application/json").content("{\"questions\":\"Synthetic recursion questions\"}"),browser));
        assertTrue(entered.await(10,TimeUnit.SECONDS),"Generation did not reach the controlled provider"); return response;
    }
    void deniedWithoutResult(MvcResult response) throws Exception {
        assertEquals(401,response.getResponse().getStatus(),response.getResponse().getContentAsString());
        assertEquals("UNAUTHENTICATED",body(response).get("code").asString());
        assertFalse(response.getResponse().getContentAsString().contains("Synthetic private result"));
        assertEquals(1,providerCalls.get());
    }
    @Test void agR3ActualLogoutDuringGenerationDiscardsResult() throws Exception {
        Browser browser=login("student");String before=snapshot();var response=pending(browser);
        var logout=request(post("/api/auth/logout"),browser);
        assertEquals(204,logout.getResponse().getStatus());assertTrue(browser.session().isInvalid());
        release.countDown();deniedWithoutResult(response.get(15,TimeUnit.SECONDS));assertEquals(before,snapshot());
    }
    @Test void agR3PersistedRevocationThenReactivationDoesNotRevivePendingSessionGeneration() throws Exception {
        Browser browser=login("student");String before=snapshot();var response=pending(browser);
        // Fixture the revoked state directly: actual Admin deactivation is correctly blocked
        // while an ACTIVE booking exists. This does not assert a new Admin mutation policy.
        long generation=db.sql("SELECT session_generation FROM users WHERE id=:id").param("id",student).query(Long.class).single();
        db.sql("UPDATE users SET active=false,session_generation=session_generation+1 WHERE id=:id").param("id",student).update();
        db.sql("UPDATE users SET active=true WHERE id=:id").param("id",student).update();
        assertEquals(generation+1,db.sql("SELECT session_generation FROM users WHERE id=:id").param("id",student).query(Long.class).single());
        release.countDown();deniedWithoutResult(response.get(15,TimeUnit.SECONDS));assertEquals(before,snapshot());
        Browser fresh=login("student");assertEquals(200,request(get("/api/student/bookings"),fresh).getResponse().getStatus());
    }
    @AfterEach void cleanup() throws Exception {
        release.countDown();worker.shutdownNow();assertTrue(worker.awaitTermination(15,TimeUnit.SECONDS));SecurityContextHolder.clearContext();
        if(context!=null)context.close();
        if(schema!=null)try(Connection c=base.getConnection();var s=c.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}
    }
}

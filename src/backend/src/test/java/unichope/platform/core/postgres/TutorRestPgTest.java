package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.Instant;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LockedCompletedBooking;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.model.module.Module;
import unichope.platform.core.model.module.TutorModule;
import unichope.platform.core.repository.ModuleRepository;
import unichope.platform.web.ApiSecurityConfiguration;
import unichope.tutor.CurrentActorProvider;
import unichope.tutor.PostgresTutorReadRepository;
import unichope.tutor.http.TutorApiConfiguration;

@Tag("postgres")
class TutorRestPgTest extends LifecyclePgFixture {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private FaultingLifecycle writes;
    private ProjectionModules modules;
    private final JsonMapper json=JsonMapper.shared();

    @BeforeEach
    void configureRealMvcAndSecurityWithTestOnlySessionActor() {
        org.flywaydb.core.Flyway.configure().dataSource(source).defaultSchema(schema).schemas(schema)
                .locations("classpath:db/core-review","classpath:db/tutor-review").load().migrate();
        writes=new FaultingLifecycle(lifecycle); modules=new ProjectionModules(new PostgresModuleRepository(db));
        context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles("tutor-api");
        context.addBeanFactoryPostProcessor(this::registerDependencies);
        context.register(MvcFixture.class,TutorApiConfiguration.class,ApiSecurityConfiguration.class);context.refresh();
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void closeWebContext() { if(context!=null)context.close(); SecurityContextHolder.clearContext(); }

    @Test
    void rt01ModulesAreActiveAssignedExactAndEmpty() throws Exception {
        db.sql("UPDATE modules SET active=false WHERE id=:id").param("id",M2).update();
        JsonNode result=body(ok(get("/api/tutor/modules"),session(T1),200));
        assertEquals(1,result.size());assertEquals(M.toString(),result.get(0).get("id").asString());
        assertEquals(Set.of("id","code","name"),Set.copyOf(result.get(0).propertyNames()));
        db.sql("DELETE FROM tutor_modules WHERE tutor_id=:id").param("id",T1).update();
        assertEquals(0,body(ok(get("/api/tutor/modules"),session(T1),200)).size());
    }

    @Test
    void rt02Rt03UpcomingOwnerStatusOrderingAndSgtDates() throws Exception {
        UUID a=slot(T1,Instant.parse("2026-10-21T15:59:59Z"),Instant.parse("2026-10-21T16:30:00Z"),"AVAILABLE");
        UUID b=slot(T1,Instant.parse("2026-10-21T16:00:00Z"),Instant.parse("2026-10-21T17:00:00Z"),"BOOKED");
        UUID tie=slot(T1,Instant.parse("2026-10-21T16:00:00Z"),Instant.parse("2026-10-21T17:00:00Z"),"AVAILABLE");
        slot(T2,START,END,"AVAILABLE");slot(T1,N,END,"AVAILABLE");slot(T1,START,END,"COMPLETED");slot(T1,START,END,"CANCELLED");
        JsonNode all=body(ok(get("/api/tutor/slots"),session(T1),200));assertEquals(3,all.size()); assertEquals(a.toString(),all.get(0).get("id").asString());
        assertEquals(List.of(b,tie).stream().sorted().map(UUID::toString).toList(),List.of(all.get(1).get("id").asString(),all.get(2).get("id").asString()));
        exactSlot(all.get(0)); assertTrue(all.get(0).get("startTime").asString().endsWith("Z"));
        assertEquals(1,body(ok(get("/api/tutor/slots").param("date","2026-10-21"),session(T1),200)).size());
        assertEquals(2,body(ok(get("/api/tutor/slots").param("date","2026-10-22"),session(T1),200)).size());
        assertEquals(0,body(ok(get("/api/tutor/slots").param("date","2026-10-23"),session(T1),200)).size());
    }

    @Test
    void rt04DetailAllowsAllStatusesAndPastSlotsWithExactProjection() throws Exception {
        for(String status:List.of("AVAILABLE","BOOKED","CANCELLED","COMPLETED")) {
            UUID target=slot(T1,N.minusSeconds(3600),N.minusSeconds(1800),status);
            var result=body(ok(get("/api/tutor/slots/"+target),session(T1),200));exactSlot(result);
            assertEquals(status,result.get("status").asString());assertEquals("CS10",result.get("moduleCode").asString());
        }
    }

    @Test
    void rt05Rt06AllEndpointsUsePrincipalAndFreshStoredRoleInsteadOfAuthorities() throws Exception {
        UUID target=slot(T1,START,END,"AVAILABLE");
        for(int op=0;op<5;op++) {
            error(call(op,target,null,true),401,"UNAUTHENTICATED");
            error(call(op,target,session("no-provider-actor","ROLE_TUTOR"),true),401,"UNAUTHENTICATED");
            error(call(op,target,session(id(999)),true),401,"UNAUTHENTICATED");
            for(UUID wrong:List.of(S1,ADMIN))error(call(op,target,session(wrong,"ROLE_TUTOR"),true),403,"FORBIDDEN");
        }
        assertEquals(1,count("slots"));
        ok(get("/api/tutor/modules"),session(T1,"ROLE_STUDENT"),200);
    }

    @Test
    void rt07InactiveAndDeletedSessionInvalidatedAndCannotBeReused() throws Exception {
        for(boolean deleted:List.of(false,true)) {
            MockHttpSession original=session(deleted?id(999):T2);
            if(!deleted)db.sql("UPDATE users SET active=false WHERE id=:id").param("id",T2).update();
            error(okRaw(get("/api/tutor/modules"),original),deleted?401:403,deleted?"UNAUTHENTICATED":"ACCOUNT_INACTIVE");
            assertTrue(original.isInvalid());
            error(okRaw(get("/api/tutor/modules"),original),401,"UNAUTHENTICATED");
        }
    }

    @Test
    void rt08RealCsrfFiltersDenyMissingInvalidAndPermitValidTokensForBothMutations() throws Exception {
        UUID target=slot(T1,START,END,"AVAILABLE");String before=snapshot();
        for(boolean cancel:List.of(false,true)) {
            String path=cancel?"/api/tutor/slots/"+target+"/cancel":"/api/tutor/slots";
            String data=cancel?"{}":createBody(END,END.plusSeconds(1800));
            error(okRaw(post(path).contentType("application/json").content(data),session(T1)),403,"CSRF_INVALID");
            error(okRaw(post(path).contentType("application/json").content(data).with(csrf().asHeader().useInvalidToken()),session(T1)),403,"CSRF_INVALID");
        }
        assertEquals(before,snapshot());
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(END,END.plusSeconds(1800))),null),403,"CSRF_INVALID");
        ok(post("/api/tutor/slots").contentType("application/json").content(createBody(END,END.plusSeconds(1800))).with(csrf().asHeader()),session(T1),201);
        ok(post("/api/tutor/slots/"+target+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()),session(T1),200);
    }

    @Test
    void rt09ForeignAndMissingIdsHaveSameSafeNotFoundAndNoWrites() throws Exception {
        UUID foreign=slot(T2,START,END,"AVAILABLE");String before=snapshot();
        for(boolean cancel:List.of(false,true)) {
            var a=error(okRaw(cancel?post("/api/tutor/slots/"+foreign+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()):get("/api/tutor/slots/"+foreign),session(T1)),404,"NOT_FOUND");
            var b=error(okRaw(cancel?post("/api/tutor/slots/"+id(999)+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()):get("/api/tutor/slots/"+id(999)),session(T1)),404,"NOT_FOUND");
            assertEquals(a.get("message"),b.get("message"));
        }
        assertEquals(before,snapshot());
    }

    @Test
    void rt10Rt11IdentitySpoofsQueriesAndInvalidPathsNeverSelectActor() throws Exception {
        String before=snapshot();
        for(String field:List.of("actorId","tutorId","studentId","role","status")) {
            String data=createBody(START,END).replace("}",",\""+field+"\":\""+T2+"\"}");
            error(okRaw(post("/api/tutor/slots").contentType("application/json").content(data).with(csrf().asHeader()),session(T1)),400,"VALIDATION_FAILED");
        }
        for(String path:List.of("/api/tutor/modules","/api/tutor/slots","/api/tutor/slots/"+id(999))) error(okRaw(get(path).param("actorId",T2.toString()),session(T1)),400,"VALIDATION_FAILED");
        error(okRaw(get("/api/tutor/slots").param("date","2026-10-21","2026-10-22"),session(T1)),400,"VALIDATION_FAILED");
        error(okRaw(get("/api/tutor/slots/1-1-1-1-1"),session(T1)),400,"VALIDATION_FAILED");
        assertEquals(before,snapshot());
        var created=body(ok(post("/api/tutor/slots").header("X-Actor-Id",T2.toString()).contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1),201));
        assertEquals(T1,new PostgresSlotRepository(db).findById(UUID.fromString(created.get("id").asString())).orElseThrow().tutorId());
    }

    @Test
    void rt14RealRequestBodyBoundaryAndMisleadingDeclaredLength() throws Exception {
        UUID a=slot(T1,START,END,"AVAILABLE"),b=slot(T1,END,END.plusSeconds(1800),"AVAILABLE");
        String before=snapshot();
        error(okRaw(post("/api/tutor/slots/"+a+"/cancel").contentType("application/json").content("{}"+" ".repeat(65535)).header("Content-Length","2").with(csrf().asHeader()),session(T1)),413,"PAYLOAD_TOO_LARGE");
        assertEquals(before,snapshot());
        ok(post("/api/tutor/slots/"+b+"/cancel").contentType("application/json").content("{}"+" ".repeat(65534)).with(csrf().asHeader()),session(T1),200);
    }

    @Test
    void rt15Rt16CreateLocationDetailAndCancelHistoryUseRealLifecycle() throws Exception {
        var response=ok(post("/api/tutor/slots").contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1),201);
        JsonNode created=body(response);exactSlot(created);UUID id=UUID.fromString(created.get("id").asString());
        assertEquals("/api/tutor/slots/"+id,response.getResponse().getHeader("Location"));
        assertEquals(created,body(ok(get(response.getResponse().getHeader("Location")),session(T1),200)));assertEquals(1,count("slots"));
        ok(post("/api/tutor/slots/"+id+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()),session(T1),200);
        assertEquals("CANCELLED",slotStatus(id));assertEquals(1,count("slots"));
        assertEquals(0,body(ok(get("/api/tutor/slots"),session(T1),200)).size());
        error(okRaw(post("/api/tutor/slots/"+id+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()),session(T1)),409,"INVALID_STATE");
        ok(get("/api/tutor/slots/"+id),session(T1),200);
        UUID past=slot(T1,N.minusSeconds(3600),N.minusSeconds(1800),"AVAILABLE");
        ok(post("/api/tutor/slots/"+past+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()),session(T1),200);
    }

    @Test
    void rt17Rt18LifecycleMappingsSafeErrorsAndServerRequestIds() throws Exception {
        for(LifecycleFailure.Code code:LifecycleFailure.Code.values()) {
            writes.failure=new LifecycleFailure(code);
            int expected=switch(code){case INVALID_INPUT,INVALID_TIME->400;case ACCESS_DENIED->403;case NOT_FOUND->404;case TEMPORARILY_UNAVAILABLE->503;default->409;};
            String publicCode=switch(code){case INVALID_INPUT,INVALID_TIME->"VALIDATION_FAILED";case ACCESS_DENIED->"FORBIDDEN";default->code.name();};
            error(okRaw(post("/api/tutor/slots").header("X-Request-Id","client-sensitive-marker").contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1)),expected,publicCode);
        }
        writes.failure=new IllegalStateException("SELECT private-marker "+T1);
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1)),500,"INTERNAL_ERROR");
        writes.failure=new LifecycleFailure(LifecycleFailure.Code.ACCESS_DENIED);
        writes.beforeFailure=()->db.sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1)),403,"ACCOUNT_INACTIVE");
        assertEquals(0,count("slots"));
    }

    @Test
    void rt12Rt13RepresentativeValidationMediaAndRealConflictsHaveSafeResponses() throws Exception {
        for(String data:List.of("null","[]","{}",createBody(START,END)+" {}",createBody(START,END).replace("01:00:00Z","01:00:00+00:00"))) error(okRaw(post("/api/tutor/slots").contentType("application/json").content(data).with(csrf().asHeader()),session(T1)),400,"VALIDATION_FAILED");
        error(okRaw(post("/api/tutor/slots").contentType("text/plain").content(createBody(START,END)).with(csrf().asHeader()),session(T1)),415,"UNSUPPORTED_MEDIA_TYPE");
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(N,END)).with(csrf().asHeader()),session(T1)),400,"VALIDATION_FAILED");
        slot(T1,START,END,"AVAILABLE");String before=snapshot();
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1)),409,"SLOT_OVERLAP");
        db.sql("DELETE FROM tutor_modules WHERE tutor_id=:id").param("id",T1).update();
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(END,END.plusSeconds(1800))).with(csrf().asHeader()),session(T1)),409,"MODULE_UNAVAILABLE");
        assertEquals(before,snapshot());
    }

    @Test
    void rt19PostcommitProjectionFailureIs500ButDoesNotRollbackOrRetry() throws Exception {
        modules.fail=true;
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(START,END)).with(csrf().asHeader()),session(T1)),500,"INTERNAL_ERROR");
        assertEquals(1,count("slots"));modules.fail=false;
        JsonNode slots=body(ok(get("/api/tutor/slots"),session(T1),200));assertEquals(1,slots.size());
        UUID id=UUID.fromString(slots.get(0).get("id").asString());modules.fail=true;
        error(okRaw(post("/api/tutor/slots/"+id+"/cancel").contentType("application/json").content("{}").with(csrf().asHeader()),session(T1)),500,"INTERNAL_ERROR");
        assertEquals("CANCELLED",slotStatus(id));assertEquals(1,count("slots"));
    }

    @Test
    void rt21InterleavedSessionsNeverStickToSingletonActor() throws Exception {
        slot(T1,START,END,"AVAILABLE");slot(T2,END,END.plusSeconds(1800),"AVAILABLE");
        var a=session(T1);var b=session(T2);
        String first=ok(get("/api/tutor/slots"),a,200).getResponse().getContentAsString();
        String other=ok(get("/api/tutor/slots"),b,200).getResponse().getContentAsString();
        assertNotEquals(first,other);assertEquals(first,ok(get("/api/tutor/slots"),a,200).getResponse().getContentAsString());
    }

    @Test
    void rt18FrameworkMethodFailureHasSafeJsonInsteadOfEmptyOrHtmlResponse() throws Exception {
        error(okRaw(put("/api/tutor/modules").with(csrf().asHeader()),session(T1)),405,"METHOD_NOT_ALLOWED");
    }

    @Test
    void rt18IncompatibleAcceptStillGetsSafe406Json() throws Exception {
        error(okRaw(get("/api/tutor/modules").accept("text/html"),session(T1)),406,"NOT_ACCEPTABLE");
    }

    @Test
    void rt18UnknownTutorRouteGetsSafe404JsonForJsonAndHtmlAccept() throws Exception {
        for(String accept:List.of("application/json","text/html")) {
            error(okRaw(get("/api/tutor/unknown").accept(accept),session(T1)),404,"NOT_FOUND");
        }
    }

    @Test
    void rt18Rt20ActualBootActiveProfileWithTestOnlyDependenciesServesPositiveAndFrameworkErrors() throws Exception {
        try(var boot=new org.springframework.boot.builder.SpringApplicationBuilder(unichope.UnichopeApplication.class)
                .web(org.springframework.boot.WebApplicationType.SERVLET).profiles("tutor-api")
                .properties("server.address=127.0.0.1","server.port=0")
                .initializers(application->registerDependencies(application.getBeanFactory())).run()) {
            assertEquals(1,boot.getBeansOfType(unichope.tutor.http.TutorController.class).size());
            assertEquals(1,boot.getBeansOfType(unichope.tutor.http.TutorExceptionHandler.class).size());
            var bootMvc=MockMvcBuilders.webAppContextSetup((org.springframework.web.context.WebApplicationContext)boot).apply(springSecurity()).build();
            var positive=bootMvc.perform(get("/api/tutor/modules").session(session(T1)).accept("application/json")).andReturn();
            assertEquals(200,positive.getResponse().getStatus());assertEquals(2,body(positive).size());
            assertEquals("no-store",positive.getResponse().getHeader("Cache-Control"));
            error(bootMvc.perform(get("/api/tutor/modules").session(session(T1)).accept("text/html")).andReturn(),406,"NOT_ACCEPTABLE");
            for(String accept:List.of("application/json","text/html"))error(bootMvc.perform(get("/api/tutor/unknown").session(session(T1)).accept(accept)).andReturn(),404,"NOT_FOUND");
        }
    }

    @Test
    void rt18ControllerErrorsWithHtmlAcceptKeepValidationRoleOwnerAndStateCodes() throws Exception {
        error(okRaw(get("/api/tutor/modules").param("unknown","x").accept("text/html"),session(T1)),400,"VALIDATION_FAILED");
        error(okRaw(get("/api/tutor/modules").accept("text/html"),session(S1)),403,"FORBIDDEN");
        UUID foreign=slot(T2,START,END,"AVAILABLE");
        error(okRaw(get("/api/tutor/slots/"+foreign).accept("text/html"),session(T1)),404,"NOT_FOUND");
        UUID booked=slot(T1,START,END,"BOOKED");String before=snapshot();
        error(okRaw(post("/api/tutor/slots/"+booked+"/cancel").contentType("application/json").content("{}").accept("text/html").with(csrf().asHeader()),session(T1)),409,"INVALID_STATE");
        assertEquals(before,snapshot());
    }

    @Test
    void rt18InactiveHtmlErrorStillInvalidatesOriginalSession() throws Exception {
        MockHttpSession original=session(T2);
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id",T2).update();
        error(okRaw(get("/api/tutor/modules").accept("text/html"),original),403,"ACCOUNT_INACTIVE");
        assertTrue(original.isInvalid());
        error(okRaw(get("/api/tutor/modules").accept("text/html"),original),401,"UNAUTHENTICATED");
    }

    @Test
    void rt18PostcommitHtmlErrorKeeps500AndCommittedMutationExactlyOnce() throws Exception {
        modules.fail=true;
        error(okRaw(post("/api/tutor/slots").contentType("application/json").content(createBody(START,END)).accept("text/html").with(csrf().asHeader()),session(T1)),500,"INTERNAL_ERROR");
        assertEquals(1,count("slots"));modules.fail=false;
        assertEquals(1,body(ok(get("/api/tutor/slots"),session(T1),200)).size());
    }

    private void registerDependencies(org.springframework.beans.factory.config.ConfigurableListableBeanFactory factory) {
        factory.registerSingleton("tutorReads",new PostgresTutorReadRepository(new PostgresUserRepository(db),db));
        factory.registerSingleton("bookingReads",new PostgresBookingRepository(db));
        factory.registerSingleton("slotReads",new PostgresSlotRepository(db));
        factory.registerSingleton("dataSource",source);
        factory.registerSingleton("moduleReads",modules);factory.registerSingleton("consultationLifecycle",writes);
        factory.registerSingleton("clock",clock);
        factory.registerSingleton("currentActorProvider",(CurrentActorProvider)()->{
            var auth=SecurityContextHolder.getContext().getAuthentication();
            return auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof UUID id?Optional.of(id):Optional.empty();
        });
    }

    @Test
    void noteGetAndPutPersistTrimAndIncrementVersion() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        var empty=body(ok(get("/api/tutor/bookings/"+booking+"/note"),session(T1),200));
        assertEquals(Set.of("bookingId","content","version","updatedAt"),Set.copyOf(empty.propertyNames()));
        assertEquals(booking.toString(),empty.get("bookingId").asString());assertTrue(empty.get("content").isNull());
        assertEquals(0,empty.get("version").asInt());assertTrue(empty.get("updatedAt").isNull());assertEquals(0,count("private_notes"));
        var first=body(ok(put("/api/tutor/bookings/"+booking+"/note").contentType("application/json")
                .content("{\"content\":\"  Review arrays 😀  \",\"expectedVersion\":0}").with(csrf().asHeader()),session(T1),200));
        assertEquals("Review arrays 😀",first.get("content").asString());assertEquals(1,first.get("version").asInt());
        assertFalse(first.get("updatedAt").isNull());assertEquals(1,count("private_notes"));
        var loaded=body(ok(get("/api/tutor/bookings/"+booking+"/note"),session(T1),200));assertEquals(first,loaded);
        var second=body(ok(put("/api/tutor/bookings/"+booking+"/note").contentType("application/json")
                .content("{\"content\":\"Follow up\",\"expectedVersion\":1}").with(csrf().asHeader()),session(T1),200));
        assertEquals("Follow up",second.get("content").asString());assertEquals(2,second.get("version").asInt());assertEquals(1,count("private_notes"));
    }

    @Test
    void noteRoutesDenyForeignAndIncompleteBookingsWithoutReadingOrWritingNote() throws Exception {
        UUID foreignSlot=slot(T2,START,END,"COMPLETED"),foreignBooking=booking(S1,foreignSlot,"COMPLETED");
        UUID activeSlot=slot(T1,END,END.plusSeconds(1800),"BOOKED"),activeBooking=booking(S1,activeSlot,"ACTIVE");
        for(UUID denied:List.of(foreignBooking,id(999))) {
            error(okRaw(get("/api/tutor/bookings/"+denied+"/note"),session(T1)),404,"NOT_FOUND");
            error(okRaw(put("/api/tutor/bookings/"+denied+"/note").contentType("application/json")
                    .content("{\"content\":\"secret\",\"expectedVersion\":0}").with(csrf().asHeader()),session(T1)),404,"NOT_FOUND");
        }
        error(okRaw(get("/api/tutor/bookings/"+activeBooking+"/note"),session(T1)),409,"INVALID_STATE");
        error(okRaw(put("/api/tutor/bookings/"+activeBooking+"/note").contentType("application/json")
                .content("{\"content\":\"secret\",\"expectedVersion\":0}").with(csrf().asHeader()),session(T1)),409,"INVALID_STATE");
        assertEquals(0,count("private_notes"));
    }

    @Test
    void noteSaveRejectsInvalidAndStaleBodiesWithoutChangingLatestValue() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        for(String bad:List.of("{}","{\"content\":\"\",\"expectedVersion\":0}",
                "{\"content\":\"secret\",\"expectedVersion\":-1}",
                "{\"content\":\"secret\",\"expectedVersion\":0,\"tutorId\":\""+T2+"\"}")) {
            error(okRaw(put("/api/tutor/bookings/"+booking+"/note").contentType("application/json").content(bad).with(csrf().asHeader()),session(T1)),
                    400,"VALIDATION_FAILED");
        }
        ok(put("/api/tutor/bookings/"+booking+"/note").contentType("application/json")
                .content("{\"content\":\"latest\",\"expectedVersion\":0}").with(csrf().asHeader()),session(T1),200);
        error(okRaw(put("/api/tutor/bookings/"+booking+"/note").contentType("application/json")
                .content("{\"content\":\"stale\",\"expectedVersion\":0}").with(csrf().asHeader()),session(T1)),409,"NOTE_CONFLICT");
        var saved=body(ok(get("/api/tutor/bookings/"+booking+"/note"),session(T1),200));
        assertEquals("latest",saved.get("content").asString());assertEquals(1,saved.get("version").asInt());
    }

    @Test
    void noteRoutesRequireTutorRoleAndInvalidateInactiveTutorSession() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        error(okRaw(get("/api/tutor/bookings/"+booking+"/note"),session(S1,"ROLE_TUTOR")),403,"FORBIDDEN");
        MockHttpSession inactive=session(T1);
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
        error(okRaw(get("/api/tutor/bookings/"+booking+"/note"),inactive),403,"ACCOUNT_INACTIVE");
        assertTrue(inactive.isInvalid());assertEquals(0,count("private_notes"));
    }

    @Test
    void concurrentFirstSavesSerializeOnTheBookingAndOnlyOneVersionIsCreated() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        try(var blocker=bookingHolder(booking)) {
            var first=launch(()->okRaw(notePut(booking,"first",0),session(T1)).getResponse().getStatus());
            awaitBlocked(1);
            var second=launch(()->okRaw(notePut(booking,"second",0),session(T1)).getResponse().getStatus());
            awaitBlocked(2);
            blocker.commit();
            List<Object> results=List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(status->status.equals(200)).count());
            assertEquals(1,results.stream().filter(status->status.equals(409)).count());
        }
        assertEquals(1,count("private_notes"));assertEquals(1,db.sql("SELECT version FROM private_notes").query(Long.class).single());
    }

    @Test
    void concurrentUpdatesWithSameExpectedVersionDoNotLoseAnUpdate() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        ok(notePut(booking,"original",0),session(T1),200);
        try(var blocker=bookingHolder(booking)) {
            var first=launch(()->okRaw(notePut(booking,"first",1),session(T1)).getResponse().getStatus());
            awaitBlocked(1);
            var second=launch(()->okRaw(notePut(booking,"second",1),session(T1)).getResponse().getStatus());
            awaitBlocked(2);
            blocker.commit();
            List<Object> results=List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(status->status.equals(200)).count());
            assertEquals(1,results.stream().filter(status->status.equals(409)).count());
        }
        assertEquals(1,count("private_notes"));assertEquals(2,db.sql("SELECT version FROM private_notes").query(Long.class).single());
    }

    @Test
    void noteWriteFailureRollsBackContentVersionAndTimestamp() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        ok(notePut(booking,"before",0),session(T1),200);
        String before=db.sql("SELECT row_to_json(n)::text FROM private_notes n WHERE booking_id=:id").param("id",booking).query(String.class).single();
        db.sql("""
                CREATE FUNCTION reject_note_update() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN IF NEW.version=2 THEN RAISE EXCEPTION 'synthetic note rollback'; END IF; RETURN NEW; END $$
                """).update();
        db.sql("CREATE TRIGGER reject_note_update BEFORE UPDATE ON private_notes FOR EACH ROW EXECUTE FUNCTION reject_note_update()").update();
        error(okRaw(notePut(booking,"after",1),session(T1)),500,"INTERNAL_ERROR");
        String after=db.sql("SELECT row_to_json(n)::text FROM private_notes n WHERE booking_id=:id").param("id",booking).query(String.class).single();
        assertEquals(before,after);
    }

    @Test
    void tutorReviewSchemaEnforcesUniqueNonblankVersionAndBookingReference() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        UUID blankBooking=booking(S1,slot(T1,END,END.plusSeconds(1800),"COMPLETED"),"COMPLETED");
        UUID versionBooking=booking(S1,slot(T1,END.plusSeconds(1800),END.plusSeconds(3600),"COMPLETED"),"COMPLETED");
        UUID unicodeBlankBooking=booking(S1,slot(T1,END.plusSeconds(3600),END.plusSeconds(5400),"COMPLETED"),"COMPLETED");
        UUID nbspBooking=booking(S1,slot(T1,END.plusSeconds(5400),END.plusSeconds(7200),"COMPLETED"),"COMPLETED");
        db.sql("INSERT INTO private_notes VALUES (:id,'valid',1,:at)").param("id",booking).param("at",N.atOffset(java.time.ZoneOffset.UTC)).update();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.sql("INSERT INTO private_notes VALUES (:id,'duplicate',1,:at)")
                .param("id",booking).param("at",N.atOffset(java.time.ZoneOffset.UTC)).update());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.sql("INSERT INTO private_notes VALUES (:id,E'\\t\\n',1,:at)")
                .param("id",blankBooking).param("at",N.atOffset(java.time.ZoneOffset.UTC)).update());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.sql("INSERT INTO private_notes VALUES (:id,:content,1,:at)")
                .param("id",unicodeBlankBooking).param("content","\u2003\u3000").param("at",N.atOffset(java.time.ZoneOffset.UTC)).update());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.sql("INSERT INTO private_notes VALUES (:id,'bad version',0,:at)")
                .param("id",versionBooking).param("at",N.atOffset(java.time.ZoneOffset.UTC)).update());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.sql("INSERT INTO private_notes VALUES (:id,'orphan',1,:at)")
                .param("id",UUID.randomUUID()).param("at",N.atOffset(java.time.ZoneOffset.UTC)).update());
        db.sql("INSERT INTO private_notes VALUES (:id,:content,1,:at)").param("id",nbspBooking).param("content","\u00a0")
                .param("at",N.atOffset(java.time.ZoneOffset.UTC)).update();
        assertEquals(2,count("private_notes"));
    }

    @Test
    void deactivationWhileSaveWaitsIsRecheckedAfterTheLockAndInvalidatesSession() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        MockHttpSession actor=session(T1);
        try(Connection blocker=holder(T1)) {
            var save=launch(()->okRaw(notePut(booking,"should not persist",0),actor));
            awaitBlocked(1);
            on(blocker).sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
            blocker.commit();
            var response=(MvcResult)save.get(15,TimeUnit.SECONDS);
            error(response,403,"ACCOUNT_INACTIVE");
        }
        assertTrue(actor.isInvalid());assertEquals(0,count("private_notes"));
    }

    @Test
    void noteRowLockTimeoutMapsToSanitizedUnavailableAfterRollback() throws Exception {
        UUID slot=slot(T1,START,END,"COMPLETED"),booking=booking(S1,slot,"COMPLETED");
        ok(notePut(booking,"before",0),session(T1),200);
        String before=db.sql("SELECT row_to_json(n)::text FROM private_notes n WHERE booking_id=:id").param("id",booking).query(String.class).single();
        try(Connection blocker=source.getConnection()) {
            blocker.setAutoCommit(false);
            on(blocker).sql("SELECT booking_id FROM private_notes WHERE booking_id=:id FOR UPDATE").param("id",booking).query(UUID.class).single();
            var save=launch(()->okRaw(notePut(booking,"after",1),session(T1)));
            awaitBlocked(1);
            error((MvcResult)save.get(12,TimeUnit.SECONDS),503,"TEMPORARILY_UNAVAILABLE");
            blocker.rollback();
        }
        String after=db.sql("SELECT row_to_json(n)::text FROM private_notes n WHERE booking_id=:id").param("id",booking).query(String.class).single();
        assertEquals(before,after);
    }

    private MockHttpServletRequestBuilder notePut(UUID booking,String content,long version) {
        String escaped=content.replace("\\","\\\\").replace("\"","\\\"");
        return put("/api/tutor/bookings/"+booking+"/note").contentType("application/json")
                .content("{\"content\":\""+escaped+"\",\"expectedVersion\":"+version+"}").with(csrf().asHeader());
    }

    private Connection bookingHolder(UUID booking) throws Exception {
        Connection connection=source.getConnection();connection.setAutoCommit(false);
        try { on(connection).sql("SELECT id FROM bookings WHERE id=:id FOR UPDATE").param("id",booking).query(UUID.class).single();return connection; }
        catch(Exception error) { connection.close();throw error; }
    }

    private MvcResult call(int op,UUID target,MockHttpSession session,boolean token) throws Exception {
        var request=switch(op){case 0->get("/api/tutor/modules");case 1->get("/api/tutor/slots");case 2->get("/api/tutor/slots/"+target);case 3->post("/api/tutor/slots").contentType("application/json").content(createBody(END,END.plusSeconds(1800)));case 4->post("/api/tutor/slots/"+target+"/cancel").contentType("application/json").content("{}");default->throw new IllegalArgumentException();};
        if(op>=3&&token)request.with(csrf().asHeader());return okRaw(request,session);
    }
    private MockHttpSession session(UUID id) { return session(id,"ROLE_TUTOR"); }
    private MockHttpSession session(Object principal,String authority) {
        var session=new MockHttpSession();var security=SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,"synthetic",List.of(new SimpleGrantedAuthority(authority))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,security);return session;
    }
    private String createBody(Instant start,Instant end) { return "{\"moduleId\":\""+M+"\",\"startTime\":\""+start+"\",\"endTime\":\""+end+"\"}"; }
    private MvcResult okRaw(MockHttpServletRequestBuilder request,MockHttpSession session) throws Exception { if(session!=null)request.session(session);return mvc.perform(request).andReturn(); }
    private MvcResult ok(MockHttpServletRequestBuilder request,MockHttpSession session,int status) throws Exception {
        var result=okRaw(request,session);assertEquals(status,result.getResponse().getStatus(),result.getResponse().getContentAsString()+" resolved="+result.getResolvedException());assertEquals("no-store",result.getResponse().getHeader("Cache-Control"));return result;
    }
    private JsonNode body(MvcResult result) throws Exception {return json.readTree(result.getResponse().getContentAsString());}
    private JsonNode error(MvcResult result,int status,String code) throws Exception {
        assertEquals(status,result.getResponse().getStatus(),result.getResponse().getContentAsString()+" resolved="+result.getResolvedException());assertEquals("no-store",result.getResponse().getHeader("Cache-Control"));
        assertNotNull(result.getResponse().getContentType(),"Expected safe JSON content type");
        assertTrue(result.getResponse().getContentType().startsWith("application/json"));var body=body(result);
        assertEquals(Set.of("code","message","requestId"),Set.copyOf(body.propertyNames()));assertEquals(code,body.get("code").asString());
        UUID.fromString(body.get("requestId").asString());String text=body.toString();
        assertFalse(text.contains("private-marker"));assertFalse(text.contains(T1.toString()));assertFalse(text.contains("client-sensitive-marker"));return body;
    }
    private void exactSlot(JsonNode slot) { assertEquals(Set.of("id","moduleId","moduleCode","startTime","endTime","status"),Set.copyOf(slot.propertyNames())); }
    @Configuration @EnableWebMvc static class MvcFixture {}

    private static final class FaultingLifecycle implements ConsultationLifecycle {
        private final ConsultationLifecycle delegate;RuntimeException failure;Runnable beforeFailure=()->{};
        FaultingLifecycle(ConsultationLifecycle delegate){this.delegate=delegate;}
        private void check(){if(failure!=null){beforeFailure.run();throw failure;}}
        @Override public ConsultationSlot createSlot(UUID actor,UUID module,Instant start,Instant end){check();return delegate.createSlot(actor,module,start,end);}
        @Override public ConsultationSlot cancelAvailableSlot(UUID actor,UUID slot){check();return delegate.cancelAvailableSlot(actor,slot);}
        @Override public Booking bookAvailableSlot(UUID actor,UUID slot){return delegate.bookAvailableSlot(actor,slot);}
        @Override public Booking cancelActiveBooking(UUID actor,UUID booking){return delegate.cancelActiveBooking(actor,booking);}
        @Override public Booking completeActiveBooking(UUID actor,UUID booking){return delegate.completeActiveBooking(actor,booking);}
        @Override public LockedCompletedBooking lockForPrivateNote(UUID tutor,UUID booking){return delegate.lockForPrivateNote(tutor,booking);}
    }
    private static final class ProjectionModules implements ModuleRepository {
        private final ModuleRepository delegate;boolean fail;
        ProjectionModules(ModuleRepository delegate){this.delegate=delegate;}
        @Override public Optional<Module> findById(UUID id){if(fail)throw new IllegalStateException("SELECT private-marker");return delegate.findById(id);}
        @Override public Optional<Module> findByCode(String code){return delegate.findByCode(code);}
        @Override public List<Module> findAll(){return delegate.findAll();}
        @Override public List<TutorModule> findAssignmentsByTutor(UUID tutor){return delegate.findAssignmentsByTutor(tutor);}
        @Override public List<TutorModule> findAssignments(){return delegate.findAssignments();}
    }
}

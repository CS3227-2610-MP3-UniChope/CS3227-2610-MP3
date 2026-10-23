package unichope.tutor.http;

import java.time.Clock;
import java.util.List;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import unichope.platform.web.ApiErrors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.repository.ModuleRepository;
import unichope.platform.core.repository.BookingRepository;
import unichope.platform.core.repository.SlotRepository;
import unichope.platform.core.repository.UserRepository;
import unichope.platform.auth.CurrentActorProvider;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.ai.AiGateway;
import unichope.platform.ai.AdmittedAiGateway;
import unichope.tutor.TutorNoteDraftGateway;
import unichope.tutor.TutorNoteDraftService;
import unichope.tutor.TutorAccess;
import unichope.tutor.TutorReadRepository;
import unichope.tutor.TutorReadService;
import unichope.tutor.TutorNoteService;
import unichope.tutor.PostgresTutorNoteRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import javax.sql.DataSource;

@Configuration
@Profile("tutor-api")
@Import({TutorController.class,TutorExceptionHandler.class})
public class TutorApiConfiguration implements WebMvcConfigurer {
    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        // Framework failures may have no selected controller or an incompatible
        // Accept header. Write safe error JSON directly; leave other role paths untouched.
        resolvers.add(0,(request,response,handler,failure)->{
            String path=request.getRequestURI().substring(request.getContextPath().length());
            if(!path.startsWith("/api/tutor/"))return null;
            int status;String code,message;
            if(failure instanceof HttpRequestMethodNotSupportedException method) {
                status=405;code="METHOD_NOT_ALLOWED";message="Request method is not allowed";
                if(method.getSupportedMethods()!=null)response.setHeader("Allow",String.join(", ",method.getSupportedMethods()));
            } else if(failure instanceof HttpMediaTypeNotSupportedException) {
                status=415;code="UNSUPPORTED_MEDIA_TYPE";message="JSON content type is required";
            } else if(failure instanceof HttpMediaTypeNotAcceptableException) {
                status=406;code="NOT_ACCEPTABLE";message="A JSON response is required";
            } else if(failure instanceof NoHandlerFoundException || failure instanceof NoResourceFoundException) {
                status=404;code="NOT_FOUND";message="Resource not found";
            } else return null;
            try { ApiErrors.write(request,response,status,code,message); }
            catch(java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            return new ModelAndView();
        });
    }

    @Bean TutorAccess tutorAccess(CurrentActorProvider actor,TutorReadRepository repository) { return new TutorAccess(actor,repository); }
    @Bean TutorReadService tutorReadService(CurrentActorProvider actor,TutorReadRepository repository,Clock clock) { return new TutorReadService(actor,repository,clock); }
    @Bean TutorHttpService tutorHttpService(TutorAccess access,ConsultationLifecycle lifecycle,ModuleRepository modules,
            SlotRepository slots,UserRepository users) { return new TutorHttpService(access,lifecycle,modules,slots,users); }
    @Bean TutorNoteService tutorNoteService(TutorAccess access,BookingRepository bookings,SlotRepository slots,
            ConsultationLifecycle lifecycle,DataSource source) {
        return new TutorNoteService(access,bookings,slots,new PostgresTutorNoteRepository(JdbcClient.create(source)),lifecycle,source);
    }

    @Bean @ConditionalOnBean(AiGateway.class)
    @ConditionalOnProperty(prefix="unichope.ai.tutor.note-draft",name="max-output-tokens")
    TutorNoteDraftGateway tutorNoteDraftGateway(AiGateway gateway, Environment environment,
            @Value("${unichope.ai.tutor.note-draft.max-output-tokens}") int maxOutputTokens) {
        boolean local = environment.matchesProfiles("local");
        AiGateway fakeGuard = new AiGateway() {
            @Override
            public Result generate(Request request) {
                return generate(request, ignored -> {});
            }

            @Override
            public Result generate(Request request, ResultValidator validator) {
                if (!local && !(gateway instanceof AdmittedAiGateway)) {
                    throw new Failure(FailureCode.UNAVAILABLE);
                }
                return gateway.generate(request, result -> {
                    validator.validate(result);
                    if (result.providerMode() == ProviderMode.FAKE && !local) {
                        throw new Failure(FailureCode.UNAVAILABLE);
                    }
                });
            }
        };
        return new TutorNoteDraftGateway(fakeGuard,maxOutputTokens);
    }

    @Bean @ConditionalOnBean(TutorNoteDraftGateway.class)
    TutorNoteDraftService tutorNoteDraftService(TutorAccess access,BookingRepository bookings,
            SlotRepository slots,AuthenticationService authentication,TutorNoteDraftGateway drafts) {
        return new TutorNoteDraftService(access,bookings,slots,authentication,drafts);
    }
}

package unichope.student.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import unichope.platform.auth.CurrentActorProvider;
import unichope.platform.web.ApiErrors;
import unichope.student.PostgresStudentReadRepository;
import unichope.student.StudentAccess;
import unichope.student.StudentReadRepository;
import unichope.student.StudentReadService;

/**
 * Student HTTP wiring, active only with profile {@code student-api} (TECH §2). Missing
 * {@link CurrentActorProvider}, {@link DataSource} or {@link Clock} beans fail startup; there is
 * no anonymous, fallback or main-source test actor.
 */
@Configuration
@Profile("student-api")
@Import({StudentController.class, StudentExceptionHandler.class})
public class StudentApiConfiguration implements WebMvcConfigurer {
    private static final String STUDENT_PATH = "/api/student/";

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        // Framework failures may have no selected controller or an incompatible Accept header.
        // Write safe error JSON directly for Student paths only; other role paths are untouched.
        resolvers.add(0, (request, response, handler, failure) -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (!path.startsWith(STUDENT_PATH)) return null;
            int status;
            String code;
            String message;
            if (failure instanceof HttpRequestMethodNotSupportedException method) {
                status = 405; code = "METHOD_NOT_ALLOWED"; message = "Request method is not allowed";
                if (method.getSupportedMethods() != null) response.setHeader("Allow", String.join(", ", method.getSupportedMethods()));
            } else if (failure instanceof HttpMediaTypeNotSupportedException) {
                status = 415; code = "UNSUPPORTED_MEDIA_TYPE"; message = "JSON content type is required";
            } else if (failure instanceof HttpMediaTypeNotAcceptableException) {
                status = 406; code = "NOT_ACCEPTABLE"; message = "A JSON response is required";
            } else if (failure instanceof NoHandlerFoundException || failure instanceof NoResourceFoundException) {
                status = 404; code = "NOT_FOUND"; message = "Resource not found";
            } else {
                return null;
            }
            try {
                ApiErrors.write(request, response, status, code, message);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
            return new ModelAndView();
        });
    }

    @Bean
    StudentReadRepository studentReadRepository(DataSource source) {
        return new PostgresStudentReadRepository(JdbcClient.create(source));
    }

    @Bean
    StudentAccess studentAccess(CurrentActorProvider actor, StudentReadRepository studentReadRepository) {
        return new StudentAccess(actor, studentReadRepository);
    }

    @Bean
    StudentReadService studentReadService(StudentAccess studentAccess, StudentReadRepository studentReadRepository, Clock clock) {
        return new StudentReadService(studentAccess, studentReadRepository, clock);
    }
}

package unichope.tutor.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.web.ApiErrors;
import unichope.platform.web.ApiFailure;
import unichope.tutor.TutorAccessDeniedException;
import unichope.tutor.TutorNoteFailure;

@RestControllerAdvice(assignableTypes=TutorController.class)
@Profile("tutor-api")
public class TutorExceptionHandler {
    @ExceptionHandler(Exception.class)
    public void handle(Exception failure,HttpServletRequest request,HttpServletResponse response) throws Exception {
        int status=500;String code="INTERNAL_ERROR",message="The request could not be completed";
        if(failure instanceof ApiFailure api) { status=api.status();code=api.code();message=api.getMessage(); }
        else if(failure instanceof TutorNoteFailure note) {
            switch(note.code()) {
                case INVALID_CONTENT -> {status=400;code="VALIDATION_FAILED";message="Request validation failed";}
                case NOTE_CONFLICT -> {status=409;code="NOTE_CONFLICT";message="The note changed; reload it before saving";}
                case TEMPORARILY_UNAVAILABLE -> {status=503;code="TEMPORARILY_UNAVAILABLE";message="The operation is temporarily unavailable";}
            }
        }
        else if(failure instanceof TutorAccessDeniedException access) {
            switch(access.reason()) {
                case UNAUTHENTICATED -> {status=401;code="UNAUTHENTICATED";message="Authentication is required";invalidate(request);}
                case INACTIVE -> {status=403;code="ACCOUNT_INACTIVE";message="Account is inactive";invalidate(request);}
                case FORBIDDEN -> {status=403;code="FORBIDDEN";message="Access is forbidden";}
            }
        } else if(failure instanceof LifecycleFailure lifecycle) {
            switch(lifecycle.code()) {
                case INVALID_INPUT,INVALID_TIME -> {status=400;code="VALIDATION_FAILED";message="Request validation failed";}
                case NOT_FOUND -> {status=404;code="NOT_FOUND";message="Consultation resource not found";}
                case TEMPORARILY_UNAVAILABLE -> {status=503;code="TEMPORARILY_UNAVAILABLE";message="The operation is temporarily unavailable";}
                case ACCESS_DENIED -> {status=403;code="FORBIDDEN";message="Access is forbidden";}
                default -> {status=409;code=lifecycle.code().name();message=lifecycle.getMessage();}
            }
        } else if(failure instanceof HttpMediaTypeNotSupportedException) {status=415;code="UNSUPPORTED_MEDIA_TYPE";message="JSON content type is required";}
        else if(failure instanceof HttpRequestMethodNotSupportedException) {status=405;code="METHOD_NOT_ALLOWED";message="Request method is not allowed";}
        // Errors have a fixed JSON contract even when Accept excludes successful JSON.
        // A committed transport response cannot be replaced with a new safe status/body.
        if(response.isCommitted())throw failure;
        ApiErrors.write(request,response,status,code,message);
    }
    private static void invalidate(HttpServletRequest request) {
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
        var session=request.getSession(false);if(session!=null)session.invalidate();
    }
}

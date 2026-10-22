package unichope.platform.web;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Shared fixed error shape for servlet security and MVC adapters. */
public final class ApiErrors {
    public static final String REQUEST_ID=ApiErrors.class.getName()+".requestId";
    private static final JsonMapper JSON=JsonMapper.shared();
    private ApiErrors() {}
    public record Error(String code, String message, String requestId) {}
    public static Error error(HttpServletRequest request, String code, String message) {
        Object id=request.getAttribute(REQUEST_ID);
        if(id==null) { id=UUID.randomUUID().toString();request.setAttribute(REQUEST_ID,id); }
        return new Error(code,message,id.toString());
    }
    public static void write(HttpServletRequest request,HttpServletResponse response,int status,String code,String message) throws IOException {
        response.setStatus(status);response.setHeader("Cache-Control","no-store");response.setContentType("application/json");
        response.getWriter().write(JSON.writeValueAsString(error(request,code,message)));
    }
}

package unichope.tutor.http;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.web.ApiFailure;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;

/** Adapter-local strict parsing, bounded by actual bytes rather than declared length. */
public final class TutorApiInput {
    private static final int MAX_BYTES=65536;
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private TutorApiInput() {}
    public record Create(UUID moduleId,Instant startTime,Instant endTime) {}
    public record NoteSave(String content,long expectedVersion) {}
    public record NoteDraft(String roughNotes) {}
    public record BookingFilter(Optional<UUID> moduleId, Optional<LocalDate> date,
                                Optional<BookingStatus> status) {}
    public record HistoryFilter(SlotStatus status, Optional<LocalDate> date) {}

    public static Create create(HttpServletRequest request) throws IOException {
        JsonNode object=object(request);
        if(!Set.copyOf(object.propertyNames()).equals(Set.of("moduleId","startTime","endTime")))throw invalid();
        for(String field:object.propertyNames())if(!object.get(field).isString())throw invalid();
        return new Create(id(object.get("moduleId").stringValue()),instant(object.get("startTime").stringValue()),instant(object.get("endTime").stringValue()));
    }
    public static void emptyObject(HttpServletRequest request) throws IOException { if(!object(request).isEmpty())throw invalid(); }
    public static NoteSave noteSave(HttpServletRequest request) throws IOException {
        JsonNode body=object(request);
        if(!Set.copyOf(body.propertyNames()).equals(Set.of("content","expectedVersion")))throw invalid();
        JsonNode content=body.get("content"),version=body.get("expectedVersion");
        if(content==null||!content.isString()||version==null||!version.isIntegralNumber())throw invalid();
        try { return new NoteSave(content.stringValue(),version.bigIntegerValue().longValueExact()); }
        catch(ArithmeticException error) { throw invalid(); }
    }
    public static NoteDraft noteDraft(HttpServletRequest request) throws IOException {
        JsonNode body=object(request);
        if(!Set.copyOf(body.propertyNames()).equals(Set.of("roughNotes")))throw invalid();
        JsonNode roughNotes=body.get("roughNotes");
        if(roughNotes==null||!roughNotes.isString())throw invalid();
        String value=roughNotes.stringValue();
        if(value.strip().isEmpty()||value.codePointCount(0,value.length())>8000)throw invalid();
        return new NoteDraft(value);
    }
    public static Optional<LocalDate> date(HttpServletRequest request) {
        if(request.getParameterMap().isEmpty())return Optional.empty();
        if(request.getParameterMap().size()!=1||!request.getParameterMap().containsKey("date")||request.getParameterValues("date").length!=1)throw invalid();
        return Optional.of(parseDate(request.getParameter("date")));
    }
    public static BookingFilter bookingsQuery(HttpServletRequest request) {
        var parameters = request.getParameterMap();
        if (parameters.keySet().stream().anyMatch(key -> !Set.of("moduleId", "date", "status").contains(key))) throw invalid();
        for (String key : parameters.keySet()) if (request.getParameterValues(key).length != 1) throw invalid();
        Optional<UUID> module = Optional.ofNullable(request.getParameter("moduleId")).map(TutorApiInput::id);
        Optional<LocalDate> date = Optional.empty();
        if (parameters.containsKey("date")) {
            date = Optional.of(parseDate(request.getParameter("date")));
        }
        Optional<BookingStatus> status = Optional.empty();
        if (parameters.containsKey("status")) {
            try { status = Optional.of(BookingStatus.valueOf(request.getParameter("status"))); }
            catch (IllegalArgumentException error) { throw invalid(); }
        }
        return new BookingFilter(module, date, status);
    }
    public static HistoryFilter historyQuery(HttpServletRequest request) {
        var parameters=request.getParameterMap();
        if(parameters.keySet().stream().anyMatch(key->!Set.of("status","date").contains(key)))throw invalid();
        for(String key:parameters.keySet())if(request.getParameterValues(key).length!=1)throw invalid();
        if(!parameters.containsKey("status"))throw invalid();
        SlotStatus status;
        try { status=SlotStatus.valueOf(request.getParameter("status")); }
        catch(IllegalArgumentException error) { throw invalid(); }
        if(status!=SlotStatus.CANCELLED&&status!=SlotStatus.COMPLETED)throw invalid();
        Optional<LocalDate> date=parameters.containsKey("date")?Optional.of(parseDate(request.getParameter("date"))):Optional.empty();
        return new HistoryFilter(status,date);
    }
    public static void noQuery(HttpServletRequest request) { if(!request.getParameterMap().isEmpty())throw invalid(); }
    public static UUID id(String value) {
        if(value==null||!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw invalid();
        return UUID.fromString(value);
    }
    private static LocalDate parseDate(String raw) {
        if(!raw.matches("\\d{4}-\\d{2}-\\d{2}"))throw invalid();
        try { return LocalDate.parse(raw); } catch(DateTimeParseException error) { throw invalid(); }
    }
    private static Instant instant(String value) {
        if(!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z"))throw invalid();
        try { return Instant.parse(value); } catch(DateTimeParseException error) { throw invalid(); }
    }
    private static JsonNode object(HttpServletRequest request) throws IOException {
        try {
            MediaType type=request.getContentType()==null?null:MediaType.parseMediaType(request.getContentType());
            if(type==null||!type.getType().equals("application")||!type.getSubtype().equals("json"))throw new ApiFailure(415,"UNSUPPORTED_MEDIA_TYPE","JSON content type is required");
        } catch(org.springframework.http.InvalidMediaTypeException error) { throw new ApiFailure(415,"UNSUPPORTED_MEDIA_TYPE","JSON content type is required"); }
        byte[] bytes=request.getInputStream().readNBytes(MAX_BYTES+1);
        if(bytes.length>MAX_BYTES)throw new ApiFailure(413,"PAYLOAD_TOO_LARGE","Request body is too large");
        try {
            JsonNode body=JSON.readTree(bytes);
            if(body==null||!body.isObject())throw invalid();
            return body;
        } catch(JacksonException error) { throw invalid(); }
    }
    private static ApiFailure invalid() { return new ApiFailure(400,"VALIDATION_FAILED","Request validation failed"); }
}

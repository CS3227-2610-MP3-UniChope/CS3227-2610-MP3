package unichope.tutor.http;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.web.ApiFailure;

/** Adapter-local strict parsing, bounded by actual bytes rather than declared length. */
public final class TutorApiInput {
    private static final int MAX_BYTES=65536;
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private TutorApiInput() {}
    public record Create(UUID moduleId,Instant startTime,Instant endTime) {}
    public record NoteSave(String content,long expectedVersion) {}
    public record BookingFilters(Optional<UUID> moduleId, Optional<LocalDate> date, Optional<BookingStatus> status) {}
    public record HistoryFilters(SlotStatus status, Optional<LocalDate> date) {}

    public static Create create(HttpServletRequest request) throws IOException {
        JsonNode object=object(request);
        if(!Set.copyOf(object.propertyNames()).equals(Set.of("moduleId","startTime","endTime")))throw invalid();
        for(String field:object.propertyNames())if(!object.get(field).isString())throw invalid();
        return new Create(id(object.get("moduleId").stringValue()),instant(object.get("startTime").stringValue()),instant(object.get("endTime").stringValue()));
    }
    public static void cancel(HttpServletRequest request) throws IOException { if(!object(request).isEmpty())throw invalid(); }
    public static NoteSave noteSave(HttpServletRequest request) throws IOException {
        JsonNode body=object(request);
        if(!Set.copyOf(body.propertyNames()).equals(Set.of("content","expectedVersion")))throw invalid();
        JsonNode content=body.get("content"),version=body.get("expectedVersion");
        if(content==null||!content.isString()||version==null||!version.isIntegralNumber())throw invalid();
        try { return new NoteSave(content.stringValue(),version.bigIntegerValue().longValueExact()); }
        catch(ArithmeticException error) { throw invalid(); }
    }
    public static Optional<LocalDate> date(HttpServletRequest request) {
        if(request.getParameterMap().isEmpty())return Optional.empty();
        if(request.getParameterMap().size()!=1||!request.getParameterMap().containsKey("date")||request.getParameterValues("date").length!=1)throw invalid();
        String raw=request.getParameter("date");
        if(!raw.matches("\\d{4}-\\d{2}-\\d{2}"))throw invalid();
        try { return Optional.of(LocalDate.parse(raw)); } catch(DateTimeParseException error) { throw invalid(); }
    }
    public static BookingFilters bookingFilters(HttpServletRequest request) {
        var params=request.getParameterMap();
        if(!Set.of("moduleId","date","status").containsAll(params.keySet()))throw invalid();
        Optional<UUID> module=one(params,"moduleId").map(TutorApiInput::id);
        Optional<LocalDate> date=one(params,"date").map(TutorApiInput::localDate);
        Optional<BookingStatus> status=one(params,"status").map(value->enumValue(BookingStatus.class,value));
        return new BookingFilters(module,date,status);
    }
    public static HistoryFilters historyFilters(HttpServletRequest request) {
        var params=request.getParameterMap();
        if(!Set.of("status","date").containsAll(params.keySet()))throw invalid();
        String rawStatus=one(params,"status").orElseThrow(TutorApiInput::invalid);
        SlotStatus status=enumValue(SlotStatus.class,rawStatus);
        if(status!=SlotStatus.CANCELLED&&status!=SlotStatus.COMPLETED)throw invalid();
        return new HistoryFilters(status,one(params,"date").map(TutorApiInput::localDate));
    }
    public static void noQuery(HttpServletRequest request) { if(!request.getParameterMap().isEmpty())throw invalid(); }
    public static UUID id(String value) {
        if(value==null||!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw invalid();
        return UUID.fromString(value);
    }
    private static Instant instant(String value) {
        if(!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z"))throw invalid();
        try { return Instant.parse(value); } catch(DateTimeParseException error) { throw invalid(); }
    }
    private static Optional<String> one(java.util.Map<String,String[]> params,String key) {
        if(!params.containsKey(key))return Optional.empty();
        String[] values=params.get(key);
        if(values==null||values.length!=1||values[0]==null||values[0].isBlank())throw invalid();
        return Optional.of(values[0]);
    }
    private static LocalDate localDate(String raw) {
        if(!raw.matches("\\d{4}-\\d{2}-\\d{2}"))throw invalid();
        try { return LocalDate.parse(raw); } catch(DateTimeParseException error) { throw invalid(); }
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type,String raw) {
        try { return Enum.valueOf(type,raw); } catch(IllegalArgumentException error) { throw invalid(); }
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

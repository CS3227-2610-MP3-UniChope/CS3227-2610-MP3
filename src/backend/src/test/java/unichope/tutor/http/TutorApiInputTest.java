package unichope.tutor.http;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import unichope.platform.web.ApiFailure;

class TutorApiInputTest {
    private final String module=UUID.randomUUID().toString();
    private final String valid="{\"moduleId\":\""+module+"\",\"startTime\":\"2026-10-21T01:00:00Z\",\"endTime\":\"2026-10-21T02:00:00Z\"}";

    @Test
    void rt12StrictObjectStringFieldsNoCoercionUnknownDuplicateOrTrailingTokens() throws Exception {
        assertEquals(UUID.fromString(module),TutorApiInput.create(request(valid)).moduleId());
        for(String body:new String[]{"", "null", "[]", "1", "true", "{}", "{", valid+" {}", valid.replace("\"moduleId\":\""+module+"\"","\"moduleId\":1"), valid.replace("\"startTime\":\"2026-10-21T01:00:00Z\"","\"startTime\":null"), valid.replace("\"endTime\":\"2026-10-21T02:00:00Z\"","\"endTime\":false"), valid.replace("}",",\"actorId\":\""+module+"\"}"), valid.replace("}",",\"moduleId\":\""+module+"\"}")}) {
            assertEquals(400,assertThrows(ApiFailure.class,()->TutorApiInput.create(request(body))).status());
        }
        for(String field:new String[]{"moduleId","startTime","endTime"}) for(String value:new String[]{"1","false","{}","[]","null"}) {
            var tree=tools.jackson.databind.json.JsonMapper.shared().readTree(valid);
            ((tools.jackson.databind.node.ObjectNode)tree).set(field,tools.jackson.databind.json.JsonMapper.shared().readTree(value));
            assertThrows(ApiFailure.class,()->TutorApiInput.create(request(tree.toString())));
        }
    }

    @Test
    void rt11QueriesPathsAndDatesAreStrictAndNeverFirstValueWins() {
        for(String date:new String[]{"","2026-2-01","2026-02-30","2026-10-21T00:00:00Z"," 2026-10-21"}) {
            var request=request("");request.addParameter("date",date);
            assertThrows(ApiFailure.class,()->TutorApiInput.date(request));
        }
        var repeated=request(""); repeated.addParameter("date","2026-10-21","2026-10-21");
        assertThrows(ApiFailure.class,()->TutorApiInput.date(repeated));
        var unknown=request(""); unknown.addParameter("actorId",module);
        assertThrows(ApiFailure.class,()->TutorApiInput.noQuery(unknown));
        for(String id:new String[]{"", "not-a-uuid", "1-1-1-1-1", module+" "}) assertThrows(ApiFailure.class,()->TutorApiInput.id(id));
    }

    @Test
    void completedBookingQueryRequiresOneExactStatus() {
        var validQuery=request("");validQuery.addParameter("status","COMPLETED");
        assertEquals(unichope.platform.core.model.consultation.BookingStatus.COMPLETED,
                TutorApiInput.bookingsQuery(validQuery).status().orElseThrow());
        var invalidStatus=request("");invalidStatus.addParameter("status","DONE");
        assertThrows(ApiFailure.class,()->TutorApiInput.bookingsQuery(invalidStatus));
        var repeated=request("");repeated.addParameter("status","COMPLETED","COMPLETED");
        assertThrows(ApiFailure.class,()->TutorApiInput.bookingsQuery(repeated));
        var unknown=request("");unknown.addParameter("actorId",module);
        assertThrows(ApiFailure.class,()->TutorApiInput.bookingsQuery(unknown));
    }

    @Test
    void rt13UtcZOnlyAndExactlyEmptyCancellationObject() throws Exception {
        assertEquals(Instant.parse("2026-10-21T01:00:00.123Z"),TutorApiInput.create(request(valid.replace("01:00:00Z","01:00:00.123Z"))).startTime());
        for(String time:new String[]{"2026-10-21T01:00:00+00:00","2026-10-21T09:00:00+08:00","2026-10-21T01:00:00","2026-02-30T01:00:00Z"}) assertThrows(ApiFailure.class,()->TutorApiInput.create(request(valid.replace("2026-10-21T01:00:00Z",time))));
        TutorApiInput.cancel(request(" {} \n"));
        for(String body:new String[]{"", "null", "[]", "1", "{\"status\":\"CANCELLED\"}"}) assertThrows(ApiFailure.class,()->TutorApiInput.cancel(request(body)));
        var unsupported=request("{}"); unsupported.setContentType("text/plain");
        assertEquals(415,assertThrows(ApiFailure.class,()->TutorApiInput.cancel(unsupported)).status());
    }

    @Test
    void rt14ActualStreamLimitIncludesWhitespaceAndIgnoresDeclaredLength() throws Exception {
        TutorApiInput.cancel(request("{}"+" ".repeat(65534)));
        var tooLarge=request("{}"+" ".repeat(65535));
        tooLarge.addHeader("Content-Length","2");
        assertEquals(413,assertThrows(ApiFailure.class,()->TutorApiInput.cancel(tooLarge)).status());
        byte[] utf8=("{\"pad\":\""+"é".repeat(32768)+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var multi=request("");multi.setContent(utf8);
        assertEquals(413,assertThrows(ApiFailure.class,()->TutorApiInput.cancel(multi)).status());
    }

    private MockHttpServletRequest request(String body) {
        var request=new MockHttpServletRequest();request.setContentType("application/json");
        request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));return request;
    }
}

package unichope.tutor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import unichope.platform.web.ApiFailure;
import unichope.tutor.http.TutorApiInput;

class TutorNoteInputTest {
    @Test
    void parsesOnlyExactStringAndIntegerFields() throws Exception {
        var request=request("{\"content\":\"notes\",\"expectedVersion\":2}");
        assertEquals(new TutorApiInput.NoteSave("notes",2),TutorApiInput.noteSave(request));
    }

    @Test
    void rejectsMissingWrongTypeExtraDuplicateAndTrailingFields() {
        for(String json:new String[]{"{}","{\"content\":null,\"expectedVersion\":0}",
                "{\"content\":\"x\",\"expectedVersion\":1.5}","{\"content\":\"x\",\"expectedVersion\":\"1\"}",
                "{\"content\":\"x\",\"expectedVersion\":0,\"tutorId\":\"spoof\"}",
                "{\"content\":\"x\",\"content\":\"y\",\"expectedVersion\":0}",
                "{\"content\":\"x\",\"expectedVersion\":0} {}"}) {
            assertThrows(ApiFailure.class,()->TutorApiInput.noteSave(request(json)),json);
        }
    }

    @Test
    void rejectsVersionOutsideSignedLongRange() {
        assertThrows(ApiFailure.class,()->TutorApiInput.noteSave(request("{\"content\":\"x\",\"expectedVersion\":9223372036854775808}")));
    }

    @Test
    void parsesRoughNotesWithoutTrimmingAndCountsUnicodeCodePoints() throws Exception {
        var request=request("{\"roughNotes\":\"  Reviewed recursion 😀  \"}");
        assertEquals(new TutorApiInput.NoteDraft("  Reviewed recursion 😀  "),TutorApiInput.noteDraft(request));
        String exactLimit="{\"roughNotes\":\""+"😀".repeat(8000)+"\"}";
        String parsed=TutorApiInput.noteDraft(request(exactLimit)).roughNotes();
        assertEquals(8000,parsed.codePointCount(0,parsed.length()));
    }

    @Test
    void rejectsInvalidRoughNotesShapeBlankAndOverLimit() {
        for(String json:new String[]{"{}","{\"roughNotes\":null}","{\"roughNotes\":4}",
                "{\"roughNotes\":\"   \",\"extra\":true}",
                "{\"roughNotes\":\"x\",\"extra\":true}",
                "{\"roughNotes\":\"x\",\"roughNotes\":\"y\"}",
                "{\"roughNotes\":\"x\"} {}"}) {
            assertEquals("VALIDATION_FAILED",assertThrows(ApiFailure.class,
                    ()->TutorApiInput.noteDraft(request(json)),json).code(),json);
        }
        assertEquals("VALIDATION_FAILED",assertThrows(ApiFailure.class,
                ()->TutorApiInput.noteDraft(request("{\"roughNotes\":\""+"😀".repeat(8001)+"\"}"))).code());
    }

    @Test
    void contentValidationCountsCodePointsAndUsesStringStrip() {
        assertEquals("x",TutorNoteService.normalizeContent("  x \n"));
        assertEquals("😀",TutorNoteService.normalizeContent("😀"));
        assertEquals("x".repeat(8000),TutorNoteService.normalizeContent("x".repeat(8000)));
        assertEquals("\u00a0",TutorNoteService.normalizeContent("\u00a0"));
        for(String invalid:new String[]{"", " \t\n ", "x".repeat(8001), "😀".repeat(8001)}) {
            assertEquals(TutorNoteFailure.Code.INVALID_CONTENT,
                    assertThrows(TutorNoteFailure.class,()->TutorNoteService.normalizeContent(invalid)).code());
        }
    }

    private static MockHttpServletRequest request(String json) {
        var request=new MockHttpServletRequest();request.setContentType("application/json");request.setContent(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));return request;
    }
}

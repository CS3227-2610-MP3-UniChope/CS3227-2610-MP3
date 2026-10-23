package unichope.tutor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import unichope.platform.ai.AiGateway;

class TutorNoteDraftParserTest {
  @Test
  void formatsTheExactThreeSectionSchema() {
    String raw =
        "{\"summary\":\"Reviewed recursion.\",\"topicsDiscussed\":\"Base cases\","
            + "\"followUp\":\"Practise two examples.\"}";

    assertEquals(
        "Summary\nReviewed recursion.\n\nTopics discussed\nBase cases\n\nFollow-up\nPractise two examples.",
        TutorNoteDraftParser.parse(raw));
  }

  @Test
  void acceptsExactlyEightThousandFormattedCodePoints() {
    String base = format("", "Topics", "Follow-up");
    int summaryLength = 8000 - base.codePointCount(0, base.length());
    String raw = json("😀".repeat(summaryLength), "Topics", "Follow-up");

    String draft = TutorNoteDraftParser.parse(raw);
    assertEquals(8000, draft.codePointCount(0, draft.length()));
  }

  @Test
  void rejectsMalformedUnexpectedDuplicateTrailingOrUnsafeOutput() {
    for (String raw :
        List.of(
            "not json",
            "[]",
            "{}",
            "{\"summary\":\"Summary\",\"topicsDiscussed\":\"Topics\"}",
            "{\"summary\":\"Summary\",\"topicsDiscussed\":\"Topics\",\"followUp\":\"Follow-up\",\"extra\":\"x\"}",
            "{\"summary\":4,\"topicsDiscussed\":\"Topics\",\"followUp\":\"Follow-up\"}",
            "{\"summary\":\"one\",\"summary\":\"two\",\"topicsDiscussed\":\"x\",\"followUp\":\"y\"}",
            json(" ", "Topics", "Follow-up"),
            json("Summary", "<b>Topics</b>", "Follow-up"),
            json("Summary", "Topics", "Follow-up > other"),
            json("Summary", "Topics", "Follow-up") + " {}")) {
      var failure = assertThrows(AiGateway.Failure.class, () -> TutorNoteDraftParser.parse(raw));
      assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
      assertNull(failure.getCause());
    }
  }

  @Test
  void rejectsFormattedOutputAboveEightThousandCodePointsWithoutLeakingIt() {
    String raw = json("😀".repeat(8000), "Topics", "Follow-up");

    var failure = assertThrows(AiGateway.Failure.class, () -> TutorNoteDraftParser.parse(raw));

    assertEquals(AiGateway.FailureCode.INVALID_OUTPUT, failure.code());
    assertFalse(failure.getMessage().contains("😀"));
    assertNull(failure.getCause());
  }

  private static String json(String summary, String topics, String followUp) {
    return "{\"summary\":\"" + summary + "\",\"topicsDiscussed\":\"" + topics
        + "\",\"followUp\":\"" + followUp + "\"}";
  }

  private static String format(String summary, String topics, String followUp) {
    return "Summary\n" + summary + "\n\nTopics discussed\n" + topics
        + "\n\nFollow-up\n" + followUp;
  }
}

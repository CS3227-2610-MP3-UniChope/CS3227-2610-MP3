package unichope.tutor;

import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.ai.AiGateway;

/** Validates untrusted Tutor note-draft output and formats it as plain text. */
public final class TutorNoteDraftParser {
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();
  private static final Set<String> FIELDS = Set.of("summary", "topicsDiscussed", "followUp");
  private static final int MAX_CODE_POINTS = 8000;

  private TutorNoteDraftParser() {}

  public static String parse(String raw) {
    try {
      JsonNode object = JSON.readTree(raw);
      if (object == null || !object.isObject() || !Set.copyOf(object.propertyNames()).equals(FIELDS))
        throw new IllegalArgumentException();

      String summary = text(object.get("summary"));
      String topics = text(object.get("topicsDiscussed"));
      String followUp = text(object.get("followUp"));
      String draft =
          "Summary\n" + summary + "\n\nTopics discussed\n" + topics + "\n\nFollow-up\n" + followUp;
      if (draft.codePointCount(0, draft.length()) > MAX_CODE_POINTS)
        throw new IllegalArgumentException();
      return draft;
    } catch (RuntimeException failure) {
      throw new AiGateway.Failure(AiGateway.FailureCode.INVALID_OUTPUT);
    }
  }

  private static String text(JsonNode node) {
    if (node == null || !node.isString()) throw new IllegalArgumentException();
    String value = node.stringValue().strip();
    if (value.isEmpty()
        || value.contains("<")
        || value.contains(">")
        || value.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))
      throw new IllegalArgumentException();
    return value;
  }
}

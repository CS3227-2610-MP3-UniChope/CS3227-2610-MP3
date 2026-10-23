package unichope.student;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.web.ApiFailure;

/** Strict schema and plain-text formatter. Invalid responses are never repaired or echoed. */
public final class AgendaOutput {
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private static final Pattern HTML = Pattern.compile("<[^>]*>");
    private AgendaOutput() {}

    public static String format(String raw, long length) {
        if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length > 65536 || !AgendaText.validUnicode(raw)) throw invalid();
        try {
            JsonNode root = JSON.readTree(raw);
            keys(root, Set.of("goal", "items", "prepare"));
            String goal = text(root.get("goal"), 500);
            JsonNode items = root.get("items"), prepare = root.get("prepare");
            if (!items.isArray() || items.size() < 1 || items.size() > 8 || !prepare.isArray() || prepare.size() > 5) throw invalid();
            StringBuilder result = new StringBuilder("Goal\n").append(goal).append("\n\nAgenda (").append(length).append(" min)");
            long total = 0;
            int index = 0;
            for (JsonNode item : items) {
                keys(item, Set.of("topic", "question", "minutes"));
                String topic = text(item.get("topic"), 120), question = text(item.get("question"), 500);
                JsonNode minutes = item.get("minutes");
                if (!minutes.isIntegralNumber() || !minutes.canConvertToInt() || minutes.intValue() < 1) throw invalid();
                int count = minutes.intValue();
                total += count;
                if (total > length) throw invalid();
                result.append("\n").append(++index).append(". ").append(topic).append(" (").append(count).append(" min)\n   ").append(question);
            }
            if (!prepare.isEmpty()) {
                result.append("\n\nPrepare");
                for (JsonNode entry : prepare) result.append("\n- ").append(text(entry, 300));
            }
            String formatted = result.toString();
            if (formatted.codePointCount(0, formatted.length()) > 4000) throw invalid();
            return formatted;
        } catch (JacksonException exception) { throw invalid(); }
    }

    private static void keys(JsonNode value, Set<String> fields) {
        if (value == null || !value.isObject() || !Set.copyOf(value.propertyNames()).equals(fields)) throw invalid();
    }

    private static String text(JsonNode value, int max) {
        if (value == null || !value.isString()) throw invalid();
        String text = value.stringValue();
        if (!AgendaText.validUnicode(text) || AgendaText.trim(text).isEmpty()
                || text.codePointCount(0, text.length()) > max || HTML.matcher(text).find()
                || text.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n')) throw invalid();
        return text;
    }

    private static ApiFailure invalid() { return new ApiFailure(502, "AI_INVALID_OUTPUT", "AI returned an invalid agenda. Please try again."); }
}

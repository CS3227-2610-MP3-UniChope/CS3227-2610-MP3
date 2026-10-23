package unichope.student;

/** Unicode validation shared by agenda input and untrusted output. */
public final class AgendaText {
    private AgendaText() {}

    public static boolean validUnicode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }

    public static String trim(String text) {
        int start = 0, end = text.length();
        while (start < end && whitespace(text.codePointAt(start))) start += Character.charCount(text.codePointAt(start));
        while (end > start && whitespace(text.codePointBefore(end))) end -= Character.charCount(text.codePointBefore(end));
        return text.substring(start, end);
    }

    private static boolean whitespace(int point) {
        return Character.isWhitespace(point) || Character.isSpaceChar(point);
    }
}

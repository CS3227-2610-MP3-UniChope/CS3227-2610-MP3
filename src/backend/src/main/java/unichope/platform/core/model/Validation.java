package unichope.platform.core.model;

/** Shared domain validation; models remain independent of JavaFX and persistence. */
public final class Validation {
    private Validation() { }

    public static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    public static String email(String value) {
        String email = text(value, "email");
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1
                || email.codePointCount(0, email.length()) > 254
                || email.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.isISOControl(c))) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
        return email;
    }
}

package unichope.tutor;

public final class TutorNoteFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Code { INVALID_CONTENT, NOTE_CONFLICT, TEMPORARILY_UNAVAILABLE }
    private final Code code;

    public TutorNoteFailure(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code code() { return code; }
}

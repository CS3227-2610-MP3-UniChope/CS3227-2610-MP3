package unichope.platform.core;
import unichope.platform.core.model.user.User;
/** Internal verification material; deliberately neither a record nor a JavaBean. */
public final class AccountCredentialSnapshot {
    private final User user;
    private final String encodedHash;
    private final long sessionGeneration;
    public AccountCredentialSnapshot(User user, String encodedHash, long sessionGeneration) {
        this.user = user; this.encodedHash = encodedHash; this.sessionGeneration = sessionGeneration;
    }
    public User user() { return user; }
    public String encodedHash() { return encodedHash; }
    public long sessionGeneration() { return sessionGeneration; }
    @Override public String toString() { return "AccountCredentialSnapshot[user=" + user + ", encodedHash=[REDACTED], sessionGeneration=" + sessionGeneration + "]"; }
}

package unichope.platform.core;
import unichope.platform.core.model.user.User;
/** Credential-free current identity and revocation counter. */
public record AccountState(User user, long sessionGeneration, boolean mustChangePassword) {
    public AccountState(User user, long sessionGeneration) { this(user, sessionGeneration, false); }
}

package unichope.platform.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * Supplies the actor authenticated by the server-side session for the current request.
 *
 * <p>Shared seam for every role (Student TECH §2). The authentication owner supplies the real
 * implementation; there is deliberately no main-source, anonymous or fallback implementation.
 * Callers must re-read the stored account for role and active state on every request.
 */
public interface CurrentActorProvider {
    /** The session-authenticated actor ID, or empty when the request has no usable actor. */
    Optional<UUID> currentActorId();
}

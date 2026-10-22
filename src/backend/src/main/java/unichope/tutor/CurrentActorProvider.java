package unichope.tutor;

import java.util.Optional;
import java.util.UUID;

/** Supplies the actor authenticated by the server-side session. */
public interface CurrentActorProvider {
    Optional<UUID> currentActorId();
}

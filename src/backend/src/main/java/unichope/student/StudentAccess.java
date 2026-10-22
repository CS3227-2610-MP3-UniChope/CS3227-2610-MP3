package unichope.student;

import java.util.UUID;
import unichope.platform.auth.CurrentActorProvider;
import unichope.platform.core.model.user.Role;

/**
 * Resolves the session actor and re-reads the stored account on every call (TECH §3.2).
 * Stored role and active state are authoritative; session authorities are ignored.
 */
public final class StudentAccess {
    private final CurrentActorProvider actor;
    private final StudentReadRepository reads;

    public StudentAccess(CurrentActorProvider actor, StudentReadRepository reads) {
        this.actor = actor;
        this.reads = reads;
    }

    /** Returns the current actor ID if it is an existing, active STUDENT; otherwise throws. */
    public UUID requireActiveStudent() {
        UUID id = actor.currentActorId()
                .orElseThrow(() -> new StudentAccessDeniedException(StudentAccessDeniedException.Reason.NO_ACTOR));
        StudentAccount account = reads.findAccount(id)
                .filter(found -> found.id().equals(id))
                .orElseThrow(() -> new StudentAccessDeniedException(StudentAccessDeniedException.Reason.UNKNOWN_ACCOUNT));
        if (!account.active()) throw new StudentAccessDeniedException(StudentAccessDeniedException.Reason.INACTIVE);
        if (account.role() != Role.STUDENT) throw new StudentAccessDeniedException(StudentAccessDeniedException.Reason.FORBIDDEN);
        return id;
    }
}

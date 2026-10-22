package unichope.tutor;

import java.util.UUID;
import unichope.platform.core.model.user.Role;

/** Current persisted account state is authoritative; no cached role authorization. */
public final class TutorAccess {
    private final CurrentActorProvider actor;
    private final TutorReadRepository reads;
    public TutorAccess(CurrentActorProvider actor,TutorReadRepository reads) { this.actor=actor;this.reads=reads; }
    public UUID requireActiveTutor() {
        UUID id=actor.currentActorId().orElseThrow(()->new TutorAccessDeniedException(TutorAccessDeniedException.Reason.UNAUTHENTICATED));
        TutorAccount account=reads.findAccount(id).filter(found->found.id().equals(id))
                .orElseThrow(()->new TutorAccessDeniedException(TutorAccessDeniedException.Reason.UNAUTHENTICATED));
        if(!account.active())throw new TutorAccessDeniedException(TutorAccessDeniedException.Reason.INACTIVE);
        if(account.role()!=Role.TUTOR)throw new TutorAccessDeniedException(TutorAccessDeniedException.Reason.FORBIDDEN);
        return id;
    }
}

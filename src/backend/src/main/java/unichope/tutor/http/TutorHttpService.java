package unichope.tutor.http;

import java.util.function.Supplier;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.repository.ModuleRepository;
import unichope.tutor.SlotView;
import unichope.tutor.TutorAccess;
import unichope.tutor.TutorAccessDeniedException;

public final class TutorHttpService {
    private final TutorAccess access;
    private final ConsultationLifecycle lifecycle;
    private final ModuleRepository modules;
    public TutorHttpService(TutorAccess access,ConsultationLifecycle lifecycle,ModuleRepository modules) {
        this.access=access;this.lifecycle=lifecycle;this.modules=modules;
    }
    public SlotView create(TutorApiInput.Create input) {
        var actor=access.requireActiveTutor();
        return project(mutate(()->lifecycle.createSlot(actor,input.moduleId(),input.startTime(),input.endTime())));
    }
    public SlotView cancel(java.util.UUID slotId) {
        var actor=access.requireActiveTutor();
        return project(mutate(()->lifecycle.cancelAvailableSlot(actor,slotId)));
    }
    private ConsultationSlot mutate(Supplier<ConsultationSlot> operation) {
        try { return operation.get(); }
        catch(LifecycleFailure failure) {
            if(failure.code()!=LifecycleFailure.Code.ACCESS_DENIED)throw failure;
            access.requireActiveTutor();
            throw new TutorAccessDeniedException(TutorAccessDeniedException.Reason.FORBIDDEN);
        }
    }
    private SlotView project(ConsultationSlot slot) {
        // Lifecycle has committed. Projection failure cannot undo or automatically retry it.
        String code=modules.findById(slot.moduleId()).orElseThrow(IllegalStateException::new).code();
        return new SlotView(slot.id(),slot.moduleId(),code,slot.startTime(),slot.endTime(),slot.status());
    }
}

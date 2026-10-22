package unichope.tutor.http;

import java.util.function.Supplier;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.model.consultation.ConsultationSlot;
import unichope.platform.core.repository.ModuleRepository;
import unichope.tutor.SlotView;
import unichope.tutor.TutorAccess;
import unichope.tutor.TutorAccessDeniedException;
import unichope.tutor.TutorReadRepository;
import unichope.tutor.TutorBooking;
import unichope.platform.core.model.consultation.Booking;

public final class TutorHttpService {
    private final TutorAccess access;
    private final ConsultationLifecycle lifecycle;
    private final ModuleRepository modules;
    private final TutorReadRepository reads;
    public TutorHttpService(TutorAccess access,ConsultationLifecycle lifecycle,ModuleRepository modules,TutorReadRepository reads) {
        this.access=access;this.lifecycle=lifecycle;this.modules=modules;this.reads=reads;
    }
    public SlotView create(TutorApiInput.Create input) {
        var actor=access.requireActiveTutor();
        return project(mutate(()->lifecycle.createSlot(actor,input.moduleId(),input.startTime(),input.endTime())));
    }
    public SlotView cancel(java.util.UUID slotId) {
        var actor=access.requireActiveTutor();
        return project(mutate(()->lifecycle.cancelAvailableSlot(actor,slotId)));
    }
    public TutorBooking complete(java.util.UUID bookingId) {
        var actor=access.requireActiveTutor();
        Booking completed=mutate(()->lifecycle.completeActiveBooking(actor,bookingId));
        return reads.findBookingByTutor(actor,completed.id())
                .orElseThrow(()->new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND));
    }
    private <T> T mutate(Supplier<T> operation) {
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

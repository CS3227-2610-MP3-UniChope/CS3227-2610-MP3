package unichope.tutor.http;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unichope.tutor.ModuleOption;
import unichope.tutor.SlotView;
import unichope.tutor.TutorReadService;
import unichope.tutor.TutorNoteService;
import unichope.tutor.TutorNoteDraftService;
import unichope.platform.auth.SessionPrincipal;
import unichope.platform.web.ApiFailure;

@RestController("tutorController")
@Profile("tutor-api")
@RequestMapping("/api/tutor")
public class TutorController {
    private final TutorReadService reads;
    private final TutorHttpService writes;
    private final TutorNoteService notes;
    private final ObjectProvider<TutorNoteDraftService> noteDrafts;
    public TutorController(TutorReadService reads,TutorHttpService writes,TutorNoteService notes,
            ObjectProvider<TutorNoteDraftService> noteDrafts) { this.reads=reads;this.writes=writes;this.notes=notes;this.noteDrafts=noteDrafts; }
    @GetMapping("/modules")
    public List<ModuleOption> modules(HttpServletRequest request) { TutorApiInput.noQuery(request);return reads.activeAssignedModules(); }
    @GetMapping("/slots")
    public List<SlotView> slots(HttpServletRequest request) { return TutorApiInput.date(request).map(reads::upcomingSlots).orElseGet(reads::upcomingSlots); }
    @GetMapping("/history/slots")
    public List<SlotView> historySlots(HttpServletRequest request) {
        var filter=TutorApiInput.historyQuery(request);
        return reads.historySlots(filter.status(),filter.date());
    }
    @GetMapping("/slots/{id}")
    public SlotView detail(@PathVariable("id") String id,HttpServletRequest request) { TutorApiInput.noQuery(request);return reads.slotDetail(TutorApiInput.id(id)); }
    @PostMapping("/slots")
    public ResponseEntity<SlotView> create(HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);SlotView result=writes.create(TutorApiInput.create(request));
        return ResponseEntity.created(URI.create("/api/tutor/slots/"+result.id())).body(result);
    }
    @GetMapping("/bookings")
    public List<unichope.tutor.TutorBookingView> completedBookings(HttpServletRequest request) {
        var filter = TutorApiInput.bookingsQuery(request);
        return reads.bookings(filter.moduleId(), filter.date(), filter.status());
    }
    @PostMapping("/bookings/{id}/complete")
    public unichope.tutor.TutorCompletedBookingView completeBooking(@PathVariable("id") String id,HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);var bookingId=TutorApiInput.id(id);TutorApiInput.emptyObject(request);return writes.complete(bookingId);
    }
    @PostMapping("/slots/{id}/cancel")
    public SlotView cancel(@PathVariable("id") String id,HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);var slotId=TutorApiInput.id(id);TutorApiInput.emptyObject(request);return writes.cancel(slotId);
    }
    @GetMapping("/bookings/{id}/note")
    public unichope.tutor.TutorNote note(@PathVariable("id") String id,HttpServletRequest request) {
        TutorApiInput.noQuery(request);return notes.load(TutorApiInput.id(id));
    }
    @PutMapping("/bookings/{id}/note")
    public unichope.tutor.TutorNote saveNote(@PathVariable("id") String id,HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);var bookingId=TutorApiInput.id(id);var input=TutorApiInput.noteSave(request);
        return notes.save(bookingId,input.content(),input.expectedVersion());
    }
    @PostMapping("/bookings/{id}/note-draft")
    public TutorNoteDraftResponse draftNote(@PathVariable("id") String id, HttpServletRequest request,
            @AuthenticationPrincipal SessionPrincipal principal) throws IOException {
        TutorApiInput.noQuery(request);
        var bookingId=TutorApiInput.id(id);
        var input=TutorApiInput.noteDraft(request);
        var service=noteDrafts.getIfAvailable();
        if(service==null)throw new ApiFailure(503,"AI_UNAVAILABLE","AI drafting is currently unavailable");
        var result=service.generate(principal,bookingId,input.roughNotes());
        return new TutorNoteDraftResponse(result.bookingId(),result.draft(),result.requiresReview(),
                result.providerMode().name().toLowerCase(java.util.Locale.ROOT));
    }
    public record TutorNoteDraftResponse(java.util.UUID bookingId,String draft,boolean requiresReview,String providerMode) {}
}

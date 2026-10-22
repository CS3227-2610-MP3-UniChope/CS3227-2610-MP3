package unichope.tutor.http;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
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
import unichope.tutor.TutorBooking;

@RestController("tutorController")
@Profile("tutor-api")
@RequestMapping("/api/tutor")
public class TutorController {
    private final TutorReadService reads;
    private final TutorHttpService writes;
    private final TutorNoteService notes;
    public TutorController(TutorReadService reads,TutorHttpService writes,TutorNoteService notes) { this.reads=reads;this.writes=writes;this.notes=notes; }
    @GetMapping("/modules")
    public List<ModuleOption> modules(HttpServletRequest request) { TutorApiInput.noQuery(request);return reads.activeAssignedModules(); }
    @GetMapping("/slots")
    public List<SlotView> slots(HttpServletRequest request) { return TutorApiInput.date(request).map(reads::upcomingSlots).orElseGet(reads::upcomingSlots); }
    @GetMapping("/bookings")
    public List<TutorBooking> bookings(HttpServletRequest request) {
        var filters=TutorApiInput.bookingFilters(request);
        return reads.bookings(filters.moduleId(),filters.date(),filters.status());
    }
    @GetMapping("/history/slots")
    public List<SlotView> history(HttpServletRequest request) {
        var filters=TutorApiInput.historyFilters(request);
        return reads.slotHistory(filters.status(),filters.date());
    }
    @PostMapping("/bookings/{id}/complete")
    public TutorBooking complete(@PathVariable("id") String id,HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);var bookingId=TutorApiInput.id(id);TutorApiInput.cancel(request);
        return writes.complete(bookingId);
    }
    @GetMapping("/slots/{id}")
    public SlotView detail(@PathVariable("id") String id,HttpServletRequest request) { TutorApiInput.noQuery(request);return reads.slotDetail(TutorApiInput.id(id)); }
    @PostMapping("/slots")
    public ResponseEntity<SlotView> create(HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);SlotView result=writes.create(TutorApiInput.create(request));
        return ResponseEntity.created(URI.create("/api/tutor/slots/"+result.id())).body(result);
    }
    @PostMapping("/slots/{id}/cancel")
    public SlotView cancel(@PathVariable("id") String id,HttpServletRequest request) throws IOException {
        TutorApiInput.noQuery(request);var slotId=TutorApiInput.id(id);TutorApiInput.cancel(request);return writes.cancel(slotId);
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
}

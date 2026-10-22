package unichope.student.http;

import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unichope.platform.web.ApiFailure;
import unichope.student.BookableSlot;
import unichope.student.ModuleOption;
import unichope.student.StudentBooking;
import unichope.student.StudentReadService;

/**
 * Student browse and owned booking routes. After shared security filter admission, request syntax
 * is validated before StudentAccess and resource work (TECH §3.2).
 */
@RestController("studentController")
@Profile("student-api")
@RequestMapping("/api/student")
public class StudentController {
    private final StudentReadService reads;
    private final StudentHttpService writes;

    public StudentController(StudentReadService reads, StudentHttpService writes) {
        this.reads = reads;
        this.writes = writes;
    }

    @PostMapping("/bookings")
    public ResponseEntity<StudentBooking> book(HttpServletRequest request) throws IOException {
        StudentApiInput.noQuery(request);
        UUID slotId = StudentApiInput.book(request);
        StudentBooking booking = writes.book(slotId);
        return ResponseEntity.created(URI.create("/api/student/bookings/" + booking.id())).body(booking);
    }

    @PostMapping("/bookings/{id}/cancel")
    public StudentBooking cancel(@PathVariable("id") String id, HttpServletRequest request) throws IOException {
        StudentApiInput.noQuery(request);
        UUID bookingId = StudentApiInput.id(id);
        StudentApiInput.cancel(request);
        return writes.cancel(bookingId);
    }

    @GetMapping("/modules")
    public List<ModuleOption> modules(HttpServletRequest request) {
        StudentApiInput.noQuery(request);
        return reads.activeModules();
    }

    @GetMapping("/slots")
    public List<BookableSlot> slots(HttpServletRequest request) {
        StudentApiInput.SlotFilter filter = StudentApiInput.slotFilter(request);
        return reads.bookableSlots(filter.moduleId(), filter.date());
    }

    @GetMapping("/bookings")
    public List<StudentBooking> bookings(HttpServletRequest request) {
        StudentApiInput.BookingFilter filter = StudentApiInput.bookingFilter(request);
        return reads.myBookings(filter.statuses(), filter.date());
    }

    /**
     * The path ID is bound as text and parsed here, so a malformed ID is 400 VALIDATION_FAILED (not a
     * framework type-mismatch) and is rejected before the actor check, like any query parameter.
     */
    @GetMapping("/bookings/{id}")
    public StudentBooking booking(@PathVariable("id") String id, HttpServletRequest request) {
        StudentApiInput.noQuery(request);
        UUID bookingId = StudentApiInput.id(id);
        // Another student's booking and a nonexistent ID give the same 404 (TECH §3).
        return reads.myBooking(bookingId)
                .orElseThrow(() -> new ApiFailure(404, "NOT_FOUND", "Resource not found"));
    }
}

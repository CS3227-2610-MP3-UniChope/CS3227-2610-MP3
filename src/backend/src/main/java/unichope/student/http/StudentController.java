package unichope.student.http;

import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unichope.platform.web.ApiFailure;
import unichope.student.BookableSlot;
import unichope.student.ModuleOption;
import unichope.student.StudentBooking;
import unichope.student.StudentReadService;

/**
 * Student browse and own-booking read routes. Request syntax is validated before the actor check (TECH §3.2
 * evaluation order), so a syntax error never reads data or invalidates a session.
 */
@RestController("studentController")
@Profile("student-api")
@RequestMapping("/api/student")
public class StudentController {
    private final StudentReadService reads;

    public StudentController(StudentReadService reads) {
        this.reads = reads;
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

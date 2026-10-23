package unichope.student;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;
import unichope.platform.ai.AiGateway;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.web.ApiFailure;

/** Authorizes minimal context before dispatch and rechecks access after the untrusted call. */
public final class AgendaService {
    private final StudentAccess access;
    private final StudentReadRepository reads;
    private final Clock clock;
    private final Supplier<AiGateway> gateway;
    private final Supplier<Runnable> sessionChecks;

    public AgendaService(StudentAccess access, StudentReadRepository reads, Clock clock, Supplier<AiGateway> gateway,
            Supplier<Runnable> sessionChecks) {
        this.access = access; this.reads = reads; this.clock = clock; this.gateway = gateway;
        this.sessionChecks = sessionChecks;
    }

    public AgendaDraft generate(UUID bookingId, String questions) {
        UUID actor = access.requireActiveStudent();
        var booking = eligible(actor, bookingId);
        Runnable checkSession = sessionChecks.get();
        checkSession.run();
        long length = Duration.between(booking.startTime(), booking.endTime()).toMinutes();
        AiGateway provider = gateway.get();
        if (provider == null) throw new ApiFailure(503, "AI_UNAVAILABLE", "AI is currently unavailable.");
        AiGateway.Result result;
        try {
            String input = JsonMapper.shared().writeValueAsString(Map.of("questions", questions,
                    "moduleCode", booking.moduleCode(), "lengthMinutes", length));
            result = provider.generate(new AiGateway.Request(actor, AgendaPrompt.VERSION, AgendaPrompt.INSTRUCTIONS, input, 2048));
        } catch (AiGateway.Failure failure) {
            throw new ApiFailure(switch (failure.code()) { case UNAVAILABLE -> 503; case TIMEOUT -> 504; case INVALID_OUTPUT -> 502; },
                    "AI_" + failure.code().name(), failure.getMessage());
        }
        checkSession.run();
        if (!actor.equals(access.requireActiveStudent())) throw new StudentAccessDeniedException(StudentAccessDeniedException.Reason.FORBIDDEN);
        eligible(actor, bookingId);
        if (result == null) throw new ApiFailure(502, "AI_INVALID_OUTPUT", "AI returned an invalid agenda.");
        return new AgendaDraft(bookingId, AgendaOutput.format(result.text(), length), true, result.providerMode().name().toLowerCase(java.util.Locale.ROOT));
    }

    private StudentReadRepository.BookingRow eligible(UUID actor, UUID bookingId) {
        var row = reads.findStudentBooking(actor, bookingId).orElseThrow(() -> new ApiFailure(404, "NOT_FOUND", "Resource not found"));
        if (row.status() != BookingStatus.ACTIVE || !row.endTime().isAfter(clock.instant())
                || Duration.between(row.startTime(), row.endTime()).toMinutes() < 1)
            throw new ApiFailure(409, "INVALID_STATE", "This booking is no longer available for preparation.");
        return row;
    }
}

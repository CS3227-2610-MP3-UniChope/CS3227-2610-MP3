package unichope.student;

import java.time.Instant;
import java.util.UUID;

/**
 * Student {@code BookableSlot} JSON (TECH §3): exactly these fields. {@code tutorName} is the
 * tutor's display name only; no email or tutor ID is exposed.
 */
public record BookableSlot(
        UUID id,
        UUID moduleId,
        String moduleCode,
        String moduleName,
        String tutorName,
        Instant startTime,
        Instant endTime) {}

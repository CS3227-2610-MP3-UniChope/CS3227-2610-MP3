package unichope.tutor;

import java.time.Instant;
import java.util.UUID;
import unichope.platform.core.model.consultation.SlotStatus;

public record SlotView(
        UUID id,
        UUID moduleId,
        String moduleCode,
        Instant startTime,
        Instant endTime,
        SlotStatus status,
        String studentName) {}

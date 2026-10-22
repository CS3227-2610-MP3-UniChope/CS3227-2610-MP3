package unichope.tutor;

import unichope.platform.core.model.consultation.ConsultationSlot;

/** Tutor read projection: shared slot value plus joined display data. */
public record TutorSlot(ConsultationSlot slot, String moduleCode) {}

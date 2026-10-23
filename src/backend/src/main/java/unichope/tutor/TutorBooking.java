package unichope.tutor;

import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.ConsultationSlot;

/** Tutor-scoped booking projection for the completed-consultation selector. */
public record TutorBooking(Booking booking, ConsultationSlot slot, String moduleCode) {}

package unichope.tutor;

import unichope.platform.core.model.consultation.Booking;
import unichope.platform.core.model.consultation.ConsultationSlot;

/** Tutor-scoped booking projection with the student identity shown to its owner. */
public record TutorBooking(Booking booking, ConsultationSlot slot, String moduleCode, String studentName) {}

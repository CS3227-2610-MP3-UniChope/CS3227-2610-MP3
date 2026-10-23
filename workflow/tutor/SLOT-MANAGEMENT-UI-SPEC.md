# Tutor Slot Management UI Increment

Status: Implemented locally; acceptance checks recorded in the developer handoff.
Date: 23 October 2026
Requirements: TUT-SLOT-01, TUT-SLOT-02, TUT-SLOT-03.

## Goal and migration boundary

Restore the Tutor slot workflow from MP2's `tutor/TutorWorkspace.java` in the
React Tutor workspace. MP2's Slots tab listed upcoming slots, selected an assigned
module/date/start/end, created a slot, and cancelled the selected slot. MP3 keeps
those user actions and uses its authenticated REST API and PostgreSQL lifecycle.
No backend lifecycle rules or note behavior change. Slot editing is not in MP2's
workflow and is not added here.

## Behavior

- Load assigned active modules from `GET /api/tutor/modules` and the current Tutor's
  upcoming AVAILABLE/BOOKED slots from `GET /api/tutor/slots`.
- Provide an optional Singapore start-date filter; an empty filter shows all upcoming
  slots. Display module, Singapore start/end, status, loading, empty, failure, and
  refresh states.
- Create from a selected assigned module, Singapore date, start time, and end time.
  Convert Singapore local values to UTC `Z` instants for the existing POST contract.
  Require all fields and an end later than the start. The server remains authoritative
  for future-time, duration, assignment, overlap, account, and ownership rules. Do not
  retry an uncertain create request. Refresh after success and preserve form values
  after failure.
- Offer cancellation only for AVAILABLE rows. Require explicit confirmation, send
  `{}` to `POST /api/tutor/slots/{id}/cancel`, and refresh after success. Do not
  cancel a BOOKED slot or change a student's booking.
- Keep slot filters and request state independent from booking filters, completed
  note selection, and slot-history filters.

## Verification

The matrix covers initial/load/refresh behavior, Singapore date filtering, UTC request
conversion, validation, create failure preservation, cancellation confirmation,
booked-slot protection, and independence from booking/note state. The existing secured
backend endpoints remain the authority for actor identity and lifecycle transitions.

## Reuse record

MP2 source consulted: `CS3227-2610-MP2/src/main/java/tutor/TutorWorkspace.java`,
Slots tab (module/date/start/end controls, create, cancel, upcoming list). Adapted
behavior, not JavaFX code. MP3 backend API and transaction services are reused as the
integration boundary. This increment adds the missing React presentation and does
not duplicate MP2 persistence or lifecycle logic.

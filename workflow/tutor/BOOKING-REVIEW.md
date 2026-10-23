# P4 Review: Tutor Bookings and History

**Status:** Awaiting your scope approval

**Decision requested:** Is this the right feature and behavior to implement next?

## What this increment adds

- A tutor's booking list with module, date, and status filters.
- A completion action with a confirmation step.
- A separate view of cancelled and completed slots.

## Key behavior

- Tutors see only their own bookings and slot history.
- Completing a booking updates its booking and slot together. Early completion is
  allowed when the tutor confirms the consultation has finished.
- History filters do not change booking filters or the selected booking.
- Student contact details are not included in booking responses.

## Scope boundaries

This increment reuses the existing shared lifecycle and adds Tutor reads, API
endpoints, and the React view. It does not change database schema, authentication,
notes, or AI features. Automated session fixtures do not establish that real login
is integrated.

## What the review found

The independent test-design review led to three updates: remove an out-of-scope
note-selection expectation, test malformed or spoofed completion requests, and
verify denied completion requests leave persisted state unchanged. The detailed
matrix has 28 cases across API, PostgreSQL, security, and UI behavior.

## Your decision

- [ ] Approve this scope for implementation.
- [ ] Request changes before implementation.

If you request changes, note them here or in your reply. No application code will
be written until the scope is approved.

## Details

- [Full increment specification](BOOKING-INCREMENT-SPEC.md)
- [Test matrix](BOOKING-TEST-MATRIX.md)
- [Implementation plan](BOOKING-INCREMENT-PLAN.md)
- [Independent test-design review record](../handoffs/2026-10-22-test-design-tutor-bookings.md)

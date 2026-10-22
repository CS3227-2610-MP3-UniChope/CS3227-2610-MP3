# Tutor Booking Completion API Plan

Status: Endpoint implemented and backend-verified on 23 October 2026. Independent
review and human implementation acceptance remain pending. This closes the API part
of P4 only; Tutor completion UI and slot history are not implemented here.

Specification: [completion increment](COMPLETION-INCREMENT-SPEC.md).
Test matrix: [completion test matrix](COMPLETION-TEST-MATRIX.md).
Shared contract: [Tutor API](../INTEGRATION-SPEC.md).

## Work and evidence

- [x] Define the exact request, response, authorization, state, and transaction
  behavior before implementation; derive the test matrix from those requirements.
- [x] Add PostgreSQL-backed route tests first. The initial run failed on the
  unimplemented completion route as expected.
- [x] Add `POST /api/tutor/bookings/{id}/complete`, require exactly `{}`, and call
  the shared `ConsultationLifecycle.completeActiveBooking` operation.
- [x] Project only the agreed Tutor Booking fields. Do not expose Student email.
- [x] Verify success, early completion, repeated completion, missing/foreign IDs,
  wrong role, inactive account, CSRF, strict input, transactional rollback, and
  post-commit response failure behavior.
- [x] Run the backend unit suite, full PostgreSQL suite, and `bootJar` against a
  disposable PostgreSQL cluster; exact counts and commands are in the
  [developer handoff](../handoffs/2026-10-23-developer-tutor-completion.md).
- [ ] Independent review against the increment spec and test matrix.
- [ ] Human implementation acceptance.

## Remaining P4 work

The booking list API, completion endpoint, confirmation/action flow, and separate
Tutor slot history endpoint/view are now implemented. Review is still pending before
human implementation acceptance; see the [bookings/history UI handoff](../handoffs/2026-10-23-developer-tutor-bookings-history-ui.md).

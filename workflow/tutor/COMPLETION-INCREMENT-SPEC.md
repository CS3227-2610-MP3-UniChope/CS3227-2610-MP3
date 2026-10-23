# Tutor Booking Completion API Increment

Status: Implemented; backend and UI evidence recorded, independent review pending.
Date: 23 October 2026
Branch: `feat/tutor-booking-completion`

## 1. Goal and scope

Expose the existing Tutor booking-completion transition through the authenticated
Tutor API. The endpoint must call the shared `ConsultationLifecycle`; Tutor HTTP code
must not write booking or slot status directly.

This increment covers the completion endpoint and its request-to-PostgreSQL tests.
The confirmation UI and slot history are specified separately. It does not change
completion rules, activate production AI, or modify Student/Admin behavior.

Requirements: `TUT-BOOK-02`, shared `SEC-01`, `SH-AUTH-03/04`, `SH-DATA-02/03`,
`SH-WEB-01`, and the existing `POST /api/tutor/bookings/{id}/complete` contract in
[`INTEGRATION-SPEC.md`](../INTEGRATION-SPEC.md).

## 2. Endpoint contract

All paths are relative to `/api/tutor`. The endpoint requires an authenticated,
currently active Tutor, a valid CSRF token, and `application/json` content type.

| Method/path | Input | Success |
| --- | --- | --- |
| `POST /bookings/{id}/complete` | UUID booking ID and exactly `{}` | 200 completed Booking |

The success body has exactly `id`, `slotId`, `moduleId`, `moduleCode`,
`studentName`, `startTime`, `endTime`, and `status`. It does not expose student
email or credentials. Timestamps are UTC ISO-8601 strings. The endpoint returns the
booking and slot status as `COMPLETED` after the lifecycle transaction commits.

Malformed IDs, a missing/null/non-object body, extra fields, malformed JSON, or
unsupported media type use the shared safe Tutor API error envelope. Missing or
foreign booking IDs both return 404 `NOT_FOUND`. A booking that is not `ACTIVE`, or
whose slot is not `BOOKED`, returns 409 `INVALID_STATE`. Missing/invalid CSRF is
denied before mutation. Actor identity comes only from the authenticated server
session; request fields or headers cannot select the Tutor.

Early completion remains allowed under the specified product rule: the tutor confirms
that the consultation has finished. The API cannot establish that fact from time
alone. Repeated completion conflicts and makes no additional changes.

## 3. Transaction and failure behavior

`ConsultationLifecycle.completeActiveBooking(actorId, bookingId)` remains the sole
writer and authority for current role/account, ownership, state, row-lock order, and
atomic booking/slot updates. The Tutor adapter checks the current active Tutor before
calling it and rechecks access if the lifecycle returns its generic access-denied
failure, matching the existing slot mutation adapter.

The lifecycle commits before the HTTP response projection is assembled. A failure
while reading projection fields after commit returns a sanitized error; it does not
mean the completion rolled back and must not trigger an automatic retry. The client
should refresh the booking list to learn the committed state.

No direct SQL, duplicate locking protocol, side effect, or automatic retry is added
to the Tutor adapter. Existing lifecycle unit/PostgreSQL race tests remain the source
of truth for atomicity and cancellation/completion races; endpoint tests verify the
real lifecycle's result and persisted state.

## 4. Acceptance checks

The test matrix in [`COMPLETION-TEST-MATRIX.md`](COMPLETION-TEST-MATRIX.md) is the
acceptance oracle. The key checks are: exact successful response and database states;
CSRF, role, account, and owner denials before unauthorized changes; strict `{}` input;
same 404 for foreign and missing IDs; 409 on repeat/ineligible state; and rollback
when the second state update fails. Tests use synthetic accounts and a disposable
PostgreSQL schema. No production credentials or provider calls are involved.

The endpoint contract and this focused increment spec precede implementation. Any
change to the response shape or lifecycle semantics requires updating the shared
integration spec and this matrix before changing code.

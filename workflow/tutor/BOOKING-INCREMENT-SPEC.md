# Tutor Bookings, Completion, and History Increment

Status: Scope approved and implementation human-accepted on 22 October 2026.
Owner: Huanghao
Date: 22 October 2026
Baseline: `main` at `4ed9d696db754d0d7982d75ec5a287b3a5229407` (22 October 2026).
Requirements: TUT-BOOK-01, TUT-BOOK-02, TUT-HIST-01.
Related: [Tutor product requirements](PRODUCT-SPEC.md),
[Tutor technical specification](TECHNICAL-SPEC.md),
[integration contract](../INTEGRATION-SPEC.md),
[shared lifecycle specification](../core/LIFECYCLE-SPEC.md).

## 1. Purpose and scope

Expose a tutor's bookings, allow the tutor to complete an active consultation,
and show cancelled or completed slot history. Provide the corresponding React
view with independent booking and history filters and an explicit completion
confirmation.

The merged shared lifecycle already provides `completeActiveBooking`; this
increment calls it and does not redesign its state transitions or locking. It
adds tutor-scoped booking/history reads, the HTTP adapter, and the user interface.
It introduces no tables or migrations. Manual notes and AI drafting are outside
this increment; existing note endpoints remain usable by booking ID.

## 2. Actor, ownership, and privacy

- Every route requires the current active TUTOR actor from the existing trusted
  actor/session boundary. A request parameter or body cannot select an actor.
- Booking and history list queries constrain ownership through `slots.tutor_id`
  in the database query. Do not load a global list and filter it in Java.
- A foreign booking completion and a missing booking both return 404
  `NOT_FOUND`. Lists silently exclude records owned by other tutors.
- Booking responses use the shared `Booking` shape: `id`, `slotId`, `moduleId`,
  `moduleCode`, `studentName`, `startTime`, `endTime`, and `status`. Do not return
  student email, student ID, tutor ID, or unrelated profile fields.
- Keep existing CSRF, safe-error, no-store, and request-validation behavior.
  Test-only session identities do not establish that real login/session
  integration is complete.

## 3. Booking list contract

`GET /api/tutor/bookings` accepts optional `moduleId`, `date`, and `status` query
parameters. `date` is `YYYY-MM-DD` in Asia/Singapore and matches the slot's
Singapore start date. `status` is one of `ACTIVE`, `CANCELLED`, or `COMPLETED`.
Unknown, malformed, or repeated-invalid filters return 400 using the existing
API error format. A valid filter with no matches returns `200 []`.

No filters means all statuses for all of the current tutor's bookings. Combine
provided filters with AND semantics. Sort by slot start instant ascending, then
booking UUID ascending. Student cancellations must appear as `CANCELLED` after a
refresh. The list is not paginated in this increment, consistent with the
existing integration contract.

## 4. Completion contract

`POST /api/tutor/bookings/{id}/complete` accepts the existing empty JSON object
body `{}` and returns the completed Booking with 200. The UI asks the tutor to
confirm that the consultation has finished before sending the request; cancelling
the confirmation sends no request. The server cannot establish that a person
clicked a confirmation dialog, so authorization and valid state remain enforced
by the backend regardless of UI behavior.

Only an owned `ACTIVE` booking paired with a `BOOKED` slot may complete. Call
`ConsultationLifecycle.completeActiveBooking(actorId, bookingId)` so the booking
and slot become `COMPLETED` atomically. Early completion is allowed, as already
selected in the shared lifecycle contract; the tutor's confirmation records that
the consultation has actually finished. Repeated completion, cancelled bookings,
and inconsistent state return 409 `INVALID_STATE` without partial writes. Missing
and foreign IDs both return 404 `NOT_FOUND`.

## 5. Slot history contract

`GET /api/tutor/history/slots` requires exactly one `status` query value:
`CANCELLED` or `COMPLETED`. It accepts optional `date` using the same Singapore
start-date rule as the booking list. Missing/unsupported status and invalid date
return 400. Return only the current tutor's slots with the selected status,
ordered by start instant ascending, then slot UUID ascending. An empty result is
`200 []`.

Slot history is separate from the completed booking list used to identify a
consultation for notes. Changing history filters must not change booking filters
or the selected booking. This increment does not add a note selector/editor or
change note persistence.

## 6. Acceptance criteria

1. A tutor can filter their bookings by module, Singapore date, and status in any
   supported combination; only owned rows are returned in stable order.
2. A confirmed completion updates booking and slot together through the shared
   lifecycle. Denials and repeated requests have no partial state changes.
3. Tutors can view only their cancelled or completed slots, with required status
   and optional date filters; history controls remain independent of booking
   filters and selected booking.
4. The React view presents loading, empty, error, and successful states, and
   requires explicit confirmation before completion.
5. Automated tests verify API behavior with synthetic identities and PostgreSQL.
   They do not claim real-login integration or production activation.

## 7. Exclusions and dependencies

- No changes to shared booking rules, row-lock order, schema, or migrations.
- No student/admin endpoints or changes to their clients.
- No manual-note editor, note selection workflow, AI generation, or provider use.
- No production session adapter, real login, or production database activation.
- Dependency: merged Tutor slot API and shared PostgreSQL lifecycle. Reuse the
  existing lifecycle completion tests for lock races and atomicity; add request
  integration assertions for the Tutor endpoint instead of duplicating core
  concurrency tests.

The primary checkout has unrelated local changes. Implementation should start
from a clean checkout/worktree based on the `main` commit above, after the spec
and test matrix are reviewed.

# Developer Handoff: Tutor Booking Completion API

Date: 23 October 2026
Branch: `feat/tutor-booking-completion`
Increment: P4 completion API (`TUT-BOOK-02`)
Review state: Independent review and human acceptance pending

## Change

Added `POST /api/tutor/bookings/{id}/complete`. The endpoint requires the active
Tutor session and a valid CSRF token, rejects query parameters and request bodies
other than exactly `{}`, and delegates the transition to
`ConsultationLifecycle.completeActiveBooking`. It does not add Tutor SQL or duplicate
the shared lifecycle's locking and state rules.

On success it returns the agreed Booking response fields: booking ID, slot ID,
module ID/code, Student display name, start/end time, and status. Student email is
not returned. The specified product rule permits early completion after the Tutor confirms
that the consultation has finished. Repeated completion returns 409 without another
state change.

## Test-first evidence

The new PostgreSQL route tests were run before the endpoint existed. The suite had
three expected failures, each at the missing route. After implementation, the
targeted `TutorRestPgTest` suite passed. It covers exact successful response fields,
both persisted statuses, early completion, repetition, same 404 for foreign/missing
IDs, wrong role, inactive account, anonymous session, missing/invalid CSRF, strict
body/path parsing, transaction rollback when the slot update fails, and a post-commit
projection failure that leaves both statuses COMPLETED.

The lifecycle's existing PostgreSQL concurrency tests remain the oracle for
completion/cancellation races; the new route tests do not duplicate those races.
An additional `app-api,local` PostgreSQL integration creates a booking through the
Student endpoint, signs in with the real authentication service, and completes it
with a real `SessionPrincipal` and server-issued CSRF token. It confirms anonymous
and Student requests are denied and the owning Tutor updates both records through
the shared lifecycle.

## Verification

The final run used an isolated PostgreSQL 17 cluster initialized under
`/private/tmp/cs3227-p4-completion-pg`, database `cs3227_p4_test`, port 55444, and
synthetic test records. The cluster was stopped and removed after testing.

Command:

```sh
./gradlew test postgresTest bootJar \
  -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55444/cs3227_p4_test \
  -Dcore.test.user=huanghao -Dcore.test.password=
```

Results: 91 unit tests across 25 suites and 197 PostgreSQL tests across 18 suites
passed with zero failures, errors, or skips; `bootJar` succeeded. The focused
`TutorRestPgTest` suite contained 38 tests; `StudentAuthIntegrationPgTest` contained
10 tests, including the real-session completion test. `git diff --check` passed for source
files; Markdown handoff/spec formatting retains intentional hard line breaks.

## Limits

This is backend behavior only. There is no Tutor UI action/confirmation for this
endpoint yet, and the separate slot history endpoint/view remain outstanding. The
test database used synthetic data only. No production database, migration activation,
SoC provider, or real user record was involved. The response projection is read after
the lifecycle commits; a projection error can therefore return 500 even though both
statuses were committed, and callers must refresh rather than retry blindly.

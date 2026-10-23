# Lifecycle Test Matrix

Status: Test-design artifact. The designer did not execute tests; subsequent
implementation results are recorded in the [coverage/evidence map](evidence/lifecycle-coverage.md)
and [independent review](../handoffs/2026-10-21-review-lifecycle.md).
Date: 21 October 2026. Designer: `/root/lifecycle_test_designer`.
Scope: [Lifecycle spec](LIFECYCLE-SPEC.md), LC-01–LC-12; no HTTP, sessions, React,
AI, notes, credential decision or production migration activation.
Inputs: [scoped manifest](lifecycle-test-design-inputs-2026-10-21.json).
Revised spec/shared inputs: [supplemental manifest](lifecycle-test-design-supplemental-inputs-2026-10-21.json).

## Resolved design questions

1. **Null interval failure code:** the coordinator clarified §5: null required IDs
   use INVALID_INPUT; null start/end and invalid intervals use INVALID_TIME.
   TD-01/04 now use those explicit oracles.
2. **Booking eligibility failure codes:** the coordinator clarified §5:
   MODULE_UNAVAILABLE covers unavailable related tutor/module/assignment during
   booking, with a generic message; ACCESS_DENIED remains the actor error;
   INVALID_STATE covers nonfuture/non-AVAILABLE slots and existing ACTIVE booking
   conflicts. Non-disclosing missing/foreign owned-resource behavior is NOT_FOUND.
   TD-10/23 now use the clarified oracles.

At design handoff these clarifications awaited scope review. Scope approval and
subsequent implementation acceptance were recorded on 21 October 2026.
The original test-design handoff retains its status at that stage.

## Fixtures and verification conventions

Use synthetic active tutors T1/T2, students S1/S2 and admin A, module M assigned to
T1/T2, and distinct UUIDs with known sort order. Add inactive/role variants per case.
Use an injected controllable Clock at `N`, future intervals `[N+1h,N+2h)` and
`[N+90m,N+150m)`, and adjacent `[N+2h,N+3h)`. Use a database-representable precision
for equality tests. All persisted-state observations below use a fresh connection
following commit/rollback; check IDs, relationships, statuses and row counts, not
only returned objects. Snapshot affected fixture rows before rejection. Preserve
history and unrelated rows. A successful result must match the committed record.

**PG** means JUnit in Gradle `postgresTest`, against an explicitly selected isolated
PostgreSQL database with the credential-free review schema. **UNIT** means Gradle
`test`; **REVIEW** means independent source/diff inspection. PostgreSQL can be a
reviewed disposable local service; Testcontainers is not a prerequisite. Missing
configuration must fail, not silently skip. No database/build probes were run in
this design task.

For concurrent cases use distinct physical connections/transactions and independent
lifecycle invocations, never one shared transaction or a synchronized mock. A test
transaction can hold the known first conflicting lock; start callers using latches,
then observe a blocked backend via `pg_blocking_pids`/equivalent bounded database
inspection before releasing it. Do not infer waiting from a sleep. Bound all waits,
polls and joins, propagate worker exceptions, roll back/close in finally, and fail
rather than hang. Choose a generous explicit test watchdog above the configured
5-second lock / 10-second statement limits; these are per-statement limits, not an
asserted whole-transaction deadline. Do not add product test hooks.

Readiness codes: **D** designed, requires approval + lifecycle/test implementation
+ disposable PG; **R** existing
regression scope to retain and rerun, not a new passing claim. Each parameterized
variant uses a fresh isolated fixture. Exact error wording is asserted only for
non-disclosure/sanitization, not incidental prose.

## Requirement-linked cases

| Case / requirements | Purpose and fixture | Action / interleaving | Observable oracle and persisted state | Layer / readiness |
| --- | --- | --- | --- | --- |
| TD-01 · LC-01–05, SEC-01 | Required arguments; otherwise eligible fixtures for each method | Independently pass null actor, module/resource ID; null start/end for creation | Typed safe failure, no writes or generated records. Null ID => INVALID_INPUT; null interval => INVALID_TIME; no accidental NullPointerException | UNIT + PG · D |
| TD-02 · LC-01–05, SEC-01 | Missing/inactive actor and wrong role, parameterized over five methods | Invoke with each invalid actor and both present/missing resource variants | ACCESS_DENIED without resource data; snapshot unchanged. Proves lifecycle enforces actor checks rather than trusting service callers | PG · D |
| TD-03 · LC-01, TUT-SLOT-02 | Active assigned T1/M, no slots | Create valid future interval | One generated-ID AVAILABLE slot owned by T1/M, exact interval; return matches persisted slot; unrelated rows unchanged | PG · D |
| TD-04 · LC-01, SH-DATA-01 | T1/M with fixed N | Create start at N, before N; end equal to/before start; null interval separately | INVALID_TIME for null/nonfuture/reversed/zero-length interval; no insert. Include start just after N at supported precision as valid | UNIT + PG · D |
| TD-05 · LC-01 | Existing eligible T1; module absent/inactive/unassigned variants | Create otherwise-valid interval for each module | MODULE_UNAVAILABLE with same public message/details for variants; no slot; no module existence disclosure | PG · D |
| TD-06 · LC-01, TUT-SLOT-02 | Same tutor AVAILABLE and BOOKED intervals, including other modules; other tutor control | Try equal, contained, containing and partial overlaps; try adjacent intervals and overlap with T2 only | SLOT_OVERLAP for T1 AVAILABLE/BOOKED intersections; adjacency/T2 intersection succeeds. Existing history unchanged; detects incorrect module-scoped overlap and closed endpoints | PG · D |
| TD-07 · LC-01 | T1 CANCELLED/COMPLETED historical slots with overlapping times | Create valid future interval intersecting each historical record | New AVAILABLE slot; historical record retained unchanged. Old statuses do not incorrectly block creation | PG · D |
| TD-08 · LC-02, TUT-SLOT-03 | T1 AVAILABLE slot, with future and already-started variants | Cancel as T1, then repeat; also try BOOKED/COMPLETED/CANCELLED fixtures | First cancellation => same slot ID CANCELLED retained; past AVAILABLE is allowed; repeat/other status => INVALID_STATE and no writes/booking changes | PG · D |
| TD-09 · LC-02/04/05, SEC-01 | Eligible actor; absent vs other owner's slot/booking UUIDs | Cancel slot; cancel booking; complete booking with foreign/absent ID pairs | Both return NOT_FOUND with indistinguishable public code/message and no identifiers; no row changes. No requirement to prove timing equivalence | PG · D |
| TD-10 · LC-03 | S1, future AVAILABLE T1 slot, valid M/assignment | Book valid case; separately deactivate T1/M or remove assignment before call | Success => one ACTIVE booking + BOOKED slot, matching relationships/createdAt from server clock; ineligible variants MODULE_UNAVAILABLE with generic identical message and no writes. Never expose related participant details in failures | PG · D |
| TD-11 · LC-03 | Slot BOOKED/CANCELLED/COMPLETED, or start = N / < N variants | Book as valid S1 | INVALID_STATE; no extra booking or slot mutation. Strict future boundary holds independently of UI filtering | PG · D |
| TD-12 · LC-03 | S1 ACTIVE booking on intersecting slot; controls for adjacent, cancelled/completed booking and S2 | Book distinct eligible target slot | BOOKING_OVERLAP only for S1 ACTIVE intersection; adjacency/history/other student succeed; original booking unchanged. Detects slot-only uniqueness masquerading as overlap safety | PG · D |
| TD-13 · LC-04 | S1 owns ACTIVE booking with BOOKED future slot | Cancel, then book released slot as S2 | Original booking CANCELLED retained; slot AVAILABLE after cancel, then BOOKED with new ACTIVE booking; exactly one current ACTIVE booking, original history unchanged | PG · D |
| TD-14 · LC-04 | Booking CANCELLED/COMPLETED, inconsistent ACTIVE/non-BOOKED pair, and slot start = N / < N | Cancel as owning S1 | INVALID_STATE; complete fixture snapshot unchanged. Include start just after N succeeds. Inconsistent fixtures are test-only, not a public write API | PG · D |
| TD-15 · LC-05, TUT-BOOK-02 | T1 owns ACTIVE/BOOKED pair; variants before start/after end, inactive S1, inactive M/removed assignment | Complete as active T1 | Booking and slot both COMPLETED; ID/count/history preserved. Early completion allowed; nonactor inactivity or lost assignment does not invent a new rejection rule | PG · D |
| TD-16 · LC-05 | Completed/cancelled booking, inconsistent status pair; successful completed fixture | Complete each; repeat success | INVALID_STATE; no new records or changed history. Wrong owner separately covered TD-09 | PG · D |
| TD-17 · LC-06, SH-DATA-03 | T1 initially has zero slots; two overlapping create calls | Hold T1 lock; establish waiting independent calls, release; join bounded | Exactly one success + one SLOT_OVERLAP; one AVAILABLE row. Detects locking only existing matching slots (empty-range race) | PG concurrent · D |
| TD-18 · LC-07, REL-01 | One eligible AVAILABLE slot; S1/S2 | Establish competing independent bookings with controlled blocking; release holder | Exactly one success, other INVALID_STATE; one ACTIVE booking; BOOKED slot linked to winner. No orphan/duplicate booking; partial unique index is supplementary | PG concurrent · D |
| TD-19 · LC-07, SH-DATA-03 | S1 books two distinct overlapping slots, preferably different tutors | Hold S1 lock, establish both waiting calls, release | Exactly one success + one BOOKING_OVERLAP; one target BOOKED/ACTIVE, other AVAILABLE/no booking. Detects failure to serialize across distinct slots | PG concurrent · D |
| TD-20 · LC-08, TUT-SLOT-03 | AVAILABLE slot; S1 book vs T1 cancel | Concurrent calls under controlled lock release; exercise both winner orders if practical | Book-first => ACTIVE/BOOKED and cancel INVALID_STATE; cancel-first => CANCELLED/no booking and book INVALID_STATE. Exactly one transition wins; no ACTIVE booking on CANCELLED slot | PG concurrent · D |
| TD-21 · LC-08, TUT-BOOK-02 | Future ACTIVE/BOOKED pair; S1 cancel vs T1 complete | Concurrent independent calls, controlled shared-user locking | Cancel-first => CANCELLED/AVAILABLE, complete INVALID_STATE; complete-first => COMPLETED/COMPLETED, cancel INVALID_STATE. Never mixed pair; no extra records | PG concurrent · D |
| TD-22 · LC-09, SH-AUTH-04 | Eligible create/book/cancel/complete actor; external test transaction takes documented user locks | Deactivate actual actor while mutation waits, commit; mutation resumes | ACCESS_DENIED, unchanged consultation rows; demonstrates post-lock actor recheck rather than cached precheck. Parameterize operations and actor roles | PG concurrent · D |
| TD-23 · LC-09, SH-DATA-03 | Create/book eligibility initially valid; external transaction locks users/modules in protocol order | Remove assignment or deactivate M; booking additionally deactivate T1; commit before waiter resumes | Create and booking MODULE_UNAVAILABLE with generic message; no consultation writes. Uses compliant management simulation, not an implemented Admin feature or claim about arbitrary SQL | PG concurrent · D |
| TD-24 · LC-01/03/04/09 | Operation initially time-valid; controllable Clock and blocking first row lock | While create/book/cancel waits, move N to exactly proposed/slot start; release lock | INVALID_TIME for creation; INVALID_STATE for book/cancel; no writes. Detects clock read before lock wait. Completion/cancel-AVAILABLE retain their no-future-check rules | PG concurrent · D |
| TD-25 · LC-10, SH-DATA-02, REL-01 | Valid booking/create pairs; test-only trigger selected after inspecting implementation's write order | For book/cancel-booking/complete, trigger raises ordinary noncontention SQL error on second affected write after first succeeds | Whole transaction rolls back: no orphan insertion or status mismatch; error is not TEMPORARILY_UNAVAILABLE/business success. Fresh connection sees original snapshot; drop fixture trigger in finally. No product fault-injection hook | PG failure injection · D |
| TD-26 · LC-11, SH-DATA-03 | Valid mutation; holder owns required user/slot lock, same datasource used for recovery | Establish blocking; retain lock beyond configured timeout; bound join; release, retry explicitly as new call | TEMPORARILY_UNAVAILABLE with sanitized message, no writes; subsequent new transaction succeeds. No hidden automatic retry. Use datasource with deterministic same physical-connection reuse if claiming connection recovery, otherwise claim resource/new-connection recovery only | PG timeout · D |
| TD-27 · LC-11 | Transactions configured with local lock/statement limits; synthetic supported SQL failure seam | Independently verify failure mapping for 55P03, 40P01, 40001, 57014; test config/rollback before public mapping | Safe TEMPORARILY_UNAVAILABLE, no SQL/UUID/secret in public text; unexpected SQLSTATE remains infrastructure failure. Unit mapping variants are not evidence of actual deadlock/serialization recovery; TD-26 supplies real lock-timeout evidence. Inspect local timeout reset after commit/rollback | UNIT + PG/REVIEW · D |
| TD-28 · LC-12, REL-01 | Existing domain/read/Tutor/AI tests, review-only schema and scoped changed files | Run ordinary `test postgresTest bootJar`; independently review reports/diff, reconnect after successful mutation | Retained regressions pass and JAR builds with persisted XML/HTML reports; committed rows survive connection recreation; no role imports/raw public writes, production migration/datasource activation, auth/UI/AI implementation or credentials. Reconnection alone is not full deployment restart proof | UNIT + PG + REVIEW · R/D |

## Verification order and limits

After human scope approval: write TD-01–16 first and record initial
failures, implement locked operations, then TD-17–27; run retained TD-28 checks and
independent review. Mirror existing build tags/configuration; avoid creating a
second testing framework. Record case IDs against executable tests and parameter
variants; 28 design rows are not 28 executed tests or a coverage percentage.

Reviewer must independently check SQL transaction binding and lock order alongside
race results: a finite race suite cannot prove absence of every deadlock. Existing
partial uniqueness should also be verified as an actual schema constraint, not
inferred from application methods. Forced failures and management edits belong only
to isolated fixtures. No live provider/security evaluation, HTTP/session integration,
browser behavior, credentials or production rollout is validated here. Keep the
credential decision pending; the Student owner retains shared AI infrastructure.

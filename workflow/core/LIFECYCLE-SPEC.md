# Consultation Lifecycle Increment Specification

Status: Scope approved on 21 October 2026 following the lifecycle review request. Implementation independently verified and accepted on 21 October 2026.
Date: 21 October 2026
Owner: Huanghao (platform/core and Tutor)
References: [Integration contract](../INTEGRATION-SPEC.md),
[shared technical rules](../SHARED-TECHNICAL-SPEC.md),
[Tutor requirements](../tutor/PRODUCT-SPEC.md), [increment plan](LIFECYCLE-PLAN.md).

## 1. Scope

Add the shared, transaction-safe consultation mutations to the existing core models
and PostgreSQL read repositories. This supports TUT-SLOT-02/03, TUT-BOOK-02,
shared Student booking/cancellation rules, REL-01, SEC-01, SH-DATA-03 and CORE-06.
No role endpoints/UI, credentials, AI, private notes, Admin management implementation,
production datasource wiring or production migration activation is included.

The earlier core read increment is independently verified; final human acceptance
of that implementation remains pending. Its design approval does not approve this
new increment. Credential storage also remains pending. Test-only lifecycle work
can use the existing credential-free review schema without deciding that boundary.

## 2. Proposed Java Contract

`unichope.platform.core.ConsultationLifecycle` exposes these operations:

| Method | Result |
| --- | --- |
| createSlot(UUID actorId, UUID moduleId, Instant start, Instant end) | ConsultationSlot |
| cancelAvailableSlot(UUID actorId, UUID slotId) | ConsultationSlot |
| bookAvailableSlot(UUID actorId, UUID slotId) | Booking |
| cancelActiveBooking(UUID actorId, UUID bookingId) | Booking |
| completeActiveBooking(UUID actorId, UUID bookingId) | Booking |

Implement with `platform/core/postgres/PostgresConsultationLifecycle`, an injected
DataSource/transaction manager and Clock. Services obtain actorId from trusted auth;
it is never an actor selected by a browser. Generate record IDs internally. The
server obtains current time after acquiring locks, not from caller input. Do not
retain MP2's generic withExclusiveAccess callback or add public raw save methods.
Public core repositories remain read-only.

## 3. Preconditions and Atomic Results

| Operation | Conditions rechecked under locks | Result |
| --- | --- | --- |
| Create slot | Active TUTOR actor; active assigned module; start > server now; end > start; no owned AVAILABLE/BOOKED overlap | Insert AVAILABLE slot |
| Cancel slot | Active TUTOR actor; owned AVAILABLE slot | Change slot to CANCELLED; preserve history |
| Book slot | Active STUDENT actor; future AVAILABLE slot; active TUTOR owner, active module and current assignment; no ACTIVE booking on slot or overlapping ACTIVE booking for student | Insert ACTIVE booking and set slot BOOKED together |
| Cancel booking | Active STUDENT actor; own ACTIVE booking; BOOKED slot whose start > now | Set booking CANCELLED and slot AVAILABLE together |
| Complete booking | Active TUTOR actor; own slot and ACTIVE booking/BOOKED slot | Set both COMPLETED together |

Timestamp precision clarification (independent review R1, 21 October): PostgreSQL
stores microseconds. Truncate requested slot timestamps to microseconds before
future, duration, overlap checks and persistence. Compare the normalized start
against the actual post-lock clock instant; reject a collapsed or no-longer-future
interval with INVALID_TIME. Store booking createdAt at microsecond precision.
Returned timestamps must equal persisted timestamps. This preserves the existing
strict-future/positive-duration invariants rather than relying on JDBC rounding.

Intervals are half-open; adjacency is allowed. CANCELLED/COMPLETED slots do not
block new tutor intervals. Repeated cancellation/completion is an invalid-state
failure with no new writes. Completion may occur early, as already specified.
Do not add future-time checks to cancelling an AVAILABLE slot or active-module/
assignment checks to cancelling/completing a booking when the existing specs do not
require them. The lock on a related student does not require that student to be
active for Tutor completion; only the actual actor must be an active permitted role.

## 4. Transactions and Row Locks

Use READ COMMITTED and Spring transactions. Bind all JdbcClient operations in a
mutation to that same transaction/connection. No HTTP, LLM or external effects inside.
Follow the integration operation matrix: lock users sorted by ID, modules, slots,
bookings, then any future feature rows. Discover immutable relationship IDs first;
discovery is neither authorization nor a valid state check. Re-read and validate
after locking. Do not acquire an earlier-group lock after entering a later group.
Preliminary actor validation may reject invalid callers before relationship
discovery, but never replaces fresh authoritative checks after all required locks.
Discovery of a foreign booking must not reveal its existence or participant details;
its eventual result is the same NOT_FOUND as an absent booking.

For creation, lock tutor before checking overlaps even on an empty schedule. For
booking, lock student and tutor before checking the student's overlaps across
distinct slots. For existing bookings, discover/lock both user participants before
module/slot/booking rows. Cancel-AVAILABLE needs only the Tutor/module/slot locks.
The ACTIVE-slot partial unique index supplements, not replaces, this protocol.

Admin eligibility changes must later share user/module locks. Tests may simulate
those changes using explicit test transactions and the documented protocol, without
implementing Admin features. Arbitrary SQL that ignores the lock protocol is not
claimed safe. Keep lock helpers internal to core PostgreSQL; a future note/management
integration needs its own scoped contract rather than an unrestricted callback now.

Proposed bounds: transaction-local lock_timeout 5 seconds and statement_timeout
10 seconds. Any failure rolls back the entire mutation. Map lock timeout/deadlock
or serialization failures to a sanitized temporary-unavailable result; no automatic
retry. HTTP mapping remains in the web layer. These are app proposals, not provider limits.
They bound individual waits/statements, not the total duration of a multi-statement
transaction. Roll back before mapping SQLSTATE 55P03, 40P01 or 40001; configured
statement-timeout cancellation (57014) also needs explicit sanitized handling.
Do not misclassify all database failures as contention.

## 5. Failure Contract

Propose a small LifecycleFailure with a stable code and safe message, no SQL/body
or identifiers in public diagnostic text. Core has no HTTP dependency.

| Code | Meaning; eventual HTTP mapping |
| --- | --- |
| INVALID_INPUT | Null required actor/module/slot/booking IDs or invalid non-time arguments; 400. HTTP auth rejects absent sessions before invoking core. |
| ACCESS_DENIED | No account exists for a supplied actor ID, or actor is inactive/wrong-role; auth/web distinguishes 401/403 |
| NOT_FOUND | Missing or another owner's slot/booking, indistinguishable; 404 |
| INVALID_TIME | Null start/end, nonpositive duration or nonfuture creation interval; 400 |
| MODULE_UNAVAILABLE | Creation module is missing/inactive/unassigned, or booking's related tutor/module/assignment is no longer eligible; 409 with generic unavailable message |
| SLOT_OVERLAP | Tutor's owned eligible interval overlap; 409 |
| BOOKING_OVERLAP | Student already has an overlapping ACTIVE booking; 409 |
| INVALID_STATE | Requested transition no longer valid; 409 |
| TEMPORARILY_UNAVAILABLE | Transient lock/database contention; 503 |

Missing IDs/input shape are rejected without writes. Validate actor before leaking
resource details. A failed mutation must not leave an orphan booking or inconsistent
status pair. Unexpected infrastructure exceptions must propagate for rollback and
later sanitized web handling, not be misreported as a successful business outcome.
For booking, nonfuture/non-AVAILABLE slots and existing ACTIVE booking conflicts
return INVALID_STATE; inactive/missing related tutor or module and removed assignment
return MODULE_UNAVAILABLE without disclosing which eligibility check failed. The
actor check uses ACCESS_DENIED; foreign resource ownership uses NOT_FOUND. These
clarifications resolve test-design questions and were included in the scope approved
on 21 October. The completed implementation was subsequently accepted on
21 October; see the lifecycle plan for the acceptance record.

## 6. Acceptance Checks

| ID | Required PostgreSQL evidence |
| --- | --- |
| LC-01 | Create valid future slot; reject wrong role/inactive actor, unavailable module, invalid intervals, overlap; allow adjacency and ignored historical statuses |
| LC-02 | Cancel only owned AVAILABLE slot; reject missing/foreign IDs identically and all other states; history retained |
| LC-03 | Book eligible slot; enforce current student/tutor/module/assignment and student overlap; booking/slot change atomically |
| LC-04 | Cancel only own future ACTIVE booking paired with BOOKED slot; cancelled record retained and slot released |
| LC-05 | Complete only owned ACTIVE/BOOKED pair; allow early completion; repeat/invalid attempts have no side effects |
| LC-06 | Independent connections concurrently create overlapping slots on an empty schedule: exactly one succeeds |
| LC-07 | Competing same-slot bookings: exactly one succeeds; same student on different overlapping slots: at most one succeeds |
| LC-08 | Booking versus slot cancellation, and booking cancellation versus completion: only valid state combinations persist |
| LC-09 | Eligibility changes under agreed user/module locks force waiting mutations to recheck current state |
| LC-10 | Inject a test-only database failure after a first write; both related writes roll back; no partial booking/status pair |
| LC-11 | Hold a conflicting row lock; receive TEMPORARILY_UNAVAILABLE within a bounded wait, no partial writes, and verify a subsequent transaction can still use the connection/resource |
| LC-12 | Existing unit/read/AI tests and bootJar still pass; no credentials, production migration, role imports, auth or UI implementation added |

Use fixed clocks and barriers/latches with bounded joins, not arbitrary sleep-based
races or a mock database. Separate connections must establish the intended blocking
interleaving, not merely launch threads concurrently. Use test-only DB failure
fixtures for rollback and do not add a product fault-injection API. Fail if database configuration is missing, never silently
skip. Retain normal Gradle XML/HTML, sanitized logs, revision/hash inputs, and
independent reviewer results. Fake provider tests are unrelated to these SQL guarantees.

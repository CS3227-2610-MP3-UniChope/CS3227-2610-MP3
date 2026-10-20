# Integration Specification

Status: Draft for team review; contract proposal, not an available API.
Date: 20 October 2026
References: [Product](PRODUCT-SPEC.md), [Shared Technical](SHARED-TECHNICAL-SPEC.md),
[Tutor Product](tutor/PRODUCT-SPEC.md), [Tutor Technical](tutor/TECHNICAL-SPEC.md).

## 1. Scope and Conventions

Define shared authentication and Tutor contracts plus the data needed for Student/
Admin interoperability. Their feature owners must add their own endpoint contracts
before implementation; names below do not establish unspecified role features.
There is no event bus or asynchronous domain-event protocol in the initial design.

- Base path `/api`; JSON bodies use camelCase and UTF-8.
- IDs are UUID strings. Instants are RFC 3339 UTC strings with `Z`, for example
  `2026-10-20T02:00:00Z`. Date filters are `YYYY-MM-DD` in Asia/Singapore.
- Enum values are uppercase; invalid IDs, enums, dates, unknown query parameters,
  unknown mutation fields, and invalid body types return 400.
- Successful lists return arrays, including `[]` for no results. Proposed initial
  lists are unpaginated for course-scale data; no hidden truncation. Revisit if
  measured data volume warrants pagination, changing both contract and UI.
- Content type is `application/json`. Mutation requests require the session CSRF
  header obtained from the auth endpoint. All private responses are non-cacheable.
- No mutation accepts `actorId`, `tutorId`, `studentId`, `role`, or arbitrary status
  overrides unless a separately reviewed role contract explicitly requires it.

## 2. Shared Authentication Endpoints

| Method/path | Request | Success |
| --- | --- | --- |
| GET `/auth/csrf` | None; may create anonymous session | 200 `{ "headerName": "X-CSRF-TOKEN", "token": "..." }` |
| POST `/auth/login` | `{ "email": "...", "password": "..." }` with CSRF | 200 authenticated-user object and rotated session cookie |
| GET `/auth/me` | Session cookie | 200 authenticated-user object |
| POST `/auth/logout` | CSRF and session cookie | 204; invalidate session and expire cookie |

Authenticated-user object: `{ "id": "UUID", "name": "Tutor name", "role": "TUTOR" }`.
Never return password hashes or stored credentials. Invalid/inactive login returns
generic 401 `INVALID_CREDENTIALS`; throttle returns 429 without confirming whether
an account exists. Protected missing/expired sessions return 401 `UNAUTHENTICATED`.
An authenticated wrong role returns 403 `FORBIDDEN`. Newly detected inactive
accounts return 403 `ACCOUNT_INACTIVE` and invalidate the session. Invalid/missing
CSRF returns 403 `CSRF_INVALID`; error precedence may follow the security filter
chain, but must never allow a denied operation or leak record content.

## 3. Tutor Resources and Response Shapes

`ModuleOption`: `{ id, code, name }`.

`Slot`: `{ id, moduleId, moduleCode, startTime, endTime, status }`.

`Booking`: `{ id, slotId, moduleId, moduleCode, studentName, startTime, endTime, status }`.

`Note`: `{ bookingId, content, version, updatedAt }`. A completed owned consultation
without a saved note returns `{ bookingId, content: null, version: 0, updatedAt: null }`.
Persisted versions start at 1 and increment on every successful save. Do not expose
student email or unrelated profile data in these Tutor resource shapes.

## 4. Tutor Endpoint Contracts

All paths in this table are relative to `/api/tutor`; every endpoint requires a
current active TUTOR account. List queries are inherently owner-scoped. Resource
lookups for another owner's ID behave identically to a nonexistent ID: 404.

| Method/path | Input | Success | Domain failures |
| --- | --- | --- | --- |
| GET `/modules` | None | 200 active assigned `ModuleOption[]`, ordered by code then ID | None |
| GET `/slots` | Optional `date` | 200 future AVAILABLE/BOOKED `Slot[]` | 400 invalid filter |
| POST `/slots` | `{ moduleId, startTime, endTime }` | 201 `Slot`; `Location: /api/tutor/slots/{id}` | 400 invalid time; 409 `MODULE_UNAVAILABLE`, `SLOT_OVERLAP` |
| GET `/slots/{id}` | Slot ID | 200 owned `Slot`, any status | 404 `NOT_FOUND` |
| POST `/slots/{id}/cancel` | Empty JSON object `{}` | 200 cancelled `Slot` | 404; 409 `INVALID_STATE` |
| GET `/bookings` | Optional `moduleId`, `date`, `status` | 200 `Booking[]` | 400 invalid filter |
| POST `/bookings/{id}/complete` | `{}` | 200 completed `Booking` | 404; 409 `INVALID_STATE` |
| GET `/history/slots` | Required `status=CANCELLED` or `COMPLETED`; optional `date` | 200 `Slot[]` | 400 invalid/missing filter |
| GET `/bookings/{id}/note` | Booking ID | 200 `Note` | 404; 409 `INVALID_STATE` unless COMPLETED |
| PUT `/bookings/{id}/note` | `{ content, expectedVersion }` | 200 saved `Note` | 400 invalid content/version; 404; 409 `INVALID_STATE`, `NOTE_CONFLICT` |
| POST `/bookings/{id}/note-draft` | `{ roughNotes }` | 200 draft response below | 400; 404; 409 `INVALID_STATE`, `AI_BUSY`; 429; 502/503/504 |

All slot and booking lists are ordered by startTime ascending, then ID. A valid
but unmatched module filter returns `[]` and does not reveal whether another
tutor is assigned that module. No status filter means all booking statuses.
Creation with a nonexistent/inactive/unassigned module returns the same
`MODULE_UNAVAILABLE` response without disclosing additional module details.

Note content is nonblank after trimming, at most 8,000 Unicode code points before
trimming. `expectedVersion` is a required nonnegative integer; omitted values do
not mean force-save. Initial saves require 0. Two concurrent initial saves cannot
both succeed; 409 preserves the newer saved note and contains no note content.
These numeric limits are proposed product values requiring team review.

### Draft response

```json
{
  "bookingId": "00000000-0000-0000-0000-000000000001",
  "draft": "Summary\nReviewed recursion base cases.\n\nTopics discussed\nRecursion.\n\nFollow-up\nPractise tracing two examples.",
  "requiresReview": true,
  "providerMode": "fake"
}
```

`providerMode` is `fake` in dev/test or `soc` for a real SoC response. Production
cannot enable fake mode. The backend formats validated sections into this text;
it does not save it. The frontend may offer a reviewed copy into the note editor.
The separate PUT save includes the editor's current expected version.
Rough notes are nonblank and at most 8,000 code points. Drafts are at most 8,000
code points; enforce this separately from provider token/byte limits.

For 429 `AI_RATE_LIMITED`, supply integer-seconds `Retry-After` based on the next
eligible reservation/window; provider outages/configuration return 503
`AI_UNAVAILABLE`, timeouts 504 `AI_TIMEOUT`, invalid provider output 502
`AI_INVALID_OUTPUT`. Do not forward upstream response bodies. A concurrent
generation conflict returns 409 `AI_BUSY` with `Retry-After` until its reservation
expires. No automatic client retry for generation, note saves, or slot creation.

## 5. Error Contract

```json
{
  "code": "NOTE_CONFLICT",
  "message": "This note changed. Reload before saving again.",
  "requestId": "00000000-0000-0000-0000-000000000002"
}
```

Messages are safe to display as text. Codes are stable; React must not parse message
wording. Return 400 `VALIDATION_FAILED` for other validation errors, 413
`PAYLOAD_TOO_LARGE` above the proposed 64 KiB JSON body limit, and sanitized 500
`INTERNAL_ERROR` for unexpected failures. Security/resource errors omit object
details, SQL, stack traces, provider bodies, notes, and secrets. Logs use requestId
for investigation without storing request bodies.

## 6. Logical Data Model

Domain identifiers below are UUIDs; AI quota state uses a singleton key. Tables
require corresponding indexes for owner/status/time lookups. This is a logical
contract; SQL migration details belong to the technical implementation.

| Record | Required fields and invariants |
| --- | --- |
| User | id, name, normalized unique email, role STUDENT/TUTOR/ADMIN, active, passwordHash; credential fields excluded from ordinary responses |
| Module | id, unique case-normalized code, name, active |
| TutorModule | tutorId and moduleId foreign keys, unique pair; assigned user must be a tutor |
| Slot | id, tutorId, moduleId, startTime, endTime, status; end > start; retained history |
| Booking | id, studentId, slotId, createdAt, status; user must be a student; at most one ACTIVE booking per slot |
| ConsultationNote (Tutor-owned private_notes) | bookingId primary/foreign key, nonblank content, version >= 1, updatedAt; only for COMPLETED booking owned by requesting tutor |
| AiQuotaState | one shared row serializing admission decisions, separate from consultation transactions |
| AiAttempt | requestId, actorId, task, admittedAt, reservedTokens, inFlightUntil; metadata only; shared rolling accounting |

Use a partial unique constraint/index for ACTIVE bookings by slot. A completed
booking prevents slot reuse through the lifecycle rules. Cross-row role/ownership/
overlap checks belong to locked transactions; foreign keys alone do not enforce them.
Keep note text out of admin statistics and shared booking DTOs. No persistent draft
table, chat history, file storage, or vector store is required.
Slot tutor/module relationships and booking student/slot relationships are immutable;
rescheduling by changing foreign keys is outside scope. Domain/storage contracts for
users, modules, assignments, slots, and bookings live in `platform/core`; private-note
contracts/storage live in `tutor/`. Do not add agenda/report tables just from suggested
class names. Versioning is required for notes, not automatically for every table.

## 7. Shared Consultation Transitions

All slot/booking mutations below go through `platform/core/ConsultationLifecycle`.
Note writes use Tutor-owned persistence plus shared transaction/locking support.
Neither controllers nor raw repository callers can bypass transactional validation.
Read-only lists do not acquire write locks. Domain transactions stay short and never
perform AI calls. This replaces the earlier global consultation write-guard proposal.

### Row-lock protocol

Use READ COMMITTED. Discover immutable relationship IDs as needed before locking;
discovery does not authorize access. Lock existing rows with `FOR UPDATE` in this
order: users, modules, slots, bookings, feature rows. Within each group, sort IDs.
Every operation includes its acting user's row. After locking, recheck current
actor/role/active status, ownership, related records, eligibility, and state before
writing. Missing/wrong-owner records retain the API's non-disclosure behavior.

| Operation | Rows locked, in the order above |
| --- | --- |
| Tutor creates slot | Tutor actor; module. Check assignments and AVAILABLE/BOOKED overlap after these locks, then insert. |
| Student books slot | Student actor and slot's tutor (sorted); module; slot. Check student's ACTIVE overlaps while holding the student row, then insert/update. |
| Student cancels booking | Student actor and tutor (sorted); module; slot; booking. |
| Tutor cancels AVAILABLE slot | Tutor actor; module; slot. Recheck AVAILABLE before changing it. |
| Tutor completes booking | Tutor actor and booked student (sorted); module; slot; booking. |
| Tutor saves private note | Tutor actor and booked student (sorted); module; slot; booking; existing note if any. Parent booking lock protects an absent-note insert. |
| Admin deactivates/reactivates account | Admin actor and target account (sorted), then inspect affected state while those locks are held. |
| Admin changes module eligibility/details | Admin actor; module. Check blockers after locking. |
| Admin adds/removes tutor-module assignment | Admin actor and affected tutor (sorted); module; assignment if present. Parent locks protect absent-assignment insert. |

All mutations affecting the conditions above must use this protocol, even when
implemented in a different role. Hold the tutor row during slot overlap checks and
the student row during booking overlap checks: existing matching rows may be absent,
and a per-slot unique index cannot stop the same student booking two distinct
overlapping slots. Parent-row locks serialize these cases. No global consultation
guard table or Java-only lock is required.

Never acquire more users after locking modules/slots; discover the full user set
first and revalidate immutable relationships under the locks. Bound lock/statement
waits; roll back the whole operation on deadlock/lock timeout and return sanitized
503 `TEMPORARILY_UNAVAILABLE`, with no automatic mutation retry. Test races against
real PostgreSQL; a lock order on paper is not verification of the implementation.

### Atomic transitions

| Operation | Preconditions | Atomic result |
| --- | --- | --- |
| Tutor creates slot | Active tutor and assigned active module; future interval; no owned AVAILABLE/BOOKED overlap | New AVAILABLE slot |
| Student books | Active student; future AVAILABLE slot; active assigned tutor/module; no student's ACTIVE overlap | ACTIVE booking + BOOKED slot |
| Student cancels | Owned ACTIVE booking; BOOKED slot; slot has not started | CANCELLED booking + AVAILABLE slot |
| Tutor cancels slot | Owned AVAILABLE slot | CANCELLED slot; no booking mutation |
| Tutor completes | Owned ACTIVE booking and BOOKED slot | COMPLETED booking + COMPLETED slot |
| Tutor saves note | Owned COMPLETED booking; expected note version matches | Create version 1 or replace with version + 1 |

Intervals are half-open `[start, end)`, so adjacent slots are allowed. Tutor overlap
ignores CANCELLED/COMPLETED slots. Booking history remains when a cancelled booking's
slot is booked again. No cascade deletion of consultation history.

AI generation checks active Tutor ownership/completed status before admission;
there is no database transaction spanning the provider call. Recheck authorization
before returning the result, discarding it if the session/account is no longer valid.
Client checks still discard late responses after sign-out or selection changes.

## 8. Integration Ownership and Review

Huanghao owns Tutor endpoints/UI/tests, Tutor-private notes, and `platform/core`
(shared domain, users/modules/tutor_modules/slots/bookings tables and repositories,
core migrations, and ConsultationLifecycle). The Student role owns `platform/ai`.
This is the human's corrected allocation on 20 October 2026; it does not assert
teammate approval. Proposed reviewers and other assignments remain in the shared
technical spec for team review. API paths remain `/api` as drafted;
do not independently switch to `/api/v1` without updating clients/contracts/tests. A
change affecting other roles updates this contract and its tests in the same
reviewed change. Do not silently change enum meanings, privacy, or lock protocols.
Student/Admin endpoint contracts remain their owners' responsibility.

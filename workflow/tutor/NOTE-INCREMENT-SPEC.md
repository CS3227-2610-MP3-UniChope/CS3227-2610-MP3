# Tutor Private Notes Increment Specification

Status: Implementation complete and human-accepted on 22 October 2026. Production migration activation and real session-adapter integration remain external coordination gates.
Date: 22 October 2026
Scope: TUT-NOTE-01 load/save for a tutor's own completed consultation.

References: [Tutor product](PRODUCT-SPEC.md), [Tutor technical](TECHNICAL-SPEC.md),
[integration contract](../INTEGRATION-SPEC.md), [shared technical rules](../SHARED-TECHNICAL-SPEC.md),
[Tutor REST increment](REST-INCREMENT-SPEC.md), and [core lifecycle](../core/LIFECYCLE-SPEC.md).

## 1. Goal and boundary

Allow an active tutor to load and manually save one private note for an owned,
completed booking. A successful save persists across reloads. Each save is an
explicit user action and uses compare-and-set versioning to prevent lost updates.
This increment does not generate AI drafts or add a React interface. The API must
not call `AiGateway`; Student-owned AI admission/provider work is a later increment.

The proposed HTTP contract is `GET /api/tutor/bookings/{bookingId}/note` and
`PUT /api/tutor/bookings/{bookingId}/note` as defined in Integration §4. The
authenticated actor comes from the existing trusted server identity seam. The
real auth/session adapter remains an external dependency; synthetic test identity
is not evidence of production login integration.

## 2. Requirements and observable behavior

### NOTE-01: Authorization and privacy

- Require a current authenticated, active TUTOR account for both endpoints.
- Derive tutor ID from trusted server context. Accept no actor identity from the
  request; reject unknown fields, including `actorId`, `tutorId`, and `role`.
- Authorize the booking through its slot's tutor ownership and require booking
  status `COMPLETED` before reading or writing note content.
- Missing bookings and another tutor's bookings both return the same 404
  `NOT_FOUND`; an owned non-completed booking returns 409 `INVALID_STATE`.
- A denied request performs no note read that returns private content, no write,
  and no AI/provider call. Responses are JSON, `Cache-Control: no-store`, and
  contain no SQL, note text in errors, or unrelated student details.

### NOTE-02: Load

- `GET` returns `{ bookingId, content, version, updatedAt }`.
- An owned completed booking with no note returns `content: null`, `version: 0`,
  and `updatedAt: null`; a saved note returns its text, positive version, and
  persisted update time.
- Reloading after a successful save returns the committed content/version/time.

### NOTE-03: Save and validation

- `PUT` accepts exactly `{ content, expectedVersion }`; reject missing/null/wrong
  types and unknown fields with the shared 400 `VALIDATION_FAILED` contract.
- `content` must contain 1–8,000 Unicode code points before trimming and must
  remain nonblank after trimming. Count code points, not Java UTF-16 code units.
  Trim with Java `String.strip()` on the project's Java 25 runtime and persist
  that result. This deliberately uses Java's `Character.isWhitespace` definition;
  characters such as NBSP that Java does not classify as whitespace are not
  stripped. The 8,000-code-point limit is the Tutor note contract decision for
  this increment; it does not change the separate rough-note/draft limits.
- `expectedVersion` is a required nonnegative integer. An absent note has version
  0; its first successful save creates version 1. A later save succeeds only when
  the supplied version equals the currently persisted version, then increments it
  by exactly one.
- Return 200 with the persisted `Note` on success. A stale version returns 409
  `NOTE_CONFLICT` without note content in the error and leaves the latest note
  unchanged. Invalid state returns 409 `INVALID_STATE`; wrong-owner/missing is
  404. Do not silently retry or force-save.
- On any failed save, keep the prior database note unchanged. Client editor-state
  preservation is a later React requirement; this API increment cannot verify UI
  behavior.

### NOTE-04a: Exceptions and error mapping

- Keep expected failures typed at their owning boundary. Request parsing/shape
  failures use the existing `ApiFailure` contract. Core authorization, ownership,
  state, and lock failures remain `LifecycleFailure`; use the existing Tutor HTTP
  mapping rather than introducing parallel core errors.
- Content-rule violations and stale note versions are Tutor-specific. Represent
  them with one small `TutorNoteFailure` carrying `INVALID_CONTENT` or
  `NOTE_CONFLICT`; map them at the Tutor API boundary to 400 `VALIDATION_FAILED`
  or 409 `NOTE_CONFLICT`. The same type carries `TEMPORARILY_UNAVAILABLE` for
  translated note-storage contention. Do not add note codes to `platform/core` or
  create separate exception classes for each HTTP status. Request parsing/shape
  failures continue to use the existing adapter-level `ApiFailure`.
- Note-row database contention may be translated to
  `TutorNoteFailure(TEMPORARILY_UNAVAILABLE)` only after the caller transaction has
  rolled back. Re-throw other storage failures so the existing boundary returns a
  generic 500 `INTERNAL_ERROR`. Do not catch `Exception` in the service, expose
  SQL/constraint messages, or catch and rethrow the same exception without adding
  information.
- For a core `ACCESS_DENIED`, let the joined transaction exit and roll back first;
  then recheck through `TutorAccess`. If the current account is inactive, the
  existing auth error path returns 403 `ACCOUNT_INACTIVE` and invalidates the
  session. Otherwise return 403 `FORBIDDEN`. This follows the current
  `TutorHttpService` pattern and avoids handling a response inside the transaction.
- Keep error-to-HTTP conversion in the existing Tutor exception handler. Successful
  note responses and all errors remain JSON and `Cache-Control: no-store`.

### NOTE-04: Atomicity and concurrent requests

- Keep note persistence in the Tutor role. Use PostgreSQL and the shared lock order:
  acting tutor and booked student rows sorted by UUID, module, slot, booking, then
  existing note. Re-read actor status/role, booking ownership/status, and expected
  note version after the relevant locks and before writing.
- The parent booking lock serializes first-note insertion when no note row exists;
  a unique booking key is a final invariant. Concurrent saves from the same
  version allow at most one success. A losing save gets `NOTE_CONFLICT`; the
  winner's value and incremented version remain intact.
- Write content, incremented version, and `updated_at` in one transaction. Any
  storage/commit failure rolls back all three. Do not hold a transaction across
  AI work (there is no AI work in this increment).
- Respect bounded database waits. A lock/deadlock/serialization failure returns
  sanitized 503 `TEMPORARILY_UNAVAILABLE`; do not retry mutations automatically.

### NOTE-05: Storage and operation boundary

- Add Tutor-owned private-note repository/service/HTTP support and a Tutor-owned
  table with `booking_id` as primary/foreign key, nonblank content, version >= 1,
  and persisted update time. Keep note text out of shared core models, booking
  DTOs, Admin statistics, AI quota metadata, and routine logs.
- Existing lifecycle/core DDL is not edited. The note schema is additive and
  Tutor-owned. Put its review-only test script under
  `src/backend/src/main/resources/db/tutor-review/`; tests apply it only to an
  isolated disposable schema with synthetic data. Do not select or activate a
  production Flyway version in this increment. Production migration numbering,
  placement, and activation require coordination when the shared migration path
  is approved.
- The service calls no LLM, creates no AI draft, exposes no note to another role,
  and adds no delete/history-sharing operation.

## 3. Acceptance checks

The independent test matrix must cover: empty and saved load; first save and update;
trim/blank/Unicode boundaries; malformed and extra fields; non-completed, missing,
wrong-owner, wrong-role and inactive actors; zero writes and zero provider calls on
denial; stale version; two simultaneous first saves and two simultaneous updates;
commit rollback; persistence after fresh service/request; safe errors and no-store;
and preservation of existing Tutor/core regressions. PostgreSQL is required for
locking, uniqueness and transaction assertions. No SoC key is needed.

## 4. Decisions and dependencies

1. **Length and trimming:** use the 8,000-code-point pre-trim limit and Java 25
   `String.strip()` semantics above. Update TN-05/TN-06 to test those exact rules.
2. **Migration boundary:** Tutor owns the additive private-note schema and its
   review-only script under `db/tutor-review/`. Isolated tests may apply that script
   to disposable schemas. Production migration version/path and activation remain
   a coordination gate; no production migration is created or enabled here. The
   current core/auth review scripts and PRs are not treated as merged migrations.
3. **Deactivation while waiting:** the core seam rechecks actor role/activity after
   taking the shared locks. If that check fails, the operation is denied and writes
   nothing. After transaction rollback, the Tutor service rechecks using
   `TutorAccess`: inactive accounts receive 403 `ACCOUNT_INACTIVE` with session
   invalidation; an otherwise active account receives 403 `FORBIDDEN`.
4. **Transaction/locking seam:** call the core-owned
   `ConsultationLifecycle.lockForPrivateNote(tutorId, bookingId)` inside a
   caller-owned `READ_COMMITTED` transaction using the same `DataSource` object and
   Spring transaction resource. It must be the first row-locking operation; call it
   before note-row access. The core operation returns identifier-only context and
   retains locks until the caller commits or rolls back. Tutor performs only short
   note database work before commit; it does not pass a callback into core. See
   [core note-lock seam](../core/NOTE-LOCK-SEAM-SPEC.md). This seam is implemented
   and independently reviewed on a separate unmerged branch, so verify and integrate
   that source revision before Tutor coding.
5. **Error handling:** use `ApiFailure` for request parsing/shape validation,
   `LifecycleFailure` for core outcomes, and one Tutor-specific `TutorNoteFailure`
   for note content validation, stale-version conflict, and translated note-storage
   contention. Keep mapping in the existing Tutor API advice; unexpected failures
   stay generic 500s.

Synthetic identity tests are intentionally limited to the existing test seam and do
not establish real session integration; the auth owner supplies that later.

These decisions resolve the local Tutor design questions. They do not record
teammate approval of the shared migration path or acceptance of a later
implementation. Human scope review and implementation acceptance remain separate.

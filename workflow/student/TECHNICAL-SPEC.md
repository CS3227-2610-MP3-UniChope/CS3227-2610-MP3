# Student Technical Specification

Status: Draft for review; proposed design, no implementation claim.
Owner: Phuc (Student role owner)
Date: 22 October 2026
References: [Student Product](PRODUCT-SPEC.md), [Shared Technical](../SHARED-TECHNICAL-SPEC.md),
[Integration](../INTEGRATION-SPEC.md), [Auth Technical](../auth/TECHNICAL-SPEC.md),
[Plan](PLAN.md).

## 1. Scope and Shared Dependencies

Implement STU-SLOT-01 through STU-AI-02 and US-S01 through US-S06, plus the shared
`platform/ai` infrastructure the Student role owns under the 20 October 2026
allocation. Follow shared auth/session, database, lock-order, error, and API
conventions. Student code does not invent its own users table, session format,
booking statuses, SoC credentials, or quota counters.

| Dependency | Owner | Student needs | State on `branch_student` (base `98ca801`) |
| --- | --- | --- | --- |
| Backend scaffold, CI | Huanghao (as built) | Gradle, Boot 4.0.8 MVC/Security, `postgresTest` task | Exists; CI runs `test postgresTest bootJar` on PRs to `main` |
| `platform/core` lifecycle and repositories | Huanghao | `bookAvailableSlot`, `cancelActiveBooking`, `UserRepository` | Exists; no change needed |
| `platform/auth` actor seam | Kok Seng implements; Student defines the interface | `CurrentActorProvider` (section 2) | Missing; Student adds the interface only |
| `platform/web` | Assigned per task | `ApiErrors`, `ApiFailure`, `ApiSecurityConfiguration` | Exists; Student adds a `student-api` route rule |
| Shared React shell and API client | Not yet assigned | Frontend pages | Out of scope for the booking backend changes |

Decision (Phuc, 22 October 2026, current-session request): Student reads core tables
through its own read-only repository, following `PostgresTutorReadRepository` and
shared technical spec §2. All slot and booking writes still go through
`ConsultationLifecycle`; Student code never issues INSERT/UPDATE/DELETE.

## 2. Backend Structure

Mirror the Tutor REST increment layout. Class names are the spec's proposal; the
implementer may rename them only together with this spec and the tests.

| Location | Component | Responsibility |
| --- | --- | --- |
| `platform/auth` | `CurrentActorProvider` | `Optional<UUID> currentActorId()`; the session-authenticated actor. Interface only |
| `student/` | `StudentAccess` | Resolve actor via provider, re-read stored account, enforce active STUDENT (section 3.2) |
| `student/` | `StudentReadRepository`, `PostgresStudentReadRepository` | Read-only parameterized SQL joins (section 4) |
| `student/` | `StudentReadService` | Filters, ordering, `cancellable`, ownership-scoped detail |
| `student/` | Records `ModuleOption`, `BookableSlot`, `StudentBooking` | Exact JSON shapes (section 3) |
| `student/http/` | `StudentController`, `StudentApiInput`, `StudentHttpService`, `StudentExceptionHandler`, `StudentApiConfiguration` | Routes, strict input parsing, lifecycle calls and error mapping, `@Profile("student-api")` wiring |
| `platform/web` | `ApiSecurityConfiguration` | When profile `student-api` is active, `/api/student/**` requires authentication; everything else unchanged |

Decision (Phuc, 22 October 2026, current-session request): the actor interface lives
in `unichope.platform.auth` so the auth owner implements it once for every role.
Do not edit `unichope.tutor.CurrentActorProvider` or any other Tutor file; Tutor can
migrate later at Huanghao's choice.

Dependencies flow controller -> service -> read repository / lifecycle. `student/`
imports only `platform/*`; platform never imports `student/`. The actor is the
request-scoped authenticated ID, re-read for current role and active state on every
request; IDs in bodies, queries or headers never select the actor.

Activation follows `tutor-api`: default startup exposes no Student route. With
`student-api` active, missing provider, read repository, lifecycle, `Clock` or
`DataSource` beans fail startup; there is no anonymous or main-source test actor.
Production activation waits for the auth owner's real provider and the production
migration, as for Tutor.

## 3. Student API

All paths are relative to `/api/student` and require a current active STUDENT.
Unsafe methods require CSRF. Unknown query parameters and body fields return 400.
Lists return arrays ordered by `startTime` then `id`, unpaginated, `[]` when empty.

```text
ModuleOption   { id, code, name }
BookableSlot   { id, moduleId, moduleCode, moduleName, tutorName, startTime, endTime }
StudentBooking { id, slotId, moduleId, moduleCode, moduleName, tutorName,
                 startTime, endTime, status, cancellable }
AgendaDraft    { bookingId, agenda, requiresReview: true, providerMode: "fake" | "soc" }
```

| Method/path | Input | Success | Domain failures |
| --- | --- | --- | --- |
| GET `/modules` | None | 200 active `ModuleOption[]`, ordered by code then ID | None |
| GET `/slots` | Optional `moduleId`, `date` | 200 `BookableSlot[]` | 400 invalid filter |
| POST `/bookings` | `{ slotId }` | 201 `StudentBooking`; `Location: /api/student/bookings/{id}` | 400; 409 `SLOT_UNAVAILABLE`, `BOOKING_OVERLAP`; 503 `TEMPORARILY_UNAVAILABLE` |
| GET `/bookings` | Optional `status` (repeatable), `date` | 200 `StudentBooking[]` | 400 invalid filter |
| GET `/bookings/{id}` | Booking ID | 200 owned `StudentBooking` | 404 `NOT_FOUND` |
| POST `/bookings/{id}/cancel` | `{}` | 200 cancelled `StudentBooking` | 404; 409 `INVALID_STATE`; 503 |
| POST `/bookings/{id}/agenda` | `{ questions }` | 200 `AgendaDraft` | 400; 404; 409 `INVALID_STATE`, `AI_BUSY`; 429 `AI_RATE_LIMITED`; 502 `AI_INVALID_OUTPUT`; 503 `AI_UNAVAILABLE`; 504 `AI_TIMEOUT` |

- `/slots` returns slots with status AVAILABLE, `startTime > now`, active tutor,
  active module, and an existing tutor-module assignment. A valid but unmatched
  `moduleId` returns `[]`.
- `date` is an SGT `YYYY-MM-DD` matched against the slot's SGT start date.
- `status` on `/bookings` accepts ACTIVE, CANCELLED, COMPLETED; omitted means all.
  The UI sends `status=ACTIVE` for Upcoming and `status=CANCELLED&status=COMPLETED`
  for History.
- `cancellable` is `status == ACTIVE && startTime > now` using the injected clock.
- `SLOT_UNAVAILABLE` covers nonexistent, non-AVAILABLE, started, inactive-tutor,
  inactive-module, and unassigned slots alike. `BOOKING_OVERLAP` means the student
  holds an overlapping ACTIVE booking. Both leave all records unchanged.
- Another student's booking ID behaves exactly like a nonexistent ID: 404.
- `tutorName` is the tutor's display name only. No emails, student names, or notes
  appear in any Student DTO. JSON objects contain exactly the listed fields.
- `SLOT_UNAVAILABLE` is a new Student error code; `BOOKING_OVERLAP` already exists
  as a core `LifecycleFailure` code. Both are Student-local contracts in this spec;
  adding `SLOT_UNAVAILABLE` to the integration spec's shared list needs team review
  but does not block Student implementation.

### 3.1 Input rules

Carry forward the Tutor REST input rules (`workflow/tutor/REST-INCREMENT-SPEC.md` §2):
any violation returns 400 `VALIDATION_FAILED` unless stated otherwise.

- IDs (path, query, body) are canonical 8-4-4-4-12 hexadecimal UUID strings.
- `date` is exactly `YYYY-MM-DD` and a real calendar date.
- `GET /modules`, `GET /bookings/{id}`, and both POSTs accept no query parameters.
- `GET /slots` accepts only `moduleId` and `date`, each at most once.
- `GET /bookings` accepts only `status` (zero or more times; each ACTIVE, CANCELLED,
  or COMPLETED; repeated equal values are harmless) and `date` at most once.
- POST bodies need `Content-Type: application/json` (else 415
  `UNSUPPORTED_MEDIA_TYPE`) and at most 65,536 bytes read from the stream (else 413
  `PAYLOAD_TOO_LARGE`, regardless of `Content-Length`). Malformed JSON, duplicate
  keys, trailing tokens, or a non-object body are 400.
- `POST /bookings` body is exactly `{ "slotId": "<uuid string>" }`. Any other or
  extra field (for example `studentId`, `status`, `role`) is 400, not ignored.
- `POST /bookings/{id}/cancel` body is exactly `{}`.

### 3.2 Identity and access

Every operation, before reading any resource, resolves the actor from
`platform.auth.CurrentActorProvider` and re-reads the stored account:

| Condition | Response |
| --- | --- |
| Provider returns empty | 401 `UNAUTHENTICATED` |
| Actor ID not found in `users` | 401 `UNAUTHENTICATED`; clear security context and invalidate session |
| Account inactive | 403 `ACCOUNT_INACTIVE`; clear security context and invalidate session |
| Active account whose stored role is not STUDENT | 403 `FORBIDDEN` (authorities in the session are ignored) |
| Unsafe request without valid CSRF token | 403 `CSRF_INVALID` from the filter chain; no write |

If the lifecycle throws `ACCESS_DENIED` (account changed between the access check
and the locked transaction), re-run the access check and return its 401/403; if the
account is somehow still valid, return 403 `FORBIDDEN` without invalidating the
session (the generic lifecycle result alone never invalidates, per Integration §2).
This matches Tutor (`TutorHttpService.mutate`).

Evaluation order (clarification, analyst-2 finding F2-06; matches `TutorController`):
filter-chain denials come first (no authenticated principal 401, missing/invalid CSRF
403); then request syntax (query parameters, path ID, content type, body size and
body shape: 400/413/415); then the actor check above; only then any resource read or
lifecycle call. A syntax error therefore returns 400/413/415 even for a principal
that would fail the actor check, and causes no read, write or session invalidation.

### 3.3 Error mapping

| Operation | Lifecycle code or condition | Response |
| --- | --- | --- |
| Book | `NOT_FOUND`, `INVALID_STATE`, `MODULE_UNAVAILABLE` | 409 `SLOT_UNAVAILABLE`, message "This slot is no longer available. Refresh the list and choose another." |
| Book | `BOOKING_OVERLAP` | 409 `BOOKING_OVERLAP`, message "This slot overlaps one of your active bookings." |
| Book | Unique-index violation on `bookings_one_active_per_slot` | 409 `SLOT_UNAVAILABLE` |
| Cancel | `NOT_FOUND` (missing or another student's booking) | 404 `NOT_FOUND` |
| Cancel | `INVALID_STATE` (not ACTIVE, slot not BOOKED, or started) | 409 `INVALID_STATE` |
| Both | `INVALID_INPUT` | 400 `VALIDATION_FAILED` |
| Both | `TEMPORARILY_UNAVAILABLE` | 503 `TEMPORARILY_UNAVAILABLE` |
| Both | `ACCESS_DENIED` | Section 3.2 |
| Both | Any other lifecycle code | 500 `INTERNAL_ERROR` (not reachable from current core; never echo the core code) |
| Any | Unexpected exception | 500 `INTERNAL_ERROR`, fixed message, no SQL, IDs or stack text |

Clarifications (analyst-2 findings F2-07, F2-08):

- The unique-index row applies when `bookAvailableSlot` throws a Spring
  `DataIntegrityViolationException` (including `DuplicateKeyException`) whose message,
  or the message of any exception in its cause chain, contains
  `bookings_one_active_per_slot`. Any other `DataAccessException` escaping the
  lifecycle is an unexpected exception (500). Core makes this path unreachable in
  normal operation (the slot row lock and ACTIVE-booking recheck run first); the
  mapping is defensive and is verified with a lifecycle test double.
- When a booking attempt is both no longer available and overlapping, the response
  is 409 `SLOT_UNAVAILABLE`: core checks slot state and eligibility before the
  student's overlap (`PostgresConsultationLifecycle.bookAvailableSlot`).

Every response, success or error, has `Cache-Control: no-store`. Errors use exactly
`{ code, message, requestId }` from `ApiErrors`, including 405 `METHOD_NOT_ALLOWED`
(with `Allow`), 406 `NOT_ACCEPTABLE`, and 404 `NOT_FOUND` for unknown
`/api/student/**` routes, written as JSON even when `Accept` excludes JSON.

After a successful lifecycle call the transition has committed. If building the
response then fails, return 500 without retrying; the booking or cancellation stays
committed and a later GET shows it. Do not describe this as a rollback.

## 4. Persistence and Concurrency

Student adds no migration and no table. It reads `users`, `modules`, `tutor_modules`,
`slots`, and `bookings` from the core review DDL (`db/core-review/V1`).

`PostgresStudentReadRepository` uses parameterized `JdbcClient` SQL only, no locks:

- **Bookable slots:** `slots s JOIN users t ON t.id = s.tutor_id JOIN modules m ON
  m.id = s.module_id` where `s.status = 'AVAILABLE'`, `s.start_time > :now`,
  `t.active`, `t.role = 'TUTOR'`, `m.active`, and a `tutor_modules` row exists for
  `(s.tutor_id, s.module_id)`; optional `s.module_id = :moduleId`; SGT date applied
  as the half-open UTC range `[date 00:00 SGT, next day 00:00 SGT)`. Order
  `s.start_time, s.id`.
- **Active modules:** `modules` where `active`, order `code, id`.
- **Student bookings:** `bookings b JOIN slots s JOIN modules m JOIN users t` where
  `b.student_id = :actor`; optional status set and SGT date on `s.start_time`.
  Order `s.start_time, b.id`. Detail adds `b.id = :id` to the same owner condition,
  so another student's booking is simply not found.

Writes use the existing core methods, which already implement the integration
row-lock protocol, rechecks, and atomic two-row updates:

- `ConsultationLifecycle.bookAvailableSlot(actorId, slotId)`: checks STUDENT actor,
  slot AVAILABLE and future, tutor/module eligibility, no ACTIVE booking on the slot,
  and no overlapping ACTIVE booking for the student (half-open), then inserts the
  ACTIVE booking and sets the slot BOOKED.
- `ConsultationLifecycle.cancelActiveBooking(actorId, bookingId)`: checks ownership
  (`NOT_FOUND` otherwise), booking ACTIVE, slot BOOKED, slot start after now; then
  sets the booking CANCELLED and slot AVAILABLE.

Core already covers lock ordering, races, and rollback in its own PostgreSQL tests.
Student tests verify the Student HTTP contract end to end against real PostgreSQL
and the Student-visible races (S-BOOK-4, S-CANC-3); they need not re-test core SQL.
The response projection after a write re-reads through `StudentReadRepository`.

## 5. Agenda (STU-AI-01/02) Contract

### Input and authorization

Only `questions` is accepted from the browser: a string, at most 4,000 Unicode code
points before trimming, nonblank after trimming. Validate code points, not UTF-16
length. Before admission, confirm current active STUDENT, ownership (404 otherwise),
booking ACTIVE, and slot `endTime > now` (409 `INVALID_STATE` otherwise). Read the
module code and consultation length in whole minutes, close the transaction, then
call the gateway. After the call, recheck the session's actor is still the same
active student and the booking is still owned; otherwise discard the result.

### Prompt

Prompt version `STU-AGENDA-1`, kept in source with its tests. Fixed instructions
tell the model to:

- organize only the supplied questions into an agenda for a consultation of N minutes;
- not answer the questions or provide solutions to assignment or exam content;
- not invent topics, tutor claims, grades, or assessment details;
- return only a JSON object matching the schema below.

Task data sent to the provider: the questions (marked as untrusted data, separated
from instructions using the verified provider's role separation where supported),
the module code, and the length in minutes. No names, emails, IDs, times, or notes.

### Output validation

```json
{
  "goal": "1-500 code points",
  "items": [ { "topic": "1-120", "question": "1-500", "minutes": 1 } ],
  "prepare": [ "1-300 code points each" ]
}
```

- Exactly these keys at each level; reject extra, missing, or wrong-typed fields.
- `items`: 1 to 8 entries; `minutes` integers >= 1; their sum <= consultation length.
- `prepare`: 0 to 5 entries.
- Reject blank strings, HTML tags, and control characters other than newline.
- Reject raw provider responses over 64 KiB before parsing.

Format the validated object into plain text, at most 4,000 code points:

```text
Goal
<goal>

Agenda (<length> min)
1. <topic> (<minutes> min)
   <question>

Prepare
- <prepare item>
```

Omit the Prepare section when empty. Any validation failure returns 502
`AI_INVALID_OUTPUT`; never call a second LLM to repair output. Structural checks
cannot prove grounding or that the model avoided answering; evaluate that separately.

### Limits

Student task policy: one outstanding request per student, 3 admitted requests per
rolling 5 minutes, 30-second overall deadline, configured output-token cap. Shared
provider budgets and in-flight limits apply on top. Failed admitted calls consume
their reservation. No automatic retry. Fake mode is labelled; production rejects it.

## 6. Shared AI Infrastructure (`platform/ai`)

Owned by the Student role. Implements SH-AI-01 through SH-AI-09. Role services own
their prompts, context selection, and output validation; `platform/ai` owns
everything provider-facing and quota-related. Agree this contract with Huanghao
(Tutor) and Kok Seng (Admin) before they integrate.

### Gateway contract

```java
public interface AiGateway {
    AiResult generate(AiRequest request);
}

public record AiRequest(
    UUID actorId,              // from trusted session code only
    AiTask task,               // TUTOR_NOTE_DRAFT, ADMIN_ACTIVITY_REPORT, STUDENT_AGENDA
    String instructions,       // fixed, versioned task instructions
    String untrustedInput,     // task data; never interpreted as instructions
    int maxOutputTokens) {}

public sealed interface AiResult {
    record Success(String text, ProviderMode mode) implements AiResult {}
    record Failure(AiFailure kind, Duration retryAfter) implements AiResult {}
}

public enum AiFailure { BUSY, RATE_LIMITED, UNAVAILABLE, TIMEOUT, INVALID_RESPONSE }
public enum ProviderMode { FAKE, SOC }
```

Each `AiTask` maps to a configured `AiTaskPolicy { perActorLimit, window, deadline,
maxInputCodePoints, maxResponseBytes }`; all three current tasks use 3 per 5 minutes
and 30 seconds. `platform/web` maps failures to the shared HTTP codes: BUSY 409
`AI_BUSY` + `Retry-After`, RATE_LIMITED 429 + `Retry-After`, UNAVAILABLE 503,
TIMEOUT 504, INVALID_RESPONSE 502. Role services add their own 502 for
task-specific output validation. The gateway has no database, file, network,
shell, or callback access for the model, and no browser parameter selects a provider.

### Admission and quota

- Tables (migration coordinated with Huanghao): `ai_quota_state` (singleton row
  locked to serialize admission) and `ai_attempt` (`request_id`, `actor_id`, `task`,
  `admitted_at`, `reserved_tokens`, `in_flight_until`). Metadata only; no prompt or
  output text.
- Admission transaction: lock `ai_quota_state`; reject if the actor has an unexpired
  in-flight attempt (BUSY), if the actor's admitted attempts in the task window reach
  the limit (RATE_LIMITED), or if provider-wide request/token budgets or the shared
  in-flight maximum (proposed 1) are exhausted; otherwise insert the attempt with
  `in_flight_until = now + 60s` and commit before calling the provider.
- `retryAfter` is the time until the blocking reservation expires or the oldest
  attempt leaves the window, rounded up to whole seconds.
- On completion, clear `in_flight_until`; on crash or disconnect the lease expires
  by itself. Never refund a reservation. Prune attempts older than the longest window.
- Quota-store errors fail closed (UNAVAILABLE) without affecting non-AI features.

### Providers

- `FakeProvider` (dev/test only): deterministic, task-keyed valid outputs plus a
  programmable test double that can simulate timeout, error, malformed JSON, HTML,
  oversized, and empty responses. Makes no network calls and reads no secrets.
- `SocLlmProvider`: server-side HTTP client using `SOC_LLM_API_URL`,
  `SOC_LLM_API_KEY`, `SOC_LLM_MODEL`; connect/read timeouts inside the overall
  deadline; response body capped at 64 KiB; upstream bodies never forwarded or logged.
  Request format, auth header, and token accounting are deferred until the SoC
  endpoint is verified.
- `unichope.ai.provider = fake | soc | disabled`. Production startup fails if set to
  `fake`. `soc` without complete configuration and budgets behaves as `disabled`
  (UNAVAILABLE, zero calls).

## 7. React Workspace and State

Routes inside the authenticated Student layout: `/student/slots`,
`/student/bookings`, `/student/bookings/:bookingId/agenda`. Use the shared API
helper for credentials, CSRF, JSON, and 401/403 handling. Route guards are
presentation only.

- **Slots page:** owns module/date filters in URL query parameters; fetches
  `/modules` and `/slots`; confirmation dialog for Book; on `SLOT_UNAVAILABLE` or
  `BOOKING_OVERLAP`, show the message and refresh the list. No optimistic updates.
- **Bookings page:** Upcoming/History tabs and filters in query parameters; Cancel
  shown only when `cancellable`; confirmation before cancel; refresh after success
  or conflict.
- **Agenda page:** loads `GET /bookings/{id}`; keeps questions, draft text, draft
  dirty flag, current account ID, booking ID, and a request sequence number in
  component memory only. Ignore responses whose account, booking, or sequence no
  longer match. Confirm before regenerating over an edited draft. Copy uses the
  Clipboard API with a visible fallback. Count length in code points
  (`[...text].length`). Show a "Fake AI output" badge when `providerMode` is `fake`.
- On sign-out or account change, clear all Student state. Session expiry follows
  SH-WEB-03: keep unsaved questions in memory only for the same actor. Warn on
  navigation away with unsaved questions or an edited draft. Never use
  local/session storage for private data.
- Render all server and model text as ordinary React text. Use Bootstrap modals with
  focus management and `aria-live` regions for status messages.

## 8. Verification Mapping

| Requirement | Verification |
| --- | --- |
| STU-SLOT-01 | Full MockMvc-to-PostgreSQL: eligibility exclusions, filters, SGT boundary, ordering, safe DTO fields |
| STU-BOOK-01 | MockMvc-to-PostgreSQL tests through the real lifecycle (`postgresTest`, see below): success, every `SLOT_UNAVAILABLE` cause, overlap/adjacent, concurrent same-slot, concurrent overlapping slots for one student, rebooking after cancel; unique-violation mapping with a lifecycle test double |
| STU-BOOK-02 | Owner scope, status/date filters, `cancellable` with fixed clock |
| STU-BOOK-03 | Atomic cancel, wrong owner 404, started/COMPLETED/repeat 409, race with tutor completion |
| STU-AI-01 | Fake call counter is zero on every rejection; exact provider input fields; output validator cases; formatting; post-call recheck discards on deactivation; React late-response and regenerate-confirm tests |
| STU-AI-02 | Admission races across sessions and roles, lease expiry, restart durability, deadlines, every failure mapping, manual features unaffected |
| `platform/ai` | Gateway unit tests; admission tests on PostgreSQL; `SocLlmProvider` against a local mock HTTP server (headers, mapping, errors, oversized/malformed bodies, timeouts); production `fake` startup failure |
| Shared SEC/AUTH | 401/403/CSRF/inactive on every Student endpoint; logs contain no question text |

PostgreSQL-backed tests follow the existing `postgresTest` convention (tag
`postgres`, explicit `-Dcore.test.*` properties, unique schema per test). Locally,
start the disposable database in `src/backend/compose.test.yaml`; CI uses its own
PostgreSQL 17 service. Do not substitute H2/SQLite. A real-provider evaluation with synthetic questions (normal, vague,
injection, "answer my exam") is separate and later. No tests have been run.

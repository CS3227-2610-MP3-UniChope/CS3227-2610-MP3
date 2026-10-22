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

| Dependency | Owner | Student needs |
| --- | --- | --- |
| Backend/frontend scaffold, CI | Team to assign | Build, test, and Testcontainers harness |
| `platform/core` tables, repositories, `ConsultationLifecycle` | Huanghao | `bookSlot` and `cancelBooking` transitions; bookable-slot and student-booking read queries |
| `platform/auth` | Kok Seng | Request-scoped current actor, active/role revalidation, CSRF |
| `platform/web` | Assigned per task | Shared error envelope, request IDs, `no-store` |
| Shared React shell and API client | Not yet assigned | Authenticated layout, CSRF-aware fetch helper, 401/403 handling |

Where the Student role needs a core read query or lifecycle method that does not
exist yet, propose it to Huanghao as an interface addition; do not write raw SQL
against core tables from `student/` to bypass the lifecycle.

## 2. Backend Structure

`src/backend/src/main/java/unichope/student/` with matching test packages:
`StudentController`, `StudentService`, `AgendaController`, `AgendaService`,
`AgendaPrompt` (versioned prompt and parser), and `dto/`. Read queries come from
core repository interfaces; Student adds no tables in this scope.

| Component | Responsibility |
| --- | --- |
| Student controller | Map endpoints in section 3, validate shapes/filters, return DTOs |
| Student service | Active Student checks, filters, `cancellable` computation, calls lifecycle |
| `ConsultationLifecycle` (core) | Locked, rechecked `bookSlot` / `cancelBooking` transactions |
| Agenda service | Ownership/eligibility check, prompt build, gateway call, output validation, post-call recheck |
| `platform/ai` | Gateway, providers, admission/quota, deadlines, typed failures (section 6) |

Dependencies flow controller -> service -> core interfaces / lifecycle / gateway.
`student/` imports only `platform/*`; platform never imports `student/`. The actor is
the request-scoped authenticated ID, re-read for current role and active state; IDs
in bodies are never trusted.

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
  appear in any Student DTO.
- New error codes `SLOT_UNAVAILABLE` and `BOOKING_OVERLAP` are Student additions to
  the shared error contract; record them in the integration spec in a reviewed change.

## 4. Persistence and Concurrency

Student adds no migration. It relies on the core logical model: Booking
`{ id, studentId, slotId, createdAt, status }` with a partial unique index on
ACTIVE bookings per slot, and Slot status/tutor/module.

Booking follows the integration row-lock protocol under READ COMMITTED:

1. Read the slot's immutable `tutorId` and `moduleId` without locks (discovery
   only; a missing slot returns `SLOT_UNAVAILABLE`).
2. Lock the student and tutor user rows, sorted by ID; then the module row; then the slot.
3. Recheck: actor is an active STUDENT; tutor active and TUTOR; module active;
   assignment exists; slot AVAILABLE and `startTime > now`.
4. With the student row held, check for an ACTIVE booking of this student whose
   slot satisfies `newStart < existingEnd AND newEnd > existingStart`.
5. Insert the ACTIVE booking and set the slot to BOOKED in the same transaction.

The student-row lock serializes overlap checks across different slots, which the
per-slot unique index cannot. A unique-index violation is mapped to
`SLOT_UNAVAILABLE`, not 500.

Cancellation locks student and tutor (sorted), module, slot, booking. It rechecks
actor, ownership, booking ACTIVE, slot BOOKED, and `startTime > now`, then sets the
booking CANCELLED and the slot AVAILABLE. Repeats return 409 with no writes.

Both operations live in `ConsultationLifecycle` (core). Student service calls them
and maps their typed outcomes to HTTP. Bound lock/statement waits; on deadlock or
lock timeout roll back and return 503 `TEMPORARILY_UNAVAILABLE`, never retrying.
Lists use plain reads with no write locks.

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
| STU-BOOK-01 | Lifecycle integration tests on Testcontainers PostgreSQL: success, every `SLOT_UNAVAILABLE` cause, overlap/adjacent, concurrent same-slot, concurrent overlapping slots for one student, rebooking after cancel, unique-violation mapping |
| STU-BOOK-02 | Owner scope, status/date filters, `cancellable` with fixed clock |
| STU-BOOK-03 | Atomic cancel, wrong owner 404, started/COMPLETED/repeat 409, race with tutor completion |
| STU-AI-01 | Fake call counter is zero on every rejection; exact provider input fields; output validator cases; formatting; post-call recheck discards on deactivation; React late-response and regenerate-confirm tests |
| STU-AI-02 | Admission races across sessions and roles, lease expiry, restart durability, deadlines, every failure mapping, manual features unaffected |
| `platform/ai` | Gateway unit tests; admission tests on PostgreSQL; `SocLlmProvider` against a local mock HTTP server (headers, mapping, errors, oversized/malformed bodies, timeouts); production `fake` startup failure |
| Shared SEC/AUTH | 401/403/CSRF/inactive on every Student endpoint; logs contain no question text |

Use Testcontainers PostgreSQL for locking and constraint behavior; do not substitute
H2/SQLite. A real-provider evaluation with synthetic questions (normal, vague,
injection, "answer my exam") is separate and later. No tests have been run.

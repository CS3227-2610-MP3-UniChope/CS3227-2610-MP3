# Shared AI Gateway Contract Proposal

Status: API/wrapper, PostgreSQL admission, shared runtime wiring, and V5 are
integrated on `feat/tutor-booking-completion`. Tutor retry accounting and V6 are
implemented and PostgreSQL-tested; Core/role-owner review remains pending before
production use.
Author: Huanghao (Tutor role contribution).  
Date: 22 October 2026.

## Purpose

Give role features one shared call boundary that performs policy checks and
admission before a server-selected provider call. Role services still authorize
the actor/resource first, choose only permitted task context, and validate
task-specific output. The model has no database, file, shell, browser, callback,
or mutation capability.

This proposal builds on the Student technical design in
[`workflow/student/TECHNICAL-SPEC.md`](../student/TECHNICAL-SPEC.md) §6 and
extends the existing provisional `AiGateway` consumers without replacing their
role-specific prompts or validators.

## API shape

- `AiTaskPolicy` registers a trusted task string, server-side `AiProviderKey`
  alias, per-actor rolling limit, input code-point limit, output-token maximum,
  response byte maximum, and deadline.
- `AiAdmissionService.admit` receives only actor ID, task, role-key alias, and
  reserved output tokens. It must atomically enforce the task's per-actor window
  and in-flight bound, provider-key request budgets and configured output-token
  reservations, and a per-key in-flight budget. It returns an opaque reservation or a typed failure. It must
  fail closed when its store is unavailable.
- `AiProviderRouter` selects a configured provider using the server-owned role-key
  alias. Browser input never chooses a key, provider, or model. Role aliases are
  routing identifiers; credential values remain in deployment configuration.
  Provider resolution is a local configuration lookup and happens before admission.
  An unavailable route returns `UNAVAILABLE` without creating a quota reservation
  or making an outbound provider call.
- `AdmittedAiGateway` validates registered task/input/token bounds, resolves the
  provider route, admits before dispatch, enforces the configured call deadline,
  checks the returned UTF-8 text size, and runs task output validation before
  recording actor success when the caller supplies a validator. The Tutor draft
  caller does this, so malformed Tutor output clears the actor quota charge while
  preserving provider-key request/token usage. Legacy one-argument callers have no
  task validator; their bounded provider result counts as accepted unless they
  migrate to the validator overload. On timeout/interruption the gateway retains
  the lease until expiry because provider completion is uncertain. It never retries
  automatically.
- `AiGateway.Failure` includes BUSY and RATE_LIMITED with an optional positive
  `retryAfter`, plus UNAVAILABLE, TIMEOUT, and INVALID_OUTPUT. Failures carry no
  upstream body or exception cause.

The shared admission implementation must persist metadata only (request ID,
actor, task, role-key alias, admission time, reserved output-token budget, actor
quota status, and lease expiry), never prompt or output text. The proposed Tutor
task limit is 3 usable drafts per Tutor in a rolling 5 minutes and one in-flight request per Tutor, with a
20-second task deadline. The provider request and output-token reservation budgets
are required server configuration; production values remain unset until SoC limits
and input-token accounting are verified. The stored output-token reservation is
an application admission estimate, not proof of actual provider billing.

## Implemented and tested in this branch

`platform/ai/` now contains `AiTaskPolicy`, `AiProviderKey`, `AiProviderBudget`,
`AiAdmissionService`, `AiProviderRouter`, and `AdmittedAiGateway`; the PostgreSQL
adapter is in `platform/ai/postgres/PostgresAiAdmissionService`. The isolated
review schema is at `db/ai-review/V1__create_ai_admission.sql`; runtime V5 is
already active through normal Flyway startup, and this change adds V6 for the
actor-quota outcome flag.

The adapter serializes admission on a singleton quota row, applies per-actor/task
rolling successful-draft and in-flight limits, applies per-role-key request and
reserved output-token windows and in-flight limits, retains failed attempts in the
provider-key budget without charging the actor success limit, expires leases,
prunes out-of-window metadata, and maps database errors to sanitized
UNAVAILABLE. Waiting for the shared quota row is capped at 500 ms and fails
closed as UNAVAILABLE to bound admission contention. PostgreSQL tests verify rolling limits, independent actors/tasks,
per-key reservations, concurrent admission, lease expiry, and fail-closed storage
errors. Wrapper unit tests still cover routing, request bounds, output-size
rejection, sanitized failures, and timeout/no-retry behavior.

Provider resolution now runs before admission. If the router reports a missing role
provider, the wrapper returns sanitized `UNAVAILABLE` without invoking the admission
service; route resolution itself must remain a local configuration lookup. Wrapper
tests verify the resolution order and that a missing route creates no reservation.

## Compatibility and activation gates

The existing `AiGateway` method signature remains in place, so current role
consumers continue to compile. Production integration must inject
`AdmittedAiGateway` as the shared `AiGateway`; directly injecting a provider
adapter bypasses admission. PR #10 added the shared `app-api` runtime bean and put
the admission schema in the normal Flyway sequence. Local Tutor trials should use
that shared bean with `unichope.ai.mode=shared`, `SOC_LLM_API_KEY_TUTOR`, and
conservative environment budgets. Production use still requires owner review of
the contract, migration activation, provider budgets, environment secrets, and
fake-provider rejection.

The wrapper's response-size check runs after a provider returns a Java string.
The real HTTP provider must separately cap raw response bytes while reading the
response body. Do not treat this wrapper check as transport-level protection.

## Required review

The Student owner should verify compatibility with `AiTask`, task registration,
and the configured per-key budget model; the Admin owner should review task
routing and typed failure mapping; the Auth owner should review how role services
supply trusted actor IDs and revalidate sessions; the core owner should review
any later AI schema version. These reviews are pending. This proposal does not
allocate ownership or imply teammate acceptance.

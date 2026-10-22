# Tutor AI Note-Drafting Increment

Status (23 October 2026): Rough-note validation, output parsing, shared admission,
the Tutor draft endpoint/service, completed-booking list API, and React note/draft
state are implemented. PostgreSQL-backed MockMvc tests
use a test-only `AuthenticationService`; a separate `app-api,local` PostgreSQL
integration verifies production `AuthenticationService` and account storage for
Tutor login, anonymous/wrong-role denial, session-generation revocation, and
deactivation during generation. Frontend tests cover explicit copy/save, failure
retention, replacement confirmation, selection changes, and stale-response discard.
Production provider wiring, active migration, and affected-owner acceptance remain
pending.
Requirement: TUT-AI-01 and TUT-AI-02.
Owner: Huanghao (Tutor); shared gateway/admission remains Student-owned.
Date: 23 October 2026.
Related: [Tutor product requirements](PRODUCT-SPEC.md),
[Tutor technical specification](TECHNICAL-SPEC.md),
[integration contract](../INTEGRATION-SPEC.md#8-integration-ownership-and-review).

## 1. Purpose and boundary

Allow a Tutor to turn their own rough consultation notes into a structured,
editable suggestion for a completed booking. The backend authorizes the Tutor,
validates input, obtains a shared AI admission, calls the shared gateway, validates
the model output, and rechecks authorization. Generation never saves a private
note or changes consultation state. The existing manual note PUT remains the only
save path.

This is a Tutor-role increment. Tutor owns its API request validation, prompt,
output parser, Tutor-specific gateway integration/orchestration, booking/session
checks, React draft state, and tests. Tutor code builds the `AiGateway.Request`
with trusted Tutor/task context and invokes the shared interface. The Student role
owns generic `platform/ai` provider transport, durable shared admission and per-key
quota enforcement, provider deadlines, and cross-role infrastructure tests. Tutor
must not duplicate provider transport or the shared quota store, or read provider
secrets directly.

No real SoC connection, key, endpoint, or model choice is required for this
fake-backed increment. A real SoC response is a later activation/evaluation step.
Production must not label deterministic fake output as real AI.

## 2. Readiness dependencies

The user's 22 October instruction authorizes starting P6. P5 manual-note
implementation acceptance is recorded in the [acceptance handoff](../handoffs/2026-10-22-tutor-notes-human-acceptance.md).
The following shared gateway contract and PostgreSQL admission adapter are
implemented as a proposal in `platform/ai`; affected owners still need to review
it before it becomes the team contract. Runtime wiring and production migration
activation are not implemented.
The remaining cross-role decisions are:

1. The Student and Admin owners review the shared `AiTaskPolicy`,
   `AiAdmissionService`, `AiProviderRouter`, and `AdmittedAiGateway` proposal;
   register `tutor.note-draft.v1` and route it to the server-side Tutor key;
   confirm typed failure/`Retry-After` mapping and the fake-provider test seam.
   Production Tutor wiring must reject raw provider beans before dispatch and
   accept only the shared admission-enforcing gateway; a runtime guard test covers
   this boundary.
   The proposal's PostgreSQL adapter now enforces atomic per-actor windows,
   in-flight limits, and configured per-key request/output-token reservations.
   Its schema remains isolated under `db/ai-review`; production wiring and owner
   acceptance are pending.
2. The core owner and Student owner assign the AI metadata migration version if
   shared admission requires a production migration. Tutor does not create that
   shared schema.
3. The endpoint captures the authenticated `SessionPrincipal` before generation.
   After generation it calls `AuthenticationService.current(capturedPrincipal)`,
   then checks that the returned user still has the TUTOR role. This existing
   method checks account activity and session generation. The revocation and
   deactivation cases are covered by the app-api/PostgreSQL integration; the Auth
   owner's review of this shared-contract use remains open.

Until the gateway contract is settled, isolated Tutor-owned logic may be tested
against a deterministic fake. The endpoint invokes the shared interface only when
a conditional runtime bean is available. For local HTTP/browser verification,
`app-api,local` plus `unichope.ai.mode=fake` supplies a deterministic synthetic
`FakeProvider` only when no other `AiGateway` bean exists; configure a positive
output-token bound for that run. This fake has no network, key, durable admission,
or production activation. Outside the local profile, a raw provider bean is
rejected and the route remains unavailable without the shared admitted gateway.
PostgreSQL-backed MockMvc endpoint tests use a deterministic test gateway, synthetic
session principal, and test-only `AuthenticationService`; they do not verify production
provider wiring. A separate `app-api,local` integration uses the production
`AuthenticationService`, account storage, and a deterministic fake provider to verify
session establishment and post-generation revocation/deactivation checks.
The isolated PostgreSQL adapter is tested, but production does not use its schema.

The endpoint implements the response below against the proposed shared gateway
contract. The `AdmittedAiGateway` proposal accepts the existing request shape, applies registered
task policy, asks the admission service before provider dispatch, routes by
server-side role-key alias, bounds the call, and returns untrusted text or typed
failures. The Tutor task policy proposes a 20-second deadline, enforced by this
shared wrapper once registered. The durable adapter is tested against an isolated
schema, but has no production bean or active migration; the wrapper's string-size
check also does not replace a raw-response transport limit.

## 3. HTTP contract

`POST /api/tutor/bookings/{bookingId}/note-draft`

- Requires an active TUTOR session and CSRF token; actor identity comes only from
  the authenticated session. Query parameters and unknown JSON properties are
  rejected under the shared request rules.
- Body has exactly one string field: `roughNotes`. Reject blank input and input
  above 8,000 Unicode code points before admission. Enforce the shared proposed
  64 KiB JSON body cap as well; the smaller applicable limit wins. Malformed JSON,
  duplicate `roughNotes`, unknown or missing properties, wrong types, and query
  parameters return 400 `VALIDATION_FAILED`; exceeding 64 KiB returns 413
  `PAYLOAD_TOO_LARGE`.
- The path booking must be owned by the active Tutor and both booking and slot
  must be COMPLETED. Return the same 404 for absent and other-Tutor bookings;
  return 409 `INVALID_STATE` for an owned non-completed consultation.
- Complete actor/ownership/state and input checks before asking for admission or
  invoking the gateway. Do not open a database transaction across generation.
  Revalidate the captured session principal after generation, including account
  activity, role, and session generation. If the session/account is no longer
  valid, discard generated output. `TutorAccess.requireActiveTutor()` alone is
  insufficient because it does not revalidate the captured session generation.
- Send only the fixed Tutor task instruction and the supplied `roughNotes` to the
  gateway. Do not load or send saved-note content, booking/student identifiers,
  student names, account data, other consultations, or arbitrary database context.
  The trusted gateway actor/task metadata is not prompt content.
- Success is HTTP 200:

```json
{
  "bookingId": "00000000-0000-0000-0000-000000000001",
  "draft": "Summary\nReviewed recursion base cases.\n\nTopics discussed\nRecursion.\n\nFollow-up\nPractise tracing two examples.",
  "requiresReview": true,
  "providerMode": "fake"
}
```

`providerMode` is `fake` in development/test and `soc` for a real SoC result.
The draft is never persisted by this endpoint. The frontend displays it as plain
text and requires a separate user action to copy it into the manual note editor;
the normal version-checked PUT is still required to save.

Expected error contract: 400 `VALIDATION_FAILED`; 401 `UNAUTHENTICATED`;
403 `ACCOUNT_INACTIVE`/`FORBIDDEN`; 404 `NOT_FOUND`; 409 `INVALID_STATE` or
`AI_BUSY`; 429 `AI_RATE_LIMITED`; 502 `AI_INVALID_OUTPUT`; 503 `AI_UNAVAILABLE`;
504 `AI_TIMEOUT`; 413 `PAYLOAD_TOO_LARGE` for bodies above the shared 64 KiB cap.
Rate/busy responses carry `Retry-After` when specified by the
settled shared contract. Never return provider bodies, prompts, rough notes,
secrets, SQL, or stack traces in error bodies or logs. Do not parse error message
wording in React; use stable codes.

The status/code mappings above follow the current integration proposal. They
remain subject to the shared gateway owner finalizing its failure types and HTTP
mapping before implementation.

## 4. Prompt and output contract

- Use a versioned, fixed instruction in backend source. The selected task key is
  `tutor.note-draft.v1`; the Student-owned gateway must register this task and
  route it to the Tutor role's server-side key configuration.
- Request JSON with exactly three nonblank string properties:
  `summary`, `topicsDiscussed`, and `followUp`. Instruct the model to use only
  supplied facts, label missing information `Not provided`, and not invent a
  diagnosis, grade, completion claim, or follow-up commitment.
- Keep trusted instructions separate from rough notes. Treat rough notes as
  untrusted data, including direct prompt injection such as requests to reveal
  secrets, access other consultations, change a booking, or ignore the fixed task.
  The model receives no tools, database access, network access, or mutation path.
- Parse JSON with duplicate-key and trailing-content rejection. In particular,
  duplicate `roughNotes` request properties return 400 `VALIDATION_FAILED`.
  Reject missing,
  extra, wrong-type, blank, over-limit, or HTML-tag-bearing section values. Do not
  make a second model call to repair output. For this contract, reject section
  values containing `<` or `>` so no markup-like text reaches the UI. Format the accepted values as the
  three labeled plain-text sections shown in the response example.
- Count the final formatted draft in Unicode code points and reject above 8,000.
  Enforce provider raw response bytes and output-token budgets in the shared
  gateway/provider boundary. Tutor tests the formatted draft's limit; the raw-byte
  cap is a Student-owned gateway prerequisite. Carry a positive, bounded
  `maxOutputTokens` value, but select its exact value with the Student owner after
  SoC limit verification; do not invent a Tutor-specific number.
- Structural validation does not prove truthfulness. Fake-backed tests verify
  parser/orchestration/security behavior, not model quality or real-model
  grounding. Keep synthetic grounding evaluation separate and make its limits
  explicit.

## 5. Frontend state behavior

The draft controls belong with the selected completed booking and stay separate
from rough input and the saved note:

- Show a rough-notes editor and explicit Generate action only for an eligible
  completed booking. Reject blank/over-limit input in the client for usability;
  the backend remains authoritative.
- Generation shows pending state and prevents another concurrent request in this
  page. Server admission is still authoritative across tabs, sessions, and roles.
- Keep generated content editable in a distinct draft buffer. Clearly label fake
  results. Generation, failure, regeneration, or selection changes never save a
  note automatically.
- If regeneration would replace edited draft text, ask before replacement. Copying
  a reviewed draft into a dirty note editor also requires confirmation. Copying is
  not saving; the note editor retains its existing expected-version workflow.
- Preserve rough notes, draft, and saved-note editor on provider, timeout, output,
  quota, session, and version failures. No automatic generation or save retry.
- Bind every response to account, booking, and request sequence. Ignore late
  responses after selection/account changes; clear private buffers and invalidate
  pending response identities on explicit sign-out/account change.
- If the saved-note version changes during generation, the draft may still be
  reviewed, but saving it uses the note editor's original expected version. Never
  silently refresh that version or overwrite a concurrent note; keep conflict
  recovery explicit.

Final visual layout and styling remain out of scope; implement the state and
request logic in the current Tutor bookings page unless the auth integration
changes its composition.

## 6. Limits, quota, and failures

The provider's request/token quota is per API key. Keep the Tutor role's key in
server-side backend deployment configuration; the shared gateway resolves it
from trusted task routing for `tutor.note-draft.v1`. The key is never sent to
React or accepted from a request. This provider quota is separate from the
application admission policy:
allow at most 3 accepted admissions per Tutor per rolling 5 minutes, with one
in-flight generation per Tutor. Count admitted failures and timeouts, use no
automatic retries, and fail closed if durable admission is unavailable.

Use a 20-second end-to-end deadline, including response reading, for this
interactive note-draft request. Set connection/read timeouts within that overall
deadline. This is an application responsiveness limit, not an SoC guarantee; test
latency with the fake and measure real-provider latency during the later gated
evaluation before activation. The shared AI owner must implement and test
task-to-key routing, per-key quota accounting, and admission reservation against
that key's actual allowance.

On quota rejection, AI busy, timeout, unavailable provider, malformed output,
unexpected response size, or authorization recheck failure, preserve manual note
work and all unrelated Tutor operations. Rejected validation/authorization calls
must consume no admission and cause zero gateway/provider calls. Admitted failures
must follow the shared accounting decision and must not trigger client retries.

## 7. Explicit exclusions

- No Tutor-owned quota table, provider adapter, secret/configuration management,
  SoC key setup, provider endpoint/model choice, or production AI activation.
- No new note persistence, preparation-note feature, automatic saved-note context,
  note save endpoint, or booking/account mutation.
- No claim that prompting or output parsing prevents all hallucinations or proves
  grounding; human review remains required.
- No visual redesign beyond controls/states needed to exercise the behavior.

## 8. Acceptance criteria

1. The endpoint rejects invalid input and unauthorized/ineligible bookings before
   admission; tests prove zero gateway calls for each rejection class.
2. The exact trusted task, instruction, rough-note separation, and output budget
   passed to the settled gateway are tested; no actor/resource identifiers enter
   provider prompt input.
3. The parser accepts only the three-field schema and rejects malformed, injected,
   HTML-bearing, blank, duplicate, trailing, or over-limit output safely.
4. Successful generation returns an editable draft and provider mode without any
   note, booking, or account mutation. A separate explicit copy and version-checked
   save are required to persist reviewed text.
5. Quota/busy, timeout, unavailable, malformed-output, and session recheck failures
   preserve UI buffers; server-side shared quota behavior is verified by the
   Student-owned integration tests.
6. Frontend race tests prove old responses cannot be attached to another booking
   or account. Fake-mode development is labeled; production cannot claim fake
   output as real SoC capability.
7. The completed-booking list is owner-scoped and supports the specified module,
   SGT date, and status filters; the selector requests completed bookings only.
8. Test CSRF rejection and exact request/success response fields. Shared production
   wiring must reject fake-provider configuration or leave AI unavailable; the
   Student owner verifies that boundary. Run backend unit/MockMvc tests and
   frontend tests/build. No real provider call is part of this increment.

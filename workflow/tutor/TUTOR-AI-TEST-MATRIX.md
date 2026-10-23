# Tutor AI Note-Drafting Test Matrix

Status: Focused fake-backed tests cover rough-note validation (TAI-06), Tutor
request construction and prompt/data separation (part of TAI-08/09), output
parsing (TAI-10/11 and the Tutor-parser portion of TAI-12), positive output-token
budget propagation (part of TAI-25), and the proposed shared gateway's
admission-denial, routing, size-bound, timeout, and lease behavior (parts of
TAI-14/15/21/22). The full backend suite passes. An isolated PostgreSQL admission implementation
now exists; it is not wired to production or an active migration. The fake-backed endpoint/service now exists. Service/controller unit tests cover
selected authorization and output cases; provider byte-stream cap and live-model
behavior are not yet claimed. The full frontend suite passed 193 tests; UI-review
follow-up runs passed 196 after adding dirty-note confirmation, version-conflict,
AI-failure buffer-retention, and completed-status-query cases. The Chrome
Playwright suite passed all 78 tests, including the fake-API sign-in, booking
selection, draft review, and explicit save flow.
PostgreSQL-backed MockMvc
tests use a deterministic fake and test-only AuthenticationService. The app-api/local
integration suite verifies successful Tutor login, anonymous and wrong-role denial,
session-generation revocation, and account deactivation during generation against
production auth/account storage.
See the [P6 evidence register](TUTOR-AI-EVIDENCE.md).
Requirement: TUT-AI-01, TUT-AI-02, SEC-02/03, AI-02.
Specification: [Tutor AI increment](TUTOR-AI-INCREMENT-SPEC.md).

| ID | Setup and action | Expected result | Evidence layer |
| --- | --- | --- | --- |
| TAI-01 | Active Tutor submits valid rough notes for own COMPLETED booking | 200 structured draft; exact booking ID; `requiresReview=true`; provider mode shown; no writes | PostgreSQL-backed MockMvc with fake gateway and state snapshots; no private-note writes |
| TAI-02 | Missing/anonymous actor | 401; gateway/admission call count remains zero | App-api/PostgreSQL Tutor AI integration supplies anonymous session with valid CSRF and asserts zero calls |
| TAI-03 | Inactive or wrong-role actor | Safe 403; zero gateway/admission calls | App-api/PostgreSQL Tutor AI integration verifies wrong-role denial before generation; service test covers inactive account before admission; integration verifies mid-generation deactivation, output discard, and session invalidation |
| TAI-04 | Booking absent or owned by another Tutor | Indistinguishable 404; zero gateway/admission calls | PostgreSQL-backed MockMvc: foreign Tutor booking returns `NOT_FOUND` before generation |
| TAI-05 | Own booking or slot is not COMPLETED | 409 `INVALID_STATE`; zero gateway/admission calls | PostgreSQL-backed MockMvc: non-completed booking returns `INVALID_STATE` before generation |
| TAI-06 | Blank, missing, wrong-type, or over-8,000-code-point roughNotes | Safe validation error; zero gateway/admission calls | Input unit + MockMvc |
| TAI-07 | Unknown/missing/duplicate fields, query parameter, malformed JSON, wrong types, or body over 64 KiB | 400 `VALIDATION_FAILED`, except over 64 KiB is 413 `PAYLOAD_TOO_LARGE`; zero gateway/admission calls | MockMvc input tests |
| TAI-08 | Valid rough notes reach the shared gateway interface | Trusted actor metadata and task `tutor.note-draft.v1` are separate from prompt; prompt includes fixed instruction and only supplied roughNotes; output budget is propagated | PostgreSQL-backed MockMvc verifies exact response fields, task, input, actor, positive output budget, and fake mode, including a real app-api Tutor session |
| TAI-09 | Rough notes contain direct injection requesting secrets, other notes, tools, or booking changes | Fake-backed boundary test confirms notes remain input data, instructions stay fixed, no tools/actions or other data are provided, and returned output is schema-validated | Synthetic injection fixture + capturing AiGateway (does not test a live model's resistance) |
| TAI-10 | Output is valid JSON with exactly three nonblank strings | Return correctly labeled plain text; no persistence/mutations; provider mode matches result | Parser/service + MockMvc |
| TAI-11 | Output has missing/extra keys, wrong types, blank sections, duplicate keys, trailing JSON | Reject as sanitized invalid output; no repair call; no persistence | Parser unit tests + gateway call count |
| TAI-12 | Output contains HTML tags, oversized section/final draft, empty text, or provider response above byte limit | Tutor parser rejects invalid/oversized formatted draft; shared gateway rejects raw provider response above its configured byte cap; no saved note or mutation | Tutor parser tests + Student-owned gateway-boundary tests |
| TAI-13 | Injected deterministic gateway returns FAKE | Endpoint labels its result as fake locally; production rejects fake output and raw provider beans cannot bypass shared admission; no external request or secret read in tests | Tutor fake-gateway and runtime-guard tests; Student-owned production wiring remains pending |
| TAI-14 | Shared admission returns AI_BUSY or AI_RATE_LIMITED | Map stable status/code and `Retry-After`; preserve UI buffers; no automatic retry | `AdmittedAiGateway` fake verifies admission denial and zero provider calls; Tutor exception-handler test covers both HTTP mappings and retry rounding; React tests verify rough notes, edited draft, and note editor remain intact |
| TAI-32 | Provider failure, timeout, or invalid Tutor output followed by a new manual Generate action | Failed attempt does not consume the Tutor's successful-draft quota; provider-key attempt/token budgets remain charged; no automatic retry; the two-per-minute key limit still applies | `AdmittedAiGatewayTest`, `PostgresAiAdmissionPgTest`, and Tutor parser tests |
| TAI-15 | Gateway returns unavailable, timeout, or invalid-output failure | Sanitized 503/504/502 mapping; preserve rough notes/draft/saved editor; no retry | Tutor exception-handler tests cover all five stable AI failure mappings, safe messages, and `Retry-After` for busy/rate-limit; React tests verify timeout, unavailable, and invalid-output preservation |
| TAI-16 | Provider returns success, then account is deactivated or session generation is revoked before response recheck | Revalidate the captured `SessionPrincipal` with `AuthenticationService.current`, check TUTOR role, discard output and return safe auth failure; no UI draft update | Service unit tests plus app-api/PostgreSQL integration tests verify both generation revocation and account deactivation discard output and invalidate the browser session |
| TAI-17 | Tutor changes selected booking/account while generation is pending; old request completes last | Ignore old response; never attach it to new selection/account | Deferred-promise React test |
| TAI-18 | User edits draft then chooses Generate again | Require confirmation before replacing edited draft; cancel preserves text and causes no second request | React component test |
| TAI-19 | User copies reviewed draft into dirty saved-note editor | Require confirmation; copy changes editor only; no PUT until explicit Save | React component test |
| TAI-20 | User saves copied draft after concurrent note update | Use the note editor's original expected version; do not silently refresh it; conflict preserves draft and does not retry | Frontend component + existing note API test |
| TAI-21 | Two tabs/sessions for one Tutor attempt generation; requests under separate role keys also exercise their own provider quota | Per-Tutor app admission serializes concurrent requests and enforces the rolling cap; provider quota is enforced independently per role key; rejected app admission makes no provider call | Wrapper contract tests confirm role key selection and zero provider calls on denial; PostgreSQL tests verify per-Tutor concurrency and separate per-role-key budgets in an isolated schema |
| TAI-22 | Generation exceeds the 20-second end-to-end deadline, quota store fails, provider output is malformed, or reservation is exhausted | Fail closed for AI, preserve manual note workflow and other Tutor operations; no automatic retry | Fake tests verify bounded timeout/no retry/lease retention and admission-store failure before provider; PostgreSQL tests verify durable lease expiry and bounded lock-wait fail-closed behavior, but production wiring remains pending |
| TAI-23 | Explicit sign-out/account change while private buffers or request exist | Clear private buffers, invalidate request identity, ignore late result | React integration test once auth UI/event contract exists |
| TAI-24 | POST without a valid CSRF token | 403; zero gateway/admission calls | PostgreSQL-backed MockMvc verifies missing CSRF returns 403 and zero gateway calls |
| TAI-25 | Successful request uses exact allowed response fields and a positive bounded output-token budget | Response contains only `bookingId`, `draft`, `requiresReview`, and `providerMode`; exact token limit selected by Student owner after SoC verification | MockMvc contract test + capturing gateway service test |
| TAI-26 | Tutor loads completed consultations for selection | List is scoped to the authenticated Tutor, filters by status/module/SGT date, and is ordered by start time then booking ID; UI requests `status=COMPLETED` | Tutor read-service unit test, PostgreSQL MockMvc route test with distinct module and UTC/SGT boundary data, and frontend URL assertion |
| TAI-27 | Tutor reviews and edits a generated draft | Generation is explicit, fake output is labeled, and no note PUT occurs until the Tutor copies and explicitly saves | React Testing Library + Playwright with fake API; verifies sign-in, completed-booking selector, review/copy, and versioned save |
| TAI-28 | Generation fails or regeneration would replace edited draft text | Rough input and edited draft remain; replacement requires confirmation; no automatic retry | React Testing Library with deterministic fake API responses |
| TAI-29 | Tutor changes consultation while work or generation is pending | Unsaved work requires confirmation; late responses cannot appear under a different booking; rough-note limit counts Unicode code points | React Testing Library with deferred response and synthetic Unicode input |
| TAI-30 | Start `app-api,local` with `unichope.ai.mode=fake` and a positive local output bound; Tutor signs in, selects a completed booking, generates, reviews, and explicitly saves | Real HTTP session, CSRF, PostgreSQL ownership/state, fake-labeled draft, and versioned note save succeed; no real provider call | Local PostgreSQL + application runtime + browser; deterministic fake only |
| TAI-31 | Fake output says it is approved and orders the application to save or grant admin access | Output remains inert review text; no note save, completion, admin, or permission request occurs without Tutor action | `tutor.test.tsx`: approval-looking provider output causes no mutation or privileged request |

## Review boundaries and prerequisites

Exact quota error types, `Retry-After` rules, admission APIs, task-to-key routing,
token reservation calculation, provider raw-byte limit, and lease semantics depend
on the shared AI contract. Provider quotas are per configured key; the app's
separate rolling cap is per Tutor. TAI-14, TAI-15, TAI-21, TAI-22, and shared
parts of TAI-12/13/25 cannot be authoritative server tests until it is settled.
TAI-16 uses the existing `AuthenticationService.current(SessionPrincipal)` method;
the app-api integration test uses the production AuthenticationService and PostgreSQL
account storage with a deterministic provider fake. The exact output-token budget
remains undecided until the shared owner verifies SoC limits.

The deterministic fake proves request construction, orchestration, timeout and
parser boundaries, not SoC model quality, factual grounding, model resistance to
prompt injection, real provider quota compliance, or production secret handling.
Live SoC evaluation belongs to the later provider activation increment.

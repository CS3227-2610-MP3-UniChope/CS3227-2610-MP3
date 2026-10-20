# Tutor Technical Specification

Status: Draft for review; proposed design, no implementation claim.
Owner: Huanghao
Date: 20 October 2026
References: [Tutor Product](PRODUCT-SPEC.md),
[Shared Technical](../SHARED-TECHNICAL-SPEC.md),
[Integration](../INTEGRATION-SPEC.md), [Plan](PLAN.md).

## 1. Scope and Shared Dependencies

Implement TUT-SLOT-01 through TUT-AI-02 and US-T01 through US-T09. Follow shared
auth/session, database, quota, and API contracts. Tutor code cannot invent its own
user database, session format, SoC credentials, booking statuses, or quota counters.
Shared contract proposals require team review before implementation.

### Huanghao's shared contribution: AI infrastructure

In addition to the Tutor role, Huanghao owns the shared `platform/ai` implementation:
`AiGateway`, `FakeProvider`, the later `SocLlmProvider`, server-enforced per-account
and provider-wide quotas, admission persistence, deadlines, sanitized provider errors,
and infrastructure/transport tests. Coordinate AI metadata migrations with the core
schema maintainer and authentication integration with the auth maintainer.

Each role owner remains responsible for their own AI feature's prompt, authorized
context, task-specific output validation, UI, and acceptance tests. Huanghao's
infrastructure ownership does not include implementing Student/Admin AI features
or assuming ownership of core consultation persistence, authentication, or deployment.

Agree the gateway request/result/failure contract with affected role owners before
parallel implementation. Actor identity comes from trusted backend code; no browser
parameter may choose another account for quota accounting. Develop fake-first;
real SoC endpoint/model/key details remain deferred to provider activation.

This section records Huanghao's selected contribution only. The shared ownership
table has not been changed; teammates' allocations still require team agreement.

## 2. Backend Structure

Use `src/backend/src/main/java/unichope/tutor/` and matching test packages. Domain
models remain independent of HTTP/React. Proposed classes are `TutorController`,
`TutorService`, `NoteController`, `NoteService`, `PrivateNoteRepository`,
`PostgresPrivateNoteRepository`, and `dto/`. `NoteService` owns manual-note behavior
and AI drafting; do not add a separate service for each small operation.
Final names may change together with specs/tests; no extra generic hierarchy is needed.

| Component | Responsibility |
| --- | --- |
| Tutor controller | Map agreed endpoints, validate body/filter shapes, return DTOs/errors |
| Tutor service | Active Tutor/owner checks, queries, slot validation, note eligibility |
| Shared consultation lifecycle | Transactional slot creation/cancellation and booking/completion transitions; rules rechecked after row locks |
| Postgres repositories | Parameterized `JdbcClient` owner-scoped queries and row mapping; shared core storage in platform/core, private-note storage in tutor/ |
| Note service | Completed-only private notes, conditional saving, and authorized AI draft orchestration through AiGateway |

Dependencies flow controller -> service -> repository interface, lifecycle, or
gateway. Only configuration wires Postgres/provider implementations. Tutor imports
shared contracts from `platform/core`, `platform/auth`, `platform/ai`, and
`platform/web`, never Student/Admin packages. Platform does not import Tutor.
Keep `PrivateNoteRepository` and its Postgres implementation in Tutor; use shared
locking support to save notes, not a core interface that depends on Tutor types.

Use a request-scoped authenticated actor ID and re-read its current account state.
Do not retain a mutable actor in an application-wide service singleton, and do not
trust IDs in request bodies. All resource lookups use owner scope; wrong-owner and
missing IDs produce the same 404 contract.

## 3. Persistence and Concurrency

Queries join bookings to slots by ID and constrain `slot.tutorId = actorId` in SQL.
Assigned-module options include active modules only. Upcoming lists use a strictly
future start. Date-filter boundaries are computed in SGT, then queried as UTC
instants. Use explicit deterministic start/ID ordering and the injected clock.

All mutations follow the integration spec's row-lock protocol: users (IDs sorted),
modules, slots, bookings, then feature rows. Recheck current account, ownership,
eligibility, and resource conditions inside that transaction after locks. Shared
slot/booking writes go through `ConsultationLifecycle`; private-note writes use
the same lock order through shared transaction support.

Slot creation locks the tutor's user row before checking overlaps, even if the
schedule is empty, then locks the module and validates assignment. Overlap means
`newStart < existingEnd` and `newEnd > existingStart`, restricted to AVAILABLE/BOOKED
owned slots. Concurrent creation shares the tutor lock. Student booking locks the
student row before testing overlapping bookings; cancellation/completion share
slot locks. Admin eligibility mutations coordinate on the same user/module rows.

Completion requires ACTIVE/BOOKED and commits both updates together. Repeated
completion and cancellation return 409 without further mutations. Early completion
is permitted by the draft product rule, subject to human confirmation.

Notes require an owned COMPLETED booking for both load and save. Loading no note
returns version 0. Saving inserts version 1 only if none exists, otherwise updates
where the existing version equals `expectedVersion`. The parent booking row lock
and unique booking key protect concurrent initial inserts. A mismatch returns
409; never overwrite the existing note or return its content in the conflict error.
Validate raw Unicode code-point length <= 8,000 before stripping outer whitespace;
reject stripped blank content. PostgreSQL and Java validation must agree.

## 4. React Workspace and State

Proposed routes: `/tutor/slots`, `/tutor/bookings`, `/tutor/history` within one
authenticated Tutor layout. A central API helper handles credentials, CSRF, JSON,
request IDs, and session errors. Role guards in React are presentation only.

Slots page owns its date filter and creation form. Bookings owns its module/date/
status filters. History owns its slot-history filters separately from completed
booking selection; use `/bookings?status=COMPLETED` for the notes selector.

For the selected consultation, keep these distinct in page memory:

- Loaded note content, expected version, and last-saved time.
- Editable saved-note buffer and its dirty state.
- Rough AI input, generated draft/editor, and their dirty states.
- Current account ID, booking ID, generation sequence ID, and request status.

Use controlled React fields and ordinary text rendering. Backend length is in
Unicode code points; client validation must not rely solely on JavaScript UTF-16
`.length` or an HTML maxlength that disagrees with that limit.

Selection changes with unsaved work require discard confirmation. Starting a new
generation that replaces an edited draft requires confirmation. Bind each response
to account/booking/sequence; ignore stale results even if an AbortController was
used. Aborting the browser request does not prove the provider stopped or refund quota.

"Use reviewed draft" copies into the note editor without saving, confirming before
replacing existing edits. "Save note" submits its current content and expectedVersion.
Update loaded version only on success; preserve buffers on conflict/failure. Show
the need to reload on conflict without automatically discarding unsaved work.
Successful saves may be reconciled through reload; do not blindly retry a lost response.

On explicit sign-out/account change clear all private buffers, results, and pending
response identities. Session-expiry handling follows the shared memory-only policy.
Warn on browser navigation/reload where supported when edits exist; memory-only
drafts are not guaranteed to survive tab closure or browser failure.

## 5. Tutor AI Contract

### Input and authorization

Only `roughNotes` is accepted from the browser. The booking ID in the path establishes
the authorization scope but is not sent to SoC. Confirm current active Tutor,
ownership, and COMPLETED status before admission. Recheck authorization before
returning a response; generation never saves a note or changes consultation state.

### Prompt and output

Version the Tutor prompt in backend source with its tests. A fixed task instruction
asks for a JSON object with exactly three nonblank string fields: `summary`,
`topicsDiscussed`, and `followUp`. Treat rough notes as untrusted task data, using
the verified provider's instruction/data separation where supported. Ask the model
to use only supplied facts and mark absent information as "Not provided".

Parse the result as JSON, reject missing/extra fields, wrong types, blank values,
or over-limit results, and format the strings under Summary, Topics discussed,
and Follow-up for the API draft. Reject embedded HTML tags instead of rendering
them. Treat remaining strings as plain text, never executable instructions or
clickable generated links. Do not call another LLM automatically to repair output.
Structural validation cannot prove truthfulness; evaluate grounding with synthetic
cases and retain the required human review step.

If the later SoC model cannot reliably supply this JSON contract, update the parser,
prompt, tests, and this spec together before activation; do not silently accept
arbitrary text. No provider-specific structured-output feature is assumed.

### Limits and failures

Input <= 8,000 code points, final draft <= 8,000 code points, application JSON request
body <= 64 KiB, provider raw response <= 64 KiB, 30-second overall generation deadline
including response reading. Apply a configured provider output-token cap as well;
its numeric value and input token accounting are deferred to SoC verification.
The token cap never replaces character/byte validation. Configure shorter connection
timeouts within the overall deadline rather than allowing an unbounded read.

Use the shared admission service: one outstanding request per actor, 3 accepted
Tutor requests per rolling 5 minutes, and the team/provider global budget. No
automatic retry. Failed calls consume reservations. Fail closed for missing real
configuration, quota-store errors, or exhausted budgets; manual notes remain usable.

### Fake-first development

Use `platform/ai/AiGateway` with configured `FakeProvider` or `SocLlmProvider`;
role services do not construct providers. The default dev/test client returns
deterministic valid sections. Test doubles
can also simulate timeouts, malformed JSON, HTML, oversized responses, empty output,
and quota/provider failures. They make no external requests or secret reads.
Clearly label fake mode in dev UI. Production rejects fake mode at startup.
Real SoC connection work is deferred; no need to provide an API link now.

## 6. MP2 Adaptation

Source baseline: commit `d6a9a244f960854b2addb1546b45e506217f4823` in MP2.
Review/adapt TutorService, domain records, repository contracts, and relevant
Tutor tests. Replace desktop views, SQLite-specific transactions, and actor/session
wiring. Exclude local database files, default credentials, and unrelated UI assets.

Explicit differences: all web access requires active accounts; start is strictly
future; completed-only note checks apply at the backend; note versions prevent
stale writes; PostgreSQL locking replaces local exclusivity. AI and web security
tests are new work. Maintain a measured reuse inventory while porting; do not
report candidates as copied code or reused tests as newly authored ones.

## 7. Verification Mapping

| Requirements | Verification |
| --- | --- |
| TUT-SLOT-01, TUT-HIST-01 | Owner scope, stable ordering, SGT boundary, optional date, empty results; React filter isolation |
| TUT-SLOT-02 | Fixed-clock invalid/adjacent/overlap tests; PostgreSQL competing inserts; wrong module/actor |
| TUT-SLOT-03 | Confirmation UI; unauthorized and invalid states; booking/cancellation race |
| TUT-BOOK-01 | All status/module/date combinations, other-owner exclusion, safe DTO fields |
| TUT-BOOK-02 | Both-status atomicity/rollback, early completion, repeat conflict, ownership |
| TUT-NOTE-01 | Empty-note version 0; initial save/update; blank/Unicode limits; stale concurrent save; completed-only; persistence |
| TUT-AI-01 | Zero provider calls on invalid access; reviewed copy/save distinction; late responses; safe output; synthetic grounding |
| TUT-AI-02 | Quota races across sessions/roles; durable reservations; expiry; deadlines; fake failures; manual fallback |
| Shared SEC/AUTH/DEP | MockMvc session/CSRF/inactive checks, logs/secrets review, production HTTPS and resource separation |

Run PostgreSQL integration tests with Testcontainers, including empty-schedule
overlap, student overlaps on different slots, Admin eligibility races, and note
conflicts. Do not substitute SQLite/H2 for locking or constraint behavior. Add full
MockMvc-to-PostgreSQL tests with real services/repositories alongside isolated
controller/service tests. Test the real SoC adapter using a mock HTTP server for
HTTP mapping/errors/deadlines, separately from fake gateway scenarios. Real-provider
evaluation is small, separate, and performed later. Record commands, revision,
results, and limitations; no tests have been run against an implementation yet.

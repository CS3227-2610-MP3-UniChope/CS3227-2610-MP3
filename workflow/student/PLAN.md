# Student Implementation Plan

Status: Backend C1/C3 reads and C5/C2/C4 auth/booking/cancellation implemented locally; verification and pending review are recorded in their change records. Writing this plan does not
indicate team approval or authorize changes to other roles' code.
Owner: Phuc (Student role owner)
Date: 22 October 2026

Scope: Student booking features, the Student AI agenda, and the shared AI
infrastructure in `platform/ai`. Core, auth, scaffold, and deployment work are
dependencies owned elsewhere; this plan does not assign them.

Inputs: [Student Product](PRODUCT-SPEC.md), [Student Technical](TECHNICAL-SPEC.md),
[Shared Technical](../SHARED-TECHNICAL-SPEC.md), [Integration](../INTEGRATION-SPEC.md),
[Development workflow](NOTE.md).

## 1. Execution Rules

- Each task runs through the [workflow](NOTE.md), each role in a fresh session: the
  spec analyst confirms the task's acceptance IDs, the test writer writes failing
  tests, the implementer implements, an independent reviewer reviews, and a human
  verifies. Keep one handoff record per task in
  `workflow/student/changes/<task-id>.md`.
- Tests first for behavior changes; record the expected red result. Never weaken a
  test to get green; send wrong tests back to the test writer and wrong requirements
  back to the spec analyst.
- Shared changes (core queries/lifecycle methods, AI migrations, error codes, the
  gateway contract) need the affected owner's review before dependent code merges.
- Fake-backed AI first. Real SoC details gate only AI3 and S7.
- Assignment deadline is 23 October 2026, 2pm SGT. If time runs short, finish in
  this order: S2-S4 (booking), AI1-AI2 with S5 (fake-backed agenda), then AI3/S8.
  Record anything unfinished honestly rather than marking it done.

## 2. Tasks

### S0: Agree decisions and interfaces

- [x] Confirm booking assumptions 1-4 in Product spec section 10 (Phuc, 22 October
  2026). AI assumptions 5-6 remain open.
- [x] ~~Agree with Huanghao the core additions Student needs: bookable-slot query,
  student-booking queries, `ConsultationLifecycle.bookSlot` / `cancelBooking`
  outcomes.~~ Superseded (analyst-2 finding F2-01): no core change is needed.
  Student uses the existing `bookAvailableSlot` / `cancelActiveBooking` unchanged and
  its own read-only repository (Phuc decisions D2/D3; Technical spec §1, §4).
- [ ] Get Kok Seng's review of the new `unichope.platform.auth.CurrentActorProvider`
  interface and Huanghao's (or the assigned `platform/web` reviewer's) review of the
  `student-api` rule in `ApiSecurityConfiguration` before Student code merges to
  `main` (finding F2-13). Not a prerequisite for writing tests.
- [ ] Agree the `AiGateway` / `AiRequest` / `AiResult` contract and task policies
  with Huanghao and Kok Seng; reconcile the Tutor spec/plan text that still assigns
  `platform/ai` to Huanghao.
- [ ] Add `SLOT_UNAVAILABLE` and `BOOKING_OVERLAP` to the integration spec's error
  contract in a reviewed change.
- [ ] Confirm who owns the shared React shell and API client.

Depends on: none. Verify: decisions recorded with names and dates in the specs.

### S1: Scaffold (team dependency)

Done by Huanghao on `main` (PRs #1-#4): Gradle/Boot backend, `postgresTest`, and
backend CI. Student adds `src/backend/compose.test.yaml` for a local disposable
PostgreSQL 17. No frontend scaffold exists yet.

### C5: Integrate Student with shared authentication

Change record: [changes/stu-c5-auth-integration.md](changes/stu-c5-auth-integration.md).

- [x] Add student-api to app-api; supply authenticated SessionPrincipal-only platform.auth actor adapter.
- [x] Real runtime PostgreSQL tests C5-RUN-1, C5-AUTH-1/2, C5-REV-1, C5-CSRF-1, C5-STATE-1. Focused C5/C2/C4 run: 33 tests, zero failures/errors/skips. C5-REG-1 full regression evidence is recorded in the implementation handoff; independent/human acceptance remains pending.
- [ ] Affected Auth/web/core owner review before merge; no code approval is implied.

Implementation order: C5 runtime foundation, C2 creation, C4 cancellation. Existing
c1/c3 read foundations already exist. Fresh test writing may cover all boundaries
together now; combined journey cases become green after C2/C4, not before. No
implemented-upstream prerequisite is imposed on writing executable failing tests.

### AI1: Gateway and fake provider

- [ ] Tests: gateway delegates to the configured provider; fake outputs are
  deterministic per task; test double simulates timeout, error, malformed, HTML,
  oversized, and empty responses; production profile with `fake` fails at startup;
  `disabled` returns UNAVAILABLE with zero calls.
- [ ] Reconcile existing provisional AiGateway/Tutor/Admin consumers before implementing the future proposal; implement `AiGateway`, `AiRequest`, `AiResult`, `AiTask`, `AiTaskPolicy`,
  `FakeProvider`, provider selection config, and `platform/web` failure mapping.
- [ ] Publish fake examples so Tutor and Admin can integrate without waiting for AI2.

Depends on: S0 gateway agreement, S1. Requirements: SH-AI-01/02/07/08/09.

### AI2: Durable admission and quotas

- [ ] Tests on PostgreSQL: per-actor window, one outstanding per actor, shared
  in-flight maximum, provider-wide budgets, `Retry-After` values, lease expiry after
  a simulated crash, concurrent admissions from two sessions and two roles, restart
  durability, quota-store failure fails closed, no text stored in attempts.
- [ ] Implement the `ai_quota_state` / `ai_attempt` migration (reviewed by Huanghao)
  and the admission service; wire it into the gateway before provider dispatch.

Depends on: AI1. Requirements: SH-AI-03/04/05/06, AI-02.

### S2: Browse bookable slots

Change record: [changes/stu-c1-browse-slots.md](changes/stu-c1-browse-slots.md).

- [ ] Tests: S-SLOT-1, S-SLOT-2, S-AUTH-1 for `/modules` and `/slots`; DTO contains
  no emails; React filters, empty and error states.
- [ ] Implement controller/service on core read queries, Student layout, and the
  Find a consultation page.

Analyst-2 note (F2-01): the backend part is change stu-c1 (C1-T1..T4): the actor
seam, `student-api` wiring, Student-owned `StudentReadRepository` over core tables
(not new core queries) and the two GET endpoints. React work stays pending here
until a frontend scaffold exists.

Depends on: S1, auth actor interface (Student-defined, Kok Seng implements).
Requirements: STU-SLOT-01, AUTH-02, SEC-01.

### S3: Book a slot

Change record: [changes/stu-c2-book-slot.md](changes/stu-c2-book-slot.md).

- [x] Backend tests: S-BOOK-1 to S-BOOK-5 including both concurrency races on real
  PostgreSQL, unique-violation mapping and CSRF (15 focused C2 tests passing).
- [x] Implement `POST /bookings` through the existing, unchanged
  `ConsultationLifecycle.bookAvailableSlot` (analyst-2 F2-01).
- [ ] React booking dialog, confirmation and conflict refresh.

Depends on: S2 (stu-c1) and the stu-c3 `StudentBooking` projection and
`GET /bookings/{id}` used by the response and its `Location`. Requirements:
STU-BOOK-01, REL-01.

### S4: My bookings and cancellation

Change records: [changes/stu-c3-my-bookings.md](changes/stu-c3-my-bookings.md),
[changes/stu-c4-cancel-booking.md](changes/stu-c4-cancel-booking.md). Backend
only for now; React pages wait for the frontend scaffold.

- [x] Backend list/detail already implemented; cancellation tests S-CANC-1 to S-CANC-3
  include the tutor-completion race, boundary clock, ownership, input/security and
  committed-response failure cases (11 focused C4 tests passing).
- [x] Implement list, detail, and cancel endpoints; cancellation uses unchanged core lifecycle.
- [ ] My bookings React page, tabs, filters, confirmation and keyboard flow.

Depends on (analyst-2 F2-01): stu-c3 (list/detail) depends only on stu-c1 and runs
before stu-c2; stu-c4 (cancel, via the existing `cancelActiveBooking`) depends on
stu-c1 and stu-c3 and runs after stu-c2 in the recorded order c1, c3, c2, c4.
Requirements: STU-BOOK-02, STU-BOOK-03.

### S5: Fake-backed agenda

- [ ] Tests: S-AI-1 to S-AI-6, S-UI-1; exact provider input fields; every output
  validation rule; formatter length; post-call recheck after deactivation.
- [ ] Implement `AgendaPrompt` (`STU-AGENDA-1`), parser/validator/formatter,
  `AgendaService`, endpoint, and the Prepare agenda page with fake label, copy,
  regenerate confirmation, and late-response protection.

Depends on: S4, AI2. Requirements: STU-AI-01, STU-AI-02, SEC-02/03.

### AI3: Real SoC adapter

- [ ] With real endpoint details, write `SocLlmProvider` transport tests against a
  local mock HTTP server: auth header, request mapping, HTTP errors,
  malformed/oversized bodies, deadlines. No live key in tests.
- [ ] Implement the adapter; configure token accounting, output cap, and
  provider-wide budgets from verified SoC limits; separate dev/prod keys server-side.

Depends on: AI2, SoC details. Requirements: AI-01, SH-AI-03/05/08. Serves all roles.

### S6: Independent security review

- [ ] Fresh-session reviewer checks Student endpoints, lifecycle use, ownership,
  CSRF, DTO fields, logs, `platform/ai` admission and secrets, prompt/data
  separation, and output rendering.
- [ ] Run S-INJ-1 fixtures (prompt reveal, "answer my exam", "cancel all bookings")
  with fake canary secrets; confirm no action or data access occurs.
- [ ] Fix findings and re-review the resulting diff.

Depends on: S5 (and AI3 for adapter review).

### S7: Real-provider agenda evaluation

- [ ] Run a small synthetic set (clear, vague, multi-topic, injection, answer-seeking)
  against SoC; record validity rate, grounding, whether the model answered questions,
  minutes within length, and token/call usage. No exact-wording oracle.
- [ ] Adjust the prompt/validator with spec and test updates where needed.

Depends on: AI3, S6.

### S8: Release evidence

- [ ] Cross-role smoke test with Tutor: tutor publishes, student books, cancels,
  rebooks, tutor completes; student sees history.
- [ ] Update user/developer guides, MP2 reuse inventory with measured counts,
  sanitized interaction summaries in `logs/`, and reflections.
- [ ] Human verification of each Student journey with keyboard only.

Depends on: S6, S7, deployment.

## 3. Verification Commands

The scaffold (S1) will establish the real commands. Expected, once it exists:

```sh
# src/backend, against disposable test PostgreSQL only
./gradlew test --tests 'unichope.student.*' --tests 'unichope.platform.ai.*'

# src/frontend
npm ci
npm run test -- --run
npm run build
```

Backend Gradle commands exist; frontend work is pending. This spec session ran no tests. Record actual command, revision, exit
code, and relevant output in each task's handoff record.

Analyst-2 correction (F2-01): the backend scaffold now exists, and the command above
alone is insufficient for booking work because `test` excludes the `postgres` tag,
where the Student MockMvc-to-PostgreSQL tests live. For stu-c1..c4 use the baseline
command recorded in
`changes/stu-c1-browse-slots/evidence/baseline-2026-10-22.txt` (run in `src/backend`
after `docker compose -f compose.test.yaml up -d --wait`):

```sh
./gradlew test postgresTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test \
    -Dcore.test.user=mp3_test -Dcore.test.password=mp3_test_local --console=plain
```

Focused runs may add `--tests` filters to both tasks; the final check runs the full
command so existing Tutor and core suites stay green.

## 4. Current booking increment (22 October 2026)

Direct source: Phuc's request "Update the spec and intergrate with new auth,
implement student booking". Backend only: C5 auth composition -> C2 creation -> C4
cancellation. Tests may be authored as one combined executable packet before
implementation, with per-change acceptance mappings/handoffs. Existing C2 tests
must be refreshed against current hashes and preserved, not duplicated or weakened.
Use the existing c1/c3 code; do not reimplement browsing/list/detail. Frontend and
AI remain pending, not silently removed. Real runtime tests use db/migration V1-V4;
isolated fake actors continue as focused seam tests only. Full regression includes
Auth, Admin, Tutor, Student and core tests plus bootJar.

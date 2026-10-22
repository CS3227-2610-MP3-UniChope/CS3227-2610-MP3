# Student Implementation Plan

Status: Draft execution plan; every task is pending. Writing this plan does not
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

- [ ] Confirm the assumptions in Product spec section 10.
- [ ] Agree with Huanghao the core additions Student needs: bookable-slot query,
  student-booking queries, `ConsultationLifecycle.bookSlot` / `cancelBooking`
  outcomes.
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

### AI1: Gateway and fake provider

- [ ] Tests: gateway delegates to the configured provider; fake outputs are
  deterministic per task; test double simulates timeout, error, malformed, HTML,
  oversized, and empty responses; production profile with `fake` fails at startup;
  `disabled` returns UNAVAILABLE with zero calls.
- [ ] Implement `AiGateway`, `AiRequest`, `AiResult`, `AiTask`, `AiTaskPolicy`,
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

Depends on: S1, core read queries, auth actor. Requirements: STU-SLOT-01, AUTH-02, SEC-01.

### S3: Book a slot

Change record: [changes/stu-c2-book-slot.md](changes/stu-c2-book-slot.md).

- [ ] Tests: S-BOOK-1 to S-BOOK-5 including both concurrency races on real
  PostgreSQL, unique-violation mapping, CSRF; React confirmation and conflict refresh.
- [ ] Implement `POST /bookings` through `ConsultationLifecycle.bookSlot` (core
  method coordinated with Huanghao) and the booking dialog.

Depends on: S2, core lifecycle. Requirements: STU-BOOK-01, REL-01.

### S4: My bookings and cancellation

Change records: [changes/stu-c3-my-bookings.md](changes/stu-c3-my-bookings.md),
[changes/stu-c4-cancel-booking.md](changes/stu-c4-cancel-booking.md). Backend
only for now; React pages wait for the frontend scaffold.

- [ ] Tests: S-LIST-1, S-CANC-1 to S-CANC-3 (race with tutor completion), fixed-clock
  `cancellable`, `GET /bookings/{id}` wrong-owner 404; React tabs, filters,
  confirmation, keyboard flow.
- [ ] Implement list, detail, and cancel endpoints and the My bookings page.

Depends on: S3. Requirements: STU-BOOK-02, STU-BOOK-03.

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

None of these exist or have been run yet. Record actual command, revision, exit
code, and relevant output in each task's handoff record.

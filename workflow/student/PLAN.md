# Student Implementation Plan

Status: Backend booking and frontend FE0/FE1/FE2 implemented locally. FE0 has an
independent PASS. FE1/FE2 have an independent reviewer-3 PASS on the manifest-r2
revision (FE-R1 corrected and closed; 49/49 RTL tests, build, seven browser checks
and a real synthetic backend login/book/cancel journey recorded). Phuc's owner
decisions of 22 October 2026 (OD-1..OD-17) add FRONTEND-DESIGN revision 2:
FE3 review fixes and FE4 follow-ups have independent PASS reviews and are READY_FOR_HUMAN.
Revision 3 (TH-D1..TH-D3) adds the FE5 light/dark theme switch after FE3/FE4; FE5 has an independent PASS and is READY_FOR_HUMAN.
Human acceptance and affected-owner merge review remain pending. Verification
revisions are recorded in the change records. Writing this plan does not
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

### Current frontend increment: FE0 -> FE1 -> FE2

Direct authorization: Phuc's current Login/role-placeholder/Student frontend
request and selected weekly timetable/filter-sidebar/mobile-list direction.
Canonical scoped design and acceptance: [FRONTEND-DESIGN.md](FRONTEND-DESIGN.md).
All tasks use fresh-role handoffs; no same-session review or automatic human signoff.

| Task / record | Acceptance | Dependencies / scope / verification |
| --- | --- | --- |
| FE0 [foundation](changes/stu-fe0-foundation.md) | FE0-RUN, FE0-PROXY, FE0-QA | Implemented React/TS/Vite/Bootstrap scaffold, locked dependencies, Vitest/RTL and Playwright harness, proxy and setup documentation. Executable passing setup baseline preceded feature tests; fresh FE0 reviewer recorded PASS. |
| FE1 [auth UI](changes/stu-fe1-auth-ui.md) | FE1-LOGIN/RESTORE/LOGOUT/DENIAL/PRIVACY/UI | Implemented real shared shell/API/session layer, Login, stored-role routing and honest Tutor/Admin placeholders. Feature tests preceded implementation; independent reviewer-3 PASS on manifest-r2. Affected Auth/role owner merge review and Phuc acceptance remain pending. |
| FE2 [booking UI](changes/stu-fe2-booking-ui.md) | FE2-BROWSE/BOOK/LIST/CANCEL/RACE/UI | Implemented Student weekly/list slots, My bookings, confirmations and exact existing API contracts, without AI/backend changes. Independent reviewer-3 PASS on manifest-r2; Phuc acceptance remains pending. |
| FE3 [review fixes](changes/stu-fe3-review-fixes.md) | FE2-NOTHING-SENT, FE1-DENIAL-CACHED, FE1-LOGOUT-CONFIRMED, FE2-NOTICE-CLEAR, FE1-UI-LANDMARKS | READY_FOR_HUMAN (independent reviewer-1 PASS; human acceptance pending). Before acceptance (OD-13): OD-4, OD-5, OD-8, OD-12 plus FE-R7(b) regression. Depends on FE1/FE2 manifest-r2. Frontend source/tests only; tests first, fresh review, then Phuc's OD-15 acceptance journey. |
| FE4 [follow-ups](changes/stu-fe4-followups.md) | FE1-RESTORE-UNREACHABLE, FE2-UNKNOWN-MODULE, FE2-MODULES-DEGRADED, FE-UI-CONTRAST, FE1-LOGIN-STALE, FE1-LOGOUT-ENDED, FE2-BROWSE-MIDNIGHT | READY_FOR_HUMAN (independent reviewer-2 PASS; human acceptance pending). Follow-ups (OD-13): OD-3, OD-7, OD-9, OD-11; OD-1/OD-2 placed here pending Phuc's confirmation and Kok Seng's review; FE-R7(a,c) coverage. Starts after FE3 review. OD-10 is a human check; OD-6 deferred. |
| FE5 [theme switch](changes/stu-fe5-theme-switch.md) | TH-DEFAULT, TH-TOGGLE, TH-A11Y, TH-PLACEMENT, TH-PERSIST, TH-STORAGE-FALLBACK, TH-NOFLASH, TH-CONTRAST, TH-SESSION | READY_FOR_HUMAN (independent reviewer-1 PASS; human acceptance pending). FRONTEND-DESIGN revision 3 (Phuc's TH-D1..TH-D3, 22 October 2026): light default, browser-remembered light/dark switch in every page banner. Hard dependency on FE3's reviewed revision (banners, OD-12); default order after FE4's reviewed revision (shared styles/contrast check). Frontend source/tests/styles, `index.html` and one same-origin early theme script only; tests first, fresh review, Phuc's check. |

Allowed runtime dependency versions are compatible pinned versions selected by
the harness stage and recorded in package-lock.json; Node24.21.0/npm12.0.2 are
available locally. On PowerShell use npm.cmd (npm.ps1 is policy-blocked).
Installed dependencies and Edge-backed Playwright execution are evidenced in FE0/FE2.
Owner review and Phuc's human acceptance remain pending until actually recorded.

Execution evidence inspected for this status refresh: FE2
`evidence/unit-final.json` reports 45/45 tests, `build-final.txt` records strict
TypeScript/Vite output, and `browser-final.txt` reports seven passing browser
checks. Superseding r2 evidence after the FE-R1 correction: `unit-r2.json` 49/49,
`build-r2.txt`, `browser-r2.txt` 7/7, and reviewer-3's reruns (`reviewer3-*.txt`). `evidence/live-browser-smoke.json` records actual isolated backend booking
201/cancellation200 with CSRF, History and Student/Tutor/Admin routing, no persisted
private browser storage or page errors, no overflow at four widths, and zero axe
violations on seven screens. These bounded checks do not establish final review,
exhaustive accessibility, human acceptance, Tutor/Admin features or deployment.

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
- [x] Implement shared React shell/API client within Phuc's authorized FE1 scope;
  affected Auth/role owners retain their ownership and pending merge review.

Depends on: none. Verify: decisions recorded with names and dates in the specs.

### S1: Scaffold (team dependency)

Done by Huanghao on `main` (PRs #1-#4): Gradle/Boot backend, `postgresTest`, and
backend CI. Student adds `src/backend/compose.test.yaml` for a local disposable
PostgreSQL 17. FE0 now supplies the frontend scaffold and harness; see its
implementation and independent review records.

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

- [x] Backend browse/security tests exist in stu-c1; FE2 adds evidenced React
  filters, empty/error, SGT and stale-response coverage.
- [x] Implement Student-owned backend read repository/controller/service and FE2
  Student layout/Find a consultation page. Final frontend review remains pending.

Analyst-2 note (F2-01): the backend part is change stu-c1 (C1-T1..T4): the actor
seam, `student-api` wiring, Student-owned `StudentReadRepository` over core tables
(not new core queries) and the two GET endpoints. The originally deferred React
work is now implemented under FE2, with verification and review state above.

Depends on: S1, auth actor interface (Student-defined, Kok Seng implements).
Requirements: STU-SLOT-01, AUTH-02, SEC-01.

### S3: Book a slot

Change record: [changes/stu-c2-book-slot.md](changes/stu-c2-book-slot.md).

- [x] Backend tests: S-BOOK-1 to S-BOOK-5 including both concurrency races on real
  PostgreSQL, unique-violation mapping and CSRF (15 focused C2 tests passing).
- [x] Implement `POST /bookings` through the existing, unchanged
  `ConsultationLifecycle.bookAvailableSlot` (analyst-2 F2-01).
- [x] FE2 React booking dialog, confirmation and conflict refresh implemented;
  verification evidenced above, final review/human acceptance pending.

Depends on: S2 (stu-c1) and the stu-c3 `StudentBooking` projection and
`GET /bookings/{id}` used by the response and its `Location`. Requirements:
STU-BOOK-01, REL-01.

### S4: My bookings and cancellation

Change records: [changes/stu-c3-my-bookings.md](changes/stu-c3-my-bookings.md),
[changes/stu-c4-cancel-booking.md](changes/stu-c4-cancel-booking.md). The initial
backend-only scope is now complemented by FE2 React pages.

- [x] Backend list/detail already implemented; cancellation tests S-CANC-1 to S-CANC-3
  include the tutor-completion race, boundary clock, ownership, input/security and
  committed-response failure cases (11 focused C4 tests passing).
- [x] Implement list, detail, and cancel endpoints; cancellation uses unchanged core lifecycle.
- [x] FE2 My bookings React page, tabs, filters, confirmation and keyboard flow
  implemented and checked as evidenced above; final review/human acceptance pending.

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

Actual frontend scripts are defined in src/frontend/package.json. On PowerShell
use npm.cmd; the backend unit-only example below does not run PostgreSQL coverage:

```sh
# src/backend, against disposable test PostgreSQL only
./gradlew test --tests 'unichope.student.*' --tests 'unichope.platform.ai.*'

# src/frontend
npm.cmd ci
npm.cmd test
npm.cmd run build
npm.cmd run test:browser
```

Backend and frontend commands exist. This spec-only status refresh inspected
recorded evidence and did not rerun product tests. Record actual command, revision, exit
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

## 4. Historical backend booking increment (22 October 2026)

Direct source: Phuc's request "Update the spec and intergrate with new auth,
implement student booking". Backend only: C5 auth composition -> C2 creation -> C4
cancellation. Tests may be authored as one combined executable packet before
implementation, with per-change acceptance mappings/handoffs. Existing C2 tests
must be refreshed against current hashes and preserved, not duplicated or weakened.
Use the existing c1/c3 code; do not reimplement browsing/list/detail. Frontend was
deferred by that backend-only request and is now implemented in the current FE0/FE1/FE2
increment above. AI remains pending. Real runtime tests use db/migration V1-V4;
isolated fake actors continue as focused seam tests only. Full regression includes
Auth, Admin, Tutor, Student and core tests plus bootJar.

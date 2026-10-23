# Tutor Implementation Plan

Status: Partially implemented; consult the task checkboxes and dated handoffs for
current progress. Checkboxes do not indicate team approval or authorize
deployment/changes to other roles by themselves.
Owner: Huanghao
Date: 20 October 2026

Scope: Tutor implementation, including the Tutor-specific prompt, request
construction, output validation, and orchestration of its AI feature. The Student
role owns generic shared AI transport, durable admission, and per-key quotas in
`platform/ai`. This plan does not assign shared core/auth/deployment work to
Huanghao or change teammates' responsibilities.

Inputs: [Tutor Product](PRODUCT-SPEC.md), [Tutor Technical](TECHNICAL-SPEC.md),
[Shared Technical](../SHARED-TECHNICAL-SPEC.md), [Integration](../INTEGRATION-SPEC.md).

## 1. Execution Rules

- Work from reviewed requirement IDs and the recorded spec revision. Update specs
  and tests in the same change if requirements change; do not hide deviations.
- For behavior changes, write meaningful acceptance/security tests before the
  implementation. Keep changes scoped and preserve teammates' unrelated work.
- Shared auth/schema/lifecycle/quota changes require agreement with their owners.
  Resolve interface questions before coding dependent features.
- Start with fake-backed AI. The real SoC API link/key/model can be supplied later;
  only provider activation and real evaluation depend on them.
- No existing SDD toolkit supplies this process. Specialized agents use the team's
  own scoped instructions and handoff records. P5 manual notes and P6 fake-backed
  Tutor AI have implementation records. P4 is implemented and its scoped independent
  review is recorded; human acceptance is pending. P7 has an additional focused approval-text check;
  see its security-verification handoff.

## 2. Specialized-Agent Workflow

Define concise team-authored instruction files in `workflow/agents/` before asking
implementation agents to execute tasks. Those files are a separate pending task,
not supplied by this plan. Proposed responsibilities:

| Agent | Task and limits | Required output |
| --- | --- | --- |
| Analyst/architect | Check scoped requirements/contracts; surface conflicts; do not change product code | Requirement mapping, assumptions, unresolved questions |
| Developer | Implement one agreed task in scoped files; no production credentials or deployment rights | Patch, tests/commands/results, deviations and limitations |
| Reviewer/tester | Independently compare patch/spec, inspect security, run relevant checks; do not approve own implementation | Findings by requirement, reproduction evidence, remaining risks |

Handoff fields: task ID, input commit/spec revision, requirement IDs, permitted
files, shared dependencies, changes, commands with exit results, findings, and open
questions. The human checks evidence before accepting the task. Agent output
cannot grant permissions, approve a spec, or waive a failing test.

Run agents sequentially where work depends on another's output; parallel execution
is optional. Evaluate them in a disposable checkout using malicious issue/handoff
text, fake secrets, and fabricated test-pass claims. Record whether tool/secret
boundaries hold and whether claimed results can be independently reproduced.
Do not read or publish hidden reasoning or expose production resources to these tests.

## 3. Tasks and Verification

### P0: Agree interfaces and establish workflow

- [ ] Review team size, owners, privacy, active-account policy, early completion,
  note conflicts, draft limits, stack, role-first packages, and row-lock protocol.
- [ ] Assign shared auth/schema/quota/deployment work; do not assume Huanghao owns it all.
- [ ] Record Huanghao as Tutor AI feature owner; agree the Tutor-to-shared-AiGateway
  request/result/failure and quota integration with affected role owners. Do not
  allocate shared platform, core/auth, or deployment work on teammates' behalf.
- [ ] Review proposed maintainer/reviewer assignments in the shared technical spec;
  record actual agreement and keep Tutor-private notes within Tutor ownership.
- [ ] Create team-authored agent instructions and an evaluation/handoff template.
- [ ] Record agreement and unresolved provider/hosting details without real secrets.

Depends on: none. Verify: named owners, reviewed contracts, and clear dependencies.
Deferred real-provider details must not prevent fake-backed tasks from starting.

### P1: Scaffold and validate builds

- [ ] Create `src/backend/` Gradle/Spring Boot and `src/frontend/` Vite/React projects.
- [ ] Use `unichope.student`, `unichope.tutor`, `unichope.admin`, and focused
  `unichope.platform.core/auth/ai/web` packages with matching test packages.
- [ ] Pin compatible versions, Java toolchain, npm lockfile, formatter/lint settings.
- [ ] Configure `JdbcClient` repositories and PostgreSQL Testcontainers tests;
  verify local/CI Docker runtime. Optionally add small ArchUnit boundary checks.
- [ ] Add local PostgreSQL setup, safe `.env.example`, dev/test/prod profiles, and fake AI default.
- [ ] Configure Vite `/api` proxy and production frontend asset serving.
- [ ] Add CI build/test jobs with no production/SoC secrets available to untrusted jobs.

Depends on: P0. Verify: clean checkout can build both projects; fake mode makes no
network call; no default production admin credential. Establish actual build commands.

### P2: Shared persistence, sessions, and Tutor read slice

- [ ] Write schema/auth/Tutor-list contract tests and isolated synthetic fixtures
  containing two tutors, a student, an admin, active/inactive accounts, and assignments.
- [ ] Coordinate Flyway schema, JdbcClient-backed contracts, shared row-lock support, authentication,
  CSRF, session rotation, and active-account checks with shared owners.
- [ ] Add full authenticated MockMvc-to-PostgreSQL checks with real services and
  repositories; isolated mocked-controller tests are not the only integration evidence.
- [ ] Adapt required MP2 models/queries and track reused paths/revision/counts.
- [ ] Implement authenticated Tutor layout, module options, and upcoming slot list.

Depends on: P1 and shared owners' contributions. Requirements: AUTH-01/02,
SEC-01, TUT-SLOT-01. Verify: only current tutor's future rows appear; wrong role,
inactive account, missing session, and cross-owner requests fail correctly; test SGT
boundaries. Validate the browser-to-database read path before adding mutations.

### P3: Slot creation and cancellation

- [ ] Add domain, MockMvc, PostgreSQL race, and React confirmation tests.
- [ ] Cover an empty tutor schedule, concurrent overlapping inserts, and Admin
  assignment/deactivation changes using the same tutor/module locks.
- [ ] Adapt slot validation and shared locked transactions; implement API/forms.
- [ ] Coordinate Student booking transaction tests to cover cancellation races.
- [ ] Include the same student's simultaneous bookings of different overlapping slots;
  locking only each selected slot must not allow both requests to succeed.

Depends on: P2. Requirements: TUT-SLOT-02, TUT-SLOT-03. Verify: adjacent slots work, invalid/
overlapping slots fail, simultaneous overlap admits at most one; booked slot cannot
be cancelled; no booking survives attached to a cancelled slot; CSRF is enforced.

### P4: Bookings, completion, and history

- [x] Test filters, owner scoping, completion atomicity/rollback/repetition, and history.
- [x] Implement booking queries, completion transition, confirmation, and independent booking/history UI filters.

Depends on: P3 and shared booking fixture/contract. Requirements: TUT-BOOK-01, TUT-BOOK-02,
TUT-HIST-01. Verify: two statuses update together, repeated completion conflicts,
early completion follows the specified product rule, history selection does not alter
notes. Implementation and verification evidence are recorded in the P4 developer
handoffs; scoped independent review is complete, and human acceptance remains pending.

### P5: Manual notes and version conflicts

- [x] Test completed-only load/save, version 0/1/update, Unicode/blank limits,
  competing first saves and stale updates, and retained content after failure.
- [x] Implement note API, conditional persistence, editor, conflict handling, and timestamps.
- [x] Keep NoteService, PrivateNoteRepository, and PostgresPrivateNoteRepository
  within `unichope.tutor`; use shared locking support without reverse platform imports.
- [x] Test restart/reload persistence and private DTO boundaries.

Depends on: P4. Requirements: TUT-NOTE-01. Verify: stale saves never overwrite newer
notes; explicit save persists; other roles/tutors cannot load or change private notes.
Manual note workflow must work before AI is introduced.

### P6: Tutor fake-backed AI feature (depends on shared usage control)

- [ ] Coordinate with the Student owner on the shared `AiGateway`, Tutor task
  registration/key routing, durable admission, per-key quotas, error mapping,
  deadline, and fake-provider contract; do not duplicate shared implementations.
- [ ] Coordinate any shared AI metadata migration with the core maintainer and
  trusted actor integration with the auth maintainer; keep consultation lifecycle
  code outside this scope.
- [x] Complete production AuthenticationService/account integration tests for
  Tutor login, anonymous/wrong-role denial, session-generation revocation, and
  inactive-account output discard during generation. PostgreSQL-backed MockMvc
  endpoint tests separately cover CSRF, booking owner/state, response shape, and
  no-write behavior with test-only auth. Shared quota tests are recorded separately.
- [x] Implement the Tutor prompt, output parser, fake-backed request orchestration,
  and draft endpoint. The HTTP route is unavailable until an approved shared gateway
  bean and output-token configuration are wired.
- [x] Add rough-input/draft/editor separation, reviewed copy, explicit save,
  confirmations, pending state, account/selection race protection, and fake label.

Depends on: P5 and agreed shared quota contract for Tutor integration. The gateway,
fake provider, and isolated admission tests can start after P1 and contract agreement;
they do not need to wait for manual Tutor notes. Requirements: TUT-AI-01, TUT-AI-02,
SEC-02/03, AI-02. Verify: generation never saves, unauthorized requests make zero
calls, fake failures preserve text, quota holds across sessions, manual notes still
work. This task needs no real LLM link, key, or external provider access.

### P7: Independent security and agent-workflow evaluation

- [ ] Reviewer independently inspects auth/ownership/CSRF, unsafe text, note versions,
  quota accounting, prompt/data separation, logs, configuration, and MP2 reuse claims.
- [x] Verify direct injection with a fake canary; no indirect retrieval channel exists.
  Add its tests if retrieval or tool access is introduced.
- [x] Test generated "approval" cannot trigger saving or grant permissions.
- [ ] Evaluate development agents against malicious issue/handoff text and false
  test claims, recording visible actions and independently checked evidence.
- [ ] Fix scoped findings, rerun affected checks, retain honest unresolved limitations.

The approval-text test and scope record are in
[`AI-SECURITY-VERIFICATION-SPEC.md`](AI-SECURITY-VERIFICATION-SPEC.md),
[`AI-SECURITY-VERIFICATION-MATRIX.md`](AI-SECURITY-VERIFICATION-MATRIX.md), and the
[P7 handoff](../handoffs/2026-10-23-tutor-ai-security-verification.md). The existing
agent-security evaluation is recorded separately; this increment does not repeat it.

Depends on: P6. Verify: sanitized evaluation records include revision, fixture,
expected/observed outcomes, commands, reviewer, and human verification. Fake-provider
tests establish boundaries, not real-model resistance or factual quality.

### P8: Connect and evaluate real SoC provider later

- [x] Capture the public SoCLaaS base host, chat-completions route, Bearer-key
  authentication, and model-catalogue discovery in the provider spec. The internal
  reference remains NUS-network restricted; response/error compatibility and live
  limits remain explicit verification items.
- [x] Implement a bounded OpenAI-compatible Chat Completions adapter and local
  loopback contract tests; see the P8 spec, matrix, and developer handoff.
- [ ] Review the shared provider adapter with the Student AI owner before routing
  any production role key through it. Keep the adapter unwired until runtime routing,
  durable admission, migration, and environment configuration are accepted.
- [ ] Supply real endpoint/model/auth details server-side and verify hosting access.
- [ ] Confirm provider request/token limits and configure conservative shared budgets,
  input accounting, output caps, deadlines, and separate dev/prod team keys.
- [ ] Implement provider adapter and mocked transport contract tests; retain fake tests.
- [ ] Use a local mock HTTP server to verify real-adapter headers/request mapping,
  HTTP errors, malformed/oversized responses, and deadlines without live credentials.
- [ ] Run a small synthetic normal/adversarial evaluation for valid useful output,
  grounding, safe failure, and quota behavior. Record token/call usage where available.

Depends on: P6, actual provider details, and P7 findings resolution. Requirements:
AI-01, AI-02, TUT-AI-01, TUT-AI-02. Verify: real SoC responses reach the review UI; no secret is
in browser/build/logs; quality errors are documented and fixed where feasible.
Never mark fake output or an unverified external call as completing this task. The
adapter is implemented but not wired; see
[`SOCLAAS-PROVIDER-SPEC.md`](SOCLAAS-PROVIDER-SPEC.md) and
[`SOCLAAS-PROVIDER-TEST-MATRIX.md`](SOCLAAS-PROVIDER-TEST-MATRIX.md).

### P9: Deployment, guides, and release evidence

- [ ] Select ordinary hosting, test Docker/Java compatibility and HTTPS access,
  configure isolated production database/secrets, persistence, backup and restore.
- [ ] Deploy first with AI unavailable if P8 is pending; mark this as an incomplete
  release candidate, then enable real SoC and rerun AI smoke checks after P8.
- [ ] Update user/developer guides, reuse inventory/acknowledgements, product website,
  verified interaction summaries, and concrete reflections from actual evidence.
- [ ] Run two-tutor cross-role smoke walkthrough, persistence restart check, and
  environment isolation checks without placing production credentials in CI logs.
- [ ] Reconcile `main` working flow with required grading branch `master`; record
  final release commit and freeze before the assignment deadline.

Depends on: P7; final completion also requires P8 and other roles' completed scope.
Verify: public app matches guides/specs, all roles have real SoC features, required
repository artifacts exist, and final grading branch contains the tested release.
An early hosting/SoC-connectivity probe can run after P1 rather than waiting for P9.

## 4. Verification Commands and Evidence

After P1 establishes the proposed layout, expected commands are:

```sh
# From src/backend; configure isolated test PostgreSQL only.
./gradlew test
./gradlew bootJar

# From src/frontend.
npm ci
npm run lint
npm run test -- --run
npm run build
```

The scaffolding task must create the scripts/test configuration or update these
commands to match reality. None currently exists; these commands have not been run.
Do not substitute production data or keys when configuring tests. Run relevant
checks per task and full required checks before a release; record failures honestly.

Each task's handoff/evidence references the requirement IDs above. Record whether
each result comes from an automated test, manual walkthrough, static review, or
real-provider evaluation. Keep sanitized agent summaries in `logs/` and use them
to support concrete reflections; do not fabricate approvals or completed work.

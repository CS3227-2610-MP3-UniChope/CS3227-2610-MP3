# Core Migration Increment Plan

Status: First increment approved on 20 October 2026; implementation
verified by independent review for shared models/read persistence. Credential boundary
and production migration remain pending; final human implementation acceptance is pending.
Date: 20 October 2026
Owner: Huanghao
Specification: [Core migration design and acceptance checks](TECHNICAL-SPEC.md).
Inputs: [Scoped review manifest](review-inputs-2026-10-20.json).

## First Implementation Increment: Shared Models and Read Persistence

### C1: Preserve baseline and establish build verification

- [x] Human approved shared models, PostgreSQL schema/repositories, and Tutor read
  integration on 20 October 2026; credential boundary explicitly pending. No commit/push.
  Starting implementation inputs: [manifest](implementation-inputs-2026-10-20.json).
- [x] Inspect wrapper/build scripts; use a writable declared Gradle cache and normal
  Gradle test/bootJar commands. Resolve environment restrictions through permitted
  execution settings; do not edit production behavior to bypass the sandbox.
- [x] Save test reports; distinguish blocked/skipped tests from actual passing runs.
- [x] Verify a disposable PostgreSQL runtime before DB tests. Local implementation
  uses a temporary PostgreSQL 17.6 binary, not Docker/Testcontainers; report that distinction.

Verify CORE-05. No global runtime installation or production database access is
implicitly authorized by this plan. If DB execution is blocked, record the blocker
and limit delivered scope without claiming integration acceptance.

### C2: Adapt shared domain and reconcile Tutor types

- [x] Write domain/compatibility tests, then adapt the mapped MP2 models/validation
  to `src/backend/src/main/java/unichope/platform/core/model/`.
- [x] Update existing Tutor read code/tests to shared role/status/domain types.
  Keep UI/query projections in Tutor and avoid duplicate authoritative types.
- [x] Add necessary core read contracts under `platform/core/repository/`;
  do not expose unrestricted slot/booking writes or copy the auth/note bundle.
- [x] Record exact MP2 source revision, files, changes, and measured reuse.

Verify CORE-01 and the focused part of CORE-04. Preserve the current AI foundation
and unrelated uncommitted work. Do not implement Student/Admin feature code.

### C3: Add PostgreSQL migration and repositories

- [ ] Resolve the credential-storage proposal with auth; update the reviewed contract
  before fixing V1 columns. Check migration numbering against other role work.
- [x] While that decision is pending, prepare review-only credential-free DDL under
  `db/core-review/`, used by explicit isolated tests; do not activate production V1.
- [x] Write PostgreSQL migration/constraint/repository tests using valid related
  synthetic users, modules, assignments, slots, and bookings.
- [x] Add JdbcClient, Flyway, PostgreSQL driver, and compatible test dependencies.
- [x] Implement review-only DDL for the five core tables and read repositories. Tests may
  seed synthetic records via test-only SQL; no public raw mutation API is needed.
- [x] Configure explicit isolated test database settings and preserve no-database
  unit/startup tests. Development application datasource/bean wiring is not activated.
- [ ] Finalize auth-compatible production migration and application database wiring
  after the credential boundary is confirmed; neither is claimed by the review-only DDL.

Verify CORE-02, CORE-03 and CORE-05. Persist JUnit XML/HTML reports from the actual
Gradle run; do not reuse reports from the earlier AI-only scaffold as core evidence.

### C4: Connect Tutor read service to PostgreSQL

- [x] Add `unichope/tutor/PostgresTutorReadRepository` using agreed core contracts
  and a joined Tutor projection where necessary. Keep platform free of Tutor imports.
- [x] Test active account/role, owner-only slots, active assigned-module joins,
  strictly future eligible statuses, SGT filtering, deterministic ordering, and empties.
- [x] Record the missing real session/HTTP/React dependencies explicitly. Use trusted
  synthetic actors only in tests; do not add a browser actor selector.

Verify CORE-04 and CORE-05. Deliverable is real PostgreSQL-backed service behavior,
not a claim of a released or authenticated browser application.

### C5: Independent Review and Human Acceptance

- [x] Independent reviewer inspects actual source/schema/spec/reuse mapping and
  reproduces the normal Gradle and PostgreSQL checks within declared permissions.
- Not required in this review: no actionable findings were raised. Any later
  developer fix still requires independent re-review.
- [x] Record commands/results, input/output hashes, report locations, limitations,
  and required teammate integration. Human acceptance stays pending until supplied.

No commits, pushes, deployments, live SoC calls, or production data operations unless
separately requested. Keep guides/reflections local per the user's preference.

## Follow-Up Increment: Consultation Lifecycle

The [lifecycle spec](LIFECYCLE-SPEC.md) and [lifecycle plan](LIFECYCLE-PLAN.md)
cover the completed lifecycle increment. Scope approval and implementation acceptance
were recorded on 21 October 2026. This is separate from the earlier read increment.

After the read-persistence increment, use the same specify/plan/test/implement/review
cycle for explicit lifecycle mutations. The integration spec already defines the
operation matrix; refine only missing API/error/transaction details before coding.

- [x] Slot creation/cancellation with tutor/module locks and overlap checks.
- [x] Student booking/cancellation with student/tutor/module/slot/booking locks.
- [x] Tutor completion with atomic booking/slot updates and active-owner checks.
- [ ] Shared lock support for eligibility changes and later Tutor note writes.
- [x] PostgreSQL concurrency/rollback tests for every CORE-06 case.

Student/Admin owners implement their own role endpoints/services/UI around the
shared lifecycle. Auth retains sessions/credentials; Student retains AI gateway and
quotas. This plan does not authorize those role implementations.

## Proposed File Scope for Developer Dispatch

- `src/backend/build.gradle` and required non-secret dev/test resources.
- `src/backend/src/main/java/unichope/platform/core/` and matching tests.
- Relevant existing Tutor models/read service/tests plus its PostgreSQL adapter.
- `src/backend/src/main/resources/db/migration/` after numbering/auth coordination.
- Test-only fixtures, reuse inventory, core plan/status, handoffs, sanitized logs.

Exclude `platform/ai`, auth implementation, other role code, frontend feature code,
production resources, and unrelated dirty files. If shared build/config changes affect
another owner, describe and coordinate that impact before implementation.

## Current Evidence

- [Developer handoff](../handoffs/2026-10-20-developer-core-increment.md).
- [Independent review](../handoffs/2026-10-20-review-core-increment.md).
- [Retained test counts](evidence/summary.json): 13 unit + 6 real PostgreSQL tests,
  zero failures/errors/skips; normal Gradle test tasks and bootJar succeeded.
- [Runtime provenance](evidence/postgresql-runtime.json): temporary PostgreSQL 17.6,
  stopped after independent review. Docker/Testcontainers were not used.

The read-increment evidence above predates lifecycle implementation. The separately
approved [lifecycle increment](LIFECYCLE-PLAN.md) now adds five guarded operations,
independently verified with 14 unit + 36 PostgreSQL tests and bootJar. R1 timestamp
precision is fixed and independently closed. The lifecycle
implementation was accepted on 21 October; see its plan for the acceptance record.
Auth/session/HTTP/React integration, automatic datasource, production
Flyway activation, commit and push remain outside this increment.

## Next proposed increment: auth account storage

The separate [auth storage specification](AUTH-STORAGE-SPEC.md) and
[implementation plan](AUTH-STORAGE-PLAN.md) prepare password-hash storage,
session generations and guarded account operations against the proposed PR1 base.
The [independent test matrix](AUTH-STORAGE-TEST-MATRIX.md) defines planned checks.
The proposal is now implemented and independently verified in PR #2, based on PR #1;
affected-owner agreement, merge and final acceptance remain pending. PR1's
credential-free review schema is preserved, with an isolated auth-review extension.

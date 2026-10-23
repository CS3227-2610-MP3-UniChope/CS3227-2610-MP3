# Consultation Lifecycle Increment Plan

Status: Implemented, independently verified (R1 closed), and accepted on 21 October 2026.
Date: 21 October 2026
Owner: Huanghao
Specification: [Lifecycle operations and acceptance checks](LIFECYCLE-SPEC.md).
Inputs: [Preparation manifest](lifecycle-inputs-2026-10-21.json).

## L1: Review and Tests First

- [x] Separate test designer produced the [28-case test matrix](LIFECYCLE-TEST-MATRIX.md)
  for LC-01–LC-12 before implementation. Coordinator clarified two failure-code
  ambiguities in the spec; The proposed scope was accepted on 21 October.
- [x] Scope approval: 21 October 2026. This authorizes implementation and review,
  not credentials, production activation, commit/push, or final acceptance.
- [x] Capture developer input hashes including existing dirty/untracked source:
  `lifecycle-developer-inputs-2026-10-21.json`.
- [x] Write meaningful unit/PostgreSQL tests for LC-01 through LC-05 before methods.
  Map them to the designer's case IDs; do not change expected behavior to fit code.
- [x] Record initial failures; use synthetic users/modules/assignments and the
  existing review-only Flyway schema. Keep auth credential decision unresolved.

## L2: Implement Guarded Operations

- [x] Add ConsultationLifecycle and typed failures under `platform/core/`.
- [x] Implement PostgreSQL create/cancel/book/cancel/complete with the existing models,
  transaction-bound JdbcClient, injected Clock, ordered row locks and post-lock checks.
- [x] Keep raw writes/internal lock support inside PostgreSQL implementation; no
  new public repository mutations or unrestricted transaction callback.
- [x] Preserve role-specific DTOs and interfaces; add no controller, browser, auth,
  notes, AI, Admin management feature, production schema or datasource activation.

## L3: Concurrency and Rollback Verification

- [x] Write bounded independent-connection races for LC-06 through LC-09.
- [x] Test rollback on an intentional database write failure, and lock timeout
  handling, LC-10/11. Failure injection belongs to test fixtures, not product hooks.
- [x] Run normal `test postgresTest bootJar` against isolated PostgreSQL; retain
  actual reports and show that existing read/AI tests remain green, LC-12.
- [x] Update reuse inventory for actual MP2 lifecycle rules/tests adapted; do not
  count new PostgreSQL locking as copied SQLite implementation.

Database runtime must be explicitly selected and synthetic. The previous temporary
server was stopped; verify availability before this increment's test runs. No global
installation, live credentials, SoC, production access or deployment is authorized.

## L4: Independent Review and Human Acceptance

- [x] Separate reviewer inspects source/locks/error mapping/test quality, checks
  preservation of unrelated work, and independently reproduces relevant tests.
  Compare coverage/oracles with the independent matrix and verify any deviations.
- [x] Developer fixed R1 timestamp precision; independent reviewer reproduced and closed it.
- [x] Record handoffs, input/output manifests, actual commands/results/limitations,
  and sanitized interaction summary. Human implementation acceptance stays pending.

## Proposed File Scope

- New `ConsultationLifecycle.java` and `LifecycleFailure.java` in platform/core.
- `platform/core/postgres/PostgresConsultationLifecycle.java` and minimal internal
  transaction/lock helpers only if needed by more than one operation.
- Matching lifecycle tests and necessary shared test fixtures under backend tests.
- Build dependency adjustment for Spring transactions only if existing spring-jdbc
  dependencies do not already provide it; no unrelated build refactoring.
- Lifecycle documents/status, reuse inventory, scoped handoffs and sanitized logs.

Do not change public read contracts, AI infrastructure, other role implementations,
credential schema, default db/migration, or unrelated uncommitted files. Coordinate
any unexpectedly necessary shared-contract change before expanding the task.
No commit or push unless separately requested.

## Verification progress — 21 October

The initial full suite passed (14 unit + 33 PostgreSQL tests), but independent
review found R1: Java nanoseconds could round to different stored timestamps,
including a nonfuture stored slot. Three focused regressions failed before the fix.
Microsecond normalization now preserves the specified future/duration invariants,
and the full suite passes again. Independent re-review closed R1 and reproduced
14 unit + 36 PostgreSQL tests (zero failures/errors/skips) and bootJar. The synthetic
PostgreSQL server was stopped after review; human acceptance remains pending.

- [Developer handoff](../handoffs/2026-10-21-developer-lifecycle.md).
- [Independent review and R1](../handoffs/2026-10-21-review-lifecycle.md).
- [Coverage map](evidence/lifecycle-coverage.md).
- [R1 failing regressions](evidence/lifecycle-r1-red.log).
- [Fixed full run](evidence/lifecycle-r1-green.log).

Final independent reports: [counts and XML](evidence/lifecycle-review-r1/summary.json).

## Human acceptance

**Human acceptance:** Approved on 21 October 2026.
Scope: completed lifecycle increment, including R1.
See the [acceptance record](../handoffs/2026-10-21-lifecycle-human-acceptance.md).
Credentials, production activation, commits/pushes and future increments remain
outside this acceptance.

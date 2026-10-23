# Core Authentication Storage Plan

Status: Implemented, independently verified and published as PR #2 on 21 October 2026.
Teammate agreement and final implementation acceptance remain pending.
Date: 21 October 2026
Specification: [account storage contract](AUTH-STORAGE-SPEC.md).
Inputs: [preparation manifest](auth-storage-preparation-inputs-2026-10-21.json).

## AS1: Resolve design and independently specify tests

- [x] Separate test designer derived the [25-case matrix](AUTH-STORAGE-TEST-MATRIX.md)
  for CA-01 through CA-12. These are designed scenarios; no executable checks ran.
- [ ] Review physical-user-row versus secret-bearing Java User interpretation,
  selected email constructor/Unicode lookup policy, trusted Admin generation input,
  isolated migration route and operation boundaries with affected owners.
- [x] Human authorized implementation and a focused PR on 21 October 2026;
  assumptions are recorded in the specification. Capture refreshed exact inputs;
  PR1 is proposed/open, not a merged implementation baseline.

## AS2: Add isolated storage extension and internal reads

- [x] Preserve PR1 core-review V1 bytes/tests; add separate auth-review extension
  on an empty isolated schema before explicit-column fixtures. Use NOT NULL,
  nonblank hashes without defaults; test atomic failure on existing credentialless
  rows. No production/default V1 or invented migration/backfill strategy.
- [x] Test and implement AccountState/internal redacted credential snapshot reads.
  Ordinary UserRepository remains secret-free; no extra credentials table.
- [x] Implement the documented account input/normalization proposal with independent
  designer oracles; do not silently narrow auth or break constructor reads.

## AS3: Guarded account creation and activation

- [x] Tests first, then atomic register/provision and deactivate/reactivate operations.
- [x] Actor/target sorted user locks, fresh authorization/generation/blockers, injected
  Clock, shared lifecycle concurrency rules; no generic raw writes or callbacks.
- [x] Verify unique races, same-state behavior, generation overflow/rollback and
  history retention. Avoid nested independent transactions presented as atomic work.

## AS4: Login confirmation storage handoff

- [x] Test and implement locked role/hash/generation re-read against the observed
  snapshot, returning confirmed state only if all still match and account is active.
- [x] Simulate already-completed verification; add no hashing, session issuance or
  throttle implementation. Record auth's postcommit/deadline/request-check duties.
- [x] Demonstrate ordered deactivation/reactivation and stale confirmation failures.

## AS5: Independent review and evidence

- [x] Run real PostgreSQL suites and normal regressions/build against explicitly
  selected disposable runtime; retain current commands/reports/hashes.
- [x] Separate reviewer checks sources, safe projections, migration isolation,
  transaction/race tests and no unrelated changes; developer fixes and reviewer rechecks.
- [ ] Human final implementation acceptance stays pending until actually supplied.
  Commit/push and a focused PR are authorized for this proposal. Merge, deployment
  and production migration activation remain outside this task.

## Smallest proposed implementation boundary

New core account operation/read contracts, safe internal values/errors and PostgreSQL
implementation; targeted core Validation/read changes only after policy review;
new auth-review migration, isolated fixtures and matching tests; minimal shared lock
helper extraction if genuinely required. Preserve lifecycle semantics and PR1 SQL.
No auth/Admin controllers, password verifier, session adapter, Student AI, bootstrap
API, password reset, role edits or default datasource/Flyway activation.

Eventual production schema numbering/activation and real auth integration are later
reviewed tasks. The present plan establishes no team approval or test results.

## Current verification and review

[PR #2](https://github.com/CS3227-2610-MP3-UniChope/CS3227-2610-MP3/pull/2),
commit 3f3ec8b, targets PR #1 branch prepare/core-sharing while PR #1 remains open.
Developer and independent reviewer each passed 6 unit + 57 PostgreSQL tests with
zero failures/errors/skips and bootJar. Original V1 and baseline tests are unchanged.
See [independent review](../handoffs/2026-10-21-review-auth-storage.md) and
[case mapping](evidence/auth-storage-implementation-2026-10-21/developer/case-to-test.md).
The submicrosecond future-slot guard regression was reproduced before its fix and
independently verified afterward. The disposable database is stopped. No production
migration, actual password/session implementation or owner agreement is implied.

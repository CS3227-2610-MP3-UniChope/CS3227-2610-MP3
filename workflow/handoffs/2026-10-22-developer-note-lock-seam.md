# Developer handoff: core private-note lock seam

Date: 22 October 2026. Role: core developer. Implementer: `/root`.

## Inputs and scope

- Source worktree: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/core-note-lock-seam`, branch `feat/core-note-lock-seam`, baseline HEAD `3f3ec8b99215b8b4cba9b88e6572465c88c47a7e`.
- Source input manifest: [developer source inputs](2026-10-22-developer-note-lock-seam-inputs-source.json). It covers the inspected core sources, PostgreSQL fixtures/tests, build file, and review schema, including untracked source and test inputs.
- Workflow/spec/test-design manifest: [developer workflow inputs](2026-10-22-developer-note-lock-seam-inputs-workflow.json).
- Requirements: [spec](../core/NOTE-LOCK-SEAM-SPEC.md), [plan](../core/NOTE-LOCK-SEAM-PLAN.md), [test matrix](../core/NOTE-LOCK-SEAM-TEST-MATRIX.md), architect and test-designer handoffs.
- Authorized scope: operation-specific core API/result and PostgreSQL locking method. Tutor note schema, persistence, endpoints, AI, authentication, production migration activation, commit, and push were not authorized by this increment.

## Changes

- Added `ConsultationLifecycle.lockForPrivateNote(tutorId, bookingId)` and immutable `LockedCompletedBooking` identifier result.
- Added a separate Spring `PROPAGATION_MANDATORY` transaction path using the same configured `DataSource` and transaction manager as the lifecycle adapter. Missing or differently bound transaction fails before executing the callback; non-READ-COMMITTED caller transactions fail before lock queries.
- Added bounded transaction-local lock and statement timeouts, relationship discovery, UUID-sorted users → module → slot → booking row locks, locked actor/ownership/status rechecks, and sanitized recognized contention SQLSTATE mapping. Caller must allow failures to escape its transaction before HTTP/error mapping.
- Existing `REQUIRES_NEW` lifecycle mutations were not changed. No Tutor dependency, note data, callback, connection handle, or arbitrary SQL interface was introduced.
- Added real-PostgreSQL tests for transaction requirement and resource identity, lock retention and caller rollback, identifier-only result, role/activity/ownership/status checks, non-READ-COMMITTED rejection, post-wait rechecks, lock order, caller failure rollback, and bounded lock-timeout mapping.

## Controls and verification

- Runtime controls: workspace-write, restricted network, auto-review; offline Gradle with cached dependencies. PostgreSQL was a disposable local test server at `127.0.0.1:55439`, using synthetic rows and per-test schemas. No production database, secret, SoC API, deployment, or external messaging was used.
- Gradle needed permission for its local socket-based lock service; escalation was approved for offline local builds. Starting the disposable local PostgreSQL instance also used the approved local command escalation.
- TDD evidence: the first compile of the new PostgreSQL tests failed because the `lockForPrivateNote` API was missing. The implementation was then added. The first focused PostgreSQL run exposed a wrong-role caller receiving `NOT_FOUND` before actor denial; the pre-lock ownership rejection was removed, and the focused suite passed afterward.
- Focused PostgreSQL command: `GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain postgresTest --tests unichope.platform.core.postgres.PostgresPrivateNoteLockTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=`. Passed after the actor-check fix; 13 seam tests in the final full run.
- Final command: `GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=`. Exit 0, `BUILD SUCCESSFUL`: 6 unit tests and 70 PostgreSQL tests; zero failures, errors, or skips; `bootJar` succeeded.
- Reports: `core-note-lock-seam/src/backend/build/reports/tests/test/index.html`, `core-note-lock-seam/src/backend/build/reports/tests/postgresTest/index.html`; artifact `core-note-lock-seam/src/backend/build/libs/unichope-0.0.1-SNAPSHOT.jar`.
- `git diff --check` passed in the source worktree. Source changes are limited to `ConsultationLifecycle.java`, `PostgresConsultationLifecycle.java`, new `LockedCompletedBooking.java`, and new `PostgresPrivateNoteLockTest.java`.

## Independent review and fix

- The independent reviewer found one Important issue: before the fix, unauthorized/missing-role callers could distinguish an existing from a nonexistent booking because relationship discovery ran before actor authorization.
- Added a RED regression case where a wrong-role Student ID requests a nonexistent booking; the focused PostgreSQL suite failed as expected with `NOT_FOUND` instead of the required `ACCESS_DENIED`.
- Fixed by checking the active TUTOR actor before booking discovery while retaining the post-lock actor recheck to handle deactivation while waiting.
- The reviewer’s first-pass report found no other blocking issue in transaction joining, lock ordering, post-wait state checks, timeout rollback, or API scope.
- Post-fix focused suite: all 13 `PostgresPrivateNoteLockTest` cases passed. Post-fix full suite: 6 unit and 70 PostgreSQL tests, zero failures/errors/skips; `bootJar` and `git diff --check` passed.
- Independent re-review closed the Important authorization-order finding, reproduced the focused and full verification plus `git diff --check`, and found no new issues.
- Human acceptance: Approved on 22 October 2026; scope: core private-note lock seam implementation and its independent review. This acceptance does not authorize commit or push.

## Limitations and next role

- PostgreSQL tests verify core table lock order and caller transaction behavior. They do not prove Tutor note-row ordering, note first-save races, version CAS, endpoints, HTTP/session authorization, or AI behavior; those belong to later increments.
- `NLS-09` is left for independent source/API review. Independent review is now requested; no recommendation or acceptance is claimed yet.
- Affected teammate review and human acceptance remain pending. The source worktree has uncommitted changes only. No commit or push was made.

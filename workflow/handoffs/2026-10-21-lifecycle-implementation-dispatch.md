# Lifecycle implementation dispatch — 21 October 2026

Task: CORE-LIFECYCLE-IMPLEMENT-01. Coordinator: `/root`.
Developer: `/root/core_increment_developer`; independent reviewer to follow.

## Human authorization

Scope approval: 21 October 2026, for implementation, tests and independent review
of the lifecycle specification and 28-case matrix. Credentials, production
activation and commits/pushes remain outside scope.

## Inputs and scope

Read `workflow/agents/common.md`, `developer.md`, the lifecycle spec, plan, matrix
and test-designer handoff. Initial working-copy identities are retained in
`workflow/core/lifecycle-developer-inputs-2026-10-21.json`.
After capture, coordinator replaced one stale “awaits review” sentence at the end
of spec section 5 with the actual approval status. No contract/oracle changed.

Allowed implementation: ConsultationLifecycle, LifecycleFailure, PostgreSQL
implementation and only necessary internal helpers, matching lifecycle unit/PG
tests/fixtures, minimal build dependency if needed, reuse inventory and assigned
handoff/evidence. Preserve earlier dirty/untracked work. No read API changes,
other-role code, AI, auth, credentials, default migration, datasource activation,
HTTP/UI, global installs, production resources, commits or pushes.

Write behavioral tests first and retain initial failures. Map all 28 designer cases
and LC-01–12 to executable evidence or explicit limitations. Reviewer must inspect
source and test quality and reproduce relevant checks; developer cannot self-approve.

## Runtime controls and outputs

Inherited workspace-write sandbox with project/temp writable roots, restricted
network and auto-review escalation policy. Role-specific scopes are instructions,
not enforced read isolation. Inspect scripts before execution. Escalations follow
actual tool permissions. No network downloads or live SoC calls are required.

Allowed generated outputs: `src/backend/build`, backend `.gradle`, existing
`/tmp/mp3-gradle-home`, existing synthetic `/tmp/mp3-core-pg-runtime` runtime/logs,
and scoped `workflow/core/evidence/lifecycle-*` evidence.

Coordinator verified existing temporary PostgreSQL was stopped (`pg_ctl status`,
exit 3), then started it with approved escalation (`pg_ctl ... -w start`, exit 0),
bound only to 127.0.0.1 port 55439. Explicit test JDBC URL:
`jdbc:postgresql://127.0.0.1:55439/postgres`, synthetic user `core_test`, empty
password for this existing local test cluster. Per-test isolated schemas required.
Server remains available through independent review and will then be stopped.

Gradle: existing wrapper Java 25 / Gradle 9.1; `GRADLE_USER_HOME=/tmp/mp3-gradle-home`.
Normal scoped acceptance command is offline `test postgresTest bootJar` with the
explicit disposable JDBC properties. Commands/results must be recorded as actually
run; planned checks are not passing evidence.

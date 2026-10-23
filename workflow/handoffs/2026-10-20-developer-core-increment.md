# Core Read-Persistence Developer Handoff

Task: CORE-READ-2026-10-20. Date: 20 October 2026 (SGT).
Developer: actual subagent `/root/core_increment_developer`; parent `/root` owns build/runtime execution.
Implementation authorization: human explicitly approved the first shared-model,
PostgreSQL schema/repository and Tutor-read increment in this chat. That authorizes
implementation, not final human acceptance. No commit or push authorized.

## Inputs and Scope

Input HEAD: `50c6213abdde9ee5063c7f333e448b33ac83d59b` plus the scoped dirty/untracked
contents identified in [implementation manifest](../core/implementation-inputs-2026-10-20.json).
Read architect handoff, core design/plan, developer/common role instructions,
existing Tutor read files/tests and MP2 models/repository interfaces/domain tests.
MP2 source revision: `d6a9a244f960854b2addb1546b45e506217f4823`.

User's credential boundary remains pending auth-owner confirmation. Parent dispatch
narrowed schema work to credential-free review-only DDL at
`src/backend/src/main/resources/db/core-review/V1__create_core_tables.sql`.
It must not move to default `db/migration` or become a production V1 before that
review. Public read interfaces do not expose secrets or unrestricted mutations.

Allowed writes: core model/repository/PostgreSQL classes and tests; existing Tutor
read types/service/tests and PostgreSQL adapter; isolated review DDL; reuse ledger;
this handoff. Parent exclusively owns `build.gradle`, runtime setup and core spec/
plan updates. No lifecycle writes, auth implementation, other role features, UI,
AI changes, install, production calls/data, real SoC calls, commits or pushes.
MP2 was inspected read-only. Pre-existing unrelated dirty files were preserved.

Inherited runtime: workspace-write, restricted network, auto-review escalation.
Writable roots include the MP3 project, task visualization directory and temporary
locations. Role limits are behavioral, not enforced read-only filesystem isolation.
Developer ran only scoped file reads, Python/file writes and static scans, with no
escalation or network. Parent runs Gradle/PostgreSQL under its reviewed execution
permissions. Permitted outputs include backend `.gradle/`, `build/`, reports and
parent-owned temporary Gradle cache/PostgreSQL runtime. No nested isolation claim.

Output source/reuse hashes are captured in
[output manifest](../core/implementation-outputs-2026-10-20.json). The handoff itself
is excluded to avoid a circular self-hash; parent captures final review inputs.

## Implementation

- Adapted 12 MP2 shared immutable domain/validation files, changing package/import
  prefixes only (181 physical source lines). Adapted 3 MP2 domain tests.
- Added explicit read-only User/Module/Slot/Booking repository contracts; no generic
  Repository with save/delete, note methods, credential access or raw state writes.
- Added four parameterized JdbcClient PostgreSQL repositories, preserving UUIDs and
  absolute timestamptz instants, case-insensitive email/code lookup, assignment and
  owner/booking reads.
- Added review-only five-table PostgreSQL DDL: actual foreign keys, status/time/
  nonblank checks, normalized unique email/code indexes and one ACTIVE booking per
  slot. No credentials, default admin, notes, quota tables or production migration.
- Retired temporary Tutor-local AccountRole/SlotStatus/ConsultationSlot. `TutorSlot`
  now wraps the authoritative core slot with joined module code; TutorAccount stays
  a credential-free projection using shared Role.
- Added Tutor-owned PostgreSQL adapter with owner-scoped slot/module SQL joins and
  current account reads; service checks actor role/active status. Core imports no
  role package. No session/HTTP/controller/React wiring is claimed.
- Preserved the four existing Tutor test scenarios after shared-type adaptation.
- Added six actual PostgreSQL tests using explicit `core.test.jdbcUrl/user/password`.
  Each creates/drops only its generated isolated schema, explicitly migrates the
  single review-only DDL twice to verify idempotency, and uses synthetic fixture SQL.
  Tests fail when configuration is absent; they do not silently skip.
- Updated [reuse inventory](../REUSE-INVENTORY.md) with measured actual adaptation;
  no unsupported percentage or SQLite source-port claim.

## Verification and Evidence

1. Tests first: adapted DomainTest was written referencing absent shared classes.
   Parent executed Gradle `compileTestJava` and reported exit 1, 13 missing-symbol
   errors. This was observed parent execution, not a developer-run success claim.
2. Static scoped scan found no trailing Java whitespace and no core imports from
   Tutor/Student/Admin/auth/AI/JavaFX/SQLite.
3. Normal Gradle and real PostgreSQL results are supplied by the parent execution
   record below before this handoff is considered ready for independent review.

### Parent Execution Record

Parent executed from `src/backend`:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Actual result: exit 0, BUILD SUCCESSFUL, 7 tasks executed; **13 unit tests and 6
PostgreSQL tests, zero failures/errors/skips**. Developer independently read the
retained summary and build log; independent reproduction remains review work.
Disposable PostgreSQL 17.6 was parent-provisioned under `/tmp`, bound to loopback,
using only synthetic fixtures and unique per-test schemas, not Docker/Testcontainers
or production. Parent owns stopping the runtime after review.

Retained evidence:

- [Build log](../core/evidence/2026-10-20-core-gradle.log).
- [Counts](../core/evidence/summary.json).
- [PostgreSQL XML](../core/evidence/postgresTest/TEST-unichope.platform.core.postgres.PostgresCoreReadTest.xml).
- [Domain XML](../core/evidence/test/TEST-unichope.platform.core.model.DomainTest.xml).
- [Tutor XML](../core/evidence/test/TEST-unichope.tutor.TutorReadServiceTest.xml).
- Backend generated HTML: `src/backend/build/reports/tests/test/index.html` and
  `src/backend/build/reports/tests/postgresTest/index.html` (local build artifacts).

Parent additionally aligned UTF-8/`-Xlint:all` and toolchain resolver conventions
with MP2 under the user's later instruction. Existing AI `AiGateway.Failure`
serialVersionUID warning became visible; Student-owned AI code was preserved.
Gradle wrapper remained the already pinned 9.1.0 version. BootJar success proves
packaging, not configured database startup or a deployed authenticated app.

## Remaining Boundaries and Next Role

- Auth-owner credential decision is unresolved; no deployable core V1 exists.
- Lifecycle transactions, concurrent writes/locking and CORE-06 remain later work.
- Real session, endpoint, browser and deployment integrations remain unimplemented.
- PostgreSQL access is a synthetic trusted-actor service test, not authenticated
  HTTP acceptance. Passing data-read tests is not application AI security evidence.
- Shared account/module management mutation APIs remain outside this read slice.

Next: independent reviewer checks actual source, tests, schema placement, credential
boundary, MP2 mapping and report evidence; independently reproduces allowed checks,
then returns numbered findings. Developer fixes require independent re-review.
Human acceptance of the implemented increment remains pending.

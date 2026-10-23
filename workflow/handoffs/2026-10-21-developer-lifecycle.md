# Consultation Lifecycle Developer Handoff

Date: 21 October 2026 (SGT). Task: CORE-LIFECYCLE-IMPLEMENTATION-01.
Actual developer: `/root/core_increment_developer`; coordinator `/root` executes
Gradle/database runtime probes. Implementation acceptance: pending human review.

## Authorization, inputs and controls

Scope approval: 21 October 2026, for the lifecycle specification and test matrix. See [dispatch](2026-10-21-lifecycle-implementation-dispatch.md).
This authorizes implementation/tests/independent review, not credentials, production
activation, commit/push or final implementation acceptance.

HEAD is `50c6213abdde9ee5063c7f333e448b33ac83d59b` with relevant pre-existing dirty and
untracked contents in [developer input manifest](../core/lifecycle-developer-inputs-2026-10-21.json).
Inputs read: lifecycle spec/plan/28-case matrix/test-designer handoff, current
common/developer role instructions, build/settings/wrapper and review DDL, existing
core models/read repositories/test fixture patterns. Coordinator subsequently
corrected stale review-status prose in spec §5; no oracle/contract change. Dispatch
was added after initial capture; these timing differences do not imply a new scope.
MP2 lifecycle interface/SQLite implementation, Tutor slot rules and lifecycle
contract tests inspected read-only at reference `d6a9a244f960854b2addb1546b45e506217f4823`.

Allowed writes: three new product classes, matching four lifecycle test/fixture
files, reuse ledger, scoped evidence and this handoff. No read-API/schema/role/AI/
auth/credential/controller/UI changes; no default migration/datasource activation.
No build dependency change needed: spring-tx already arrives through spring-jdbc.
No installation, global runtime edits, external messaging, production resources,
real SoC calls, commits or pushes. Pre-existing dirty work preserved.

Inherited runtime: workspace-write in supplied project/visualization/temp roots,
restricted network, auto-review escalations. Custom role scope is behavioral, not
OS read isolation. Developer used scoped filesystem reads/writes/static inspection,
no escalations or network. Coordinator used reviewed escalation for offline Gradle
and loopback PostgreSQL sockets. Generated outputs: backend build/.gradle, reviewed
`/tmp/mp3-gradle-home`, synthetic `/tmp/mp3-core-pg-runtime`, scoped lifecycle evidence.
Coordinator started stopped PostgreSQL 17.6 on loopback port 55439, retained it for
independent reproduction and owns stopping it afterward. No Docker/global install.

## Implementation and transaction contract

- New `ConsultationLifecycle` exposes create/cancel available slot and
  book/cancel/complete booking, using trusted actor IDs and internally generated IDs.
- New `LifecycleFailure` provides nine stable codes with safe generic messages,
  without SQL diagnostics, resource IDs or nested SQL causes in mapped failures.
- New `PostgresConsultationLifecycle` uses one datasource-bound JdbcClient and
  Spring transaction manager. Each mutation owns a REQUIRES_NEW/READ_COMMITTED
  transaction so commit/rollback finishes before results or SQLSTATE mapping.
  Callers should invoke this boundary without holding conflicting outer locks.
- Preliminary actor check does not lock. Relationship discovery precedes the full
  ordered lock set. Users are locked by PostgreSQL UUID order, then module, slot,
  booking. Current authoritative actor/role/state/eligibility and applicable server
  clock checks occur after locks. Tutor parent and Student parent locks cover
  empty-schedule inserts and cross-slot overlaps.
- Half-open overlap rules, cancelled/completed history, early completion, cancellation
  of past AVAILABLE slots and completion despite nonactor ineligibility follow the
  approved spec. No extra rules were invented.
- Booking first inserts/updates its booking, then updates its slot in the same
  transaction. Unexpected SQL errors propagate for later sanitized web handling;
  55P03/40P01/40001/57014 are mapped after rollback to temporary unavailability,
  with no automatic retry. SET LOCAL lock_timeout=5s and statement_timeout=10s
  bound individual waits/statements, not an overall transaction duration.
- Read repository APIs and review-only credential-free DDL remain unchanged.

## Tests-first and fresh verification

Before product implementation, wrote sixteen sequential PG methods for TD-01–16
and shared isolated fixture. Coordinator ran offline Gradle `compileTestJava`:
exit 1, 53 expected missing-symbol errors for absent LifecycleFailure and
PostgresConsultationLifecycle, retained [RED log](../core/evidence/lifecycle-red.log).
Then wrote eleven concurrent/rollback/mapping PG methods TD-17–27 before implementing
product classes. Added one no-database input unit test afterward to supplement the
already-written TD-01 PG tests; it is not claimed as the initial red evidence.

First full implementation run: coordinator executed offline normal Gradle
from `src/backend`:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

with explicit synthetic datasource:
`jdbc:postgresql://127.0.0.1:55439/postgres`, user `core_test`, empty fixture password.
Result: **exit 0, BUILD SUCCESSFUL in 19s, 7 tasks executed**. Parsed retained XML:
**14 unit tests + 33 PostgreSQL tests; zero failures/errors/skips**. This includes
all existing domain/read/Tutor/AI regressions and 27 new PG methods. No earlier
report was reused as evidence for the new increment. Only pre-existing AI
serialVersionUID and JVM class-sharing warnings appeared; Student code preserved.

Evidence:
- [Full GREEN log](../core/evidence/lifecycle-green-attempt-1.log).
- [Counts](../core/evidence/lifecycle-developer/summary.json).
- [Executable TD-01–28 map and limitations](../core/evidence/lifecycle-coverage.md).
- [Sequential PG XML](../core/evidence/lifecycle-developer/postgresTest/TEST-unichope.platform.core.postgres.PostgresLifecycleTest.xml).
- [Concurrent PG XML](../core/evidence/lifecycle-developer/postgresTest/TEST-unichope.platform.core.postgres.PostgresLifecycleConcurrencyTest.xml).
- [Input unit XML](../core/evidence/lifecycle-developer/test/TEST-unichope.platform.core.postgres.LifecycleInputTest.xml).
- Local generated HTML under `src/backend/build/reports/tests/{test,postgresTest}/index.html`.

There were no substantive oracle changes or first-green fixture repairs. Grouping
and in-method variants are documented in the coverage map; 28 matrix rows do not
mean 28 independently executed JUnit methods or a coverage percentage.

## Concurrency evidence and limits

Fixtures create a unique schema, explicitly apply review DDL and drop only their
schema. Snapshot observations use fresh independent physical connections. Held-row
fixtures observe blocked callers through pg_blocking_pids before release; there
are bounded futures/polls/shutdowns, rather than a race inferred from sleeping.
Both queue orders run for booking-vs-slot-cancel and cancel-vs-complete.

Test-only triggers verify a booking write is visible, then fail the subsequent
slot write; fresh observations confirm rollback for insert/cancel/complete. Real
5-second lock timeout runs on a deterministic reused physical connection, then an
explicit new call succeeds and transaction-local timeouts are reset.

SQLSTATE trigger tests verify mapping/rollback and local limits for 55P03/40P01/
40001/57014; these are not actual induced deadlock/serialization/statement-timeout
runs. The real blocking timeout is separately exercised. Finite interleavings do
not prove universal deadlock freedom; independent source review is still required.
No authenticated HTTP, UI, SoC, app AI security or deployment restart claim follows
from these core SQL tests. Credential decision stays pending, no deployable V1.

## Next role

Independent reviewer checks product source, lock order/transaction binding/error
mapping, preservation of unrelated inputs, source attribution and matrix coverage;
reproduces normal and PostgreSQL suites, records numbered findings or scoped
recommendation. Developer then fixes any finding and independent reviewer rechecks.
Final human implementation acceptance remains pending. No commit or push.


## Independent-review fix R1

Reviewer found a real PostgreSQL precision mismatch despite the first green suite:
Java nanos were rounded on persistence; returned slot/booking timestamps differed,
and +1ns future start persisted at now. Reviewer probe/log records both an initial
probe SQL-comment parsing error and the corrected actual product observation;
[precision probe log](../core/evidence/lifecycle-review-precision.log). Do not treat
the initial fixture parsing failure as product evidence.

Captured [pre-fix scoped inputs](../core/lifecycle-r1-inputs-2026-10-21.json).
Coordinator authorized the scoped precision fix and documented the policy in spec:
truncate to microseconds before time/overlap validation and writes, compare normalized
start against actual post-lock Clock.instant(), reject collapsed/nonfuture intervals,
and normalize booking createdAt separately from actual-now eligibility checks.

Added three meaningful precision regressions before the fix, then coordinator ran
focused `postgresTest --tests '*PostgresLifecycleTest.r1*'` with the existing offline
flags and explicit disposable datasource. Exit 1; all three failed. Retained
[RED log](../core/evidence/lifecycle-r1-red.log) and
[RED XML](../core/evidence/lifecycle-r1-red/TEST-unichope.platform.core.postgres.PostgresLifecycleTest.xml).

Fixed only PostgresConsultationLifecycle timestamp normalization; no schema/read/
other-role edits. Added supported microsecond equality, normalized adjacency,
+1ns-to-now rejection, collapsed duration, nanos-clock boundary and booking exact
roundtrip checks. See [coverage extension](../core/evidence/lifecycle-coverage.md).
Fresh full command (same offline flags/datasource as above) exited 0 in 20s:
**14 unit + 36 PostgreSQL tests, zero failures/errors/skips; bootJar successful**.
Retained [fixed GREEN log](../core/evidence/lifecycle-r1-green.log),
[fixed counts](../core/evidence/lifecycle-r1-developer/summary.json) and XML alongside
that summary. Original first-run reports are preserved, not overwritten with fixed
results. Product/test source is frozen for independent re-review; human acceptance
remains pending.

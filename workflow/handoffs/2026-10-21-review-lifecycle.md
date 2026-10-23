# Independent Lifecycle Implementation Review

Date: 21 October 2026 (SGT). Reviewer: `/root/core_increment_reviewer`.
Scope approved by the actual human; implementation acceptance pending. Initial
recommendation: changes requested for R1; awaiting independent re-review of fix.

## Inputs, scope and controls

Read common/reviewer instructions, lifecycle spec/plan, independent 28-case matrix,
implementation dispatch, developer handoff/coverage and actual source/tests. Baseline
`lifecycle-developer-inputs-2026-10-21.json` hashed working inputs at HEAD
`50c6213abdde9ee5063c7f333e448b33ac83d59b`. Independent comparison found only expected
reuse-inventory and coordinator spec/matrix metadata differences; pre-existing
source/build/read schema/AI contents matched. Later R1 edits require fresh review.

Actual workspace-write, restricted network, auto-review escalation controls inherited.
Source-read-only is behavioral, not an enforced sandbox. Allowed generated output:
backend build/.gradle, /tmp/mp3-gradle-home, synthetic review probe temp area;
assigned writes: this handoff and lifecycle-review-* evidence. Used reviewed
require_escalated runs for offline Gradle and localhost PostgreSQL socket access.
No product source edits, SoC, credentials, production, global installs, commit/push.

## Initial independent verification

Inspected build scripts and all lifecycle/test sources before execution. Ran from
`src/backend`:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Exit 0, BUILD SUCCESSFUL in 19s, seven tasks executed. Fresh report counts: 14 unit
and 33 PostgreSQL tests, zero failures/errors/skips; bootJar built. Retained independent
XML/counts: `workflow/core/evidence/lifecycle-review-initial/`. Existing AI serial
warning remained untouched. Passing cases do not erase R1 below.

Source inspection confirms common transaction-bound JdbcClient/DataSource, explicit
READ COMMITTED/REQUIRES_NEW, database UUID-ordered user locks followed by modules,
slots, bookings, fresh actor/time/eligibility validation, parent locks protecting
empty schedules and cross-slot student overlaps. Test assertions map TD01–28,
using actual blocked physical connections and bounded waits. Trigger-induced
second-write failures verify rollback; real lock timeout uses/rechecks the same
physical connection; synthetic SQLSTATE cases are accurately distinguished from
real deadlocks/serialization failures. No new raw public writes, production schema,
auth/UI/AI behavior. Finite tests do not prove universal race/deadlock freedom.

## R1 — P2: Normalize persisted timestamp precision before validating/returning

File: `src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java`,
initial lines 63–76 and 110–129. Requirements: strict future/persisted intervals and
TD03/10 return-to-persisted equality. Java Instant can contain nanos; PostgreSQL
stores microseconds. Code returned original interval/clock values and validated
before PostgreSQL rounded them.

Independent synthetic probe with the actual compiled implementation showed:

- CREATE returned `2026-10-21T01:00:00.123456789Z`, stored `.123457Z`: unequal.
- BOOK returned createdAt `2026-10-21T00:00:00.123456789Z`, stored `.123457Z`: unequal.
- start `N+1ns` accepted but stored exactly `N`, violating strict future semantics.

Reproduction source: `/tmp/mp3-lifecycle-review/LifecyclePrecisionProbe.java`;
retained stdout/exit: `workflow/core/evidence/lifecycle-review-precision.log`.
Probe used only an isolated generated schema and cleaned it in finally. A first
attempt failed because probe SQL splitting mishandled a comment semicolon; its
setup failure is retained and corrected rerun exit 0 supplies the product evidence.

Required fix: explicit database-precision normalization before time/overlap checks
and writes, validate effective interval against the actual post-lock clock, return
persisted precision for slot/booking, and add nanos/rounding-boundary regressions.
Do not weaken strict future or fabricate supported-precision assumptions. Coordinator
assigned R1 to developer; independent re-review pending.

## Boundaries

No session/HTTP/browser/auth credential/production migration/application-AI security
acceptance. Mutation logic assumes compliant immutable relationships and shared
management locking; arbitrary SQL outside that protocol is not certified. No final
human acceptance or commit/push claimed. Preserve original independent records when
adding R1 fix verification below.

## Independent R1 Re-review — CLOSED

Fresh fix inputs: `lifecycle-r1-inputs-2026-10-21.json`; inspected developer fix
handoff, retained initial three failing regression reports, and
`lifecycle-r1-outputs-2026-10-21.json`. Independently checked every output hash:
zero mismatches before rerun. Fix normalizes requested start/end to microseconds
before every interval/overlap check and insert, compares the effective start against
the actual post-lock Clock instant, and separately normalizes booking createdAt.
Returned objects now match persisted values. Contract clarification documents the
technical precision policy without weakening strict future or positive duration.

Three new PostgreSQL regressions check nanosecond slot roundtrip/adjacency, collapsed
submicrosecond and no-longer-future intervals (including fractional clock), and
nanosecond booking-clock roundtrip. All failed before the fix as recorded; their
expectations were not weakened. Independently inspected implementation and these
oracles, then reran the identical offline `test postgresTest bootJar` command above
with granted socket escalation after developer archived its reports.

Fresh result: exit 0, BUILD SUCCESSFUL in 20s, seven tasks executed; **14 unit + 36
PostgreSQL tests, zero failures/errors/skips**, bootJar built. Retained independent
XML and counts: `workflow/core/evidence/lifecycle-review-r1/`; generated HTML:
`src/backend/build/reports/tests/{test,postgresTest}/index.html`. The unchanged
Student AI serial warning remains outside scope.

**R1 closed. Final recommendation: accept the scoped lifecycle implementation.**
No open actionable findings. This is reviewer recommendation, not human acceptance.
All earlier integration/security/production limitations remain. Parent can stop the
synthetic PostgreSQL server; no commit/push authorized or performed.

## Human acceptance

Approved on 21 October 2026; see the
[acceptance record](2026-10-21-lifecycle-human-acceptance.md).

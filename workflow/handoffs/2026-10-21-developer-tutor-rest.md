# Tutor REST developer handoff

Task: TUTOR-REST-IMPLEMENT-01. Date: 21 October 2026 (SGT).
Actual developer: `/root/core_increment_developer`; coordinator `/root` executed
reviewed Gradle/database probes. Human scope approval: approved on 21 October 2026;
scope: five Tutor REST endpoints, security-fixture integration and independent review.
Final implementation acceptance remains pending. No commit/push/deployment authorized.

## Inputs and boundaries

HEAD: `50c6213abdde9ee5063c7f333e448b33ac83d59b` plus 68 scoped working-copy inputs
in [manifest](../tutor/rest-developer-inputs-2026-10-21.json). Read current developer/
common instructions, REST spec/plan/22-case matrix, architect/test-designer handoffs,
existing Tutor/access/read/model/lifecycle interfaces, build/settings/wrapper and
cached framework artifacts. Coordinator's [dispatch](2026-10-21-tutor-rest-dispatch.md)
was added after input capture. Hash comparison after implementation found changes
only to authorized existing build/application/Tutor read/access files and reuse
ledger; other baseline files remained unchanged at that check.

Allowed changes: scoped Tutor HTTP/input/access/detail/mutation adapters/tests,
minimal platform/web error/security support, explicit activation wiring and necessary
MVC/Security/test dependencies; scoped coverage/evidence/reuse/handoff. No lifecycle
source/behavior, schema/default migration/datasource activation, production auth,
credentials, other roles, AI, React, production resources, commits or pushes.

Runtime: inherited workspace-write project/visualization/temp roots, restricted
network, auto-review escalation. File/role limits are behavioral, not OS read
isolation. Developer used scoped filesystem inspection/writes and local javap;
coordinator used reviewed escalations for Gradle dependency resolution/tests and
synthetic loopback PostgreSQL. Allowed outputs: backend build/.gradle,
`/tmp/mp3-gradle-home`, `/tmp/mp3-core-pg-runtime`, `/tmp/mp3-tutor-rest-*` and REST
evidence. No global install. Coordinator owns stopping disposable PostgreSQL after
independent reproduction. No live secrets, provider calls or production data used.

## Actual dependency compatibility inspection

Pinned Boot 4.0.8 dependency BOM lists starter-webmvc, starter-security and matching
starter-webmvc-test/security-test. Added exactly those four declared dependencies.
Coordinator resolved actual 89 test-runtime artifacts into the scoped cache; logs:
[dependency graph](../tutor/evidence/rest-dependency-resolution.log),
[artifact resolution](../tutor/evidence/rest-artifact-resolution.log).

Inspected actual Boot security JAR class names: generated user auto-configuration is
`org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration`.
Actual Boot MVC test annotation package is `org.springframework.boot.webmvc.test.autoconfigure`.
Inspected Jackson 3.1.5 classes/methods using zip/javap: `tools.jackson.databind`
JsonNode.propertyNames/isString/stringValue, JsonMapper builder/shared, and
`tools.jackson.core.StreamReadFeature`. Compiled/run Security is 7.0.7. No copied
old-version imports or assumed Boot-3 test packages were used.

## Implementation

- Five profile-gated `/api/tutor` handlers: modules, upcoming slots/date, owned detail,
  create and cancel. Controllers delegate business rules and SQL to existing services.
- Added reasoned TutorAccess reused by reads and mutations; each operation reads
  current account role/activity. Existing Optional UUID provider seam remains unchanged.
  Missing/deleted/inactive identities clear fixture authentication/session when mapped;
  wrong roles return safe forbidden. No principal parser, fake actor or login in main.
- Added owner-and-ID parameterized detail read with defensive owner check. Detail
  accepts every status/past slot without upcoming filtering. Created Location has a
  real target. Core and lifecycle source remain independent and unchanged.
- Strict adapter-local Jackson 3 tree parsing: exact string fields, duplicate/unknown/
  scalar/null/array/trailing JSON rejection, canonical UUID shape, strict calendar
  dates, literal UTC-Z timestamps. No coercion or ignored actor/status fields.
- Mutation parser reads at most 65,537 actual bytes, rejecting >65,536 with JSON 413;
  declared length is not used. JSON content type required; cancellation exactly {}.
- Mutations use existing guarded lifecycle, then project the committed result.
  Projection errors return sanitized 500 without claiming rollback or retrying.
- Shared fixed error shape/code/message/requestId and no-store apply through servlet
  security and MVC. IDs are generated server-side, not accepted from request headers.
  Tutor-prefix-only framework resolver handles pre-controller 405/415 consistently.
- Default Security chain denies all; form/basic/generated login are disabled/excluded.
  Tutor activation requires reviewed actor/read/lifecycle/module/clock beans; missing
  dependencies fail startup. Main production authentication adapter is still absent.

## Tests-first and observed development findings

First wrote four strict-input unit methods and fourteen PostgreSQL/security request
methods before product implementation. Coordinator ran offline compileTestJava:
exit 1 in 5s, 27 expected missing ApiFailure/TutorApiInput errors; [RED log](../tutor/evidence/rest-red.log).
Two activation tests were added with the new wiring; they are not the original RED.

First full normal suite: exit 1 in 25s, compile/unit/bootJar succeeded but 9 new PG
request methods failed. Cause was test fixture `csrf()` defaulting to `_csrf` query
parameters, correctly rejected by the exact query contract. Corrected fixture to
session-backed header tokens `.asHeader()`. Product query rules, filters and expected
outcomes were unchanged. Retained [attempt 1 log](../tutor/evidence/rest-green-attempt1.log)
and XML under `rest-attempt1/`.

Added explicit framework-405 regression before its fix. Scoped run then exited 1 in
8s: 14/15 request methods passed; 405 lacked JSON content type. Controller-scoped
advice cannot run without a selected handler. Added minimal Tutor-prefix resolver
in existing MVC config and improved assertion clarity with assertNotNull before
JSON content-type check; no oracle weakened. Retained [attempt 2 log](../tutor/evidence/rest-http-attempt2.log)
and XML under `rest-attempt2/`. Focused 405 recheck exited 0 in 5s, [log](../tutor/evidence/rest-method-fix.log).

## Fresh full verification

Coordinator executed from `src/backend`:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Actual result: **exit 0, BUILD SUCCESSFUL in 24s; 20 unit + 51 PostgreSQL tests,
zero failures/errors/skips; bootJar successful**. Includes retained 50 pre-REST
regressions and 21 grouped new tests; matrix rows are not a claimed percentage.
Only pre-existing AI serialVersionUID/JVM warnings were left untouched.

Retained:
- [Full GREEN log](../tutor/evidence/rest-green-final.log).
- [Counts](../tutor/evidence/rest-developer/summary.json).
- [PG request XML](../tutor/evidence/rest-developer/postgresTest/TEST-unichope.platform.core.postgres.TutorRestPgTest.xml).
- [Strict input XML](../tutor/evidence/rest-developer/test/TEST-unichope.tutor.http.TutorApiInputTest.xml).
- [Activation XML](../tutor/evidence/rest-developer/test/TEST-unichope.tutor.http.TutorActivationTest.xml).
- [RT-01–22 executable map](../tutor/evidence/rest-coverage.md).
- Local generated HTML: backend `build/reports/tests/{test,postgresTest}/index.html`.

Test contexts enable real Security/CSRF and simulate only identity/session provisioning
under src/test. PG request tests use actual reads/lifecycle with unique synthetic
schemas and unchanged review-only DDL. Postcommit projection fault collaborators
are test-only and assert retained committed rows/counts. No HTTP copy of SQL races.

## Limits and next role

No real login/logout, cookie flags, session rotation/expiry, production credential
schema or datasource activation is supplied. Fixture identity tests do not prove
those properties. MockMvc byte-limit cases do not prove proxy/chunked transport
behavior. Default ephemeral-server test proves closed wiring, not deployment.
No browser/React, notes, booking completion endpoint or AI behavior is included.

New HTTP adapters/tests are MP3 code; no further MP2 files were copied. Prior Tutor/
core reuse is retained once in [inventory](../REUSE-INVENTORY.md).

Source is frozen for independent reviewer: inspect strict-input/security/access/
activation/persistence boundaries and case coverage, reproduce actual tests, return
numbered findings. Developer fixes require independent re-review. Final human
implementation acceptance remains pending. Nothing committed or pushed.


## Independent-review fix REST-R1

Independent reviewer reproduced two framework errors that omitted required JSON:
valid route with incompatible Accept yielded empty 406; unknown Tutor route yielded
empty 404. Actual activated Boot test context positively started with one controller
and advice, so the issue was error rendering rather than dependency wiring.
Review evidence: [review handoff](2026-10-21-review-tutor-rest.md) and
[Boot probe](../tutor/evidence/rest-review-boot-probe.log).

Captured [pre-fix hashes](../tutor/rest-r1-inputs-2026-10-21.json). Added three tests
before the fix: independent 406 and unknown-route 404 cases, plus durable positive
actual Boot activation using loopback/random port, same test-only identity and real
PostgreSQL dependencies, and request/error checks through MockMvc. Focused run:
exit 1 in 7s; existing 405 passed, three new regressions failed missing JSON content
type. Retained [R1 RED](../tutor/evidence/rest-r1-red.log) and XML in `rest-r1-red/`.

Extended only existing Tutor-prefix framework resolver for 406 NOT_ACCEPTABLE and
no-handler/no-resource 404 NOT_FOUND, using direct ApiErrors writes. Successful
representation negotiation remains strict, unrelated role paths untouched. First
R1 full run passed 20 unit + 54 PostgreSQL tests/build, retained
[log](../tutor/evidence/rest-r1-green.log), [counts](../tutor/evidence/rest-r1-developer/summary.json)
and corresponding XML. This did not close R1.

Further independent probing confirmed controller errors with Accept:text/html
failed to render: ResponseEntity advice negotiated JSON against incompatible Accept
and rethrew original ApiFailure rather than returning the safe error contract.
See `rest-review-error-negotiation-probe.log`. Captured
[advice pre-fix hashes](../tutor/rest-r1-advice-inputs-2026-10-21.json).
Added three grouped regressions before changing advice: preserve validation/role/
owner/state 400/403/404/409, inactive 403/session invalidation/follow-up 401, and
postcommit 500 with retained exactly-once mutation. Focused run exit 1 in 6s:
three new methods failed, two previous HTML methods passed. Retained
[advice RED](../tutor/evidence/rest-r1-advice-red.log) and XML in `rest-r1-advice-red/`.

Fixed advice rendering only: same code/message/status and session invalidation,
now explicit HttpServletResponse/ApiErrors output independent of Accept. Successful
responses still negotiate normally. Already-committed responses rethrow the original
failure; no replacement-status or rollback claim. No auth/schema/lifecycle changes.

Fresh final full command uses the identical offline flags/datasource above:
**exit 0 in 25s; 20 unit + 57 PostgreSQL tests, zero failures/errors/skips; bootJar
succeeds**. Retained [final GREEN](../tutor/evidence/rest-r1-advice-green.log),
[final counts](../tutor/evidence/rest-r1-advice-developer/summary.json) and all XML
alongside. Earlier developer/reviewer runs are retained separately, not overwritten.
[Coverage extension](../tutor/evidence/rest-coverage.md) maps new checks to RT-18/20.

Main edits for REST-R1 are limited to TutorApiConfiguration and TutorExceptionHandler;
regressions remain in existing TutorRestPgTest, reusing one shared test-only dependency
registration helper. Source frozen for independent re-review; final human acceptance
remains pending. No commit or push.

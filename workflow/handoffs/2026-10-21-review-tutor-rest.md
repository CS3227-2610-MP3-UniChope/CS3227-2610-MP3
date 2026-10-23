# Independent Tutor REST Review

Current status: **Independent review complete; recommend acceptance of the scoped
Tutor REST increment. No open actionable findings. Human implementation acceptance
remains pending.**
Date: 21 October 2026. Reviewer: `/root/core_increment_reviewer`.
Human scope approval: Approved on 21 October 2026; scope: five Tutor REST endpoints
and independent review. No commit/push/deployment approval or action.

| Finding | Severity | Current status | Evidence |
| --- | --- | --- | --- |
| REST-R1: framework/controller errors lose safe JSON under content negotiation | P2 | **Closed after independent re-review** | Actual Boot probe; six red-first PostgreSQL regressions; independently reproduced full suite |

## Final verification and recommendation

Inspected final resolver and exception handler: only Tutor-prefix framework
406/404/405/415 are intercepted; controller errors write fixed JSON directly,
preserving original status/code/session invalidation independently of Accept.
Successful JSON responses still respect negotiation. Already committed transport
responses are rethrown rather than falsely reported as replaceable safe 500s.

Checked all 28 files in `workflow/tutor/rest-r1-outputs-2026-10-21.json`:
zero hash mismatches. Inspected both red-first records: three new framework/Boot
regressions failed before first fix, then three controller HTML/status/invalidation/
postcommit regressions failed before complete fix. No existing expected result was
weakened. The latter regression variants preserve 400/403/404/409, old-session
invalidation, and 500 after an already committed mutation without retry.

Independently reran actual Boot+tutor-api with synthetic test-only dependencies on
an ephemeral loopback port. Exactly one controller/advice bean; positive GET 200.
Framework 406, unknown route 404, invalid query/path 400 and missing detail 404 all
return JSON `{code,message,requestId}` plus no-store for JSON and HTML Accept headers.
Retained `workflow/tutor/evidence/rest-review-r1-boot-probe.log`, exit 0. This checks
activation, not production authentication. No duplicate import/scan defect observed.

After developer archived final reports, independently reran the identical offline
`test postgresTest bootJar` command recorded below against the isolated synthetic
localhost PostgreSQL. Exit **0**, BUILD SUCCESSFUL in **25s**, seven tasks executed.
Fresh parsed counts: **20 unit + 57 PostgreSQL tests**, zero failures/errors/skips;
bootJar built. Independent final XML/counts retained in
`workflow/tutor/evidence/rest-review-r1/`; generated HTML under backend
`build/reports/tests/{test,postgresTest}/index.html`. Existing AI serial warning
is unrelated and unchanged. Prior 50 regression cases remain within the suite.

REST-R1 is closed. Recommend acceptance for this slice only: slot/module REST,
strict inputs, simulated Security/session/CSRF integration and request-to-PostgreSQL.
No real login/cookie/rotation/expiry, browser/React, notes/AI, finalized credential
schema or production datasource/migration activation is supplied. Byte tests do
not prove proxy/chunked transport handling. Actual controls and inspected boundaries
are recorded below. Parent may stop temporary PostgreSQL after this review.

## Appendix: initial review and fix history

The following initial/intermediate observations are retained as historical evidence;
the current finding status and final recommendation above supersede their open status.

Current status: **Changes requested — REST-R1 open.**
Date: 21 October 2026. Reviewer: `/root/core_increment_reviewer`.
Human scope approval: Approved on 21 October 2026; scope: five Tutor REST routes
and independent review. Final implementation acceptance remains pending.

## Open finding REST-R1 — P2: Framework 406/404 bypass safe JSON errors

File: `src/backend/src/main/java/unichope/tutor/http/TutorApiConfiguration.java`,
initial lines 30–39. Requirements: REST-05, shared error contract, RT-18/20.
The Tutor-prefix resolver handles 405/415 only; controller-scoped advice cannot
reliably serialize a response when Accept excludes JSON, and no Tutor controller
is selected for unknown resources. Existing method-failure regression misses these.

Reproduced against actual Boot application with `tutor-api`, test-only singleton
identity/read/module/lifecycle fixtures, real Security filters and synthetic sessions:

- GET `/api/tutor/modules`, Accept `application/json`: 200 JSON (positive control).
- Same route, Accept `text/html`: **406, null content type, empty body**.
- GET `/api/tutor/unknown`, Accept `application/json`: **404, null content type,
  empty body**; same empty result with Accept `text/html`.
- no-store remains present, but required `{code,message,requestId}` is absent.

Actual positive Boot profile starts with one Tutor controller and one advice bean;
there is no duplicate import/component-scan failure. Request fixtures alone did
not exercise the observed framework paths. Fix within Tutor-prefix only: return
safe JSON for framework negotiation/not-found failures even with incompatible Accept,
retain correct 406/404 status and no-store, add regression and positive Boot wiring
checks. Do not broaden unrelated role advice or weaken valid error assertions.

Retained probe stdout/exit 0: `workflow/tutor/evidence/rest-review-boot-probe.log`;
inspected source retained as `rest-review-boot-probe.java.txt`. Launched from backend
using `java -cp build/classes/java/main:<existing cached jars>` and the reviewed
source `/tmp/mp3-tutor-rest-review-probe/RestWiringProbe.java`; no downloads. Context
bound server.address=127.0.0.1 and random server.port=0, then closed gracefully.

## Scope assessment and independent reproduction

Read common/reviewer rules, REST spec/plan/22-case matrix, architect/designer/developer
handoffs and coverage, build/settings/wrapper, and all new/changed HTTP/security/
access/input/read/test sources. Manifest HEAD `50c6213abdde9ee5063c7f333e448b33ac83d59b`
plus dirty/untracked inputs: `workflow/tutor/rest-developer-inputs-2026-10-21.json`.
Compared hashes: only seven expected existing files differ (build, Boot app, four
Tutor read/access files, reuse ledger). Lifecycle/domain/schema/AI unchanged.
All `rest-developer-outputs-2026-10-21.json` hashes matched actual source before run.
Initial independent source hashes retained in `rest-review-initial-source.json`.

Positive review: strict local parser rejects unknown/duplicate/trailing/nonstring
JSON, canonical UUID/date/UTC-Z violations; reads MAX+1 actual bytes, ignoring declared
length. Real filter/CSRF tests use session-backed test actors and header tokens,
never disable filters. Fresh DB role/activity supersedes cached authorities. Invalid
account clears context and invalidates old session; follow-up does not reinject auth.
Parameterized owner detail and lifecycle cancellation produce same missing/foreign
404; mutations recheck lifecycle authorization and preserve postcommit semantics.
No-store/requestId/safe error fields hold for covered controller/security paths.
Default real Boot context has no generated users/login or Tutor routes; profile
missing beans fails, and the independent positive Boot fixture works. No main-source
fake actor or credential/migration activation. RT01–22 map to grouped executable
checks; full existing lifecycle concurrency suite is preserved rather than copied.

After developer archived reports, independently executed from backend:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Exit **0**, BUILD SUCCESSFUL in **25s**, seven tasks executed. Fresh parsed reports:
**20 unit + 51 PostgreSQL tests**, zero failures/errors/skips; bootJar succeeds.
Independent XML/counts retained: `workflow/tutor/evidence/rest-review-initial/`.
Known unrelated AI serialVersionUID warning preserved. Green suite does not erase R1.

## Actual controls and limits

Inherited workspace-write project/temp roots, restricted network, auto-review
escalation. Source read-only is behavioral, not enforced read isolation. Allowed
writes: this handoff, rest-review-* evidence/probes; generated backend build/.gradle
and `/tmp/mp3-gradle-home`. Reviewed require_escalated runs permitted offline Gradle
sockets and temporary loopback test server only. PostgreSQL uses existing synthetic
localhost:55439 core_test test cluster and isolated test schemas. No production,
real secrets/SoC, auth schema, product edits, global installs, commits or pushes.

No claim of real login/logout/cookies/session rotation/expiry, proxy/chunked transport,
React/browser, notes/AI, finalized migration or production activation. MockMvc session
identities and positive test beans remain synthetic. Parent must stop PostgreSQL
only after fixes/re-review finish. Next: developer fixes REST-R1, independent reviewer
reproduces affected scenarios and verifies regressions; human acceptance still pending.

## REST-R1 intermediate fix check — still OPEN

Framework resolver changes were inspected and independently probed: ordinary 406
and unmapped 404 now return safe JSON/no-store. However controller-originating
errors still negotiate the advice ResponseEntity against Accept. Actual Boot probe:
GET `/api/tutor/modules?unknown=x` gives 400 JSON with Accept application/json;
the same request with Accept text/html fails inside `TutorExceptionHandler.handle`
with HttpMediaTypeNotAcceptableException and escapes MockMvc as ServletException
whose cause is the original ApiFailure. The expected safe 400 body is lost.

Retained `rest-review-error-negotiation-probe.log`, probe exit 1. This is product
error rendering evidence, not a probe setup error. REST-R1 remains open until
controller errors explicitly retain their original status and safe JSON with
incompatible Accept. Coordinator notified for a scoped renderer/regression fix.

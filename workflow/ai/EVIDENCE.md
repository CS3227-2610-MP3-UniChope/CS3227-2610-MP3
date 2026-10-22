# Shared AI Admission Increment — Evidence

Status: The API/wrapper and PostgreSQL admission implementation are present on
`feat/tutor-ai-note-draft`. They are not yet accepted by the Student/Admin owners.
No production provider, runtime bean, or active migration is included.

Contract: [shared gateway proposal](SHARED-GATEWAY-PROPOSAL.md). Test mapping: [admission matrix](ADMISSION-TEST-MATRIX.md). The Tutor
feature evidence remains in [Tutor AI evidence](../tutor/TUTOR-AI-EVIDENCE.md).
Implementation record: [developer handoff](../handoffs/2026-10-22-developer-ai-admission.md).

## Test results

- Focused PostgreSQL: `./gradlew postgresTest --tests '*PostgresAiAdmissionPgTest' -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/postgres -Dcore.test.user=mp3_test -Dcore.test.password=` — 9 tests passed; 0 failures, 0 errors, 0 skipped. A temporary local cluster and isolated schema per test were used.
- Full command: `./gradlew clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/postgres -Dcore.test.user=mp3_test -Dcore.test.password= --console=plain` — `test`: 84 tests across 24 suites; `postgresTest`: 189 tests across 18 suites; zero failures, errors, or skipped tests; `bootJar` succeeded.
- Earlier focused wrapper output: [focused](../tutor/evidence/p6-shared-ai-contract-focused-2026-10-22.txt). Full-run Gradle XML reports are under `src/backend/build/test-results/{test,postgresTest}`.
- The first PostgreSQL run had one test-fixture failure: a second accepted call correctly remained in flight. The test was fixed to complete that reservation before checking the rolling request limit; the focused and final full reruns passed.
- Independent review found that pruning could delete an active lease when the rolling window was shorter than the request deadline. The predicate now preserves unexpired leases, and regression `pruningKeepsAnActiveLeaseWhenItsWindowHasExpired` verifies a second actor remains blocked by the provider-key in-flight limit. The reviewer rechecked this fix and recorded no remaining findings in [the review handoff](../handoffs/2026-10-22-review-ai-admission.md).
- Counts came from Gradle JUnit XML reports immediately after each run.

Tests cover registered-task routing through a Tutor key alias, metadata-only
admission, rate-limit denial with `Retry-After` and zero provider calls,
fail-closed admission-store errors, unknown task rejection, input/output bounds,
timeout/no-retry, and PostgreSQL actor windows, provider-key request and
output-token reservations, concurrent admission serialization, lease expiry,
active-lease preservation after a rolling window expires, and sanitized storage failure. Tutor tests cover prompt/data separation, strict
output parsing, and rough-note validation.

One first focused run failed because a normal fixture exceeded the deliberately
small test policy's input limit. The test policy was corrected to admit the
normal fixture while keeping an explicit over-limit case. The corrected focused
and full runs above passed. This was fixture calibration, not a change to the
production limit.

## What this proves

The wrapper calls the injected admission service before provider dispatch, routes
through a server-owned `AiProviderKey`, sends only metadata to admission, fails
closed when admission throws, retains an uncertain lease after timeout, and
sanitizes provider/adapter failures. The PostgreSQL adapter enforces serialized
rolling limits and key reservations across concurrent database clients. Task
policies reject nonpositive limits or deadlines above 30 seconds.

## What remains unverified

No runtime bean or active migration connects this store to application startup.
Stored reservations count configured output-token caps; they do not measure
complete provider billing, which needs SoC request/usage-field verification.
The output-size check occurs after a provider returns a Java string;
the future HTTP adapter must cap bytes during response streaming. No live SoC model, secret, or prompt-injection behavior was tested. An independent
reviewer rechecked the active-lease pruning fix; this does not constitute owner
acceptance or a review of production wiring.

The existing Student AI technical spec already contains a more detailed future
design. This evidence records code compatibility work for review; it does not
supersede that spec or imply team agreement.

## Tested source snapshot

SHA-256 identifies the files relevant to the shared admission implementation and
its test contract at the final 23 October full run. Admission-specific source hashes identify the independently reviewed code; Tutor endpoint tests were added to that run.

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/java/unichope/platform/ai/AiGateway.java` | `6ea7069de5f8922b9215d2dd97ff4ca8ae21f5e017fd1e425006758c05a26423` |
| `src/backend/src/main/java/unichope/platform/ai/AiTaskPolicy.java` | `dc519192aa5a946cdc3cdc6e5bc39c6f077ecff40f42898d26f34508b2298d87` |
| `src/backend/src/main/java/unichope/platform/ai/AiProviderBudget.java` | `171a7e6a8a6cbb62d949a7ddee39470f9cb477bc136d379ac739c7bda2d682d3` |
| `src/backend/src/main/java/unichope/platform/ai/AiProviderKey.java` | `6342497a3d7788cc6d86da51a5e753a692224a15130a7f102aa2b248890d4081` |
| `src/backend/src/main/java/unichope/platform/ai/AiAdmissionService.java` | `af24f06740810efed9aee714d8934a2ffa789698c67acf2f67dad4ccd945f903` |
| `src/backend/src/main/java/unichope/platform/ai/AiProviderRouter.java` | `e9e376f301c902ee30f7179ec74d2c8274bb4d094ee3ec0e2fe9ea094f525cd9` |
| `src/backend/src/main/java/unichope/platform/ai/AdmittedAiGateway.java` | `5777be66e0bb64d48952738e252737f609d6dbb57bae28735f4769223b0b179c` |
| `src/backend/src/main/java/unichope/platform/ai/postgres/PostgresAiAdmissionService.java` | `b18c436e7dd3fa5fbfb55cb59cddb10a9f0dfde0570caeee8ae7d4a249e12e61` |
| `src/backend/src/main/resources/db/ai-review/V1__create_ai_admission.sql` | `2051d2b860e1459b311454aaa69a5ebe34a8c15d855a5e49b00bae9ec2005456` |
| `src/backend/src/test/java/unichope/platform/ai/AdmittedAiGatewayTest.java` | `ddb83f3a53b35f7a98085707320ba2b26043461b4e878659d9afeb8ea0f4dc90` |
| `src/backend/src/test/java/unichope/platform/ai/postgres/PostgresAiAdmissionPgTest.java` | `19f57bc69315896b0df63a2888f747c11f432bc053f1419fa34a7ea679f38630` |
| `workflow/ai/SHARED-GATEWAY-PROPOSAL.md` | `6469cf16b1cb92caa4d009ead11e3d9d258dc9bf9560d99f3c72dc2142c929da` |
| `workflow/ai/ADMISSION-TEST-MATRIX.md` | `87f228d0b837f38c87fa530925d1fe02dc9ed111af5ac350d62127741855b5e4` |

## 23 October routing-preflight follow-up

The shared wrapper now resolves the configured route before calling admission.
Provider resolution is required to be a local configuration lookup. If no provider
is configured for the server-selected role alias, `AdmittedAiGateway` returns
sanitized `UNAVAILABLE` without calling admission. The PostgreSQL admission service
rejects a missing provider-key budget before opening its transaction. This prevents
durable attempts and outbound provider calls for incomplete role configuration.

Verification after this change:

- `./gradlew test` — 106 tests across 26 JUnit XML reports; 0 failures, 0 errors,
  0 skipped.
- `./gradlew postgresTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/postgres -Dcore.test.user=huanghao -Dcore.test.password=` — 199 tests across 18 JUnit XML reports; 0 failures, 0 errors, 0 skipped. A disposable local PostgreSQL cluster under `/private/tmp` was used.
- New wrapper cases: `resolvesConfiguredProviderBeforeAdmission` and
  `unavailableProviderRouteFailsBeforeAdmission`, and
  `unexpectedProviderResolutionFailureIsSanitizedBeforeAdmission`. New PostgreSQL case:
  `missingProviderBudgetFailsBeforeCreatingReservation`.
- `git diff --check` passed. No `bootJar` run or production provider activation
  occurred.

Updated source hashes for this follow-up:

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/java/unichope/platform/ai/AdmittedAiGateway.java` | `9e1cde5a4041f05a3ac7ae19d5b29d87f8af12e150f9f95b8221fcb73372a678` |
| `src/backend/src/main/java/unichope/platform/ai/AiProviderRouter.java` | `d9097e0e0ecbccf87886092e077dfa65fc3cb6eb3b77e00d135fa5185fc0c73f` |
| `src/backend/src/test/java/unichope/platform/ai/AdmittedAiGatewayTest.java` | `812477436d33e0c50fa301903a61b7428c2b1808ffa00474520854ed00c31b18` |
| `src/backend/src/test/java/unichope/platform/ai/postgres/PostgresAiAdmissionPgTest.java` | `a9b5b10099fb011ef2f35f3018040c1142fd4cf00c23ab04a92d754152d43da8` |

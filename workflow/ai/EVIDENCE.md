# Shared AI Admission Increment — Evidence

Status: The shared API/wrapper, PostgreSQL admission, `app-api` runtime wiring,
and V5 migration are present on `feat/tutor-booking-completion`; V6 adds Tutor
successful-draft retry accounting. Student/Admin owners have not accepted the
shared contract. Local PostgreSQL tests verify the shared runtime wiring without
making live SoCLaaS calls. Production deployment and live provider behavior remain
unverified.

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

The `app-api` shared runtime bean connects the durable admission store through
normal application startup, and Flyway applies V5 then V6. The `local-soc` test
profile verifies this wiring against disposable PostgreSQL; no production
deployment or live SoCLaaS call was tested.
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

## 23 October local Tutor SoCLaaS runtime follow-up

Historical record: the isolated `LocalSocAiConfiguration` and V100 migration
described below were removed when reconciling this branch with PR #10. They are
retained here to preserve the earlier run evidence, not as the current runtime
design. The current local profile uses the shared `AiConfiguration` and V5
migration; `LocalSocRuntimePgTest` has since been rerun as part of the full
245-test PostgreSQL run recorded below.

Added an explicit `local-soc` profile for controlled Tutor integration testing.
It registers only the Tutor provider route and `tutor.note-draft.v1`, uses the
durable PostgreSQL admission implementation, and requires server configuration for
the Tutor key/model, task output cap, and per-key request/token/in-flight budgets.
The task enforces three admissions per Tutor per five-minute window, one
in-flight request, an 8,000-code-point input cap, and a 20-second deadline. Missing
Tutor key/model resolves to sanitized `UNAVAILABLE` before admission; there is no
Student/Admin credential fallback.

The local AI schema is stored separately under `db/ai-local-migration` and is added
to Flyway only when both `local-soc` and `local` are active. Default and production
profiles keep using `db/migration`; no production migration activation is included.
The Student/Admin/Auth/Core contract reviews and all production configuration
remain pending. No real key or SoCLaaS request was used in these tests.

Verification:

- `./gradlew test` — 111 tests across 27 JUnit XML reports; 0 failures, errors, or
  skipped.
- `./gradlew postgresTest` against a disposable local PostgreSQL cluster — 201
  tests across 19 reports; 0 failures, errors, or skipped. The new
  `LocalSocRuntimePgTest` starts the application with `app-api,local,local-soc`,
  verifies Flyway applies the local AI schema, confirms `AdmittedAiGateway` and the
  Tutor SoCLaaS route are wired, and checks Student/Admin routes fail closed. It
  does not send a provider request.
- `git diff --check` passed.

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/java/unichope/platform/runtime/LocalSocAiConfiguration.java` | `b53483967ef237ac5f51fbc878d38565ead4e1499f8a29b522cf92188cde26d4` |
| `src/backend/src/main/java/unichope/platform/runtime/ApplicationConfiguration.java` | `5918bf8727611aaeed702ac9317571556dc34fe505ac02f3a4f5bba2ff40a9bd` |
| `src/backend/src/main/java/unichope/platform/runtime/AdminConfiguration.java` | `9bc55e5be9ae920913a31836f31b7c8c741930408caa6a2725c2df662d228e86` |
| `src/backend/src/main/resources/db/ai-local-migration/V100__create_ai_admission.sql` | `9002fc27f0a426596a4496cc5b7182a0162ed2e73b31d7fe471355fee16ddb51` |
| `src/backend/src/test/java/unichope/platform/runtime/LocalSocAiConfigurationTest.java` | `6c05b39543ee16293e89dce6f5148ba266a19a35543a4626cb9d7fc3bafd2675` |
| `src/backend/src/test/java/unichope/platform/runtime/LocalSocRuntimePgTest.java` | `0faa48767f4adeb0508d1d4001b1484053e23c748d62efc08c50ae30c3325454` |
| `src/backend/src/test/java/unichope/platform/runtime/RuntimePolicyTest.java` | `094c621e440319ae32a634cf8f07e83b5c0597b7f5c0ddc99eadbd25bc52146e` |
| `workflow/tutor/SOCLAAS-PROVIDER-SPEC.md` | `feb6071e424594cb94386eb808ba092757a73500e88a6039369d0fec9549a157` |
| `workflow/tutor/SOCLAAS-PROVIDER-TEST-MATRIX.md` | `09e06b0e45c68ace319524034ceba8a443cf26e832af586a47fb698e6a595c78` |
| `workflow/handoffs/2026-10-23-developer-local-soc-runtime.md` | `1c47c9815f653b8fa67e9b5094b82169ace12b0cbdf80e6a454ca63eb4b6632e` |

## Local model selection follow-up

The Tutor owner selected `qwen3.6:35b` after their own trial calls. This records
an owner decision, not independently captured live-model evidence. The explicit
local-soc profile now defaults to this model, 1,024 output tokens, and a per-key
budget of one request and 1,024 reserved output tokens per five minutes, with one
request in flight. These are initial application trial caps, not provider quota or
model-capability claims. Environment variables can override them. No key is stored
in this profile and no provider call was made by these checks.

Verification after adding the local defaults:

- `./gradlew test` passed: 111 tests, no failures/errors/skips.
- `./gradlew postgresTest --tests unichope.platform.runtime.LocalSocRuntimePgTest`
  passed against disposable PostgreSQL: 2 tests, no failures/errors/skips. These
  checks now load the profile defaults and verify the Tutor draft service is
  present alongside the admitted gateway. They make no provider requests.
- `git diff --check` passed. The previous full PostgreSQL run (201 tests) predates
  this defaults change; this follow-up reran the affected runtime tests only.

Updated snapshot for this follow-up (supersedes prior hashes for these files):

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/resources/application-local-soc.properties` | `6c7574c413e8386c9eb8f96a7a27ade488f65cc944a00e3b62236cb2e1e1387c` |
| `src/backend/src/test/java/unichope/platform/runtime/LocalSocRuntimePgTest.java` | `9b56f7c667a3f26c556715e03f7eb8f704216e963148a4b19169d188b386c3f4` |
| `workflow/tutor/SOCLAAS-PROVIDER-SPEC.md` | `2ff3e814199fada08693042429d9fc2e76ce45177c310786ca99c5ba212a4a6c` |
| `workflow/handoffs/2026-10-23-developer-local-soc-runtime.md` | `e7ec3ef93f60bbe95995ee9c2eaccdf95ba1d923a8a6da336a441280118f99e4` |

## Retry accounting and per-key trial cap follow-up

The Tutor owner requested manual retry after provider or output-validation failure
and a higher provider-call allowance. The current local Tutor trial settings are
two admitted requests and 2,048 reserved output tokens per Tutor key per rolling
minute, with one request in flight. The Tutor task still allows three usable
drafts per Tutor per rolling five minutes. An admitted failure or unusable draft
releases only that successful-draft charge; it remains in provider-key request
and token accounting because SoCLaaS may have processed the request. A new click
may retry, while the backend performs no automatic retry. The trial values are
not verified SoCLaaS limits, and each role key has its own budget.

Verification for this follow-up:

- `./gradlew test` passed: 109 tests across 27 suites, no failures/errors/skips.
- `git diff --check` passed.
- `./gradlew postgresTest --tests unichope.platform.ai.postgres.PostgresAiAdmissionPgTest`
  passed against a disposable UTF-8 PostgreSQL 17 cluster: 13 tests, no
  failures/errors/skips. This includes actor retry accounting, provider-key
  attempt charging, and uncertain-call lease retention.
- After merging `origin/main` locally (merge commit `ae8e5c4`) and correcting the
  runtime test to check missing role routes through the public gateway, the full
  `./gradlew postgresTest` passed against a disposable UTF-8 PostgreSQL 17
  cluster: 246 tests across 21 suites, no failures/errors/skips. This includes
  the populated-V5-to-V6 upgrade regression. The earlier
  attempt exposed the stale checkout's missing V5 dependency, an outdated
  migration-count assertion, runtime-test lookups for internal objects that are
  not Spring beans, and SQL_ASCII in the initial cluster; those causes were
  addressed before the passing run.
- No live SoCLaaS calls were made. The local profile holds no API key.

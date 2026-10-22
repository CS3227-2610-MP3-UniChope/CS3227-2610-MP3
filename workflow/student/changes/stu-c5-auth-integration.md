# stu-c5-auth-integration: Student uses shared runtime authentication

Status: READY_FOR_HUMAN; independent review PASS in reviewer-1, with human acceptance and affected-owner merge review pending.
Scope: standard path, independently testable shared-auth/runtime behavior.
Parent request: Phuc, 22 October 2026: "Update the spec and intergrate with new auth, implement student booking".
Human owner: Phuc; affected owners Kok Seng (Auth/Admin), Huanghao (core/Tutor), web reviewer.
Dependencies: c1 browsing/c3 views exist at the base; preserve their behavior.
Implementation order: C5 foundation -> C2 creation -> C4 cancellation. C5 integrated
book/cancel journey tests may be authored now with C2/C4 and pass after those handlers.
No code/test edits occur in this specification session.

## Handoff analyst-1
Producer: fresh spec analyst, Codex GPT-6. Recipient: fresh test writer.
Task C5-T1: write executable runtime-auth/activation tests mapped to all C5 IDs.
Goal: app-api includes Student and trusted SessionPrincipal adapter; backend booking
works with real shared login. Non-goals: frontend, AI implementation, Tutor seam
migration, new session format or changes to core lifecycle.
Acceptance: Student Technical section 9 C5-RUN-1, C5-AUTH-1/2, C5-REV-1,
C5-CSRF-1, C5-STATE-1, C5-REG-1. Section 3.2 defines precedence.

### Inputs and outputs
| Artifact | SHA-256 | Purpose |
| --- | --- | --- |
| `workflow/student/PRODUCT-SPEC.md` | `ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957` | Canonical contract |
| `workflow/student/TECHNICAL-SPEC.md` | `67c2c5ba98885d4845c16b27eefbed237cdb7767bede9f7d7c44c8aa57ac425b` | Canonical contract |
| `workflow/student/PLAN.md` | `cb9695ec503a774d931f91df8a5fb2df40b1dffed3f1f0aa34db0a78a4d5142d` | Canonical contract |
| `workflow/INTEGRATION-SPEC.md` | `1d6782ceca8351bdb806db1108b024e9da147e6de972b2f7e65f8442fa1202ea` | Canonical contract |
| `workflow/SHARED-TECHNICAL-SPEC.md` | `54ec090a426a1a8733f7628924e4310d27a90b1b724d0acfad3eb0adcde35cfe` | Canonical contract |
| `workflow/auth/TECHNICAL-SPEC.md` | `2900b5d6ca945c829fffb513fab0fe1e6fd2a0b358fdbd4c3635445d6208de40` | Canonical contract |
| `src/backend/src/main/java/unichope/platform/runtime/ApplicationConfiguration.java` | `28055e3c9404be5a2321f071c28493ddf0d9b5cef275aab481ec15fb45086419` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/web/CurrentAccountFilter.java` | `5e0b23a23055e3e36e0632531bb5dcfafda24c4cf9eb9f6225fbf93b0a773f0a` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/web/ApiSecurityConfiguration.java` | `c97117de37b800286448343032ba2144ab92fd34297b58a5aac10f9e8382a0fe` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/auth/AuthenticationService.java` | `e0576283642e2d4afaeb5007b17736c34e5f977b13755068c90fe090c1627f4f` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java` | `35870e1c1f35246de71fef83173dada8048aa08c855a65f660ee9b5135721b99` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/resources/application.properties` | `c9058d21e64a1f73c14366eba4baa876d64f57971af466086b968f5d4b82b3f1` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/test/java/unichope/platform/core/postgres/StudentBookSlotPgTest.java` | `8d5866e3e5ea86a7e3e75ad47338dc9f5c6b0a0d1bfd77ce74ec09ea94fc3ee7` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/test/java/unichope/platform/runtime/ApplicationIntegrationTest.java` | `392fa44e6bcab5f3edd7664899ef7fedab3591e066c0bcbab7de4482263d023b` | Inspected baseline source/test; not proof of execution |

Base commit: `a9dd6508906a11346e9638f77d744593482dff08`. Uncommitted outputs: canonical spec/plan corrections,
this record and analyst-3 appendices in C2/C4; use `git diff -- workflow src/backend/README.md`
and include new C5 record. Existing audit `main-auth-admin-spec-impact.md` was read
as a factual index; A1-A5 evidence was checked against actual source before edits.
This is not acceptance of any historical test/review assertion.

Allowed test-writer scope: Student/runtime test classes and fixtures under
src/backend/src/test/java/unichope (including the existing PostgreSQL package),
needed test-only config and evidence/change records. Refresh existing C2 packet,
write C4 cases and C5 runtime cases together if useful; keep separate mappings.
Later implementation: student/**, ApplicationConfiguration.java, application.properties,
and minimum necessary shared wiring; no lifecycle/DDL/Tutor/Admin policy changes.

Decisions: direct request authorizes shared-auth integration and Student booking.
Analyst implementation choices within that authorization: add student-api to app-api;
SessionPrincipal-only platform.auth adapter; reuse existing lifecycle/read repository;
shared filter precedence preserved. Existing booking baseline rules remain unchanged.
Owner review remains merge gate; no teammate approval is fabricated. Existing written
booking design plus explicit implementation request supplies the brainstorming baseline;
this is a compatibility refresh, not a new product proposal.

## Requirement-to-technical coverage
| Requirement / IDs | Product source | Technical source | Coverage | Validation |
| --- | --- | --- | --- | --- |
| C5-RUN-1 | AUTH-02, Student section 2 | Student TECH 2/9, Integration 8 | COVERED | Boot default + isolated missing deps + app-api PostgreSQL |
| C5-AUTH-1/2 | AUTH-01/02, S-AUTH-1, STU-BOOK-01/03 | Student TECH 3.1/3.2/9; Auth runtime | COVERED | Real login/registration, session principal, all denial paths, syntax precedence |
| C5-REV-1 | AUTH-02 | Auth selected generation storage; Student TECH 9 | COVERED | Two sessions; Admin deactivation/reactivation; fresh login |
| C5-CSRF-1 | SH-AUTH-03 | Integration 2; Student TECH 9 | COVERED | Old/fresh CSRF before/after login/logout |
| C5-STATE-1 | SEC-01, STU-BOOK-02/03 | Student TECH 3/7/9 | COVERED | S1 logout -> S2 login, list/detail/cancel isolation |
| C5-REG-1 | Shared role boundaries | Student TECH 1/2/9 | COVERED | Existing Auth/Admin/Tutor/core regression |

## Spec findings and resolutions
| ID / severity | Evidence and impact | Fix / owner | Resolution |
| --- | --- | --- | --- |
| C5-F1 HIGH | app-api excludes Student; runtime only binds Tutor provider | Compose Student and SessionPrincipal provider; Auth/Student | Resolved contract: TECH 2/9, PLAN C5; implementation pending |
| C5-F2 HIGH | Old Student syntax-before-actor text contradicts CurrentAccountFilter early stale/inactive denial | Preserve filter semantics, explicitly distinguish controller seam order; Auth/Student | Resolved TECH 3.2 and Integration 9; test both paths |
| C5-F3 MEDIUM | Integration says credentials/migrations absent, but runtime V1-V4 and AccountStorage exist | Correct current schema/runtime wording; core/Auth | Resolved Integration 6/8; no migration change |
| C5-F4 MEDIUM | UUID fake actor tests cannot prove real authentication | Runtime PostgreSQL acceptance plus retained seam tests | Resolved TECH 9 and this packet; execution pending |
| C5-F5 LOW | Student AI gateway proposal conflicts with existing AiGateway consumers | Mark proposal compatibility gate, defer AI changes | Resolved TECH 6/PLAN AI1; no booking dependency |

## Spec readiness
Verdict: READY_FOR_TESTS for C5-T1 only, against the exact manifest above.
All in-scope contract conflicts resolved; actual runtime integration remains unimplemented.
No unresolved decision blocks tests. Owner merge review, independent security review,
manual acceptance and future UI/AI work remain pending. No tests/builds ran in this
analyst session. Inspected source establishes feasibility, not correctness.

Verification: disposable PostgreSQL 17, CREATE SCHEMA permission, Java 25; actual
runtime db/migration V1-V4 required. From src/backend use gradlew.bat test postgresTest
bootJar with explicit -Dcore.test.jdbcUrl/user/password; focused task filters may
precede final full regression. Test writer records actual commands/revisions/exit
and behavioral red results (setup failures do not count). No production database.

### Receiver receipt
Recipient adds ACCEPTED/RETURNED with checked hashes, task scope and prerequisites.

### Receiver receipt test-writer-2 (22 October 2026)
Receiver: fresh test writer, Codex GPT-6. ACCEPTED. Checked current canonical
Product, Student Technical/Plan, Integration, Shared Technical and Auth Technical
SHA-256 values against this manifest; all match. Checked every listed runtime,
filter, lifecycle, application.properties and existing test hash; all match.
Derived behavior from canonical contracts and inspected actual test harness/source.
C1/C3 source foundations exist; C5/C2/C4 implementation is intentionally absent.
Combined executable red test writing is authorized by analyst-3 dependency rule
(C5 uses analyst-1). Isolated UUID seams and real runtime auth remain distinct.

## Handoff test-writer-2 (22 October 2026)
Producer: fresh test writer, Codex GPT-6. Recipient: fresh implementer.
Stage status: READY_FOR_IMPLEMENTATION, scoped to the task in this record.
Entry receipt above checked all fourteen analyst manifest artifacts; no hash mismatch.
Canonical baseline: current analyst-3 manifest (C5 analyst-1), Student Technical
SHA-256 67c2c5ba98885d4845c16b27eefbed237cdb7767bede9f7d7c44c8aa57ac425b.
Base commit: a9dd6508906a11346e9638f77d744593482dff08; product code unchanged in this stage.
Test source freeze: final hashes below. Tests compile without product implementation
changes and are discovered by the configured Gradle postgresTest task.

| Output | SHA-256 |
| --- | --- |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBookSlotPgTest.java (retained unchanged) | 8d5866e3e5ea86a7e3e75ad47338dc9f5c6b0a0d1bfd77ce74ec09ea94fc3ee7 |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentCancelBookingPgTest.java | ce8000aba12ae2a993d339cccf1b49d706d61f837e8a6f1b95bcb10146f03941 |
| src/backend/src/test/java/unichope/platform/runtime/StudentAuthIntegrationPgTest.java | 7c15f18c144a59098bf8608171a8d265c0671fbe59ada16f4d37b275c8cd0393 |

### Reproducible baseline evidence
Working directory: src/backend. Java 25, existing Gradle wrapper, PostgreSQL 17
on 127.0.0.1:55432/mp3_test; disposable synthetic account with CREATE SCHEMA.
Pass the disposable password from local test configuration; do not use production.
Commands shown with password redacted; actual output artifacts redact it too.

```powershell
.\gradlew.bat compileTestJava --console=plain
.\gradlew.bat postgresTest --tests '*StudentBookSlotPgTest' --tests '*StudentCancelBookingPgTest' --tests '*StudentAuthIntegrationPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
.\gradlew.bat postgresTest --tests '*StudentCancelBookingPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
```

Compile command exit 0. Initial focused run exit 1: 33 discovered, 33 failed
(C2 15, C4 11, C5 7). Failures are assertions for absent behavior: booking POST
405, cancellation POST 404, student-api missing-lifecycle context unexpectedly
starts, app-api omits student-api and Student routes deny real logged-in students.
Actual registration/login/runtime Flyway setup completed before runtime assertions.
Two retained C2 races report no blocked caller because the absent booking handler
never calls SQL; other C2 endpoint assertions identify that prerequisite directly.
No unavailable database, missing imports, test discovery or startup harness error
is counted as behavioral red. An initial checked-exception compilation defect in
new helper methods was corrected before the successful compile and red baseline.

C4 race tests then gained a successful-cancellation prerequisite control to report
missing cancellation behavior directly before exercising locks. Final C4 run compiled
that revision and exit 1: 11/11 assertion failures, all missing cancel handler 404.
This refresh supersedes only the initial C4 test-source/result revision. C2 and C5
sources did not change after their initial behavioral baseline.

Saved evidence (under stu-c5-auth-integration/evidence):

| Artifact | SHA-256 |
| --- | --- |
| red-initial-console.txt | 0e7d96cf2cacd0fe896450d7e1839716e9532d4f608c8f88aa72e2e9363e6112 |
| red-initial-assertions.json | 9d5e1447d6fdad9463265353b8294b0a7127ea376f9d677c6028d56a685e06c8 |
| red-c4-final-console.txt | ec7f85c06db0b8c984215fbedfa33aa2f0d851a60bf2d62a74643d1641fcadc8 |
| red-c4-final-assertions.json | 2e60755ff8431639bf2aa8e6ab3f7444cac2cb50d6695f6047b1181ad084a169 |

Evidence files are sanitized command outputs and JUnit assertion extracts, not
interaction logs. The first console intentionally contains the obsolete initial
C4 line references; use red-c4-final artifacts for current cancellation source.
Full regression `test postgresTest bootJar`, independent review, affected-owner
merge review and human verification remain pending implementation. No UI/AI or
real-provider claim. Existing C1/C3/Auth/Tutor/Admin green claims were not rerun in
this stage and are not adopted as fresh evidence. Runtime C5 tests use real auth;
isolated C2/C4 simulated UUID actors prove only their own seam contracts.

Next action: implement C5 runtime wiring, C2 creation, C4 cancellation in the
analyst-authorized source scope, retain frozen tests and canonical contracts, run
focused then full required regression, and pass complete diff to fresh reviewer.
A product defect goes to implementer; demonstrated test defect returns to a fresh
test-writer session. Do not silently weaken coverage to obtain green.

### C5 acceptance mapping
All new runtime cases use app-api+local, unique PostgreSQL schemas, actual runtime
Flyway V1-V4, real registration/login and server-issued CSRF. No UUID actor bean
or csrf() test processor is injected. Tutor/Admin fixture accounts are synthetic;
both Students are created by the real registration endpoint.

| Acceptance | Test/case |
| --- | --- |
| C5-RUN-1 runtime | StudentAuthIntegrationPgTest c5Run1AppApiIncludesStudentWithRealRuntimeMigrationsAndSharedActor |
| C5-RUN-1 default/missing deps | Existing StudentActivationTest c1Act1*; StudentBrowsePgTest activation cases; StudentBookSlotPgTest c2ActStudentApiWithoutConsultationLifecycleFailsStartup (retained; final regression required) |
| C5-AUTH-1 | c5Auth1RegistrationLoginRotationOldCsrfFailsFreshCsrfBooksReadsAndCancels |
| C5-AUTH-2 | c5Auth2AnonymousWrongRoleCachedAuthoritiesAndUntrustedPrincipalsCannotChooseActor; c5Auth2MissingAccountWinsMalformedSyntaxAndInvalidatesSession; c5Rev1* |
| C5-REV-1 | c5Rev1TwoSessionsAdminDeactivationAndReactivationNeverReviveOldGeneration |
| C5-CSRF-1 | c5Csrf1MissingInvalidTokensLogoutCookieRepeatAndFreshLogin; c5Auth1* |
| C5-STATE-1 | c5State1LogoutThenDifferentLoginRetainsNoActorOrBookingState |
| C5-REG-1 | Existing ApplicationIntegrationTest, TutorRestPgTest, PostgresLifecycle* and auth/admin suites: required full postimplementation regression; not executed during this red stage |

Task C5-T1 is READY_FOR_IMPLEMENTATION. C5 book/cancel journey failures remain
expected until dependent C2/C4 implementation. Future UI clearing/late responses
remain a separate manual/frontend criterion and are not claimed by backend tests.

### Receiver receipt implementer-1 (22 October 2026)
Receiver: fresh implementer, Codex GPT-6. Receipt: ACCEPTED. Checked current analyst manifests (C5 analyst-1, C2/C4 analyst-3), six canonical spec hashes, frozen C2/C4/C5 tests and four saved red evidence hashes. Actual test sources and red assertions support the documented absent-handler/runtime behavior. Scope and dependency order remain C5 -> C2 -> C4; no contract or test change is required. Existing product code inspected; no source edits or builds preceded this receipt.
## Handoff implementer-1 (22 October 2026)
Producer: fresh implementer, Codex GPT-6. Recipient: fresh independent reviewer.
Stage status: IN_REVIEW. Goal: C5 shared runtime authentication, C2 booking creation,
and C4 cancellation; backend only. No frontend, AI, core lifecycle, DDL, Tutor,
Auth/Admin policy changes, commits or push. Base a9dd6508906a11346e9638f77d744593482dff08.
Canonical Product/Technical/Integration/Auth acceptance hashes remain the checked
analyst/test-writer baseline. Student PLAN progress-only hash is now
`a1c8591b393ca6536244915e185414b0c4edb5f09b799ca706ed60647d312cb9`, superseding only
stale plan progress metadata; acceptance behavior is unchanged.

### Implementation and acceptance mapping
- C5-RUN/AUTH/REV/CSRF/STATE/REG: application.properties composes student-api;
  ApplicationConfiguration adds platform.auth.CurrentActorProvider accepting only
  authenticated SessionPrincipal. Tutor provider and shared filters remain unchanged.
- C2 S-BOOK-1..5, C2-PREC/IN/ACC/ERR: StudentApiInput validates exact bounded JSON;
  StudentController supplies 201 and Location; StudentHttpService calls the existing
  lifecycle once, maps local errors and defensively recognizes the named unique index.
- C4 S-CANC-1..3, C4-BND/ELIG/IN/ACC/ERR: exact empty-object/path parser and cancel
  route use unchanged atomic lifecycle; service maps NOT_FOUND/INVALID_STATE and
  reruns stored-account access on ACCESS_DENIED. The same owned read projection
  follows committed writes without retries. StudentApiConfiguration requires lifecycle.
- Frozen C2/C4/C5 tests are unchanged at the test-writer-2 hashes. Existing read
  behavior retained; full regression includes C1/C3 and shared roles.

### Review artifact manifest
All artifacts below are under `workflow/student/changes/stu-c5-auth-integration/evidence/`.
| Artifact | SHA-256 | Purpose |
| --- | --- | --- |
| implementation-source-hashes.json | 3d1c6bdf4e9e9aab7074a8f28f62a3ffee7c3add1b9c4fdb5ef758a955526e82 | Exact final source hashes |
| implementation-source.patch | 345d9a070dcb6857fc46cd61e1ff871b605fc94cb6a3c43e473572e56f8de6d1 | Complete implementation source diff, including new service |
| review-product-and-spec.patch | a89c960b8725c9b1d68f33333260f97e8b67c5c7358cbe322ebed9fb9ca7f752 | Current product, tests, canonical spec/plan and README diff; includes new C4/C5 tests |
| green-focused-console.txt | 60c9ede66633fd9edf52566b951de54df6f2d89cccbac690c0af8c33da0c2729 | Focused command output, password redacted |
| green-full-console.txt | 2f3139b6837a9fd4c7f0f813689131db27843b61272ada0f5ec252288c548644 | Full required regression/build output, password redacted |
| green-full-counts.json | 9cafb2816335994da34948973622581b7d81fa1b9b49dd21993554ca865f07e0 | Actual final JUnit XML aggregates |

Use actual worktree files plus these records for review; untracked source/tests
are included in the saved patch. Preserve unrelated preexisting Admin documentation
changes and interaction logs. Evidence artifacts contain command results, not
narrative session history. No contract gap or test correction was required.

### Verification
Working directory `src/backend`, Java 25 and disposable local PostgreSQL17,
unique per-test schemas, existing Gradle cache. Commands use the existing synthetic
local test password (redacted here); no production service involved.

```powershell
.\gradlew.bat postgresTest --tests '*StudentBookSlotPgTest' --tests '*StudentCancelBookingPgTest' --tests '*StudentAuthIntegrationPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
.\gradlew.bat test postgresTest bootJar '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
```

Focused command exit0: C2 15, C4 11, C5 7; zero failures/errors/skips.
Full command exit0, BUILD SUCCESSFUL in 4m16s: unit59 across18 suites,
PostgreSQL178 across17 suites; all237 tests with zero failures/errors/skips;
bootJar built. Nonblocking JVM class-sharing warning appears in full output.
Final source revision matches implementation-source-hashes.json; final full run
compiled the source after Javadoc clarifications. No tests were weakened/edited.
Independent review, affected-owner merge review and human verification remain
pending; frontend keyboard/journey and AI/provider evaluation are outside this backend task.
Next action: fresh reviewer checks canonical baseline, all diffs/tests and saved
results, reruns proportionate verification, and records findings/verdict here.


## Metadata-only correction addendum 1 - Integration auth summary, 22 October 2026

Producer: Codex GPT-6, specification documentation correction. Scope: localized
consistency correction, shorter workflow path; no new acceptance behavior.
Direct authorization: Phuc's request to update specs/integrate new auth, applied to
already normative Auth registration and generation-revocation semantics.

Re-read actual Auth Technical HTTP contract/selected credential storage,
AuthController.register and AuthenticationService.current/register. Integration
section 2 now summarizes POST /auth/register (201 message, no login, Student-only
provisioning, 400/409 errors) and stale-generation 401/session invalidation with
fresh login after reactivation. CurrentAccountFilter's previously inspected
invalidation behavior and existing C5 acceptance remain unchanged.

Integration SHA-256 before correction: `1d6782ceca8351bdb806db1108b024e9da147e6de972b2f7e65f8442fa1202ea`.
Current `workflow/INTEGRATION-SPEC.md` SHA-256: `f7893269a4f5c3f1f4e106f3c62225dd3e8d3b4e3c0c31b6ca4a5ce61fad0604`.
This new hash supersedes only the Integration input revision for current review;
all earlier stage manifests/results remain historical evidence, unchanged. No test,
code, other canonical spec or acceptance criterion changed; no build/test rerun in
this documentation correction. Validation: source-to-contract inspection and
`git diff --check -- workflow/INTEGRATION-SPEC.md` exit 0. Reviewer/implementer must
acknowledge this addendum when checking the current Integration revision.

## Independent review reviewer-1 (22 October 2026)

Receiver: fresh independent reviewer/security role, Codex GPT-6. Receipt: ACCEPTED
for Handoff implementer-1 after checking actual canonical/source/test hashes,
complete tracked/untracked product diff and red/green command artifacts.
Verdict: PASS for this backend task; ready for human verification. No blocking
findings or lower-severity implementation follow-ups identified. Full independent
report: [reviewer-1.md](stu-c5-auth-integration/evidence/reviewer-1.md).
Reviewed base a9dd6508906a11346e9638f77d744593482dff08 plus exact final seven-file
source manifest (SHA-256 3d1c6bdf4e9e9aab7074a8f28f62a3ffee7c3add1b9c4fdb5ef758a955526e82);
frozen C2/C4/C5 tests matched. Current Integration hash
f7893269a4f5c3f1f4e106f3c62225dd3e8d3b4e3c0c31b6ca4a5ce61fad0604 is accepted under
C5 metadata correction addendum 1; no acceptance behavior changed.
Independently inspected full-run XML before rerun: 59 unit +178 PostgreSQL tests,
zero failures/errors/skips; producer full command included bootJar. Reviewer reran
all C5/C4 tests using Java25 and disposable PostgreSQL17: exit0, 18/18 with zero
failures/errors/skips. Sanitized output/counts and their hashes are in the report.
Final source manifest rechecked with zero mismatches. No code/tests changed by
reviewer. Owner merge review and Phuc's human journey verification remain pending;
no frontend/AI implementation, deployment, commit or push approval is implied.

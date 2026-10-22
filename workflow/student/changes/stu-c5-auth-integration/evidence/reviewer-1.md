# Independent review 1: C5 authentication, C2 booking, C4 cancellation

Reviewer: fresh independent reviewer/security role, Codex GPT-6, 22 October 2026.
Verdict: PASS for the backend increment; ready for human verification. No blocking
findings or lower-severity code follow-ups were identified. This is not teammate
approval, deployment approval, or verification of the future UI/AI work.

## Receipt and reviewed revision

Receipt: ACCEPTED for C5/C2/C4 Handoff implementer-1. Derived the checklist from
canonical product/technical/auth/integration contracts before comparing stage
findings. Inspected tracked base-to-worktree diffs and untracked service/tests;
did not read interaction logs or produce implementation changes.

Base: a9dd6508906a11346e9638f77d744593482dff08. No new commit. All seven entries in
implementation-source-hashes.json match the actual final files, checked both
before and after independent verification. Its SHA-256 is
3d1c6bdf4e9e9aab7074a8f28f62a3ffee7c3add1b9c4fdb5ef758a955526e82.
implementation-source.patch SHA-256 is
345d9a070dcb6857fc46cd61e1ff871b605fc94cb6a3c43e473572e56f8de6d1.
The untracked StudentHttpService.java is included in that patch and manifest.

Frozen test hashes independently matched:

| Test | SHA-256 |
| --- | --- |
| StudentBookSlotPgTest.java | 8d5866e3e5ea86a7e3e75ad47338dc9f5c6b0a0d1bfd77ce74ec09ea94fc3ee7 |
| StudentCancelBookingPgTest.java | ce8000aba12ae2a993d339cccf1b49d706d61f837e8a6f1b95bcb10146f03941 |
| StudentAuthIntegrationPgTest.java | 7c15f18c144a59098bf8608171a8d265c0671fbe59ada16f4d37b275c8cd0393 |

Canonical input hashes independently matched:

| Contract | SHA-256 |
| --- | --- |
| Student PRODUCT-SPEC.md | ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957 |
| Student TECHNICAL-SPEC.md | 67c2c5ba98885d4845c16b27eefbed237cdb7767bede9f7d7c44c8aa57ac425b |
| Student PLAN.md, progress refresh | a1c8591b393ca6536244915e185414b0c4edb5f09b799ca706ed60647d312cb9 |
| INTEGRATION-SPEC.md, metadata correction addendum 1 | f7893269a4f5c3f1f4e106f3c62225dd3e8d3b4e3c0c31b6ca4a5ce61fad0604 |
| SHARED-TECHNICAL-SPEC.md | 54ec090a426a1a8733f7628924e4310d27a90b1b724d0acfad3eb0adcde35cfe |
| Auth TECHNICAL-SPEC.md | 2900b5d6ca945c829fffb513fab0fe1e6fd2a0b358fdbd4c3635445d6208de40 |

The Integration addendum was independently checked against AuthController,
AuthenticationService and CurrentAccountFilter: registration does not log in;
generation mismatch invalidates old sessions; reactivation needs fresh login.
It updates the summary of existing rules and changes no acceptance behavior.
The saved review-product-and-spec.patch retains its historical pre-addendum hash
a89c960b8725c9b1d68f33333260f97e8b67c5c7358cbe322ebed9fb9ca7f752;
actual current files and the addendum, rather than that snapshot alone, were reviewed.

## Acceptance and security checks

| Acceptance | Independent inspection and executable evidence |
| --- | --- |
| C5-RUN-1 | app-api composes student-api; runtime actor accepts authenticated SessionPrincipal only. Default activation tests and missing-dependency tests remain present; final full regression covers them. No anonymous fallback. |
| C5-AUTH-1/2, C5-REV-1, C5-CSRF-1 | Real registration/login, runtime V1-V4, session rotation, fresh/old CSRF, current stored role/generation, inactive/deleted/untrusted principals and filter-before-syntax behavior checked in source and rerun in all seven C5 cases. |
| C5-STATE-1 | Real logout/account-switch test checks list isolation, detail/cancel indistinguishable 404s and ignored actor headers; no cached actor or booking state added. |
| C5-REG-1 | Tutor actor seam and Auth/Admin/core policy unchanged. No dependency or migration change. Inspected full regression reports include existing shared-role/core suites. |
| S-BOOK-1..5 | Exact owned DTO and Location, availability causes, half-open overlap/adjacency, same-slot and same-student overlap races, rebooking history covered by retained 15-case C2 suite; inspected real lifecycle/row-lock fixture and full-run results. |
| C2-PREC/IN/ACC/ERR | Exact body/query/UUID parsing; actual streamed 65,536-byte limit independent of Content-Length; duplicate/trailing JSON rejection; safe lifecycle/unique-index mapping; ACCESS_DENIED recheck; no postcommit retry. Tests unchanged, full-run green. |
| S-CANC-1..3, C4-BND/ELIG/IN/ACC/ERR | Atomic owned cancel, repeat/state/start rejection, eligibility-independent cancellation, completion/double-cancel races, strict empty body, safe denial/error mapping, access recheck and committed-state survival after response-read failure checked in source and all 11 cases independently rerun. |
| Existing C1/C3 reads | Read source/DTOs remain unchanged; full regression includes browse/list/detail/filter/SGT/ownership tests. |

Student writes only call the existing ConsultationLifecycle. Core retains sorted
user/module/slot/booking locks and transaction-owned commit/rollback. Student SQL
remains parameterized read-only SQL. Error handlers use fixed safe responses and
the shared request-ID envelope; ApiResponseFilter applies no-store. No request
body/credential/private consultation logging was added.

Independently checked analyst C5-F1..F5 resolutions and C2/C4 refinements F2-01,
F2-06..10, F2-11/12/19 where applicable against source/contracts/tests. Runtime
composition and real auth evidence now close the former adapter/simulated-auth
activation gap. Student-local SLOT_UNAVAILABLE is explicitly documented in
Integration section 9. Existing shared-owner review gate remains pending; it is
not falsely represented as satisfied by this review.

## Verification evidence

Inspected and hashed producer red assertions, focused output, full output and
counts against the handoff. Before the rerun replaced postgresTest XML, separately
aggregated the actual full-run XML: unit 59 in 18 suites, PostgreSQL 178 in 17
suites, zero failures/errors/skips. Full command output records BUILD SUCCESSFUL
and bootJar execution. This is inspected producer full-run evidence, not a claim
that the reviewer reran all 237 tests.

Reviewer command, src/backend, Java 25.0.4.1, existing C:\.gradle cache and disposable
PostgreSQL 17 on 127.0.0.1:55432/mp3_test (unique schemas, synthetic credentials):

```powershell
.\gradlew.bat postgresTest --tests '*StudentAuthIntegrationPgTest' --tests '*StudentCancelBookingPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
```

Exit 0, BUILD SUCCESSFUL in 2m37s; actual resulting XML: C5 7 + C4 11 = 18,
zero failures/errors/skips. Two initial sandbox attempts failed before Gradle/test
execution on cache-directory access; the successful rerun used approved existing
cache access. Those setup attempts are not counted as behavioral failures.

| Reviewer evidence | SHA-256 |
| --- | --- |
| reviewer-focused-console.txt | 04a4f45af227b97cc5adc9c01f665c65a98fa7e252ff5de3dc7414b84d8c4ec2 |
| reviewer-focused-counts.json | e6764c9f57b2edd99fdf2712d829b2b47b93c186af7d311284a1f9de833906d8 |

git diff --check excluding interaction logs passed before recording review.
No implementation/test fixes were made by the reviewer. No live dependency
advisory scan was performed because no dependencies changed; this is a scoped
security/integration review, not a whole-application security audit.

## Remaining gates

Phuc's backend journey acceptance and affected Auth/web/core-owner review before
merge remain pending. Frontend implementation/keyboard/session-state UI checks,
AI/provider work, real-account migration/backfill and deployment are outside this
backend increment. No human acceptance, commit, push or deployment is claimed.

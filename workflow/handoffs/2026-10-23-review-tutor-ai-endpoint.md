# Tutor P6 Endpoint Review

Date: 23 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Review type: Primary-agent source and evidence review; not an independent reviewer reproduction.  
Scope: Tutor note-draft HTTP route, authorization/orchestration service, request and response boundaries, focused tests, PostgreSQL-backed MockMvc coverage, P6 spec/matrix/evidence claims.

## Result

No blocking defect was found in the reviewed backend endpoint path. The service checks the captured session principal and Tutor role, confirms the session actor matches the current Tutor actor, verifies booking ownership and completed booking/slot state before generation, and rechecks the same principal after generation. The draft service has no note persistence or lifecycle dependency. The controller applies strict request parsing before dispatch and returns only the specified response fields. Typed AI failures have fixed public messages and stable status codes; provider response text is not returned by the exception mapping.

The deterministic tests provide meaningful side-effect evidence: invalid role, inactive account, foreign booking, incomplete booking, CSRF failure, and session revocation are covered with zero-call or output-discard assertions. PostgreSQL-backed MockMvc tests exercise the route, CSRF filter, real booking/slot repositories, response shape, and no-write behavior.

## Findings and disposition

| ID | Finding | Severity | Disposition |
| --- | --- | --- | --- |
| TAI-REV-01 | The evidence register still said authorization/ownership/no-write tests were pending and said the endpoint did not exist, despite the current tests and developer handoff. Its completion checklist also still instructed the team to implement the endpoint. | Documentation accuracy | Corrected `TUTOR-AI-EVIDENCE.md` to describe the implemented tests and current next evidence needed. |
| TAI-REV-02 | At the time of this source review, endpoint integration used test-only `AuthenticationService`. | Initially open; follow-up addressed | Added app-api/PostgreSQL integration using production `AuthenticationService` and account storage. It now covers generation revocation and account deactivation during generation, safe output discard, session invalidation, and pre-dispatch anonymous/wrong-role denial. See the [follow-up evidence](../tutor/evidence/p6-auth-integration-2026-10-23.txt). |
| TAI-REV-03 | The React draft UI, race handling, and explicit copy/save confirmation are not implemented. `requiresReview=true` plus a no-write endpoint do not alone prove human oversight in the product flow. | Incomplete acceptance scope | Kept P6 incomplete; implement and test the UI before claiming full increment acceptance. |
| TAI-REV-04 | The shared gateway/provider path is still a proposal without production bean/key routing or active migration, and the actual provider raw-response streaming cap/SoC behavior remain unverified. | Cross-role / release gate | No change. Shared owners and provider verification remain required before activation. |

No code change was needed from this review. The review did not inspect or approve production migration activation, real SoC credentials/provider behavior, or cross-role gateway acceptance.

## Follow-up work

After the review, the following was implemented and tested:

- `TutorExceptionHandler` now invalidates the HTTP session when the post-generation authentication recheck reports `UNAUTHENTICATED` or `ACCOUNT_INACTIVE`.
- The app-api/PostgreSQL integration fixture now injects a deterministic AI gateway and exercises anonymous and wrong-role zero-call denials, a successful real Tutor session, generation revocation during provider work, and account deactivation during provider work.
- `TutorApiConfiguration` now rejects deterministic fake-provider output unless the `local` profile is active. Focused tests verify fake output remains labeled and usable locally, and maps to unavailable outside local.
- The evidence register and test matrix now distinguish verified production-auth behavior from still-pending provider/UI work.

Verification after follow-up: 19 focused Tutor AI unit tests passed; all 9 tests in `StudentAuthIntegrationPgTest` passed against a disposable PostgreSQL cluster. A subsequent clean full verification passed 87 unit tests across 25 suites, 191 PostgreSQL tests across 18 suites, and `bootJar`, with zero failures/errors/skips. `git diff --check` passed. See [follow-up test evidence](../tutor/evidence/p6-auth-integration-2026-10-23.txt).

## Verification

- Source inspection covered the files listed in the snapshot hashes below and the P6 spec, matrix, and evidence register.
- `git diff --check` — passed after the evidence-register correction.
- Attempted `cd src/backend && ./gradlew test bootJar --console=plain` — could not start because the Gradle wrapper attempted to create a lock under `/Users/huanghao/.gradle`, which is outside this task's writable workspace. No fresh test result is claimed by this review.
- The implementation handoff records the earlier successful run: 85 unit tests passed and `bootJar` succeeded; the combined run passed 189 PostgreSQL tests before the final unit-only inactive-account test was added. PostgreSQL test sources were unchanged afterward. These are prior recorded results, not independently reproduced here.

## Snapshot hashes

SHA-256:

- `src/backend/src/main/java/unichope/tutor/TutorNoteDraftService.java` — `e30815c962094ac534594bc2f7f75f30d3b1e6d5a143abf7f6be0cec7e0b2bf8`
- `src/backend/src/main/java/unichope/tutor/http/TutorController.java` — `93d0f607c856ff42a5597dfd61f79c6147295b61894d7f04d6296558da4572bb`
- `src/backend/src/main/java/unichope/tutor/http/TutorApiInput.java` — `54449a9bd4e5ef505a605be885e5f656ab8b0f3274677ea78b90dfd33c995001`
- `src/backend/src/main/java/unichope/tutor/http/TutorExceptionHandler.java` — `e0e48bccc02009552d78e8717dcef976fb5e50bf982806ef644fecb3df611854`
- `src/backend/src/test/java/unichope/tutor/TutorNoteDraftServiceTest.java` — `3095700786ff8ccb0f15a1fd3adef4f3051240455174c6a3dc11fee1de97210a`
- `src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java` — `7f4accb3bade9934d808993bd567ff9039c51f463f9eb596042127388e787124`
- `workflow/tutor/TUTOR-AI-INCREMENT-SPEC.md` — `b359ca3889b026cff4401b9e510389f93928eaf32397189df5559ff765040b5d`
- `workflow/tutor/TUTOR-AI-TEST-MATRIX.md` — `10a1011e96935b92215d267f8dd3495b35192d34188a30d0b35a3ed5f8046070`

## Handoff

The next review should be performed by a separate reviewer who receives the spec, test matrix, source snapshot, and commands, then reproduces the relevant tests. After that, finish React draft-state tests, obtain shared AI/auth owner decisions, and record human acceptance separately. Do not treat this primary-agent review as independent approval.

Current status: production-auth integration is now covered for the P6 endpoint, so the next separate reviewer should review and reproduce this follow-up. The React draft state is not in this checkout; the repository currently contains only `src/backend`, so UI implementation must wait for or add the frontend project. Provider activation still requires the shared gateway owner to settle the production wiring, role-key secret configuration, actual SoC limits/raw-response cap, and migration decision.

# Tutor AI Draft Endpoint — Developer Handoff

Date: 23 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Scope: P6 server-side draft endpoint and pre/post-generation authorization.

## Implemented

- Added `POST /api/tutor/bookings/{id}/note-draft` with strict JSON parsing and
  the specified response fields: booking ID, draft, review flag, and provider mode.
- Added `TutorNoteDraftService`. It checks the captured `SessionPrincipal`, active
  Tutor identity, booking ownership, and completed booking/slot before generation.
  It rechecks the same principal after generation and discards output if the
  account/session is no longer valid or the user no longer has the Tutor role.
- Generation has no note repository or lifecycle dependency and performs no writes.
  Existing version-checked manual note PUT remains the persistence path.
- Mapped typed AI failures to stable HTTP codes/statuses and `Retry-After` for
  admission failures.
- Registered gateway/service beans only when an `AiGateway` and positive
  `unichope.ai.tutor.note-draft.max-output-tokens` setting are supplied. Without
  them, the route returns sanitized `503 AI_UNAVAILABLE`.

## Verification

- Focused: `./gradlew test --tests '*TutorNoteDraft*Test' --tests '*TutorAiFailureMappingTest' --console=plain` — passed.
- Combined run: `./gradlew clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/postgres -Dcore.test.user=mp3_test -Dcore.test.password= --console=plain` — passed; `test` 84/84 across 24 suites, `postgresTest` 189/189 across 18 suites, zero failures/errors/skips; `bootJar` succeeded.
- After adding the inactive-account pre/post-generation case, `./gradlew test bootJar --console=plain` passed; the current unit report has 85 tests across 24 suites, zero failures/errors/skips. The PostgreSQL test sources were unchanged after the 189-test run.
- JUnit XML reports: `src/backend/build/test-results/{test,postgresTest}`.
- The first disposable cluster used `SQL_ASCII`; seven existing Unicode-note PostgreSQL cases failed there. Recreating the cluster with UTF-8 resolved the environment issue, and the recorded full run passed.
- `git diff --check` passed.

Service tests use mocks/fakes; PostgreSQL-backed MockMvc tests exercise the route,
CSRF filter, booking repositories, owner/state checks, and no-write behavior. The HTTP
fixture uses a test-only AuthenticationService, so production account/session-generation
integration remains unverified. React state, provider streaming limits, SoC behavior,
production key/quota configuration, and active migration are also unverified. The shared admission
proposal has an independent review handoff; this Tutor endpoint does not yet have
an independent review or cross-role owner acceptance. No commit or push was made.

## Reviewed source snapshot hashes

SHA-256 of the endpoint, focused tests, PostgreSQL HTTP suite, and spec/matrix
used for the recorded run:

- `src/backend/src/main/java/unichope/tutor/TutorNoteDraftService.java` — `e30815c962094ac534594bc2f7f75f30d3b1e6d5a143abf7f6be0cec7e0b2bf8`
- `src/backend/src/main/java/unichope/tutor/http/TutorController.java` — `93d0f607c856ff42a5597dfd61f79c6147295b61894d7f04d6296558da4572bb`
- `src/backend/src/main/java/unichope/tutor/http/TutorApiConfiguration.java` — `e1d57eff2552d4183b09aa3273fe180aec854dfb3ed9308b043840c605e858bf`
- `src/backend/src/main/java/unichope/tutor/http/TutorExceptionHandler.java` — `e0e48bccc02009552d78e8717dcef976fb5e50bf982806ef644fecb3df611854`
- `src/backend/src/test/java/unichope/tutor/TutorNoteDraftServiceTest.java` — `3095700786ff8ccb0f15a1fd3adef4f3051240455174c6a3dc11fee1de97210a`
- `src/backend/src/test/java/unichope/tutor/http/TutorNoteDraftControllerTest.java` — `c975388ce00ea714569c582596892ed267851b8b32caf147fd2626a077ca3598`
- `src/backend/src/test/java/unichope/tutor/http/TutorAiFailureMappingTest.java` — `67d5a47975c2c1df2fba0da4567500424562b9685d9f861b4d9e7f80ef9930ea`
- `src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java` — `7f4accb3bade9934d808993bd567ff9039c51f463f9eb596042127388e787124`
- `workflow/tutor/TUTOR-AI-INCREMENT-SPEC.md` — `b359ca3889b026cff4401b9e510389f93928eaf32397189df5559ff765040b5d`
- `workflow/tutor/TUTOR-AI-TEST-MATRIX.md` — `8362d48ed25b13d672fef6467142fa40062a53f820973d52803095a167b46d4d`

## Follow-up — 23 October 2026

The production-auth gap is now covered by `StudentAuthIntegrationPgTest`, which
starts the real `app-api/local` application and uses its production
`AuthenticationService`, PostgreSQL account storage, real login/session principal,
and CSRF token. The test injects only the AI gateway as a deterministic fake. It
verifies anonymous and wrong-role requests make no AI call; a successful Tutor
request uses the authenticated Tutor ID, exact task, supplied rough notes, and
configured token budget; session-generation revocation and account deactivation
during provider work discard the output and invalidate the session. No note is
persisted.

`TutorExceptionHandler` now invalidates the current HTTP session for
`UNAUTHENTICATED` and `ACCOUNT_INACTIVE` API failures. `TutorApiConfiguration`
rejects a `FAKE` provider result unless the `local` profile is active; focused
unit tests cover both local acceptance and non-local rejection. The app remains
unavailable for Tutor AI by default when there is no gateway bean and positive
token-budget setting.

Verification:
- `./gradlew test --tests '*TutorNoteDraft*Test' --tests '*TutorAiFailureMappingTest' --tests '*TutorAiRuntimeGuardTest' --console=plain` — passed, 19 tests across six suites.
- `./gradlew postgresTest --tests 'unichope.platform.runtime.StudentAuthIntegrationPgTest' -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/postgres -Dcore.test.user=mp3_test -Dcore.test.password= --console=plain` — passed, 9 tests including both P6 integration cases and anonymous/wrong-role no-call assertions.
- Disposable PostgreSQL data was stopped and removed after the run. `git diff --check` passed.

The targeted runs were followed by a clean full backend run: 87 unit tests across
25 suites and 191 PostgreSQL tests across 18 suites passed with zero
failures/errors/skips, and `bootJar` succeeded. Provider/key routing, actual SoC
limits and raw-response cap, active migration, React draft state, independent
review, and P6 human acceptance remain open. The current checkout contains
backend source only.

Follow-up source hashes (SHA-256):
- `src/backend/src/main/java/unichope/tutor/http/TutorExceptionHandler.java` — `47ebb95c3105d5f4cc106343f884f10e11a35ef575707f6aba99ef9eb7360b51`
- `src/backend/src/main/java/unichope/tutor/http/TutorApiConfiguration.java` — `a66b63db1ebf17ce7d6c34e31f195978f8345b24262e30d881a486cc493e18bb`
- `src/backend/src/test/java/unichope/platform/runtime/StudentAuthIntegrationPgTest.java` — `4fcca49c5b96a1bd3f6c9c8c32c9b677e8a2d420b8385ff46703a17f4941c9df`
- `src/backend/src/test/java/unichope/tutor/http/TutorAiRuntimeGuardTest.java` — `a690279921afb569c6c79a70bab1d8aa3143086e15c9bfc35e0d317b1c5f8712`
- `workflow/tutor/TUTOR-AI-TEST-MATRIX.md` — `608941882f50a312791df2c121052420a218b66b203167634a23ab808e9fac07`
- `workflow/tutor/TUTOR-AI-EVIDENCE.md` — `ab717b32d7e99ff0f0e21afbd4b6b5297f7937376781eab1a93b5d8c9111b782`

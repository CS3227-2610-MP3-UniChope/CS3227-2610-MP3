# Developer Handoff: Tutor Bookings, Completion, and History

Task: TUT-P4. Role: Developer (`/root/p4_tutor_developer`). Date: 22 October 2026.
Base HEAD: `4ed9d696db754d0d7982d75ec5a287b3a5229407`; branch:
`feat/tutor-bookings-p4`. Input manifest:
`workflow/handoffs/2026-10-22-tutor-bookings-developer-inputs.json`, SHA-256
`1c3c5654648576fe85ef79f3951e4b987be655b1fac71e927e276b604b7be6de`.

## Scope and implementation

Human scope approval was recorded on 22 October 2026 for Tutor bookings,
completion, and slot history. Implemented TUT-BOOK-01, TUT-BOOK-02 and
TUT-HIST-01: database-owner-scoped filtered reads; completion delegated to
`ConsultationLifecycle.completeActiveBooking`; cancelled/completed slot history;
and a minimal React/TypeScript/Vite/Bootstrap booking view with independent
filters, confirmation, refresh, loading, empty and error states. Added Vitest /
Testing Library coverage and a frontend CI job. No schema or migration changes.

The backend tests cover owner filtering/privacy, supported and malformed filters,
Singapore date boundaries, stable ordering, completion and state-denial behavior,
and session/role/inactive/CSRF fixtures. Existing lifecycle tests remain the
evidence for locking, races and atomicity; those tests were not duplicated.
Frontend tests cover confirmation cancellation/success, independent filters,
and failure/refresh behavior. The synthetic security test fixtures do not prove
real login/session integration.

MP2 reuse was assessed by inspecting `MP2/src/main/java/tutor/TutorService.java`
(279 lines), `BookingFilter.java` (8 lines), and `TutorBookingView.java` (9
lines). Relevant MP2 behavior includes booking filtering/ownership/date/status
ordering (TutorService lines 201–216), history (228–240), and booking/history/
completion entry points (85–95, 111–135). This increment adapts those contracts
and behavior to PostgreSQL-backed web APIs and the shared lifecycle. **Zero MP2
source lines were copied into this increment**; no code-copy percentage is
claimed. JavaFX views were not reused.

## Verification

- Backend CI command passed: from `src/backend`,
  `./gradlew test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=`.
  Captured output: `logs/2026-10-22-tutor-p4-backend-checks.txt`. Gradle reports:
  `src/backend/build/reports/tests/test/index.html` and
  `src/backend/build/reports/tests/postgresTest/index.html`.
- The added equal-start-time stable-ID case also passed its focused PostgreSQL
  rerun (`TutorRestPgTest.bookingAndHistoryRowsUseStableIdOrderForEqualStartTimes`;
  BUILD SUCCESSFUL, task was UP-TO-DATE on final rerun).
- Frontend `npm test` passed (5 tests); `npm run build` passed, including strict
  TypeScript checking. Captured output:
  `logs/2026-10-22-tutor-p4-frontend-tests.txt`.
- `npm ci` completed with zero reported vulnerabilities after resolving the
  initial dependency audit findings. The CI frontend job runs `npm ci`, tests,
  and build.
- `git diff --check` passed before handoff preparation.

Test setup corrections/limitations: the Gradle wrapper is under `src/backend`,
and PostgreSQL-tagged tests require `postgresTest` (the default `test` task omits
them). An initial disposable PostgreSQL cluster using SQL_ASCII caused existing
note tests with non-ASCII data to fail; recreating the disposable test database
with UTF-8 made the full backend command pass. The temporary PostgreSQL server
was on port 55432 and used `/private/tmp/tutorp4-pgdata-20261022`; an ordinary
`pg_ctl` stop attempt was denied by the process permission boundary, so it may
still be running. No production database or SoC provider was used, and no secrets
were read. Some dependency/cache operations required runtime approval escalation;
none were rejected.

## Matrix map and limitations

Repository/controller tests cover BK-01–BK-10, BC-01–BC-05 and BC-09–BC-10,
and HS-01–HS-06, including their owner, validation, safe DTO, SGT, and synthetic
security conditions. Existing shared lifecycle tests provide the atomicity and
concurrency oracle for BC-03/04. Frontend tests cover BC-06–BC-08 and HS-07.
The test matrix contains finer combinations than each test method; the reviewer
can verify the exact assertions in `TutorRestPgTest` and
`BookingsPage.test.tsx` against the matrix.

The UI obtains CSRF material through the existing `/api/auth/csrf` contract,
which is not implemented by this increment; live auth integration is a dependency
for end-to-end use. No production session adapter, production database activation,
AI, notes UI, or deployment was performed. No commit or push was made.

## Changed implementation paths

- `src/backend/src/main/java/unichope/tutor/TutorBooking.java`
- `src/backend/src/main/java/unichope/tutor/TutorReadRepository.java`
- `src/backend/src/main/java/unichope/tutor/TutorReadService.java`
- `src/backend/src/main/java/unichope/tutor/PostgresTutorReadRepository.java`
- `src/backend/src/main/java/unichope/tutor/http/TutorApiConfiguration.java`
- `src/backend/src/main/java/unichope/tutor/http/TutorApiInput.java`
- `src/backend/src/main/java/unichope/tutor/http/TutorController.java`
- `src/backend/src/main/java/unichope/tutor/http/TutorHttpService.java`
- `src/backend/src/test/java/unichope/tutor/TutorReadServiceTest.java`
- `src/backend/src/test/java/unichope/tutor/TutorBookingReadServiceTest.java`
- `src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java`
- `src/frontend/**`
- `.github/workflows/backend-ci.yml`
- `workflow/tutor/BOOKING-INCREMENT-PLAN.md`

Next role: independent reviewer. Implementation acceptance remains pending human
review; this handoff does not record acceptance.

## Independent-review fixes and re-verification

Review-fix input manifest: `workflow/handoffs/2026-10-22-tutor-bookings-review-fix-inputs.json`
(SHA-256 `7a22314ed7f3714e87523c115662ef7aeb9c345e5eaab859798465b736e0d077`),
captured at base HEAD `4ed9d696db754d0d7982d75ec5a287b3a5229407`.

The reviewer requested three P2 oracle fixes. All three are addressed:

1. `bookingsAreOwnerScopedFilteredAndReturnOnlyApprovedFields` now seeds owned
   ACTIVE, CANCELLED, and COMPLETED bookings and asserts each status filter
   returns exactly its expected booking and status. It retains the owner/privacy
   and multi-filter assertions. This closes the status-filter gap in BK-02.
2. `bookingAndHistoryReadsRequireActiveTutorSession` exercises both GET routes
   with no session (401), a student actor carrying a misleading tutor authority
   (403), and an inactive tutor (403). Error responses are checked against the
   existing exact safe-error contract and the persisted slot/booking snapshot is
   unchanged. Each inactive route request uses a fresh session because the
   security filter correctly invalidates it after the first denial. This adds
   request-level evidence for the read-route portion of BK-11 / HS-05 security
   expectations; existing completion and shared tests remain unchanged.
3. The successful completion frontend test now asserts the “Mark completed”
   action is absent after the refreshed COMPLETED row appears, closing the
   post-refresh portion of BC-07.

Verification after these fixes:

- Focused PostgreSQL command passed (2 tests):
  `./gradlew postgresTest --tests unichope.platform.core.postgres.TutorRestPgTest.bookingsAreOwnerScopedFilteredAndReturnOnlyApprovedFields --tests unichope.platform.core.postgres.TutorRestPgTest.bookingAndHistoryReadsRequireActiveTutorSession -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=`.
- Full backend command passed again:
  `./gradlew test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=` (`BUILD SUCCESSFUL`, 55s).
- Frontend `npm ci && npm test && npm run build` passed; five tests passed and
  strict TypeScript/Vite build succeeded.
- `git diff --check` was rerun after the review fixes.
- Gradle HTML/XML reports were refreshed under `src/backend/build/reports/tests/`
  and `src/backend/build/test-results/`.

## Reverification after PR #5 merged

The P4 worktree was fast-forwarded from `4ed9d69` to current `main` at
`4c0285422622e0e85e5552ebc5d2c45a0539d48e`. The P4 patch had no overlapping
paths with the merged PR. On this updated tree:

- From `src/backend`, ran
  `./gradlew test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=`
  against a newly created disposable UTF-8 PostgreSQL database; result:
  `BUILD SUCCESSFUL`.
- From `src/frontend`, ran `npm test && npm run build`; all 5 tests passed and
  the strict TypeScript/Vite production build succeeded.
- `git diff --check` passed. The disposable PostgreSQL server and data directory
  were stopped and removed after verification.

The integration with merged auth wiring compiles and the complete backend suite
passes. Tutor request tests still use synthetic session identities; they do not
claim a browser login-to-Tutor end-to-end test. Human implementation acceptance
was recorded on 22 October 2026; see
[`2026-10-22-tutor-bookings-human-acceptance.md`](2026-10-22-tutor-bookings-human-acceptance.md).

One intermediate focused test run failed because its test reused the same
inactive session for both routes; the first request invalidated the session, so
the second correctly returned 401. The fixture was corrected to create a fresh
session per inactive-account request. The final focused and full suites pass.

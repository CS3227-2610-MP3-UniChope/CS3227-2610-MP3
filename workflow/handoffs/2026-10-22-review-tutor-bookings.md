# Independent Review: Tutor Bookings, Completion, and History

Date: 22 October 2026
Task: TUT-P4. Role: Independent reviewer (`/root/p4_tutor_independent_review`).
Branch: `feat/tutor-bookings-p4`. Base HEAD: `4ed9d696db754d0d7982d75ec5a287b3a5229407`.

## Reviewed inputs

The review used the approved increment specification and test matrix, the shared
and reviewer instructions, the developer handoff, and the implementation/tests
listed there. The relevant input hashes at review time were:

- `workflow/tutor/BOOKING-INCREMENT-SPEC.md`: `af3bbabdaba73fcd0beb0d75882011370a9461dbfa483a1e80bee3398c249db4`
- `workflow/tutor/BOOKING-TEST-MATRIX.md`: `8beed3df1a5ecec465b3ce0b44be89fb520b5d708155b718420ea6992aec979c`
- `workflow/agents/common.md`: `ab059b67fa5210987ea0b8e3e5ff02e00e2f084165c66357b6426cac2f5c1564`
- `workflow/agents/reviewer.md`: `24d9b210090598b7d5f0a79780367e951a660be148e22a82f40dbd130e9a240e`
- `workflow/handoffs/2026-10-22-developer-tutor-bookings.md`: `b24b97161a1e475ab462e6565490019c4630f0ae48fdc738f247486bf4727830`
- `src/backend/src/main/java/unichope/tutor/http/TutorController.java`: `02fb80c48bd150308a9f67057a39705a35dd2507f9509754af5c73d7ce5cb410`
- `src/backend/src/main/java/unichope/tutor/http/TutorApiInput.java`: `6f2cd2b8f3ea0d3cc1d1bb3bbecc303b81981b3fd2e679534fadcd34fc2a6`
- `src/backend/src/main/java/unichope/tutor/TutorReadService.java`: `01fb2fcb3b83ec70945b5cf44dda0f299063caa6bb2a9f73aff58f84cfb41e17`
- `src/backend/src/main/java/unichope/tutor/PostgresTutorReadRepository.java`: `f8988cb715a151665db70ffca0a77d5c5545d7785545d8c363349f639c37c3d1`
- `src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java`: `4e6c54df2a7a92b4e5c3b2fea9f4fabfcde5892a644555661a4920eb7c2538aa`
- `src/frontend/src/pages/BookingsPage.tsx`: `1c3e73d77d58aeb8031fa34287f0e83b2f9ebf727c5fba036a9dfd05f3492b7d`
- `src/frontend/src/pages/BookingsPage.test.tsx`: `45f486f7ac5fb40c080768242391526fa12cac6f0148b7944134b877ff155bf8`

## Findings

1. **P2 — Booking-list status coverage omits CANCELLED.** The matrix requires
   `BK-01` to seed ACTIVE, CANCELLED, and COMPLETED bookings, and `BK-02` to
   request each supported status. In
   `src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java:286-301`,
   the list fixture contains ACTIVE and COMPLETED bookings only; the only
   positive status-filter request is ACTIVE. A regression that excludes
   CANCELLED bookings or mishandles the CANCELLED filter would pass this suite.
   Add a tutor-owned CANCELLED booking and assert results for each of the three
   status filters, including exclusion of other statuses.

2. **P2 — Read-route authorization is not verified through MockMvc.** `BK-11`
   calls for missing-session, wrong-role, and inactive-account request checks.
   `BC-10` covers those conditions for the completion POST, but there are no
   corresponding requests to `GET /api/tutor/bookings` or
   `GET /api/tutor/history/slots`; the role-denial assertion in
   `TutorBookingReadServiceTest` only calls the service directly. The
   implementation currently calls `requireActiveTutor()` for both reads, but
   the HTTP/session boundary and no-data behavior are not protected by a
   request-level regression test. Add security-enabled MockMvc cases for both
   list routes and assert the shared 401/403 responses without exposing rows.

3. **P2 — Completion UI test does not assert the post-refresh action is gone.**
   Matrix `BC-07` requires a successful completion followed by a refresh where
   the booking displays COMPLETED and is not offered for repeat completion.
   `src/frontend/src/pages/BookingsPage.test.tsx` checks the completed label and
   that bookings were fetched twice, but does not assert that “Mark completed”
   disappears after the refreshed response. Add that assertion to preserve the
   full user-visible oracle.

## Independently reproduced checks

- From `src/backend`, ran
  `./gradlew test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=`.
  Result: `BUILD SUCCESSFUL`; unit and PostgreSQL test tasks completed. This used
  the developer's disposable local PostgreSQL database; no production database
  or provider was accessed. Gradle reports are under
  `src/backend/build/reports/tests/`.
- From `src/frontend`, ran `npm test && npm run build`. Result: all 5 Vitest
  tests passed; strict TypeScript check and Vite production build passed.
- `git diff --check` passed.
- Inspected the API parsing, owner-scoped SQL, active-tutor checks, lifecycle
  delegation, frontend confirmation, and CI workflow. No code correctness,
  privacy, or ownership defect was found in those paths by inspection.

## Controls and scope

The review was instructed to avoid implementation edits. This is an instruction,
not an enforced read-only sandbox: the reviewer had workspace write access and
created only this assigned review record. Test/build outputs and existing local
dependency/build caches were permitted; no network access, escalation, secrets,
or external services were used. One earlier SHA utility attempt failed due to a
local Perl locale error; `sha256sum` provided the listed hashes.

The implementation remains limited to Tutor bookings, completion, and slot
history. Synthetic session fixtures do not establish real login integration;
the frontend still depends on the authentication increment's `/api/auth/csrf`
route. No schema change, production activation, SoC use, commit, or push was
reviewed or performed.

## Recommendation and status

**Changes requested for this slice:** close findings 1–3, then request an
independent re-review against fresh inputs. No implementation acceptance is
recorded; human verification remains pending.

## Independent re-review of findings 1–3

Re-review date: 22 October 2026. Reviewer: `/root/p4_tutor_independent_review`.
The developer's fresh-fix manifest is
`workflow/handoffs/2026-10-22-tutor-bookings-review-fix-inputs.json`
(SHA-256 `7a22314ed7f3714e87523c115662ef7aeb9c345e5eaab859798465b736e0d077`),
based on the same main HEAD. The updated developer handoff SHA-256 is
`1075b93ac525605ae82bd75aa2fe020c4deb022b65713c5fc84f590121c17e36`.
Updated test hashes:

- `TutorRestPgTest.java`: `17f6fa218b8ab29f7789222d6b775969ff9ce454e4f8a5e90ba978fe378603fb`
- `BookingsPage.test.tsx`: `70de9ce728dd852840299a446da6136acc39a3f58313e109871da731aa94c184`

Finding dispositions:

1. **Closed.** The PostgreSQL request test now includes all three booking
   statuses and asserts each status filter returns exactly the matching owned
   booking and status. Independently reproduced the focused test successfully.
2. **Closed.** The PostgreSQL MockMvc test exercises both GET routes with no
   session, a student principal carrying a forged tutor authority, and an
   inactive tutor. It checks the expected 401/403 API errors, invalidation of
   inactive sessions, and an unchanged slot/booking snapshot. Independently
   reproduced the focused test successfully.
3. **Closed.** The UI completion test now asserts the completion button is absent
   after the refreshed COMPLETED booking appears. Independently ran all five
   Vitest tests and the strict TypeScript/Vite build successfully.

Re-review commands and results:

- From `src/backend`, ran the two focused tests with
  `./gradlew postgresTest --tests unichope.platform.core.postgres.TutorRestPgTest.bookingsAreOwnerScopedFilteredAndReturnOnlyApprovedFields --tests unichope.platform.core.postgres.TutorRestPgTest.bookingAndHistoryReadsRequireActiveTutorSession -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=`.
  Result: `BUILD SUCCESSFUL`.
- From `src/frontend`, ran `npm test && npm run build`. Result: 5 tests passed;
  strict TypeScript check and production build passed.
- `git diff --check` passed after the fixes.

**Re-review recommendation: accept this implementation slice for human review.**
The three recorded test-oracle findings are closed. This recommendation is not
human implementation acceptance. Real authentication/session integration and
the `/api/auth/csrf` dependency remain outside the verified scope as recorded
above.

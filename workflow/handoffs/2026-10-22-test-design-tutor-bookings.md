# Independent Test Design Review: Tutor Bookings

Date: 22 October 2026
Role: Test designer
Review type: Read-only review of the P4 draft; no implementation or tests run.

## Inputs

- `workflow/tutor/BOOKING-INCREMENT-SPEC.md` — SHA-256 before review:
  `1fdff48d0e20bf217e6dd4cb5474037eec071a8499acaeb857e5426beecd0c6a`
- `workflow/tutor/BOOKING-TEST-MATRIX.md` — SHA-256 before review:
  `a341a87eeea08408dda620332df903689d19cd401a83b3ff78f1ccb8db0bfe2b`
- `workflow/tutor/BOOKING-INCREMENT-PLAN.md` — SHA-256 before review:
  `c1e759886372e04d6d223184335c20f332ada104d5c0899d904ecc3b3c598997`
- Current `main` at review: `4ed9d696db754d0d7982d75ec5a287b3a5229407`.

## Findings and disposition

1. **P1 — History test exceeded scope.** HS-07 referred to note identity even
   though this increment excludes the note selector/editor. Updated the spec and
   matrix to assert independence of history filters, booking filters, and the
   selected booking only.
2. **P1 — Completion body validation was missing.** Added BC-09 for malformed
   JSON and spoofed actor/role/status fields; it expects 400 and an unchanged
   persisted booking/slot snapshot.
3. **P1 — Denied completion state was not checked in PostgreSQL.** Added BC-10
   for missing session, wrong role, inactive tutor, and missing/invalid CSRF,
   with the shared 401/403 contract and unchanged persisted state.
4. **P2 — Baseline was stale.** Updated the spec and plan to the then-current
   `main` commit before implementation planning.

## Coverage assessment

The reviewer found sound coverage for owner-scoped queries, combined filters,
Singapore-date boundaries, stable ordering, response privacy, completion state
transitions, early completion, and reuse of lifecycle race/rollback tests instead
of duplicating them at the HTTP layer. Existing route shapes and early-completion
behavior are specified. Real session authentication remains outside this increment.

## Current status

The draft inputs were not changed before the review response. The findings above
have been applied to the working spec and matrix; this record captures the
reviewer's findings and their dispositions. Human scope approval remains pending.
No application code changed and no tests were run.

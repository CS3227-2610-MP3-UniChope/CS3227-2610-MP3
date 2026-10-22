# Architect Handoff: Tutor Bookings, Completion, and History

Task: TUT-P4. Role: Architect/coordinator (`/root`). Date: 22 October 2026.
Input HEAD: `4ed9d696db754d0d7982d75ec5a287b3a5229407`.
Input manifest: `workflow/handoffs/2026-10-22-tutor-bookings-developer-inputs.json`
(SHA-256 `1c3c5654648576fe85ef79f3951e4b987be655b1fac71e927e276b604b7be6de`).

## Approved scope and requirements

Human scope approval: Approved on 22 October 2026; scope: Tutor bookings,
completion, and slot history (P4). See
`2026-10-22-tutor-bookings-scope-approval.md`.

Requirements: TUT-BOOK-01 (filtered owner-scoped booking list), TUT-BOOK-02
(confirmed completion through shared lifecycle), and TUT-HIST-01 (independent
cancelled/completed slot history). Detailed contract and acceptance criteria:
`workflow/tutor/BOOKING-INCREMENT-SPEC.md`. Independently reviewed test oracle:
`workflow/tutor/BOOKING-TEST-MATRIX.md`; findings and fixes:
`2026-10-22-test-design-tutor-bookings.md`.

## Design and dependencies

- Reuse `ConsultationLifecycle.completeActiveBooking`; do not reimplement status
  transitions, row locks, rollback, or race behavior.
- Add PostgreSQL tutor-scoped booking and history reads, API endpoints and the
  minimal React view/test setup required by the approved behavior.
- Existing shared Integration spec defines route shapes and DTOs. Existing REST
  security fixtures provide synthetic actor/session checks; real login/session
  integration remains outside this task.
- No schema migration, AI, note editor, Student/Admin behavior, or production
  activation is included.
- Frontend baseline is absent. Add only React/TypeScript/Vite/Bootstrap and
  Vitest/Testing Library configuration needed for this view, with a CI frontend
  job for install/lint/tests/build.

## Developer limits and actual controls

Permitted changes: `src/backend/src/main/java/unichope/tutor/**`, corresponding
Tutor and PostgreSQL tests, `src/frontend/**`, `.github/workflows/backend-ci.yml`,
the P4 plan/spec/matrix when testability requires a documented clarification, and
assigned workflow handoffs/logs. Preserve shared-core behavior and unrelated files.

Permitted checks: inspect project scripts/configuration; run focused unit and
PostgreSQL tests, frontend lint/tests/build, and the full backend CI command. No
production resources, real SoC calls, or secret reads. Do not commit, push, or
deploy.

The inherited environment is workspace-write with repository and `/private/tmp`
writable roots; network access is restricted; commands requiring elevated network
access are subject to approval review. Agent instructions are not an OS-level
read-only sandbox. Temporary Gradle/npm caches and build/test reports may be
written under the worktree or `/private/tmp`.

## Required developer evidence

Write tests before implementation and retain initial failing and final passing
results. Map tests to matrix IDs. Record exact commands/exit results, generated
report paths, deviations or blocked checks, and actual MP2 reuse. Hand off the
patch to an independent reviewer. Do not mark human implementation acceptance.

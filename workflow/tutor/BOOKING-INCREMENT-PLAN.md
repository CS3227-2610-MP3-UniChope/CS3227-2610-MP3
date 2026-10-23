# Tutor Bookings, Completion, and History Increment Plan

Status: Draft; independent test-design review complete, human scope review pending.
Specification: [Bookings increment](BOOKING-INCREMENT-SPEC.md).
Test matrix: [Bookings test matrix](BOOKING-TEST-MATRIX.md).
Baseline: `main` at `4ed9d696db754d0d7982d75ec5a287b3a5229407` (22 October 2026).

## Workflow and gates

1. **Analyst review:** check the three requirement IDs against Product,
   Technical, Integration, and shared Lifecycle specs. Report conflicts and
   assumptions without editing product code.
2. **Independent test design:** review and refine the linked matrix before
   implementation. Add missing boundary/security cases and distinguish reused
   lifecycle coverage from new HTTP/UI coverage.
3. **Human scope review:** settle any findings and approve this bounded increment
   before implementation. Record the approved spec revision/input hash.
4. **Developer:** implement only the approved spec; write tests first; record
   commands, results, deviations, and reused MP2 paths. No production credentials,
   schema activation, or changes to Student/Admin behavior.
5. **Independent reviewer:** compare the patch and evidence to each requirement,
   reproduce affected checks, report findings, and verify fixes. Human acceptance
   follows the review record.

## Implementation sequence

### B1: Read contracts and tests

- Confirm the current Tutor API activation and trusted actor conventions from the
  merged source; use the clean merged `main` checkout as the implementation base.
- Add focused repository/service tests for owner scoping, filters, stable order,
  empty results, DTO fields, and Singapore date boundaries.
- Add controller/MockMvc tests for the three routes, validation, safe errors,
  ownership denial, and session/CSRF behavior using existing test fixtures.
- Prove malformed/spoofed completion requests and denied session/role/CSRF
  requests leave the persisted booking and slot unchanged.
- Add PostgreSQL-backed request tests using two tutors and synthetic student data.
- Add React component tests for confirmation and independent filters.

### B2: Implement booking and history reads

- Add Tutor-owned booking and slot-history read methods with ownership in SQL.
- Map results to the shared Booking and Slot response shapes; avoid sensitive
  student fields.
- Add `GET /api/tutor/bookings` and `GET /api/tutor/history/slots` using existing
  query parsing and API error conventions.

### B3: Expose completion

- Add `POST /api/tutor/bookings/{id}/complete` with the existing `{}` body rule.
- Derive actor from trusted request context and call the shared
  `ConsultationLifecycle.completeActiveBooking`; do not duplicate lifecycle SQL.
- Verify persisted booking and slot statuses through PostgreSQL request tests.

### B4: Tutor view

- Add booking filters for module, Singapore date, and status.
- Add a separate slot-history view with required CANCELLED/COMPLETED status and
  optional date; changing it must not alter booking or note selection.
- Require an explicit confirmation action before posting completion; render
  loading, empty, safe error, and refreshed completed states.

### B5: Independent review and regression

- Reviewer verifies requirement coverage, owner scoping in SQL, actor source,
  status transitions, safe DTOs/errors, and filter separation.
- Run focused unit/React tests, PostgreSQL request integration tests, then the
  complete backend CI command and frontend checks once after fixes.
- Preserve concise test reports and a sanitized developer/reviewer handoff. State
  clearly that session fixtures are not real authentication integration.

## Reuse and limits

Reuse `ConsultationLifecycle.completeActiveBooking`, the core completion/locking
and concurrency tests, Tutor API security/error conventions, shared response
contracts, and MP2 booking-view/complete-booking query behavior where compatible.
Record actual source paths and counts after inspection; do not label candidate code
as reused until it is copied/adapted and tested.

No production migration is expected. Start implementation from a clean
checkout/worktree based on the stated baseline. This plan makes no commit, push,
deployment, or implementation change.

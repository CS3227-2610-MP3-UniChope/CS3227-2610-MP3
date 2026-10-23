# Tutor Private Notes Increment Plan

Status: Ready for implementation planning; implementation has not started.
Date: 22 October 2026
Specification: [NOTE-INCREMENT-SPEC.md](NOTE-INCREMENT-SPEC.md)

## Sequence

1. Verify and integrate the reviewed core note-lock seam source revision before
   Tutor implementation. Confirm the local source matches
   `workflow/core/NOTE-LOCK-SEAM-SPEC.md` and its tests.
2. Have an independent test designer reconcile the existing matrix with the
   resolved note rules, then refresh the frozen input manifest.
3. Implement test-first in the Tutor role: private-note migration/repository,
   service authorization and version checks, then GET/PUT endpoints and tests.
   Keep migrations production-inactive until the shared migration owner agrees.
4. Run unit/request tests and real PostgreSQL integration/concurrency tests; retain
   red-first, fresh green, rollback and persistence evidence.
5. Have a separate reviewer reproduce the tests against frozen inputs and compare
   each test with the independent matrix. Fix and re-review findings.
6. Present the concrete patch and evidence for human implementation acceptance.
   Do not record acceptance before the human responds.

## Proposed source boundary

- New Tutor note model/DTO, service, repository interface and PostgreSQL adapter.
- Tutor HTTP controller/service adapter support for the two existing note routes.
- Additive Tutor-owned note schema script under `db/tutor-review/`, isolated
  integration fixtures, and tests. Do not add or activate a production Flyway
  version in this increment. Shared core lifecycle/domain behavior should not
  change unless review finds a concrete missing lock/transaction contract; any such
  change needs coordination.
- Only minimal backend wiring needed for the endpoint. No frontend, prompt, provider,
  quota, auth implementation, admin/student behavior, deploy/CI or broad refactor.

## Dependencies and stop conditions

- Build on the reviewed core user/module/slot/booking and auth storage contracts;
  verify the exact source revision and compatibility before coding because PR1/PR2
   are not assumed merged.
- The reviewed core seam is a no-callback, core-owned operation. Call it within the
  same READ_COMMITTED transaction and DataSource resource, before any note-row
  access; do not introduce a callback or make core depend on Tutor note types.
- Use the existing trusted actor seam and test-only authenticated identities. Real
  session adapter, CSRF lifecycle, and production account integration remain with
  the auth owner.
- Coordinate production migration numbering/path with the shared migration owners
  before adding a production migration. This does not block isolated disposable
  schema tests using the Tutor review-only script.
- Stop and return to scope review if a note write requires changing the shared
  lifecycle contract, granting another role access, changing the note visibility
  policy, or adding AI generation.

Human scope review: pending. Implementation acceptance: pending.

Post-draft architecture findings are resolved for local note design; see the
[architecture check handoff](../handoffs/2026-10-21-review-tutor-notes-design.md)
and the decision record in `NOTE-INCREMENT-SPEC.md` §4. The production migration
path/version and integration of the unmerged core seam remain coordination gates.

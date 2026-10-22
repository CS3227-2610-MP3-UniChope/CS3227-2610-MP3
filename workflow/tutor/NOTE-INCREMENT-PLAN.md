# Tutor Private Notes Increment Plan

Status: Implementation, local PostgreSQL verification, and independent review complete; human acceptance pending.
Date: 22 October 2026
Specification: [NOTE-INCREMENT-SPEC.md](NOTE-INCREMENT-SPEC.md)

## Sequence

1. [x] Integrate the reviewed lock seam into the feature worktree and compile it
   with the Tutor note implementation.
2. [x] Implement Tutor-owned note persistence, validation, authorization, version
   checks, GET/PUT endpoints, and isolated review-only DDL.
3. [x] Run the normal unit suite and build the application JAR.
4. [x] Run the PostgreSQL request, locking, rollback, concurrency, and schema
   checks against a disposable database; retain the reports and exact command.
5. [x] Complete a separate source/evidence review against the frozen input and
   compare implementation/test coverage with the matrix. Review findings were
   fixed and re-reviewed; the reviewer verified the fresh PostgreSQL reports.
6. [ ] Present the concrete patch and evidence for human implementation acceptance.
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

Human scope review: pending. Implementation acceptance: pending. No commit or
push has been made. The production migration remains inactive pending coordination.

Post-draft architecture findings are resolved for local note design; see the
[architecture check handoff](../handoffs/2026-10-21-review-tutor-notes-design.md)
and the decision record in `NOTE-INCREMENT-SPEC.md` §4. The production migration
path/version and integration of the unmerged core seam remain coordination gates.

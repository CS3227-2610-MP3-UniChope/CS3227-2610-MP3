# Core Migration Review Preparation

Task: CORE-REVIEW-2026-10-20. Date: 20 October 2026 (SGT).
Architect: `/root/core_architect_review`; recorder: parent `/root`.
Human review/acceptance: pending. User explicitly requested review before implementation.

## Inputs and Scope

MP3 HEAD: `50c6213abdde9ee5063c7f333e448b33ac83d59b` plus dirty/untracked files in
[input manifest](../core/review-inputs-2026-10-20.json). MP2 reference revision:
`d6a9a244f960854b2addb1546b45e506217f4823`.

The architect read custom architect/common instructions, shared/integration/Tutor
specs, backend build, current Tutor read types/service/tests, and MP2 models,
repository contracts, SQLite schema/lifecycle and relevant tests. Its dispatch
permitted read-only inspection and a returned report only. It reported no edits,
builds, installs, migrations, database/network access or credential inspection.

Inherited controls: workspace-write, restricted network, auto-review escalation.
Read-only behavior was an instruction boundary, not an enforced read-only sandbox.
The parent wrote only this handoff, core review documents/manifest, and a sanitized
summary. No source/build/config file was modified and no tests were run for this task.

## Architect Recommendations Incorporated

1. Use two increments: shared domain plus PostgreSQL schema/reads first, lifecycle
   writes and concurrency tests second. Do not reopen the agreed architecture.
2. Reconcile temporary Tutor-local role/status/slot models with shared domain;
   keep joined module code in Tutor projections and platform independent of Tutor.
3. Use MP3 table `slots`; MP2 `consultation_slots` is a source mapping, not a required
   compatibility table. Use fresh synthetic data, not a live SQLite import.
4. Add actual foreign keys and valid related test fixtures. MP2 enables SQLite FK
   checking but does not declare those foreign keys in its core CREATE TABLE statements.
5. Expose core reads; keep raw slot/booking writes internal to guarded lifecycle
   operations. Exclude MP2's note/auth repository bundle and generic exclusivity API.
6. Propose retaining nullable password_hash on users for auth; credential logic and
   access remain auth-owned, never ordinary core/Tutor response data.
7. Adding JDBC/Flyway must deliberately adapt startup tests/configuration; do not
   break existing tests by requiring an accidental production/default datasource.
8. Verify against real PostgreSQL/Testcontainers and normal Gradle reports. Parent
   PATH checks found no docker/psql executable; this is not proof of installation
   absence. Database execution readiness remains unverified.

The parent selected nested model/repository/postgres packages under platform/core
for the written mapping; the architect's suggested flat core paths were illustrative.
This package organization does not change component ownership or dependencies.

## Deliverables and Next Step

- [Technical proposal and acceptance checks](../core/TECHNICAL-SPEC.md).
- [Pending implementation tasks](../core/PLAN.md).

The user reviews those artifacts first, especially increment boundaries, credential
storage, and database execution prerequisites. No implementation is claimed. A
developer dispatch and independent implementation review occur only afterward.

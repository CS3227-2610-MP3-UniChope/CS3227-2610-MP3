# Tutor REST Increment Plan

Status: Implemented and independently verified; REST-R1 closed. Human implementation acceptance pending.
Date: 21 October 2026
Specification: [Tutor REST increment](REST-INCREMENT-SPEC.md).
Inputs: [preparation manifest](rest-preparation-inputs-2026-10-21.json).

## R1: Independent test design and review

- [x] Separate test designer derived the [22-case matrix](REST-TEST-MATRIX.md)
  for REST-01 through REST-09 before implementation. The omitted shared body-size
  policy was clarified in the draft; the designer did not execute tests.
- [x] Scope reviewed and approved on 21 October 2026: missing-account behavior,
  strict request rules, minimal activation wiring and auth-owned adapter boundary.
- [x] Implementation approval recorded on 21 October 2026 for this REST increment;
  inputs captured in `rest-developer-inputs-2026-10-21.json`. No commit/push or final
  implementation acceptance is included.

## R2: Test-first HTTP and security adapter

- [x] Inspect pinned Boot dependencies/auto-configuration before selecting MVC,
  Security and test modules. Write failing request/validation/security tests first.
- [x] Add focused controller/DTOs, minimal Tutor access/detail/mutation adapters and
  safe error handling; preserve Optional<UUID> CurrentActorProvider.
- [x] Implement strict fields/queries/types and bounded 64 KiB body reads with 413
  errors, safe status/error mappings, no-store,
  server request IDs and Location resource behavior.
- [x] Supply identity only through src/test security/session fixtures; exercise a
  real Security filter chain and CSRF. No fake actor or login API in main sources.
- [x] Add minimal explicit Tutor API activation wiring; default stays closed,
  activated missing dependencies fail closed. No credential/default Flyway activation.

## R3: Real PostgreSQL request integration

- [x] Select isolated synthetic PostgreSQL runtime explicitly; previous server is
  stopped. No production resources/global installs are implied.
- [x] Reuse review-only DDL and existing PostgreSQL test fixtures/services/lifecycle.
  Add request-to-DB module/list/detail and create/cancel tests with two tutors,
  student/admin and inactive/deleted session identities where relevant.
- [x] Verify error and security denials leave data unchanged. Do not equate mocked
  controllers with database integration or fixture identities with real login.
- [x] Run normal test/postgresTest/bootJar regressions once, preserving XML/HTML and
  exact command/input records. Existing SQL concurrency tests need no duplication.

## R4: Independent review and acceptance

- [x] Separate reviewer verifies contract/security/wiring boundaries and reproduces
  affected tests. Record numbered findings, fixes and independent re-review.
- [x] Preserve unrelated source; record actual reuse, sanitized handoffs and limits.
- Human implementation acceptance: pending. No commit/push/deployment.

## Proposed file boundary and later dependencies

Tutor controller/DTO/access/service/repository adapters and matching tests; minimal
platform/web safe error support; minimal API/security activation configuration;
backend dependencies and tests needed for this slice; scoped specs/evidence.
No lifecycle rule redesign, Student/Admin features, auth login/session implementation,
AI, notes, React or production migration/credential changes. Any unexpected cross-role
contract change requires explicit coordination before expanding scope.

Later auth integration supplies real session CurrentActorProvider and filters;
database wiring waits for the reviewed production schema. Those are production
activation blockers, not blockers for synthetic authenticated MockMvc/PG tests.

## Current evidence

- [Developer handoff](../handoffs/2026-10-21-developer-tutor-rest.md).
- [Independent review](../handoffs/2026-10-21-review-tutor-rest.md).
- [Case coverage](evidence/rest-coverage.md).
- [Latest complete developer run](evidence/rest-r1-advice-green.log): 20 unit + 57
  PostgreSQL tests, zero failures/errors/skips; bootJar successful.

REST-R1 identified incomplete uniform JSON errors for framework failures and
controller errors with incompatible Accept headers. Six added red-first regressions
now pass, including positive activated Boot wiring, authorization/session invalidation
and committed-mutation response failure. Independent Boot probes pass after the
fix. Independent final reproduction passed 20 unit + 57 PostgreSQL tests with
zero failures/errors/skips, plus bootJar; REST-R1 is closed. The synthetic server
was stopped after verification. Human acceptance remains pending.

Final independent [counts and XML](evidence/rest-review-r1/summary.json).

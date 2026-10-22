# Core private-note lock seam plan

Status: Implemented, independently reviewed, and human-accepted on 22 October 2026 for this increment. No commit or push was authorized.

1. Use the authorized no-callback contract in [NOTE-LOCK-SEAM-SPEC.md](NOTE-LOCK-SEAM-SPEC.md): same `DataSource` transaction resource, caller-owned READ_COMMITTED transaction, and core lock/recheck operation.
2. Independent test designer specified transaction joining, lock order, post-lock state checks, rollback, contention, and the limits of core-only note-row coverage.
3. Implemented one operation-specific core interface/result and PostgreSQL adapter method; Tutor note SQL/schema remain out of scope. Existing lifecycle mutations remain unchanged; no generic callback was added.
4. Focused PostgreSQL tests, full backend unit/PostgreSQL suites, and `bootJar` passed. Independent review closed its one Important finding after a RED→GREEN fix and reproduced the full verification. Tutor integration remains separate until its own decisions, schema ownership, and GET authorization are settled.

Expected core scope: one interface/result under `platform/core`, one adapter method under `platform/core/postgres`, focused PostgreSQL tests. Run focused PostgreSQL tests and unchanged lifecycle regressions; retain logs and report limitations. No commit/push authorization was supplied.

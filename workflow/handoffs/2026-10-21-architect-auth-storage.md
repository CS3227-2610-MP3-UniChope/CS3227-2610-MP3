# Core/auth storage design handoff

Status: Draft design prepared; implementation approval pending.
Date: 21 October 2026
Agent: /root/core_architect_review, architect.

## Proposal and next action

Prepared [specification](../core/AUTH-STORAGE-SPEC.md) and
[plan](../core/AUTH-STORAGE-PLAN.md). Next: independent CA-01–12 test matrix,
owner review of material decisions, then scoped implementation only if authorized.

Use password_hash/session_generation on the same users row, secret-free ordinary
domain projections, internal redacted credential reads, full atomic account creation
and guarded activation operations. Login confirmation only locks/rechecks observed
role/hash/generation after auth verification outside locks. It does not issue sessions.

## Material review findings

- Auth says current User includes passwordHash, but Java User and repository reads
  do not. Proposed physical-row interpretation and internal snapshot require review.
- Existing core email validation requires a dot; auth permits dotless domains.
  Constructor/read compatibility and Java/PostgreSQL Unicode normalization require
  explicit oracles, not silent narrowing or an unverified equivalence claim.
- PR1 credential-free V1 and no-credential/bare-insert fixtures must remain intact.
  New tests apply base plus separate auth-review extension to an empty isolated
  schema before seeding. Hash is NOT NULL/nonblank with no default. Existing
  credentialless accounts cause atomic migration failure; backfill remains separately
  reviewed work, with no fabricated hash or nullable legacy accommodation.
- Admin full account operation owns blockers/state/generation atomically; no
  REQUIRES_NEW callback that pretends to cover separate outer writes.
- Confirmation can become stale after commit; real auth must check current state
  and generation on protected requests. Storage tests do not prove HTTP revocation.

## Inputs, actions and limits

Primary manifest [auth-storage preparation](../core/auth-storage-preparation-inputs-2026-10-21.json),
supplied HEAD508c5bd769e889dea405cb848ddd60429f24ce3b. Proposed PR1 source in sibling
core-sharing at1c14c34f73b0792a2c46724d30e8aa7d48dd5e36; scoped manifest supplied
at /tmp/mp3-pr1-core-auth-design-inputs.json. PR is open, not claimed merged.
Hashes read, not recomputed; these new documents need a refreshed dispatch manifest.

Inspected with successful cat/sed/rg: common/architect policies; auth product,
technical and plan; Admin technical/product management rules; integration data/lock
sections; shared data rules; preparation manifest headers; PR1 User, UserRepository,
PostgresUserRepository, review DDL and relevant test credential/bare-insert matches.
Core email incompatibility was additionally identified by coordinator inspection;
design requires explicit verification of chosen Validation changes before code.

Only the three assigned Markdown outputs were written. Actual inherited controls:
workspace-write project/temp, restricted network, auto-review approvals; scoped
file boundaries are behavioral, not an OS read-only guarantee. No probes/builds,
DB/network, installs, credentials/secrets, source changes, staging, commit, push,
merge or external messaging. No tests executed or owner acceptance inferred.

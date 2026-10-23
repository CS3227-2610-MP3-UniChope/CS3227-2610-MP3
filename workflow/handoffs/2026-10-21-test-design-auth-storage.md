# Core/auth storage test-design handoff

Status: Draft design complete; owner review and implementation authorization pending.
Date: 21 October 2026. Scope: CA-01–12 core account storage; no tests executed.

## Result and decisions

Prepared [25-case matrix](../core/AUTH-STORAGE-TEST-MATRIX.md) covering isolated
migration compatibility, credential-free projections/redaction, registration and
provisioning, guarded activation/revocation generation, confirmation of observed
role/hash/generation, PostgreSQL races, atomic rollback and timeout recovery.

Owner-blocked oracles remain explicit: physical account versus secret-bearing User
interpretation; dotless/Unicode canonical lookup and DB index equivalence policy.
Separate migration-route and trusted-context approval also remains pending. The
provisioning-role question was resolved by coordinator in draft §7: valid ADMIN
target role ACCESS_DENIED, null/invalid typed input INVALID_INPUT; AS-12 now uses
those exact oracles. This clarification is not owner acceptance or authorization.

Coordinator finding resolved in draft: AS-19 originally proposed a successful
Tutor deactivation before booking an already-future AVAILABLE slot. Re-derivation
from CA-05 shows that slot already blocks deactivation; after booking, ACTIVE
booking blocks it instead. AS-19 now requires INVALID_STATE for deactivation in
either order, unchanged Tutor active/generation and an allowed eventual valid
booking. Its separate clock-advance fixture has no concurrent/ACTIVE booking and
can deactivate after the slot reaches start. No Admin blocker is weakened to force
the impossible race. AS-25 excludes Admin HTTP/role-service implementation while
retaining scoped core guarded account operations.

Next: resolve owner choices, review scope, then test-first developer and independent
reviewer. Core login confirmation assumes password verification already completed
outside locks; it issues no session and can become stale after commit. This matrix
makes no real auth/password/session, migration activation or security-pass claim.

## Inputs and technical evidence

Task: CORE-AUTH-STORAGE-TEST-DESIGN-01. Actual agent:
`/root/lifecycle_test_designer`, independent test designer.
[Input manifest](../core/auth-storage-test-design-inputs-2026-10-21.json) supplies
HEAD `508c5bd769e889dea405cb848ddd60429f24ce3b` plus dirty/untracked input hashes;
read, not recomputed. Proposed PR1 `1c14c34f73b0792a2c46724d30e8aa7d48dd5e36` remains
open/not merged; its compatibility facts are specification inputs, not independently
inspected/run source evidence here. Hashes cannot restore a lost content snapshot.
[Supplemental manifest](../core/auth-storage-test-design-supplemental-inputs-2026-10-21.json)
identifies the revised draft after role-code reconciliation; it supersedes that
specification's original hash.

Inspected requirement-first using scoped cat commands (both exit 0):
1. Common/test-designer instructions; AUTH-STORAGE-SPEC/PLAN; architect-auth-storage
   handoff and test-design manifest.
2. Auth product/technical specs and Admin technical specification.
3. Scoped `rg` of revised role-code/CA-10/context clauses and `cat` supplemental
   manifest; exit 0. Incorporated the inspected clarification in both outputs.

No implementation, Gradle or DB fixture inspection was necessary to derive cases.
Only the assigned matrix and this handoff were written via scoped shell heredoc.
Scoped document patches updated the clarified role oracle. Failure-injection design
uses a test-only deferred trigger after a row write/before successful commit; it
does not require artificial split writes or product failure hooks.
Further scoped patches corrected AS-19 after coordinator review and clarified
AS-25 boundaries. No additional source inspection or runtime execution was needed.
No tests, builds, DB/network/version probes, installs, secrets, real hashes/passwords,
source/spec edits, staging, commits, pushes or external messages. Synthetic hash
sentinels are proposed fixtures only; none were generated or processed by a hasher.
There are no executed-test reports from this run.

Inherited runtime: workspace-write project/visualization/temp roots, restricted
network, auto-review approvals; no escalation. Two-file/no-source boundaries are
behavioral instructions, not OS-enforced read-only isolation. No cache/build/temp
outputs authorized or produced. Existing product/test/SQL files preserved.

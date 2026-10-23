# Core/auth storage implementation dispatch

Date: 21 October 2026. Implementation and focused PR authorized as a proposal;
teammate agreement, merge, production activation and final acceptance remain pending.

Branch: feat/core-auth-storage in sibling core-auth-storage checkout. Base PR1
1c14c34 remains open; target PR1 branch while unmerged, without changing that PR.
Original main's dirty application/workflow files are preserved. Only source, tests,
review-only SQL extension and necessary backend build/README changes will be shared.
Workflow/handoff/evidence files remain local.

Developer /root/auth_storage_developer reads custom common/developer policy and
AUTH-STORAGE-SPEC/PLAN/25-case matrix; implement test-first, retain failure/pass
records, then freeze for separate independent review and affected re-review.
Coordinator handles commits/push/PR only after checks/review; no merge authorized.
Proposed physical-row/safe-snapshot, auth email canonicalization, unchanged stricter
DB uniqueness, trusted actor generation and isolated empty-schema route are recorded
in spec section 1. These assumptions await teammate review through the PR.

Inputs: auth-storage-implementation-inputs-2026-10-21.json plus
/tmp/mp3-auth-storage-code-baseline.json. Developer captures content hashes including
new Java credential snapshot source explicitly; filename exclusions must not omit
code while claiming complete coverage. Only synthetic fixture hashes are used.

Controls: inherited workspace-write/project/temp, restricted network; build outputs
in new checkout, cache /tmp/mp3-gradle-home, evidence /tmp/mp3-auth-storage-evidence.
Existing disposable PostgreSQL 17.6 localhost55439/postgres, core_test/empty password;
reviewed local socket escalation permitted. Independent schema per test, explicit
Flyway locations only. No real accounts/secrets/SoC, global installs, auth HTTP/hash
implementation, deployment or external messages. Root's GitHub network access is
limited to requested branch/PR actions; teammate specs are not edited or accepted.

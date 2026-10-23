# Independent Review — Shared AI Admission

Date: 22 October 2026  
Review scope: Current uncommitted AI admission increment on
`feat/tutor-ai-note-draft`. The reviewer inspected implementation, isolated
schema, tests, proposal, matrix, and evidence. Review was read-only.

## Initial findings

### ADM-R1 · P2 · Active lease could be pruned before expiry

`PostgresAiAdmissionService.admitLocked` deleted old attempts based only on
`admitted_at`. Both `AiTaskPolicy` and `AiProviderBudget` allow a rolling window
shorter than the request deadline. In that valid configuration, a later
admission could prune a row whose `in_flight_until` was still in the future,
removing actor and provider-key concurrency accounting and admitting duplicate
work.

Reviewer reproduction: static trace against the policy bounds and pruning SQL.
The existing eight PostgreSQL tests passed before the finding was fixed; none
covered a lease longer than its rolling window.

## Fix and verification

The deletion now preserves attempts with an active lease. Added
`pruningKeepsAnActiveLeaseWhenItsWindowHasExpired`, using a one-second window and
a twenty-second lease, and linked it to ADM-03a in the test matrix.

Verification after the fix:

- Focused PostgreSQL admission suite: 9 passed, 0 failures/errors/skips.
- Full `clean test postgresTest bootJar`: unit 76 passed across 21 suites;
  PostgreSQL 187 passed across 18 suites; zero failures/errors/skips; `bootJar`
  succeeded.
- `git diff --check` passed.

Independent re-review confirmed ADM-R1 is resolved: pruning preserves unexpired
leases, and the regression test shows a second actor remains rejected by the
provider-key in-flight limit after the rolling window expires. No remaining
findings were reported in the changed admission code and evidence. Evidence counts
and hashes were refreshed in `workflow/ai/EVIDENCE.md`.

## Remaining gates

No provider, runtime bean, active migration, real SoC call, or production budget
is part of this review. Student/Admin/Auth/Core owner review and human acceptance
remain separate gates. The review verifies this narrow admission increment; it does
not establish production wiring or app-wide AI security.

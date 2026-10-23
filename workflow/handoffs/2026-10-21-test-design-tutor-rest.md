# Tutor REST test-design handoff

Status: Draft preparation complete; implementation approval pending. No tests ran.
Date: 21 October 2026. Scope: five Tutor slot/module REST endpoints.

## Result and next action

Created [22-case independent test matrix](../tutor/REST-TEST-MATRIX.md) for REST-01–09.
It covers response/data shapes, SGT boundaries, current DB actor/role/active state,
real Security/CSRF fixture behavior, old-session invalidation, owner non-disclosure,
strict JSON/query/media input, writes/Location behavior, safe error/cache policy,
activation fail-closed checks, request-to-PostgreSQL integration and retained regressions.

**Q1 resolved in draft:** the coordinator carried shared Integration §5's body policy
into REST §2 and the plan: 65,536 actual UTF-8 bytes including whitespace, above-limit
413/no-store/no mutation, equal-limit normal validation; no declared-length bypass.
I inspected the revision and incorporated it into RT-14. Size tests use valid actor
and CSRF, since earlier security denial may precede size checking. Human scope
review remains pending; the clarification is not implementation approval.

Coordinator obtains scope review and captures updated
inputs; developer implements with actual tests, then independent reviewer verifies.
Simulated session identities are not real auth/login evidence. Production activation,
credential storage, real session adapter, React and AI remain outside this increment.
No approval, teammate agreement, runtime compatibility or security pass is inferred.

## Technical evidence and controls

Task: TUTOR-REST-TEST-DESIGN-01. Role/actual agent:
`/root/lifecycle_test_designer`, independent test designer. HEAD supplied:
`50c6213abdde9ee5063c7f333e448b33ac83d59b`.
[Scoped input manifest](../tutor/rest-test-design-inputs-2026-10-21.json) covers dirty
and untracked inputs; hashes identify contents, not a recoverable snapshot. I read
its contents, did not recompute hashes, and did not inspect a REST implementation.
[Supplemental manifest](../tutor/rest-test-design-supplemental-inputs-2026-10-21.json)
identifies the revised REST spec/plan after Q1; it supersedes their original hashes.

Inspected requirements/architect inputs before considering test mechanics:
- `workflow/agents/common.md`, `test-designer.md`.
- `workflow/tutor/REST-INCREMENT-SPEC.md`, `REST-INCREMENT-PLAN.md`.
- `workflow/handoffs/2026-10-21-architect-tutor-rest.md` and input manifest.
- Scoped integration/shared excerpts for JSON/resource/error/body/auth/CSRF rules.
- `CurrentActorProvider.java` only to confirm the existing Optional UUID seam.

Actual commands: one scoped `cat` of first listed inputs/manifest, then scoped
`rg -n -A 7 -B 4` over integration/shared spec and `cat` of CurrentActorProvider;
both exited 0. Wrote only assigned matrix and this handoff via shell heredoc.
Then inspected revised body-limit clauses with scoped `rg` and read the supplemental
manifest with `cat` (exit 0); applied scoped document patches for the resolved oracle.
No builds/tests, DB/network probes, installs, runtime versions, secrets, SoC,
production resources, commits, pushes or deployment. No test reports generated.
No product/test source, shared spec or agent instruction edits.

Inherited runtime: workspace-write with project/visualization/temp writable roots,
restricted network and auto-review approvals; no escalation requested/granted.
No-source-edit and two-document scope limits are behavioral, not enforced read-only
isolation. No build/cache/temp outputs authorized or created by this task. Existing
files preserved; the two output documents are the only writes. Prior 50 regression
count was supplied as retained scope, not independently rerun/verified here.

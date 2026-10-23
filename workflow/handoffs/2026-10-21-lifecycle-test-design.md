# Lifecycle Independent Test-Design Handoff

Date: 21 October 2026 (SGT). Task: CORE-LIFECYCLE-TEST-DESIGN-01.
Role/actual identity: independent test designer `/root/lifecycle_test_designer`.
Coordinator: `/root`. Human lifecycle implementation approval: **pending**.

## Inputs and controls

HEAD provided in dispatch/manifest:
`50c6213abdde9ee5063c7f333e448b33ac83d59b`.
[Input manifest](../core/lifecycle-test-design-inputs-2026-10-21.json) hashes scoped
working copies, including untracked inputs. It identifies contents, not a recoverable
snapshot. I did not independently run Git/hash commands or inspect implementation.
Additional inspected inputs: matching requirement excerpts in
`workflow/SHARED-TECHNICAL-SPEC.md` and `workflow/PRODUCT-SPEC.md`, plus the updated
lifecycle spec. The coordinator supplied their [supplemental input manifest](../core/lifecycle-test-design-supplemental-inputs-2026-10-21.json), including
updated lifecycle-spec hash `4445f61ce3d24d12d5e4c69fa2a97ccddee56e65f60bea6b1bc32f8ad2a0d3b4`.
This supersedes the original spec hash for the revised matrix. Existing build configuration was inspected only
after deriving requirements/scenarios, to identify `test` / `postgresTest` conventions.

Actual inherited controls: workspace-write filesystem sandbox; writable project,
visualization and temporary roots as supplied by runtime; restricted network;
auto-review approval policy. No escalation requested/granted. Test-designer-only
file scope and no-execution boundaries are behavioral instructions, not an enforced
read-only filesystem. No cache, generated build, database or temporary output was
requested or created by this task.

Allowed/actual writes only:
- `workflow/core/LIFECYCLE-TEST-MATRIX.md`
- This handoff.

## Work and findings

Derived a 28-case matrix for LC-01–12 plus relevant SEC-01/REL-01, SH-DATA and Tutor
criteria. Cases specify fixtures, action/interleaving, outcome and persisted-state
oracles, verification layer and prerequisites. Includes owner non-disclosure, strict
time boundaries, post-lock clock/eligibility rechecks, independent physical-connection
races, test-only second-write failures, timeout recovery, and retained regressions.

Two questions were sent to the coordinator and resolved in the draft spec:
1. Null IDs => INVALID_INPUT; null intervals => INVALID_TIME.
2. Unavailable booking tutor/module/assignment => generic MODULE_UNAVAILABLE;
   actor ACCESS_DENIED, slot/time/ACTIVE conflicts INVALID_STATE and foreign
   owned-resource NOT_FOUND remain separate.

I inspected the revised spec and incorporated those explicit oracles. They remain
coordinator draft clarifications awaiting human scope review, not implementation
authorization. No ambiguous oracle was silently selected. No product/test source,
spec or schema was edited by this designer. No lifecycle test,
adversarial-agent evaluation or application-security result was executed or claimed.

## Inspected commands and results

Read-only `exec_command` calls, all exited 0:
1. `cat workflow/agents/test-designer.md workflow/agents/common.md workflow/core/lifecycle-test-design-inputs-2026-10-21.json`
2. `cat workflow/core/LIFECYCLE-SPEC.md workflow/core/LIFECYCLE-PLAN.md workflow/handoffs/2026-10-21-lifecycle-review-preparation.md`
3. `cat workflow/INTEGRATION-SPEC.md workflow/tutor/PRODUCT-SPEC.md src/backend/build.gradle`
4. Scoped `rg -n -A 8 -B 3` for `REL-01|SEC-01|SH-DATA-03|READ COMMITTED|transaction|lock`
   in shared technical/product specs.
5. `cat workflow/core/LIFECYCLE-SPEC.md workflow/core/lifecycle-test-design-supplemental-inputs-2026-10-21.json`
   to inspect the coordinator's resolutions and supplemental manifest; exit 0.

Documents were written by a scoped shell heredoc; no scripts/hooks were executed.
The write exited 0. A final scoped `rg -n` of headings, TD case IDs, question
markers and pending-status text in these two outputs exited 0 (display truncated);
this checked document contents only, not application behavior.
No builds, installs, DB/network probes, SoC calls, production/secret reads, commits,
pushes or deployments. There are no test-report locations for this design run.

## Next role and acceptance

Coordinator retains the resolved draft and captures inputs for the next role. Human reviews
scope/material choices, then developer maps matrix IDs to executable tests and
records initial failures; independent reviewer checks implementation, matrix
coverage/deviations and real reports. Credential storage and production schema
activation remain outside scope. Shared AI remains Student-owned.

This matrix is planning evidence only. Runtime/database readiness was not probed.
No human acceptance or team agreement is inferred from this handoff.

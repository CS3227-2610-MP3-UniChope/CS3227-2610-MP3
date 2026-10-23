# Four-role workflow refinement — 21 October 2026

The human requested a better specialized-agent workflow and authorized the update.
The coordinator added an independent test-designer role between architect and
developer, with requirement-linked cases, explicit oracles and ambiguity reporting.
Developer retains executable tests; reviewer independently checks behavior and
coverage. Workflow evaluation remains a separate periodic task, rather than a
mandatory fifth agent for every increment.

An actual test-designer session produced 28 lifecycle cases covering LC-01–LC-12.
It raised two ambiguities before implementation: null interval versus null ID errors,
and failure codes for unavailable booking-related tutor/module/assignment context.
The coordinator clarified these in the draft lifecycle spec and retained initial
and supplementary input manifests. These choices still await human scope review.

Evidence: workflow/core/LIFECYCLE-TEST-MATRIX.md and
workflow/handoffs/2026-10-21-lifecycle-test-design.md. Agent role files, current plan,
shared technical specification and local Developer Guide describe the revised flow.

This run designs checks; it does not execute lifecycle tests or establish application
security. The archived 20 October adversarial evaluation does not validate this new
role/revised policy. No product implementation, credential decision, production
migration activation, commit or push is authorized by this workflow update.

Independent document review: `/root/core_increment_reviewer` inspected the revised
role instructions, lifecycle spec/plan/matrix and test-design handoff and reported
no consistency blockers. This was document inspection only, with no builds,
database checks, implementation review or adversarial-agent security evaluation.
Coordinator verified 24 scoped source/build input hashes were unchanged, local
links in agent/lifecycle documents resolved, 28 distinct TD case IDs existed, and
`git diff --check` exited 0. These are preservation/document checks, not app tests.

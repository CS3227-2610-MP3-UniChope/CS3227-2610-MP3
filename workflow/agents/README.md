# Specialized development agents

Use our own scoped workflow: [architect](architect.md) →
[test designer](test-designer.md) → human scope review → [developer](developer.md)
→ [independent reviewer](reviewer.md) → human implementation acceptance.
All four follow [common.md](common.md).
No persona, menu or existing SDD toolkit is needed.

Architect owns the scoped specification/design proposal. Test designer derives an
independent requirement-linked matrix before implementation. Developer owns code
and executable tests; reviewer independently checks both coverage and behavior.
The main chat coordinates tasks and resolves questions with the human. Do not add
an agent for every small concern or require four fresh runs for a trivial text fix.

Before dispatch, capture the relevant dirty/untracked inputs using
`python3 workflow/agents/capture_inputs.py --root . --output /tmp/task-inputs.json
workflow/agents workflow/tutor workflow/AI-CONTRACT.md src/backend/src`.
Add relevant contracts/build scripts to that explicit scope. Inspect the selected
paths first; do not capture secrets. Copy sanitized contents/patches when reproducible
input recovery is needed; hashes alone cannot restore an untracked file.

Each dispatch names task ID, input manifest/snapshot, requirements, role and common
instructions, permitted changes/commands, actual inherited sandbox/network/approval
controls, expected outputs and allowed caches/temp paths. For findings, developer
fix → independent re-review → human acceptance; see common.md for required evidence.
Instructions saying "read-only" do not create a read-only sandbox. Record actual
restrictions and instruction-only boundaries separately. Human acceptance stays
pending until supplied by the human, never inferred from a handoff.

Actual handoffs: `../handoffs/`. Evaluation protocol/runs: `../evaluations/`.
Sanitized human-verifiable interaction summaries: `../../logs/`.

## Periodic Workflow Evaluation

Workflow evaluation is a separate task/run, not a fifth mandatory agent in every
increment. Evaluate when instructions/permissions change, an observed failure
suggests a weakness, or a significant new capability is enabled. Use a different
reviewer from the agent/run being assessed and retain cases, traces and limitations
under workflow/evaluations/. Never certify an agent solely from its own report.

The existing 20 October evaluation covers its archived three-role instructions.
It does not automatically validate this four-role revision or the new test designer.
Future cases should also check whether a designer rejects malicious oracle changes,
surfaces ambiguous specs, and covers omitted authorization/concurrency failures.
Using the designer on a real increment provides workflow evidence, not an adversarial
security pass. Do not rewrite previous reports to claim the new policy was evaluated.

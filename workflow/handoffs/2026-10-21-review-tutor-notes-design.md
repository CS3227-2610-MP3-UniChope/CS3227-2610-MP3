# Post-draft architecture check: Tutor private notes

Status: Design not ready for implementation; owner decisions and shared contract work remain.
Date: 21 October 2026
Task: TUTOR-NOTES-DESIGN-01 architecture check
Agent: `/root/tutor_notes_architecture_check` (read-only post-draft check)

## Findings

1. **Blocker — callable core transaction/lock seam is undefined.** The note spec
   requires the shared row-lock order, while the lifecycle spec keeps its PostgreSQL
   lock helpers internal and says note integration needs its own scoped contract.
   The note service cannot meet that requirement without an unreviewed shared
   mechanism. Define a narrow core-owned transaction/locking seam before coding;
   do not add an unrestricted callback or make core depend on Tutor note types.
2. **Decision — note length and trimming.** Confirm the proposed 8,000 code-point
   limit and precise Unicode trim behavior (Q1).
3. **Decision — inactive Tutor during a lock wait.** Confirm recheck/reject behavior
   and the safe 403 `ACCOUNT_INACTIVE` result (Q3).
4. **Coordination — migration location/version.** Reserve the additive Tutor schema
   migration with the shared migration owner before wiring it (Q2). Production
   activation can remain separately gated.

The designer's 23-case matrix generally follows the requirements. Tightened TN-16
to use `ACCOUNT_INACTIVE` and clarified that isolated migration verification and
production activation are separate. Synthetic identity tests are correctly bounded
as non-evidence of the real session adapter.

Recommendation: **not ready to implement** until Q1, Q3 and Q4 are resolved and Q2
is coordinated. This is explicitly a post-draft check; it does not replace an
initial architect-produced specification. No human or teammate approval is claimed.

## Inputs and controls

The agent inspected the draft note spec, plan and test matrix plus relevant Tutor,
Integration, shared technical and core lifecycle contracts. The original design
dispatch used [notes-design-inputs-2026-10-21.json](../tutor/notes-design-inputs-2026-10-21.json)
and its supplemental revision. Coordinator edits after the architecture check are
identified by [notes-design-final-inputs-2026-10-21.json](../tutor/notes-design-final-inputs-2026-10-21.json),
HEAD `508c5bd769e889dea405cb848ddd60429f24ce3b`. These manifests include dirty/
untracked files by hash; hashes do not preserve their contents. The agent did not edit files or run
builds, tests, databases, network, installs or probes. This was a focused review,
not runtime verification.

Inherited controls: workspace-write project/temp roots, restricted network,
auto-review. The read-only/document scope was an instruction boundary, not an
OS-enforced permission restriction. No code, tests, migrations, commits, pushes,
deployment or external messages occurred.

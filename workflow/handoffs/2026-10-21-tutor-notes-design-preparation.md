# Tutor private-notes design preparation handoff

Status: Draft specification and plan prepared; human scope review pending.
Date: 21 October 2026
Task: TUTOR-NOTES-DESIGN-01
Prepared by: coordinator in the main task; no architect subagent report was received.

## Result

Drafted the [note increment specification](../tutor/NOTE-INCREMENT-SPEC.md) and
[plan](../tutor/NOTE-INCREMENT-PLAN.md) for TUT-NOTE-01 only, synthesizing current
draft contracts. It consolidates the
existing draft API/data contracts: owned COMPLETED bookings, private GET/PUT,
version 0 for an absent note, versioned compare-and-set saves, and PostgreSQL
locking. AI drafting and React are later work. No source, tests, migrations or
other shared specs were changed.

The independent test designer produced a [23-case matrix](../tutor/NOTE-TEST-MATRIX.md).
A post-draft architect check found one additional blocker: the shared lock order is
specified but core exposes no callable lock/transaction seam for Tutor note writes.
Q1 (limit/trim), Q3 (inactive account during save), Q4 (core seam) remain decisions;
Q2 (migration slot) requires owner coordination. See the separate
[architect check](2026-10-21-review-tutor-notes-design.md).

Next: review and resolve those questions before any developer dispatch. No team
decision, PR status or human implementation acceptance is inferred.

## Material questions

- The 8,000 Unicode code-point limit and trim-before-persist behavior are proposals
  in current product/integration drafts; affected-team confirmation is still needed.
- The exact production migration version/activation route must be coordinated with
  core and Student AI owners. Current core/auth PRs are not assumed merged.
- Recheck active tutor and completed booking under transaction locks before writing;
  the real auth session adapter remains auth-owner work.

## Inputs, actions and limits

Input HEAD: `508c5bd769e889dea405cb848ddd60429f24ce3b`.
Scoped manifest: [notes-design-inputs-2026-10-21.json](../tutor/notes-design-inputs-2026-10-21.json);
it records hashes for 68 explicitly selected spec, policy, Tutor/core source and
test files. Hashes do not restore untracked contents. Existing working changes
were preserved. The original design manifest and supplemental manifest identify
the stages of the work. After the architecture check and coordinator edits, the
[final manifest](../tutor/notes-design-final-inputs-2026-10-21.json) records the
current 69-file design state. Hashes identify files but do not archive recoverable
contents.

Inspected the custom agent workflow instructions, relevant Tutor product and
technical requirements, Integration note contract and lock protocol, shared
technical rules, current Tutor REST spec/plan, core lifecycle/auth specs, and
scoped source/test files. No source-level build probes were needed for this design.

The coordinator wrote the spec, plan, and this handoff. A separate test designer
later wrote its assigned matrix and handoff. Runtime controls: project
and temp writable roots, restricted network, auto-review; the document-only scope
was an instruction boundary, not OS-enforced read-only access. No commands that
run builds/tests, databases, network access, installs or version probes; no
credentials, SoC, production resources, commits, pushes or external messages.
No tests were executed. This design makes no security guarantee and does not claim
that the behavior is implemented.

# Tutor private-notes increment reflection

Increment: TUT-NOTE-01 design and test planning  
Date: 21 October 2026

**Intent.** Define the manual note load/save increment before implementation. The
Tutor role owns private notes; only an active tutor may access a note for their own
completed booking. Saving is explicit and versioned. AI draft generation is out of
this increment.

**Decision.** Consolidate the existing product/API/technical proposals into one
scoped specification and ask an independent test designer to derive cases before
coding. Use PostgreSQL tests for transaction, persistence, and concurrency claims.
Keep real session integration and production migration activation with their
respective owners.

**What the design surfaced.** The existing drafts do not settle the precise Unicode
trim rule and the proposed 8,000-code-point limit, the migration version/activation
coordination, or the public behavior if a tutor becomes inactive while a save waits
for a lock. The core lifecycle spec also keeps lock helpers internal and requires
a separate contract for note integration, so Tutor currently has no specified
callable seam to meet the shared lock order. Synthetic request identities cannot
prove real session integration. These remain explicit questions instead of being
hidden in implementation assumptions.

**Evidence.** [Increment specification](../workflow/tutor/NOTE-INCREMENT-SPEC.md),
[plan](../workflow/tutor/NOTE-INCREMENT-PLAN.md),
[23-case test matrix](../workflow/tutor/NOTE-TEST-MATRIX.md),
[design preparation handoff](../workflow/handoffs/2026-10-21-tutor-notes-design-preparation.md),
and [test-design handoff](../workflow/handoffs/2026-10-21-test-design-tutor-notes.md).
The matrix is designed only; no implementation tests or application code were run
or changed in this increment.

**Lesson / limitation.** A written contract is not ready to build from when important
boundary behavior or shared ownership is still uncertain. The next useful step is
to resolve the listed questions, refresh the scoped input record, and then implement
against the matrix. This evidence says nothing yet about whether the behavior works.

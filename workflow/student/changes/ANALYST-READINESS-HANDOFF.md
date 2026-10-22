# Spec analyst handoff: Student booking readiness repair

Recipient: spec analyst, fresh session (or fresh subagent with no inherited conversation).
Status: CHANGES_REQUIRED; tests and implementation have not started.

## Invocation

Act as the spec analyst using `workflow/student/agents/spec-analyst.md`.
Read `AGENTS.md`, `workflow/student/NOTE.md`, `workflow/student/HANDOFF.md`,
and the canonical requirements/contracts directly. Do not read logs, prior chats,
transcripts or narrative summaries. Verify actual artifacts before accepting.

Requested scope: all four backend capabilities:
- `changes/stu-c1-browse-slots.md`: C1-T1..T4, foundation and browsing.
- `changes/stu-c3-my-bookings.md`: owned booking views/projection.
- `changes/stu-c2-book-slot.md`: booking creation.
- `changes/stu-c4-cancel-booking.md`: cancellation.

Paths above are relative to `workflow/student/`. Preserve team ownership and local
changes. Frontend, AI, production activation and teammate approvals are pending;
this request does not authorize altering their contracts.

## Direct human authorization

Phuc, 22 October 2026, current user replies:
- Target question: "all".
- Four-rule decision question: "Approve all four proposed rules".

The four approved rules are recorded in Student PRODUCT-SPEC section 10: any active
student may book any active module; overlapping slots remain visible and are
rejected on booking; cancellation is permitted before slot start with no notice
period; missing and unavailable slots return 409 SLOT_UNAVAILABLE.

## Exact required repair

The fresh test-writer session returned c1's entry gate because the analyst packet
contains acceptance scenarios and task mapping but omits the workflow-required
requirement-to-technical coverage assessment and spec findings/resolutions.
Inspect the c1 receiver receipt for precise evidence. Product decisions are now
resolved; do not ask Phuc to approve those same rules again.

For each change, independently assess the canonical baseline, add the coverage
matrix with requirement/acceptance IDs, product and technical/integration sources,
coverage verdict and planned validation. Record concrete findings, recommended
fixes and their resolutions; if there are no findings, explicitly record the
assessment and supporting evidence. Check shared-owner boundaries and dependencies.
Update canonical contracts only where already authorized. Any material unresolved
contract or ownership decision remains a finding; do not silently waive it.

Produce a numbered analyst handoff superseding stale packet sections and tied to
actual SHA-256 revisions. Give a scoped READY_FOR_TESTS, CHANGES_REQUIRED or BLOCKED
verdict for each capability with dependency conditions. Preserve prior evidence.
Do not write tests or product implementation in this analyst session.

## Verified canonical revisions

Base HEAD: `502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d`.
The existing uncommitted work must be preserved.

| Artifact | SHA-256 |
| --- | --- |
| `workflow/student/PRODUCT-SPEC.md` | `ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957` |
| `workflow/student/TECHNICAL-SPEC.md` | `bf32463e677b057f07810a4ad46dbb24c4cb32041e44ae4a6956e6ae29281d98` |
| `workflow/student/PLAN.md` | `66e77b89e0c235cea3fada90316c0c7580f6ea7ce2816b9e35c5bf6f523e4a68` (revised for D5: role wording only) |
| `workflow/student/NOTE.md` | `5e8ba76fcd3a52a08925ba744dca8e4adda572403b2ac800ec58dbef2b5d62e4` (revised for D5) |
| `workflow/student/HANDOFF.md` | `1ee65fd926dff0a710597508226cf109cb723678b760111e7d6120adfe5b5596` (revised for D5) |
| `src/backend/compose.test.yaml` | `7a7216c023621d861e0ba9169338c3f0a10d98b7ecf24e62751df97e4718e634` |

The change records are uncommitted and include owner-decision addenda and the
receiver's return; hash them afresh when receiving this packet.

## Next stages

After current analyst readiness is recorded, start a fresh test-writer session for
c1, followed by a separate fresh implementer session. Repeat for c3, c2, and c4 in
dependency order; run executable baseline and final checks as prescribed. A fresh
independent review/security session and human verification remain required. No
session has yet performed or claimed the analyst repair or the review stage.

Workflow decision D5 (Phuc, 22 October 2026, current request): one agent tool runs
every role, each in its own fresh session. See the workflow decision addendum in
each change record.

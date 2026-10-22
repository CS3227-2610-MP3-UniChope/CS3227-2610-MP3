# Admin product specification

Status: backend implemented locally, 22 October 2026. Owner: Kok Seng.
Baseline: main 98ca801, fetched before this work. Implementation status is recorded below; release acceptance is pending.
Parents: ADM-01, ADM-AI-01, SEC-01/02/03 in [shared product](../PRODUCT-SPEC.md).
Use [shared technical](../SHARED-TECHNICAL-SPEC.md) and
[integration](../INTEGRATION-SPEC.md); this document extends rather than replaces them.

## Scope

Admin backend manages users, modules, tutor assignments, booking activity and
statistics plus an aggregate-only activity explanation. Frontend ownership is not
assigned here. Core domain/tables/repositories/lifecycle belong to Huanghao; shared
AI infrastructure belongs to Student under current shared allocation. Tutor-local
documents still claim AI ownership: reconcile that discrepancy, do not reassign it.
No private notes, autonomous actions, history deletion or core lifecycle bypass.

## Role requirements

The rules below define the implemented Admin behavior.
All calls require current active ADMIN; mutation eligibility is rechecked under locks.

| ID | Behavior | Acceptance cases |
| --- | --- | --- |
| ADM-USER-01 | List user summaries; provision Student/Tutor only using one atomic user+credential operation. No role changes or Admin provisioning through this API. | U1 valid account persists; duplicate normalized email creates neither partial user nor credential; ADMIN/body actor spoof rejected. |
| ADM-USER-02 | Deactivate/reactivate Student/Tutor; reject Admin targets. Student blocked by any ACTIVE booking; Tutor blocked by affected ACTIVE booking or strictly future AVAILABLE slot. Retain history/assignments. Same-state request is harmless. | U2 overdue ACTIVE bookings still block; eligible account changes; no history erased; concurrent booking/slot creation cannot bypass guards. |
| ADM-MOD-01 | Create/edit module code/name; deactivate instead of delete. Block deactivation for affected ACTIVE bookings/future AVAILABLE slots. No reactivation in this minimal proposal. | M1 duplicate code conflicts; edit retains ID/history; guarded change leaves all state unchanged. |
| ADM-ASG-01 | Assign active Tutor to active module; duplicate is harmless. Remove assignment only without affected ACTIVE bookings/future AVAILABLE slots; absent pair is harmless. | A1 wrong-role/inactive target rejected; only the affected pair blocks removal; assignment races follow parent locks. |
| ADM-BOOK-01 | Read booking activity with optional moduleId,tutorId,date,status filters. Exclude notes/agendas/credentials. | B1 intersect filters, SGT date edge, stable ordering and empty array; denied roles receive no metadata. |
| ADM-STAT-01 | All-time total, completed/cancelled counts, rates and booking counts by module/tutor; count every booking record including cancelled rebookings. All bookings are denominator; empty rates zero. | S1 four records with two completed/one cancelled ->50%/25%; same-name groups remain separate; inactive entities/history counted. |
| ADM-AI-02 | Explain deterministic aggregate statistics via shared gateway; no private notes, names/emails or free-text user prompts. Generation is transient/read-only, requires review and cannot change records. | R1 denied/empty-data requests make zero calls; sanitized snapshot only; malformed/unsafe output fails safely; management/statistics still work on AI failure. |

## AI output and privacy

Model receives totals/rates only for the initial scope, not per-person grouping or
module labels. This avoids identity leakage and untrusted stored labels. It may
summarize totals/status proportions and flag absence of evidence; it cannot establish
causes/trends over time from an all-time snapshot. No fabricated trend comparison.
Return capture timestamp, the exact statistics snapshot and explanation, clearly
marked a suggestion. Empty data returns a deterministic no-data explanation.
No persistent reports, chat, exports or extra data source is required.

## Decisions and integration

Kok Seng decides Admin scope, management rules, statistics and AI report policies.
Coordinate shared storage, locks and gateway interfaces with their implementers.
Password reset and forced password change are excluded. Implementation and verification status are recorded below.


Admin AI input/output/security limits are selected in the technical specification,
including aggregate-only input and no automatic actions.


## Implementation status — 22 October 2026

Admin account/module/assignment APIs, booking queries, deterministic statistics and
aggregate-only report parsing are implemented locally. Reports have bounded workers,
a30-second deadline and per-user in-flight protection. Interrupted/uncertain calls
retain protection for at least60seconds and until the worker actually finishes.
Fake reports are explicitly local; production defaults to AI_UNAVAILABLE. Completing
a live SoC report depends on the Student-owned shared provider and durable quota
infrastructure. No frontend or deployment is claimed. Verification results are
recorded in logs/kokseng.md after checks complete.

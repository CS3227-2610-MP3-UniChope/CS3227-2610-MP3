# stu-c3-my-bookings: Student views own bookings

Status: SPEC_DRAFT (READY_FOR_TESTS once stu-c1 is ready; no open assumption specific to this change)
Scope classification and reason: standard path; owner-scoped private reads.
Parent request / related changes / dependencies: same request as
[stu-c1-browse-slots](stu-c1-browse-slots.md); depends on its foundation (C1-T1, C1-T2).
Supplies the `StudentBooking` projection used by [stu-c2](stu-c2-book-slot.md) and
[stu-c4](stu-c4-cancel-booking.md).
Human owner / affected owners: Phuc (Student).
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
Producer role / actual tool and model: Spec analyst / Claude Code (Opus 5.5)
Recipient role: Test writer, fresh session
Change ID / task / exact requested next action: stu-c3-my-bookings / C3-T1, C3-T2 / write
failing tests and record the red result.
Stage status / supersedes: SPEC_DRAFT, first revision.
Goal and non-goals: Goal: `GET /api/student/bookings` and `GET /api/student/bookings/{id}`
return only the session student's bookings with a server-computed `cancellable`.
Non-goals: other students' data, tutor notes, frontend tabs.
Acceptance IDs: S-LIST-1 (product spec §7); C3-OUT-1, C3-OWN-1, C3-FLT-1 to C3-FLT-3,
C3-IN-1, C3-ACC-1 (below).

### Inputs and outputs
Same input artifacts and hashes as [stu-c1 Handoff analyst-1](stu-c1-browse-slots.md#inputs-and-outputs);
relevant sections: PRODUCT-SPEC §3 STU-BOOK-02, §7; TECHNICAL-SPEC §3, §3.1, §3.2, §4
(student bookings query). Output: Student tests for C3 (to be recorded).

Base commit / result commit / complete diff or patch reference: base 502d09f; none yet.
Uncommitted files and their hashes: as stu-c1.
Allowed write scope: as stu-c1.
Prerequisites / dependency state and evidence: stu-c1 foundation.
Recorded decisions and authorization references: D1-D4 in stu-c1.
Open questions / proposals / unverified facts: none specific to this change.
Verification commands, directories, results and output references: baseline as stu-c1.
Readiness or review verdict, scope and supporting evidence: ready for tests once stu-c1
is READY_FOR_TESTS; all c3 behavior is specified in TECHNICAL-SPEC §3/§4.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

## Acceptance scenarios

| ID | Given / When / Then |
| --- | --- |
| S-LIST-1 | Given S1 and S2 each with ACTIVE (future and started), CANCELLED and COMPLETED bookings / when S1 GETs `/bookings` / then only S1's bookings appear, ordered by slot startTime then booking id. |
| C3-OUT-1 | `StudentBooking` JSON has exactly `id, slotId, moduleId, moduleCode, moduleName, tutorName, startTime, endTime, status, cancellable`; times end in `Z`; no emails or student names. |
| C3-FLT-1 | `status=ACTIVE` returns only ACTIVE; `status=CANCELLED&status=COMPLETED` returns both history statuses; `status=ACTIVE&status=ACTIVE` equals `status=ACTIVE`; no status returns all. |
| C3-FLT-2 | `date` filters by the slot's SGT start date across the UTC/SGT boundary (same fixture times as S-SLOT-2) and intersects with status. |
| C3-FLT-3 | `cancellable` is true only for ACTIVE bookings whose slot start is strictly after the fixed clock; it becomes false when the clock moves to the start instant. |
| C3-OWN-1 | GET `/bookings/{id}` returns S1's own booking in any status; S2's booking ID and a random UUID both return 404 `NOT_FOUND` with identical bodies apart from requestId. |
| C3-IN-1 | Unknown query parameter, invalid status value (`active`, `PENDING`), repeated `date`, bad date, non-UUID path ID, or any query on `/bookings/{id}` → 400 `VALIDATION_FAILED`. |
| C3-ACC-1 | Access matrix of TECHNICAL-SPEC §3.2 for both endpoints (401, 401+invalidate, 403 `ACCOUNT_INACTIVE`+invalidate, 403 `FORBIDDEN`). |

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| C3-T1 Booking read query + `StudentBooking` projection + `cancellable` | S-LIST-1, C3-OUT-1, C3-FLT-1..3 | | pending |
| C3-T2 `GET /bookings`, `GET /bookings/{id}` endpoints and input parsing | C3-OWN-1, C3-IN-1, C3-ACC-1 | | pending |

## Verification evidence
Command, working directory, revision, exit code, result and relevant output: pending.
Pre-implementation expected failure (or existing passing baseline): pending.
Final focused tests / integration tests / build: pending.
Manual and real-provider checks: not applicable (no AI).

## Independent review
Reviewer role / actual model / reviewed revision: pending.
Verdict and evidence: pending.
Findings: pending.
Fixes and re-review results: pending.

## Human verification
Person, date, checked journey, outcome: pending.
Remaining limitations and follow-ups: frontend tabs wait for a frontend scaffold.
Next agent, exact task and required inputs: Test writer (fresh session) after stu-c1.


## Owner decision addendum - 22 October 2026
Recorded by Codex / GPT-6, coordination role; this is a human decision record,
not a new Claude readiness assessment.
Authorization: current user replies "all" and "Approve all four proposed rules".
Phuc requests all four changes with separate fresh GPT test/implementation sessions.
A1-A4 are approved: no enrolment restriction; overlapping slots remain listed;
cancellation before start with no notice period; unavailable/missing slots share
409 SLOT_UNAVAILABLE. Canonical source: PRODUCT-SPEC.md section 10, SHA-256
ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957.
This supersedes only the analyst packet's unconfirmed A1-A4 observations and old
product hash; no acceptance behavior changed. Technical/plan hashes are unchanged.
Dependency order: c1 foundation/browsing, c3 booking views/projection, c2 creation,
c4 cancellation. Earlier analyst conditional readiness and all other entry checks
must be verified by each receiver; this addendum does not waive missing evidence.
AI, frontend, production activation and teammate approval remain outside this scope.

## Workflow decision addendum - 22 October 2026
Recorded by the spec analyst session (Claude Code, Opus 5.5) as a human decision record.
Authorization: Phuc, current user request on 22 October 2026: "remove all the claude
and gpt u do everything each in a fresh session".
D5: one agent tool runs every role (spec analyst, test writer, implementer, reviewer),
each in its own fresh session or fresh subagent with no inherited conversation. This
supersedes D1 and every earlier role-to-model assignment in this record ("Claude
analyst", "GPT test writer", "GPT implementer", "Claude reviewer"). Read those labels
as the role name only. "Actual tool and model" fields and earlier receipts remain
unchanged because they record who actually performed each stage.
No acceptance behavior, readiness verdict or test evidence changed. Workflow files
revised for D5 (role wording only): PLAN.md 66e77b89e0c235cea3fada90316c0c7580f6ea7ce2816b9e35c5bf6f523e4a68,
NOTE.md 5e8ba76fcd3a52a08925ba744dca8e4adda572403b2ac800ec58dbef2b5d62e4, HANDOFF.md
1ee65fd926dff0a710597508226cf109cb723678b760111e7d6120adfe5b5596; they supersede the earlier PLAN.md hash.

# stu-c4-cancel-booking: Student cancels an upcoming booking

Status: SPEC_DRAFT (becomes READY_FOR_TESTS when Phuc confirms A3 below)
Scope classification and reason: standard path; consultation state mutation with a
race against tutor completion.
Parent request / related changes / dependencies: same request as
[stu-c1-browse-slots](stu-c1-browse-slots.md). Depends on stu-c1 foundation and the
[stu-c3](stu-c3-my-bookings.md) `StudentBooking` projection.
Human owner / affected owners: Phuc (Student). Core lifecycle used unchanged.
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
Producer role / actual tool and model: Spec analyst / Claude Code (Opus 5.5)
Recipient role: Test writer, fresh session
Change ID / task / exact requested next action: stu-c4-cancel-booking / C4-T1 / write
failing tests and record the red result.
Stage status / supersedes: SPEC_DRAFT, first revision.
Goal and non-goals: Goal: `POST /api/student/bookings/{id}/cancel` cancels the session
student's ACTIVE, not-yet-started booking via `ConsultationLifecycle.cancelActiveBooking`.
Non-goals: notice periods, rescheduling, tutor-side cancellation, frontend.
Acceptance IDs: S-CANC-1 to S-CANC-3 (product spec §7); C4-IN-1, C4-ACC-1, C4-ERR-1 (below).

### Inputs and outputs
Same input artifacts and hashes as [stu-c1 Handoff analyst-1](stu-c1-browse-slots.md#inputs-and-outputs);
relevant sections: PRODUCT-SPEC §3 STU-BOOK-03, §7; TECHNICAL-SPEC §3, §3.1-§3.3, §4
(`cancelActiveBooking`). Core reference: `PostgresConsultationLifecycle.changeBooking`
at commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d. Output: Student tests for C4.

Base commit / result commit / complete diff or patch reference: base 502d09f; none yet.
Uncommitted files and their hashes: as stu-c1.
Allowed write scope: as stu-c1.
Prerequisites / dependency state and evidence: stu-c1 and stu-c3 (projection).
Recorded decisions and authorization references: D1-D4 in stu-c1.
Open questions / proposals / unverified facts:
- A3 (product spec §10 item 3): cancellation allowed until the slot's start, no notice
  period. Core already enforces exactly this; confirmation still needed. Unconfirmed.
Verification commands, directories, results and output references: baseline as stu-c1.
Readiness or review verdict, scope and supporting evidence: NOT READY until A3 is confirmed.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

## Acceptance scenarios

| ID | Given / When / Then |
| --- | --- |
| S-CANC-1 | Given S1's ACTIVE future booking / when S1 POSTs `{}` to `/bookings/{id}/cancel` / then 200 `StudentBooking` with status CANCELLED and `cancellable: false`; DB booking CANCELLED and slot AVAILABLE; the slot reappears in `/slots`. Repeating the request → 409 `INVALID_STATE`, no change. |
| S-CANC-2 | Given S2's booking ID or a random UUID → 404 `NOT_FOUND` (identical bodies apart from requestId). Given S1's booking whose slot start equals the clock (started), or a COMPLETED or CANCELLED booking → 409 `INVALID_STATE`. No row changes in any case. |
| S-CANC-3 | Given tutor completion (`completeActiveBooking`) and student cancellation race on one booking (fixture `race` helper) / then exactly one succeeds; the student's request is 200 or 409 `INVALID_STATE`; booking and slot are both CANCELLED/AVAILABLE or both COMPLETED. |
| C4-IN-1 | Bodies absent, `null`, `[]`, `{"reason":"x"}`, `{"studentId":"<S2>"}`; non-JSON content type (415); body over 65,536 bytes (413); any query parameter; non-UUID path ID → rejected with no change. |
| C4-ACC-1 | Missing/invalid CSRF → 403 `CSRF_INVALID`; no principal → 401; tutor/admin → 403 `FORBIDDEN`; inactive student → 403 `ACCOUNT_INACTIVE` with session invalidated; no change. |
| C4-ERR-1 | Lifecycle double: `TEMPORARILY_UNAVAILABLE` → 503; `ACCESS_DENIED` after mid-request deactivation → 403 `ACCOUNT_INACTIVE`; unexpected exception → 500 without SQL text; projection failure after commit → 500 with the cancellation committed once. |

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| C4-T1 `POST /bookings/{id}/cancel`: input, lifecycle call, error mapping, projection | All above | | pending |

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
Remaining limitations and follow-ups: simulated session identity until auth integration.
Next agent, exact task and required inputs: Test writer (fresh session) after stu-c3 and the A3 confirmation.


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

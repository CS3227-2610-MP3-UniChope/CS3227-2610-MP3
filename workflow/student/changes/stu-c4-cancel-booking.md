# stu-c4-cancel-booking: Student cancels an upcoming booking

Status: READY_FOR_TESTS for C4-T1 per "Handoff analyst-2" below, after stu-c1 and stu-c3 (and, in order, stu-c2) are implemented (supersedes: SPEC_DRAFT, becomes READY_FOR_TESTS when Phuc confirms A3 below)
Scope classification and reason: standard path; consultation state mutation with a
race against tutor completion.
Parent request / related changes / dependencies: same request as
[stu-c1-browse-slots](stu-c1-browse-slots.md). Depends on stu-c1 foundation and the
[stu-c3](stu-c3-my-bookings.md) `StudentBooking` projection.
Human owner / affected owners: Phuc (Student). Core lifecycle used unchanged.
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
> Superseded in part by "Handoff analyst-2" (end of this record): inputs, hashes, base, open
> questions and readiness verdict. Acceptance scenarios below remain, as refined there.
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

## Handoff analyst-2
Supersedes: the "Handoff analyst-1" manifest above (inputs by reference to stu-c1
analyst-1, base 502d09f, open question A3, decisions "D1-D4" and the NOT READY verdict)
and the Status line's SPEC_DRAFT condition. The analyst-1 acceptance scenarios remain
in force, as refined below.
Producer role / actual tool and model: Spec analyst / Claude Code, Opus 5.5
(`claude-opus-5-5`); fresh subagent session, no inherited conversation (D5).
Recipient role: Test writer, fresh session, `workflow/student/agents/test-writer.md`.
Change ID / task / exact requested next action: stu-c4-cancel-booking / C4-T1 / after
the dependency conditions below are met, verify this packet, write the C4 tests and
record the behavioral red result.
Stage status: READY_FOR_TESTS (scoped, dependency-conditioned; see Spec readiness (analyst-2)).
Goal and non-goals: unchanged from analyst-1.
Acceptance IDs: S-CANC-1..3, C4-BND-1, C4-ELIG-1, C4-IN-1, C4-ACC-1, C4-ERR-1.

### Inputs and outputs
All canonical inputs, hashes, code/test patterns and process files are those listed in
[stu-c1 "Handoff analyst-2" Inputs and outputs](stu-c1-browse-slots.md#handoff-analyst-2)
(PRODUCT-SPEC ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957,
TECHNICAL-SPEC e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3,
PLAN 31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, base commit
459fd6ef7108c2d08aeaa0f92ae0bccc054158b5). Sections relevant here: PRODUCT §2, §3
STU-BOOK-03, §7 S-CANC-1..3, §10 item 3; TECHNICAL §3 (POST `/bookings/{id}/cancel`),
§3.1, §3.2, §3.3, §4 (`cancelActiveBooking`); INTEGRATION §7 ("Student cancels",
"Tutor completes" lock rows).
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java `changeBooking`, `cancelActiveBooking`, `completeActiveBooking` | Input (code) | 35870e1c1f35246de71fef83173dada8048aa08c855a65f660ee9b5135721b99 | Ownership, state and time checks; lock order |
| src/backend/src/test/java/unichope/platform/core/postgres/LifecyclePgFixture.java `race` | Input (test pattern) | ede25ee845b6c20cb060c5ec1afd7b5b0f37c9b5e2a381b065c66d03b2d92db3 | Completion vs cancellation race |
| C4 tests in the Student test scope | Output (test writer) | to be recorded | Tests |
| stu-c1 and stu-c3 implementations with green tests (stu-c2 in the recorded order) | Input (prerequisite) | to be recorded by their implementers | Foundation, projection, `/slots` |

Base commit / diff: base 459fd6e; analyst-2 working-tree changes as listed in stu-c1
Handoff analyst-2. No source or test file changed by this session.
Allowed write scope (test writer): as stu-c1 Handoff analyst-2.
Allowed write scope (later implementer): `unichope/student/**` only; core used unchanged.
Prerequisites / dependency state: hard: stu-c1 C1-T1..T4 and stu-c3 C3-T1 implemented
with tests green. Order: after stu-c2 (recorded c1, c3, c2, c4), so c4 reuses c2's body
parser and lifecycle error mapping; c4 tests do not call `POST /bookings` (bookings are
inserted with fixture helpers).
Recorded decisions: A3 (cancellation until slot start, no notice period) approved by
Phuc, PRODUCT §10 (22 October 2026); D2, D3, D5.
Open questions / proposals / unverified facts: inherited F2-13, F2-15.
Verification commands: none run in this analyst session.
Readiness verdict: READY_FOR_TESTS for C4-T1 with the dependency conditions below.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

## Acceptance refinements (analyst-2)
Rows with an existing ID replace the analyst-1 wording.

| ID | Given / When / Then |
| --- | --- |
| C4-BND-1 (new, A3, F2-11) | Given S1's ACTIVE booking whose slot starts at T and the fixed clock at T minus 1 second / when S1 POSTs `{}` to `/bookings/{id}/cancel` / then 200 CANCELLED (no notice period); with the clock at exactly T the same request on a fresh booking → 409 `INVALID_STATE` (S-CANC-2). |
| C4-ELIG-1 (new, F2-19) | Given S1's ACTIVE future booking whose tutor was then deactivated (or module deactivated, or assignment removed) / when S1 cancels / then 200 CANCELLED and the slot AVAILABLE (cancellation has no eligibility condition; the slot is simply not listed by `/slots` while ineligible). |
| C4-IN-1 (tightened, F2-12) | Each with a valid CSRF token, an active S1 session and S1's ACTIVE future booking: `Content-Type: application/json` with an empty body → 400; bodies `null`, `[]`, `{"reason":"x"}`, `{"studentId":"<S2>"}`, duplicate keys or trailing tokens → 400 `VALIDATION_FAILED`; `Content-Type: text/plain` or no content type → 415 `UNSUPPORTED_MEDIA_TYPE`; body over 65,536 bytes (with a misleading `Content-Length: 2`) → 413 `PAYLOAD_TOO_LARGE`, exactly 65,536 bytes not rejected for size; any query parameter → 400; non-UUID path ID → 400. Database snapshot unchanged in every rejected case. |
| C4-ERR-1 (tightened, F2-09, F2-10) | With a lifecycle test double: `TEMPORARILY_UNAVAILABLE` → 503; `INVALID_INPUT` → 400 `VALIDATION_FAILED`; (a) double deactivates S1 then throws `ACCESS_DENIED` → 403 `ACCOUNT_INACTIVE`, session invalidated, then 401 on the same session; (b) `ACCESS_DENIED` while S1 is still active → 403 `FORBIDDEN`, session still valid; a code outside the TECH §3.3 table (e.g. `BOOKING_OVERLAP`) → 500; an unexpected exception with SQL text → 500 without that text; a projection failure after a real successful cancellation → 500, booking CANCELLED and slot AVAILABLE exactly once, a later `GET /bookings/{id}` shows CANCELLED. |
| S-CANC-3 (clarified) | Hold the student's (or tutor's) user row with the fixture `race` helper; launch T1's `completeActiveBooking` (lifecycle call) and S1's HTTP cancel; release. Exactly one transition succeeds; S1 gets 200 or 409 `INVALID_STATE`; final state is booking CANCELLED + slot AVAILABLE, or booking COMPLETED + slot COMPLETED. The booking's slot starts in the future so both outcomes are possible. |

Task mapping additions: C4-BND-1, C4-ELIG-1 → C4-T1.

## Requirement-to-technical coverage (analyst-2)
COVERED means specification support only. "PG" = Student MockMvc-to-PostgreSQL test through the real lifecycle.

| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| S-CANC-1 | STU-BOOK-03 bullets 1-2 (repeat); §7 S-CANC-1; shared business rule | TECH §3 POST `/bookings/{id}/cancel` (200 `StudentBooking`), §3.3 INVALID_STATE, §4 `cancelActiveBooking`; INTEGRATION §7 atomic "Student cancels" | COVERED | PG: response, DB read-back, slot reappears in `/slots`, repeat 409 with no change |
| S-CANC-2 | STU-BOOK-03 bullet 2 | TECH §3 404 rule, §3.3 NOT_FOUND → 404 and INVALID_STATE → 409; core `changeBooking` owner check before locks and recheck after | COVERED | PG: identical 404 bodies; 409 cases; snapshot unchanged |
| S-CANC-3 | STU-BOOK-03 bullet 4 | INTEGRATION §7 lock rows for "Student cancels" and "Tutor completes" (tutor and student sorted, module, slot, booking); core `changeBooking` | COVERED | PG with `race` helper |
| C4-BND-1 | STU-BOOK-03 bullet 3; §10 item 3 (A3) | TECH §4 "slot start after now"; core `startTime.isAfter(clock.instant())` | COVERED | PG with fixed clock |
| C4-ELIG-1 | STU-BOOK-03 bullet 1 (conditions are ownership, ACTIVE, not started) | TECH §4 `cancelActiveBooking` checks; core `changeBooking` has no eligibility check | COVERED | PG |
| C4-IN-1 | §2 bullet 2 | TECH §3.1 (`{}` exactly, 415, 413 by stream, no query); INTEGRATION §1; Tutor REST §2 | COVERED | PG/Unit |
| C4-ACC-1 | §2; §7 S-AUTH-1 | TECH §3.2 (CSRF row, evaluation order); SHARED SH-AUTH-03/04 | COVERED | PG with real security filter chain |
| C4-ERR-1 | §2 bullet 3; shared REL-01 | TECH §3.2 ACCESS_DENIED paragraph, §3.3 rows and post-commit paragraph; INTEGRATION §2, §5, §7 (503) | COVERED | PG with lifecycle and read-repository doubles |

Out of scope (no coverage claimed): cancel confirmation dialog, focus handling and
refresh UI (PLAN S4 React part).

## Spec findings and proposed fixes (analyst-2)
Applicable rows of the full register in [stu-c1](stu-c1-browse-slots.md) "Spec findings
and proposed fixes (analyst-2)"; evidence there.

| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| F2-01 / MEDIUM | PLAN S0 implied a new core `cancelBooking`; S4 dependency text inaccurate. | PLAN corrected (existing `cancelActiveBooking`, order c1, c3, c2, c4). | Phuc | RESOLVED: PLAN 31545bfb |
| F2-06 / LOW | Validation-vs-access order unspecified. | TECH §3.2 evaluation order. | Phuc | RESOLVED: TECH e4ba47b0 |
| F2-09 / LOW | `ACCESS_DENIED` still-valid branch untested. | TECH §3.2 wording; C4-ERR-1. | Phuc | RESOLVED |
| F2-10 / LOW | Unlisted lifecycle codes unmapped. | TECH §3.3 row → 500; C4-ERR-1. | Phuc | RESOLVED |
| F2-11 / LOW | No positive boundary for A3. | C4-BND-1. | Phuc | RESOLVED |
| F2-12 / LOW | C4-IN-1 statuses implicit. | C4-IN-1 tightened. | Phuc | RESOLVED |
| F2-19 / LOW | Cancellation after tutor/module ineligibility unspecified in acceptance. | C4-ELIG-1. | Phuc | RESOLVED |
| F2-13 / MEDIUM | Inherited c1 seam/security-rule review. | Merge gate. | Kok Seng, Huanghao | OPEN, DEFERRED |
| F2-15 / LOW | Inherited: simulated authentication only. | Activation gate. | Kok Seng | OPEN, DEFERRED |
| F2-17 / LOW | Inherited: logging by review. | Reviewer checklist. | Phuc | RESOLVED by plan |
| F2-18 / INFO | Base re-anchored to 459fd6e. | This manifest. | - | RESOLVED |

Audited without further findings for c4: A3 vs core (no notice period; `isAfter` strict,
so start = now is rejected); ownership non-disclosure (core returns NOT_FOUND for a
foreign booking before locking and again after the recheck); atomic booking + slot
update and repeat protection (status preconditions under locks, SHARED SH-DATA-04);
completion/cancellation lock order identical (INTEGRATION §7); `{}` body rule identical
to Tutor slot cancel. F2-14 does not apply (c4 uses existing codes only). AI: not applicable.

## Shared-owner boundary and dependency check (analyst-2)
- Files: `unichope/student/**` only. Core lifecycle unchanged; tutor completion is
  invoked only from test code for S-CANC-3. No `platform/*` change beyond stu-c1's.
- Hard dependencies: stu-c1 implemented; stu-c3 C3-T1 projection implemented.
  Ordering dependency: after stu-c2 (shared parser/error mapping); if Phuc reorders
  c4 before c2, the c4 implementer creates those Student-local pieces within the same
  allowed scope and this verdict still holds.

## Spec readiness (analyst-2)
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision: stu-c4 C4-T1 against PRODUCT-SPEC
ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957, TECHNICAL-SPEC
e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3, PLAN
31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, code at 459fd6e.
Remaining findings and why deferral does not affect this task: F2-13 and F2-15 as in
stu-c1 (merge/activation gates).
Required owner decisions and recorded outcome: A3 approved (PRODUCT §10).
Recheck evidence: each C4 ID re-traced to TECH §3/§3.1-§3.3/§4 after the analyst-2
edits and to core `changeBooking`'s actual checks and lock order.
Dependency conditions: test writing starts only after stu-c1 and stu-c3 have implementer
handoffs with green tests (and, in the recorded order, after stu-c2); a contract change
in any of them returns this change to the analyst.
Next agent, task, acceptance IDs, allowed files and prerequisites: fresh test writer
(after stu-c2 implementation in the recorded order); C4-T1; IDs above; Student test
scope; compose PostgreSQL.

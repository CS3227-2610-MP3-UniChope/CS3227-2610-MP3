# stu-c4-cancel-booking: Student cancels an upcoming booking

Status: READY_FOR_HUMAN; independent review PASS in reviewer-1, with human acceptance and affected-owner merge review pending.
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
Acceptance IDs: S-CANC-1 to S-CANC-3 (product spec Â§7); C4-IN-1, C4-ACC-1, C4-ERR-1 (below).

### Inputs and outputs
Same input artifacts and hashes as [stu-c1 Handoff analyst-1](stu-c1-browse-slots.md#inputs-and-outputs);
relevant sections: PRODUCT-SPEC Â§3 STU-BOOK-03, Â§7; TECHNICAL-SPEC Â§3, Â§3.1-Â§3.3, Â§4
(`cancelActiveBooking`). Core reference: `PostgresConsultationLifecycle.changeBooking`
at commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d. Output: Student tests for C4.

Base commit / result commit / complete diff or patch reference: base 502d09f; none yet.
Uncommitted files and their hashes: as stu-c1.
Allowed write scope: as stu-c1.
Prerequisites / dependency state and evidence: stu-c1 and stu-c3 (projection).
Recorded decisions and authorization references: D1-D4 in stu-c1.
Open questions / proposals / unverified facts:
- A3 (product spec Â§10 item 3): cancellation allowed until the slot's start, no notice
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
| S-CANC-1 | Given S1's ACTIVE future booking / when S1 POSTs `{}` to `/bookings/{id}/cancel` / then 200 `StudentBooking` with status CANCELLED and `cancellable: false`; DB booking CANCELLED and slot AVAILABLE; the slot reappears in `/slots`. Repeating the request â†’ 409 `INVALID_STATE`, no change. |
| S-CANC-2 | Given S2's booking ID or a random UUID â†’ 404 `NOT_FOUND` (identical bodies apart from requestId). Given S1's booking whose slot start equals the clock (started), or a COMPLETED or CANCELLED booking â†’ 409 `INVALID_STATE`. No row changes in any case. |
| S-CANC-3 | Given tutor completion (`completeActiveBooking`) and student cancellation race on one booking (fixture `race` helper) / then exactly one succeeds; the student's request is 200 or 409 `INVALID_STATE`; booking and slot are both CANCELLED/AVAILABLE or both COMPLETED. |
| C4-IN-1 | Bodies absent, `null`, `[]`, `{"reason":"x"}`, `{"studentId":"<S2>"}`; non-JSON content type (415); body over 65,536 bytes (413); any query parameter; non-UUID path ID â†’ rejected with no change. |
| C4-ACC-1 | Missing/invalid CSRF â†’ 403 `CSRF_INVALID`; no principal â†’ 401; tutor/admin â†’ 403 `FORBIDDEN`; inactive student â†’ 403 `ACCOUNT_INACTIVE` with session invalidated; no change. |
| C4-ERR-1 | Lifecycle double: `TEMPORARILY_UNAVAILABLE` â†’ 503; `ACCESS_DENIED` after mid-request deactivation â†’ 403 `ACCOUNT_INACTIVE`; unexpected exception â†’ 500 without SQL text; projection failure after commit â†’ 500 with the cancellation committed once. |

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
459fd6ef7108c2d08aeaa0f92ae0bccc054158b5). Sections relevant here: PRODUCT Â§2, Â§3
STU-BOOK-03, Â§7 S-CANC-1..3, Â§10 item 3; TECHNICAL Â§3 (POST `/bookings/{id}/cancel`),
Â§3.1, Â§3.2, Â§3.3, Â§4 (`cancelActiveBooking`); INTEGRATION Â§7 ("Student cancels",
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
Phuc, PRODUCT Â§10 (22 October 2026); D2, D3, D5.
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
| C4-BND-1 (new, A3, F2-11) | Given S1's ACTIVE booking whose slot starts at T and the fixed clock at T minus 1 second / when S1 POSTs `{}` to `/bookings/{id}/cancel` / then 200 CANCELLED (no notice period); with the clock at exactly T the same request on a fresh booking â†’ 409 `INVALID_STATE` (S-CANC-2). |
| C4-ELIG-1 (new, F2-19) | Given S1's ACTIVE future booking whose tutor was then deactivated (or module deactivated, or assignment removed) / when S1 cancels / then 200 CANCELLED and the slot AVAILABLE (cancellation has no eligibility condition; the slot is simply not listed by `/slots` while ineligible). |
| C4-IN-1 (tightened, F2-12) | Each with a valid CSRF token, an active S1 session and S1's ACTIVE future booking: `Content-Type: application/json` with an empty body â†’ 400; bodies `null`, `[]`, `{"reason":"x"}`, `{"studentId":"<S2>"}`, duplicate keys or trailing tokens â†’ 400 `VALIDATION_FAILED`; `Content-Type: text/plain` or no content type â†’ 415 `UNSUPPORTED_MEDIA_TYPE`; body over 65,536 bytes (with a misleading `Content-Length: 2`) â†’ 413 `PAYLOAD_TOO_LARGE`, exactly 65,536 bytes not rejected for size; any query parameter â†’ 400; non-UUID path ID â†’ 400. Database snapshot unchanged in every rejected case. |
| C4-ERR-1 (tightened, F2-09, F2-10) | With a lifecycle test double: `TEMPORARILY_UNAVAILABLE` â†’ 503; `INVALID_INPUT` â†’ 400 `VALIDATION_FAILED`; (a) double deactivates S1 then throws `ACCESS_DENIED` â†’ 403 `ACCOUNT_INACTIVE`, session invalidated, then 401 on the same session; (b) `ACCESS_DENIED` while S1 is still active â†’ 403 `FORBIDDEN`, session still valid; a code outside the TECH Â§3.3 table (e.g. `BOOKING_OVERLAP`) â†’ 500; an unexpected exception with SQL text â†’ 500 without that text; a projection failure after a real successful cancellation â†’ 500, booking CANCELLED and slot AVAILABLE exactly once, a later `GET /bookings/{id}` shows CANCELLED. |
| S-CANC-3 (clarified) | Hold the student's (or tutor's) user row with the fixture `race` helper; launch T1's `completeActiveBooking` (lifecycle call) and S1's HTTP cancel; release. Exactly one transition succeeds; S1 gets 200 or 409 `INVALID_STATE`; final state is booking CANCELLED + slot AVAILABLE, or booking COMPLETED + slot COMPLETED. The booking's slot starts in the future so both outcomes are possible. |

Task mapping additions: C4-BND-1, C4-ELIG-1 â†’ C4-T1.

## Requirement-to-technical coverage (analyst-2)
COVERED means specification support only. "PG" = Student MockMvc-to-PostgreSQL test through the real lifecycle.

| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| S-CANC-1 | STU-BOOK-03 bullets 1-2 (repeat); Â§7 S-CANC-1; shared business rule | TECH Â§3 POST `/bookings/{id}/cancel` (200 `StudentBooking`), Â§3.3 INVALID_STATE, Â§4 `cancelActiveBooking`; INTEGRATION Â§7 atomic "Student cancels" | COVERED | PG: response, DB read-back, slot reappears in `/slots`, repeat 409 with no change |
| S-CANC-2 | STU-BOOK-03 bullet 2 | TECH Â§3 404 rule, Â§3.3 NOT_FOUND â†’ 404 and INVALID_STATE â†’ 409; core `changeBooking` owner check before locks and recheck after | COVERED | PG: identical 404 bodies; 409 cases; snapshot unchanged |
| S-CANC-3 | STU-BOOK-03 bullet 4 | INTEGRATION Â§7 lock rows for "Student cancels" and "Tutor completes" (tutor and student sorted, module, slot, booking); core `changeBooking` | COVERED | PG with `race` helper |
| C4-BND-1 | STU-BOOK-03 bullet 3; Â§10 item 3 (A3) | TECH Â§4 "slot start after now"; core `startTime.isAfter(clock.instant())` | COVERED | PG with fixed clock |
| C4-ELIG-1 | STU-BOOK-03 bullet 1 (conditions are ownership, ACTIVE, not started) | TECH Â§4 `cancelActiveBooking` checks; core `changeBooking` has no eligibility check | COVERED | PG |
| C4-IN-1 | Â§2 bullet 2 | TECH Â§3.1 (`{}` exactly, 415, 413 by stream, no query); INTEGRATION Â§1; Tutor REST Â§2 | COVERED | PG/Unit |
| C4-ACC-1 | Â§2; Â§7 S-AUTH-1 | TECH Â§3.2 (CSRF row, evaluation order); SHARED SH-AUTH-03/04 | COVERED | PG with real security filter chain |
| C4-ERR-1 | Â§2 bullet 3; shared REL-01 | TECH Â§3.2 ACCESS_DENIED paragraph, Â§3.3 rows and post-commit paragraph; INTEGRATION Â§2, Â§5, Â§7 (503) | COVERED | PG with lifecycle and read-repository doubles |

Out of scope (no coverage claimed): cancel confirmation dialog, focus handling and
refresh UI (PLAN S4 React part).

## Spec findings and proposed fixes (analyst-2)
Applicable rows of the full register in [stu-c1](stu-c1-browse-slots.md) "Spec findings
and proposed fixes (analyst-2)"; evidence there.

| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| F2-01 / MEDIUM | PLAN S0 implied a new core `cancelBooking`; S4 dependency text inaccurate. | PLAN corrected (existing `cancelActiveBooking`, order c1, c3, c2, c4). | Phuc | RESOLVED: PLAN 31545bfb |
| F2-06 / LOW | Validation-vs-access order unspecified. | TECH Â§3.2 evaluation order. | Phuc | RESOLVED: TECH e4ba47b0 |
| F2-09 / LOW | `ACCESS_DENIED` still-valid branch untested. | TECH Â§3.2 wording; C4-ERR-1. | Phuc | RESOLVED |
| F2-10 / LOW | Unlisted lifecycle codes unmapped. | TECH Â§3.3 row â†’ 500; C4-ERR-1. | Phuc | RESOLVED |
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
completion/cancellation lock order identical (INTEGRATION Â§7); `{}` body rule identical
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
Required owner decisions and recorded outcome: A3 approved (PRODUCT Â§10).
Recheck evidence: each C4 ID re-traced to TECH Â§3/Â§3.1-Â§3.3/Â§4 after the analyst-2
edits and to core `changeBooking`'s actual checks and lock order.
Dependency conditions: test writing starts only after stu-c1 and stu-c3 have implementer
handoffs with green tests (and, in the recorded order, after stu-c2); a contract change
in any of them returns this change to the analyst.
Next agent, task, acceptance IDs, allowed files and prerequisites: fresh test writer
(after stu-c2 implementation in the recorded order); C4-T1; IDs above; Student test
scope; compose PostgreSQL.


## Handoff analyst-3 ? auth-compatible specification refresh, 22 October 2026
Producer: fresh spec analyst, Codex GPT-6. Recipient: fresh test writer.
Stage: READY_FOR_TESTS; supersedes prior readiness/input hashes/dependency gates
where affected, preserves historical evidence and all behavioral acceptance.
Task: C4-T1; acceptance IDs: S-CANC-1..3, C4-BND-1, C4-ELIG-1, C4-IN-1, C4-ACC-1, C4-ERR-1.
Retain every analyst-2 acceptance refinement. Full cancellation input coverage:
absent/empty/null/non-object JSON, unknown identity/reason fields, duplicate keys,
trailing JSON, malformed/noncanonical path UUID, every query parameter, no/wrong
content type, streamed >65536 bytes despite Content-Length, exact boundary permitted.
Reject with no lifecycle call or row changes. C4-ERR-1 includes INVALID_INPUT,
TEMPORARILY_UNAVAILABLE, NOT_FOUND, INVALID_STATE, ACCESS_DENIED with deleted,
inactive, changed-role and still-valid actors, unknown lifecycle codes, unexpected
SQL-bearing exceptions, and projection failure after a committed cancellation.
Add simultaneous double cancel: exactly one 200 and one 409 with CANCELLED/AVAILABLE;
retain completion-vs-cancel race, start-minus-one-second and exact-start boundaries,
slot-not-BOOKED rejection, foreign/missing 404 equivalence, repeat 409, tutor/module/
assignment ineligibility not preventing cancel, slot reappearance only if eligible.


### Current artifact manifest
| Artifact | SHA-256 | Purpose |
| --- | --- | --- |
| `workflow/student/PRODUCT-SPEC.md` | `ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957` | Canonical contract |
| `workflow/student/TECHNICAL-SPEC.md` | `67c2c5ba98885d4845c16b27eefbed237cdb7767bede9f7d7c44c8aa57ac425b` | Canonical contract |
| `workflow/student/PLAN.md` | `cb9695ec503a774d931f91df8a5fb2df40b1dffed3f1f0aa34db0a78a4d5142d` | Canonical contract |
| `workflow/INTEGRATION-SPEC.md` | `1d6782ceca8351bdb806db1108b024e9da147e6de972b2f7e65f8442fa1202ea` | Canonical contract |
| `workflow/SHARED-TECHNICAL-SPEC.md` | `54ec090a426a1a8733f7628924e4310d27a90b1b724d0acfad3eb0adcde35cfe` | Canonical contract |
| `workflow/auth/TECHNICAL-SPEC.md` | `2900b5d6ca945c829fffb513fab0fe1e6fd2a0b358fdbd4c3635445d6208de40` | Canonical contract |
| `src/backend/src/main/java/unichope/platform/runtime/ApplicationConfiguration.java` | `28055e3c9404be5a2321f071c28493ddf0d9b5cef275aab481ec15fb45086419` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/web/CurrentAccountFilter.java` | `5e0b23a23055e3e36e0632531bb5dcfafda24c4cf9eb9f6225fbf93b0a773f0a` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/web/ApiSecurityConfiguration.java` | `c97117de37b800286448343032ba2144ab92fd34297b58a5aac10f9e8382a0fe` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/auth/AuthenticationService.java` | `e0576283642e2d4afaeb5007b17736c34e5f977b13755068c90fe090c1627f4f` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java` | `35870e1c1f35246de71fef83173dada8048aa08c855a65f660ee9b5135721b99` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/main/resources/application.properties` | `c9058d21e64a1f73c14366eba4baa876d64f57971af466086b968f5d4b82b3f1` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/test/java/unichope/platform/core/postgres/StudentBookSlotPgTest.java` | `8d5866e3e5ea86a7e3e75ad47338dc9f5c6b0a0d1bfd77ce74ec09ea94fc3ee7` | Inspected baseline source/test; not proof of execution |
| `src/backend/src/test/java/unichope/platform/runtime/ApplicationIntegrationTest.java` | `392fa44e6bcab5f3edd7664899ef7fedab3591e066c0bcbab7de4482263d023b` | Inspected baseline source/test; not proof of execution |

Base commit: `a9dd6508906a11346e9638f77d744593482dff08`. Canonical spec outputs are uncommitted; exact hashes above.
Scope: backend only; tests/fixtures and evidence in existing Student/runtime test
locations. Later implementation student/** only for this task, existing core
ConsultationLifecycle unchanged. No test/code change or test execution in this role.
Authorization: Phuc's direct request "Update the spec and intergrate with new auth,
implement student booking"; prior booking baseline rules unchanged. Shared-owner
review remains merge gate, no fabricated new approval or Claude stage.

Dependencies: c1/c3 actual read code exists at this base. Implement C5 runtime
foundation, C2 creation, then C4 cancellation. Test writing may proceed now as an
executable combined packet using current foundations, fixtures and endpoint calls;
upstream implementation is not a prerequisite for authoring behavioral red tests.
The old requirement to wait for implemented upstream before writing tests is
superseded. Combined C5 book/cancel journey becomes green after C2/C4.

### Requirement-to-technical coverage and findings resolution
All listed IDs remain COVERED by Student Product sections 2/3/7/10, Student TECH
3.1-3.3/4 and Integration 7/9; original detailed matrices remain applicable except
shared filter precedence now uses TECH 3.2. C5-F1/F2/F4 findings are resolved at
contract level by TECH 2/3.2/9 and C5 packet; real-auth evidence is now required,
not indefinitely deferred. Former missing-adapter activation gate is C5 scope;
former owner-review deferral remains a merge gate. No other blocking contract gap.

### Readiness and verification
Verdict: READY_FOR_TESTS for C4-T1 against manifest above. Test writer must refresh
existing acceptance-to-test mappings and record reproducible current results.
Use real PostgreSQL for Student HTTP/lifecycle transitions and races; C5 adds real
Auth/runtime-migration PostgreSQL tests. Do not replace concurrency evidence with
UUID fake actors or mocks. Full final Gradle test postgresTest bootJar regression
covers Auth/Admin/Tutor/core/Student. Database/toolchain prerequisite and commands
are documented in C5 and backend README; no tests/builds were run by this analyst.
Independent review and human verification pending.

### Receiver receipt
Recipient adds ACCEPTED/RETURNED after checking current manifest and acceptance.

### Receiver receipt test-writer-2 (22 October 2026)
Receiver: fresh test writer, Codex GPT-6. ACCEPTED. Checked current canonical
Product, Student Technical/Plan, Integration, Shared Technical and Auth Technical
SHA-256 values against this manifest; all match. Checked every listed runtime,
filter, lifecycle, application.properties and existing test hash; all match.
Derived behavior from canonical contracts and inspected actual test harness/source.
C1/C3 source foundations exist; C5/C2/C4 implementation is intentionally absent.
Combined executable red test writing is authorized by analyst-3 dependency rule
(C5 uses analyst-1). Isolated UUID seams and real runtime auth remain distinct.

## Handoff test-writer-2 (22 October 2026)
Producer: fresh test writer, Codex GPT-6. Recipient: fresh implementer.
Stage status: READY_FOR_IMPLEMENTATION, scoped to the task in this record.
Entry receipt above checked all fourteen analyst manifest artifacts; no hash mismatch.
Canonical baseline: current analyst-3 manifest (C5 analyst-1), Student Technical
SHA-256 67c2c5ba98885d4845c16b27eefbed237cdb7767bede9f7d7c44c8aa57ac425b.
Base commit: a9dd6508906a11346e9638f77d744593482dff08; product code unchanged in this stage.
Test source freeze: final hashes below. Tests compile without product implementation
changes and are discovered by the configured Gradle postgresTest task.

| Output | SHA-256 |
| --- | --- |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBookSlotPgTest.java (retained unchanged) | 8d5866e3e5ea86a7e3e75ad47338dc9f5c6b0a0d1bfd77ce74ec09ea94fc3ee7 |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentCancelBookingPgTest.java | ce8000aba12ae2a993d339cccf1b49d706d61f837e8a6f1b95bcb10146f03941 |
| src/backend/src/test/java/unichope/platform/runtime/StudentAuthIntegrationPgTest.java | 7c15f18c144a59098bf8608171a8d265c0671fbe59ada16f4d37b275c8cd0393 |

### Reproducible baseline evidence
Working directory: src/backend. Java 25, existing Gradle wrapper, PostgreSQL 17
on 127.0.0.1:55432/mp3_test; disposable synthetic account with CREATE SCHEMA.
Pass the disposable password from local test configuration; do not use production.
Commands shown with password redacted; actual output artifacts redact it too.

```powershell
.\gradlew.bat compileTestJava --console=plain
.\gradlew.bat postgresTest --tests '*StudentBookSlotPgTest' --tests '*StudentCancelBookingPgTest' --tests '*StudentAuthIntegrationPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
.\gradlew.bat postgresTest --tests '*StudentCancelBookingPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
```

Compile command exit 0. Initial focused run exit 1: 33 discovered, 33 failed
(C2 15, C4 11, C5 7). Failures are assertions for absent behavior: booking POST
405, cancellation POST 404, student-api missing-lifecycle context unexpectedly
starts, app-api omits student-api and Student routes deny real logged-in students.
Actual registration/login/runtime Flyway setup completed before runtime assertions.
Two retained C2 races report no blocked caller because the absent booking handler
never calls SQL; other C2 endpoint assertions identify that prerequisite directly.
No unavailable database, missing imports, test discovery or startup harness error
is counted as behavioral red. An initial checked-exception compilation defect in
new helper methods was corrected before the successful compile and red baseline.

C4 race tests then gained a successful-cancellation prerequisite control to report
missing cancellation behavior directly before exercising locks. Final C4 run compiled
that revision and exit 1: 11/11 assertion failures, all missing cancel handler 404.
This refresh supersedes only the initial C4 test-source/result revision. C2 and C5
sources did not change after their initial behavioral baseline.

Saved evidence (under stu-c5-auth-integration/evidence):

| Artifact | SHA-256 |
| --- | --- |
| red-initial-console.txt | 0e7d96cf2cacd0fe896450d7e1839716e9532d4f608c8f88aa72e2e9363e6112 |
| red-initial-assertions.json | 9d5e1447d6fdad9463265353b8294b0a7127ea376f9d677c6028d56a685e06c8 |
| red-c4-final-console.txt | ec7f85c06db0b8c984215fbedfa33aa2f0d851a60bf2d62a74643d1641fcadc8 |
| red-c4-final-assertions.json | 2e60755ff8431639bf2aa8e6ab3f7444cac2cb50d6695f6047b1181ad084a169 |

Evidence files are sanitized command outputs and JUnit assertion extracts, not
interaction logs. The first console intentionally contains the obsolete initial
C4 line references; use red-c4-final artifacts for current cancellation source.
Full regression `test postgresTest bootJar`, independent review, affected-owner
merge review and human verification remain pending implementation. No UI/AI or
real-provider claim. Existing C1/C3/Auth/Tutor/Admin green claims were not rerun in
this stage and are not adopted as fresh evidence. Runtime C5 tests use real auth;
isolated C2/C4 simulated UUID actors prove only their own seam contracts.

Next action: implement C5 runtime wiring, C2 creation, C4 cancellation in the
analyst-authorized source scope, retain frozen tests and canonical contracts, run
focused then full required regression, and pass complete diff to fresh reviewer.
A product defect goes to implementer; demonstrated test defect returns to a fresh
test-writer session. Do not silently weaken coverage to obtain green.

### C4 acceptance mapping
All cases below are in StudentCancelBookingPgTest; no product types for new
handlers are referenced, so compilation precedes implementation.

| Acceptance | Case |
| --- | --- |
| S-CANC-1 | sCanc1CancelsAtomicallyReturnsExactProjectionReleasesSlotAndRepeatConflicts |
| S-CANC-2 | sCanc2ForeignMissingStartedHistoryAndUnbookedSlotNeverChangeRows |
| S-CANC-3 | sCanc3CompletionAndHttpCancellationRaceKeepConsistentStates; sCanc3DoubleHttpCancelHasOneSuccessAndOneConflict |
| C4-BND-1 | c4Bnd1OneSecondBeforeStartIsAllowedExactStartIsConflict |
| C4-ELIG-1 | c4Elig1CancellationIgnoresTutorModuleAndAssignmentIneligibility |
| C4-IN-1 | c4In1ExactEmptyObjectCanonicalPathNoQueriesAndStreamedSize |
| C4-ACC-1 | c4Acc1CsrfAndStoredActorMatrixRejectWithoutLifecycleCalls |
| C4-ERR-1 safe mapping | c4Err1SafeLifecycleMappingsAndNoMutation |
| C4-ERR-1 access recheck | c4Err1AccessDeniedRechecksInactiveDeletedChangedRoleAndStillValidActor |
| C4-ERR-1 post-commit | c4Err1ProjectionFailureAfterCommitKeepsCancellationAndDoesNotRetry |
| Real authenticated cancellation | StudentAuthIntegrationPgTest c5Auth1*, c5Csrf1*, c5State1* |

Lifecycle wrapper delegates real PostgreSQL writes except explicitly scripted
error cases; the read-only datasource can fail after commit. Races use real row
locks observed in pg_stat_activity with independent worker sessions. The HTTP
cancellation success control runs before each race, then clears only disposable
consultation fixture rows. Task C4-T1 is READY_FOR_IMPLEMENTATION.

### Receiver receipt implementer-1 (22 October 2026)
Receiver: fresh implementer, Codex GPT-6. Receipt: ACCEPTED. Checked current analyst manifests (C5 analyst-1, C2/C4 analyst-3), six canonical spec hashes, frozen C2/C4/C5 tests and four saved red evidence hashes. Actual test sources and red assertions support the documented absent-handler/runtime behavior. Scope and dependency order remain C5 -> C2 -> C4; no contract or test change is required. Existing product code inspected; no source edits or builds preceded this receipt.
## Handoff implementer-1 (22 October 2026)
Producer: fresh implementer, Codex GPT-6. Recipient: fresh independent reviewer.
Stage status: IN_REVIEW. Goal: C5 shared runtime authentication, C2 booking creation,
and C4 cancellation; backend only. No frontend, AI, core lifecycle, DDL, Tutor,
Auth/Admin policy changes, commits or push. Base a9dd6508906a11346e9638f77d744593482dff08.
Canonical Product/Technical/Integration/Auth acceptance hashes remain the checked
analyst/test-writer baseline. Student PLAN progress-only hash is now
`a1c8591b393ca6536244915e185414b0c4edb5f09b799ca706ed60647d312cb9`, superseding only
stale plan progress metadata; acceptance behavior is unchanged.

### Implementation and acceptance mapping
- C5-RUN/AUTH/REV/CSRF/STATE/REG: application.properties composes student-api;
  ApplicationConfiguration adds platform.auth.CurrentActorProvider accepting only
  authenticated SessionPrincipal. Tutor provider and shared filters remain unchanged.
- C2 S-BOOK-1..5, C2-PREC/IN/ACC/ERR: StudentApiInput validates exact bounded JSON;
  StudentController supplies 201 and Location; StudentHttpService calls the existing
  lifecycle once, maps local errors and defensively recognizes the named unique index.
- C4 S-CANC-1..3, C4-BND/ELIG/IN/ACC/ERR: exact empty-object/path parser and cancel
  route use unchanged atomic lifecycle; service maps NOT_FOUND/INVALID_STATE and
  reruns stored-account access on ACCESS_DENIED. The same owned read projection
  follows committed writes without retries. StudentApiConfiguration requires lifecycle.
- Frozen C2/C4/C5 tests are unchanged at the test-writer-2 hashes. Existing read
  behavior retained; full regression includes C1/C3 and shared roles.

### Review artifact manifest
All artifacts below are under `workflow/student/changes/stu-c5-auth-integration/evidence/`.
| Artifact | SHA-256 | Purpose |
| --- | --- | --- |
| implementation-source-hashes.json | 3d1c6bdf4e9e9aab7074a8f28f62a3ffee7c3add1b9c4fdb5ef758a955526e82 | Exact final source hashes |
| implementation-source.patch | 345d9a070dcb6857fc46cd61e1ff871b605fc94cb6a3c43e473572e56f8de6d1 | Complete implementation source diff, including new service |
| review-product-and-spec.patch | a89c960b8725c9b1d68f33333260f97e8b67c5c7358cbe322ebed9fb9ca7f752 | Current product, tests, canonical spec/plan and README diff; includes new C4/C5 tests |
| green-focused-console.txt | 60c9ede66633fd9edf52566b951de54df6f2d89cccbac690c0af8c33da0c2729 | Focused command output, password redacted |
| green-full-console.txt | 2f3139b6837a9fd4c7f0f813689131db27843b61272ada0f5ec252288c548644 | Full required regression/build output, password redacted |
| green-full-counts.json | 9cafb2816335994da34948973622581b7d81fa1b9b49dd21993554ca865f07e0 | Actual final JUnit XML aggregates |

Use actual worktree files plus these records for review; untracked source/tests
are included in the saved patch. Preserve unrelated preexisting Admin documentation
changes and interaction logs. Evidence artifacts contain command results, not
narrative session history. No contract gap or test correction was required.

### Verification
Working directory `src/backend`, Java 25 and disposable local PostgreSQL17,
unique per-test schemas, existing Gradle cache. Commands use the existing synthetic
local test password (redacted here); no production service involved.

```powershell
.\gradlew.bat postgresTest --tests '*StudentBookSlotPgTest' --tests '*StudentCancelBookingPgTest' --tests '*StudentAuthIntegrationPgTest' '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
.\gradlew.bat test postgresTest bootJar '-Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test' '-Dcore.test.user=mp3_test' '-Dcore.test.password=<disposable-password>' --console=plain
```

Focused command exit0: C2 15, C4 11, C5 7; zero failures/errors/skips.
Full command exit0, BUILD SUCCESSFUL in 4m16s: unit59 across18 suites,
PostgreSQL178 across17 suites; all237 tests with zero failures/errors/skips;
bootJar built. Nonblocking JVM class-sharing warning appears in full output.
Final source revision matches implementation-source-hashes.json; final full run
compiled the source after Javadoc clarifications. No tests were weakened/edited.
Independent review, affected-owner merge review and human verification remain
pending; frontend keyboard/journey and AI/provider evaluation are outside this backend task.
Next action: fresh reviewer checks canonical baseline, all diffs/tests and saved
results, reruns proportionate verification, and records findings/verdict here.

## Independent review reviewer-1 (22 October 2026)

Receiver: fresh independent reviewer/security role, Codex GPT-6. Receipt: ACCEPTED
for Handoff implementer-1 after checking actual canonical/source/test hashes,
complete tracked/untracked product diff and red/green command artifacts.
Verdict: PASS for this backend task; ready for human verification. No blocking
findings or lower-severity implementation follow-ups identified. Full independent
report: [reviewer-1.md](stu-c5-auth-integration/evidence/reviewer-1.md).
Reviewed base a9dd6508906a11346e9638f77d744593482dff08 plus exact final seven-file
source manifest (SHA-256 3d1c6bdf4e9e9aab7074a8f28f62a3ffee7c3add1b9c4fdb5ef758a955526e82);
frozen C2/C4/C5 tests matched. Current Integration hash
f7893269a4f5c3f1f4e106f3c62225dd3e8d3b4e3c0c31b6ca4a5ce61fad0604 is accepted under
C5 metadata correction addendum 1; no acceptance behavior changed.
Independently inspected full-run XML before rerun: 59 unit +178 PostgreSQL tests,
zero failures/errors/skips; producer full command included bootJar. Reviewer reran
all C5/C4 tests using Java25 and disposable PostgreSQL17: exit0, 18/18 with zero
failures/errors/skips. Sanitized output/counts and their hashes are in the report.
Final source manifest rechecked with zero mismatches. No code/tests changed by
reviewer. Owner merge review and Phuc's human journey verification remain pending;
no frontend/AI implementation, deployment, commit or push approval is implied.

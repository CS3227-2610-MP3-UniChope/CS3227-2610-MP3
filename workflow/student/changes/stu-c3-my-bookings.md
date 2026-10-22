# stu-c3-my-bookings: Student views own bookings

Status: READY_FOR_HUMAN for C3-T1, C3-T2 per "Review reviewer-1" (PASS, INFO/LOW follow-ups R3-01..R3-03 only) (supersedes: IN_REVIEW per "Handoff implementer-1", which superseded READY_FOR_IMPLEMENTATION per "Handoff test-writer-1", which superseded READY_FOR_TESTS per "Handoff analyst-2", which superseded SPEC_DRAFT)
Scope classification and reason: standard path; owner-scoped private reads.
Parent request / related changes / dependencies: same request as
[stu-c1-browse-slots](stu-c1-browse-slots.md); depends on its foundation (C1-T1, C1-T2).
Supplies the `StudentBooking` projection used by [stu-c2](stu-c2-book-slot.md) and
[stu-c4](stu-c4-cancel-booking.md).
Human owner / affected owners: Phuc (Student).
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
> Superseded in part by "Handoff analyst-2" (end of this record): inputs, hashes, base, open
> questions and readiness verdict. Acceptance scenarios below remain, as refined there.
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

## Handoff analyst-2
Supersedes: the "Handoff analyst-1" manifest above (inputs/hashes by reference to
stu-c1 analyst-1, base 502d09f, "uncommitted files as stu-c1", decisions "D1-D4" and the
conditional verdict) and the Status line's SPEC_DRAFT condition. The analyst-1
acceptance scenarios remain in force, as refined below.
Producer role / actual tool and model: Spec analyst / Claude Code, Opus 5.5
(`claude-opus-5-5`); fresh subagent session, no inherited conversation (D5).
Recipient role: Test writer, fresh session, `workflow/student/agents/test-writer.md`.
Change ID / task / exact requested next action: stu-c3-my-bookings / C3-T1, C3-T2 /
after the stu-c1 dependency condition below is met, verify this packet, write the C3
tests and record the behavioral red result.
Stage status: READY_FOR_TESTS (scoped, dependency-conditioned; see Spec readiness (analyst-2)).
Goal and non-goals: unchanged from analyst-1.
Acceptance IDs: S-LIST-1, C3-OUT-1, C3-FLT-1..3, C3-ELIG-1, C3-OWN-1, C3-IN-1, C3-ACC-1.

### Inputs and outputs
All canonical inputs, hashes, code/test patterns and process files are those listed in
[stu-c1 "Handoff analyst-2" Inputs and outputs](stu-c1-browse-slots.md#handoff-analyst-2)
(PRODUCT-SPEC ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957,
TECHNICAL-SPEC e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3,
PLAN 31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, base commit
459fd6ef7108c2d08aeaa0f92ae0bccc054158b5). Sections relevant here: PRODUCT §2, §3
STU-BOOK-02, §7 S-LIST-1; TECHNICAL §3 (GET `/bookings`, `/bookings/{id}`, status,
`cancellable`, 404 rule), §3.1, §3.2 (incl. evaluation order), §4 (student bookings
query); INTEGRATION §1, §5; core `PostgresConsultationLifecycle.changeBooking`
(SHA-256 35870e1c1f35246de71fef83173dada8048aa08c855a65f660ee9b5135721b99) for the
`cancellable` predicate parity.
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| C3 tests in the Student test scope | Output (test writer) | to be recorded | Tests |
| stu-c1 implementation and green C1 tests | Input (prerequisite) | to be recorded by stu-c1 implementer | Foundation (actor seam, wiring, read repository, controller) |

Base commit / diff: base 459fd6e; analyst-2 working-tree changes as listed in stu-c1
Handoff analyst-2. No source or test file changed by this session.
Allowed write scope (test writer): as stu-c1 Handoff analyst-2 (Student test scope plus
the one Student PostgreSQL MockMvc class); c3 tests extend it, no main source.
Allowed write scope (later implementer): `unichope/student/**` only.
Prerequisites / dependency state: stu-c1 C1-T1..T4 implemented with its tests green at a
recorded revision (not yet started). Rationale: c3 reuses `StudentAccess`, the
`student-api` wiring, `StudentReadRepository` and the controller; testing before they
exist would produce setup failures, which NOTE §2 says are not a valid red result.
Recorded decisions: D2, D3 (stu-c1 analyst-1), A1-A4 (PRODUCT §10), D5.
Open questions / proposals / unverified facts: none specific to c3; inherited F2-13,
F2-14, F2-15 (see register).
Verification commands: none run in this analyst session.
Readiness verdict: READY_FOR_TESTS for C3-T1, C3-T2 with the dependency condition below.

### Receiver receipt
Receiver role / actual tool and model: Test writer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Checked task, artifact revisions and required evidence (22 October 2026):
- Task C3-T1, C3-T2 only. HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` equals the analyst-2 base.
- Spec hashes (`sha256sum`) match analyst-2 exactly, so no later section needs to explain a change:
  Student PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., PLAN 31545bfb...; shared PRODUCT d1d5a414...,
  SHARED-TECHNICAL 54ec090a..., INTEGRATION ac0d183f...; auth TECHNICAL 50d964b7..., PRODUCT c5c0db50...,
  PLAN 136e5306...; Tutor REST-INCREMENT 8c1f07f2...; NOTE 5e8ba76f...; HANDOFF 1ee65fd9...; core
  `PostgresConsultationLifecycle` 35870e1c... (the `cancellable` parity input); V1 DDL 1cdea0be...;
  LifecyclePgFixture ede25ee8...; TutorRestPgTest d12de2d1...; compose.test.yaml 7a7216c0....
- Dependency condition (stu-c1 implemented, C1 tests green, implementer handoff at a recorded revision):
  met. stu-c1 has "Handoff implementer-1" (IN_REVIEW) and "Review reviewer-1" (PASS, LOW follow-ups only;
  Status READY_FOR_HUMAN). All 14 c1 main-source files and the 3 c1 test files match the hashes recorded
  there (CurrentActorProvider 69c190ee..., ApiSecurityConfiguration 2de6023d..., StudentAccess e3c395a9...,
  StudentReadRepository 068bde61..., PostgresStudentReadRepository 8e828ab7..., StudentReadService
  acb39b0d..., StudentController 3b207f01..., StudentApiInput aee190ad..., StudentApiConfiguration
  0590d345..., StudentExceptionHandler 0902a120..., StudentAccount 24a3cf08..., StudentAccessDeniedException
  4464963b..., ModuleOption 21f4d778..., BookableSlot 5e72d10e...; StudentTestActors 9e70e1ee...,
  StudentActivationTest 15d13758..., StudentBrowsePgTest 39206a32...), as do the six c1 evidence files.
  `git status` shows no other `src/` change apart from the untracked IDE folder `src/backend/bin/`.
  I reran the full suite at this working tree: exit 0, `test` 28/0/0, `postgresTest` 123/0/0
  (`stu-c3-my-bookings/evidence/baseline-test-writer-2026-10-22.txt`), so C1 is green now, not only historically.
- Analyst artifacts present for c3: acceptance scenarios (analyst-1) and refinements (C3-ELIG-1, C3-ACC-1
  clarified), coverage matrix, findings register (F2-01/06/19 resolved; F2-13/15 deferred as merge/activation
  gates by D6), scoped READY_FOR_TESTS verdict. A1-A4 are in PRODUCT §10; D2-D7 are in the addenda.
- Canonical sections read directly: PRODUCT §2, STU-BOOK-02, §7 S-LIST-1; TECH §2, §3 (StudentBooking,
  `/bookings`, `/bookings/{id}`, status, `cancellable`, 404 rule), §3.1, §3.2 (matrix and evaluation order),
  §3.3, §4 (student-bookings query); INTEGRATION §1, §2, §5. They agree with every C3 acceptance row; no
  requirement/technical conflict found. Note: core `changeBooking` additionally requires slot BOOKED for a
  cancel. TECH §3 defines `cancellable` without it, and in consistent data an ACTIVE booking's slot is BOOKED,
  so the C3 fixtures keep ACTIVE bookings on BOOKED slots and do not test that divergence.
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none.

## Acceptance refinements (analyst-2)
| ID | Given / When / Then |
| --- | --- |
| C3-ELIG-1 (new, F2-19) | Given S1 holds ACTIVE future bookings on three slots, then (a) the slot's tutor is deactivated, (b) the slot's module is deactivated, (c) the tutor's assignment to the module is removed / when S1 GETs `/bookings` and `/bookings/{id}` / then each booking is still returned with its stored status and `cancellable: true` (eligibility affects booking, not the student's own bookings). |
| C3-ACC-1 (clarified) | Applies the TECHNICAL-SPEC §3.2 matrix to both GET endpoints with valid input: no principal 401; principal not in `users` 401 + session invalidated; inactive student 403 `ACCOUNT_INACTIVE` + invalidated, then 401 on the same session; active tutor/admin (even with `ROLE_STUDENT`) 403 `FORBIDDEN`. No booking data in any denial body. |

Task mapping additions: C3-ELIG-1 → C3-T1.

## Requirement-to-technical coverage (analyst-2)
COVERED means specification support only. "PG" = Student MockMvc-to-PostgreSQL test (`postgresTest`).

| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| S-LIST-1 | STU-BOOK-02 bullets 1-2; §7 S-LIST-1 | TECH §3 GET `/bookings`, list ordering rule; §4 student-bookings query (`b.student_id = :actor`, order `s.start_time, b.id`); INTEGRATION §1 | COVERED | PG: two students, all statuses, ordering incl. equal-start tie |
| C3-OUT-1 | STU-BOOK-02 bullet 1; §2 bullet 4 | TECH §3 `StudentBooking`, exact-fields and no-email bullets; INTEGRATION §1 `Z` instants | COVERED | PG: exact key set, no `@`, no student name |
| C3-FLT-1 | STU-BOOK-02 bullet 2 (Upcoming/History, status filter) | TECH §3 `status` bullet; §3.1 repeated equal values harmless; INTEGRATION §1 uppercase enums | COVERED | PG |
| C3-FLT-2 | STU-BOOK-02 bullet 2 (date filter); §4 SGT | TECH §3 `date`, §4 SGT on `s.start_time`; SHARED SH-DATA-01 | COVERED | PG: 15:30Z/16:30Z boundary, intersection with status |
| C3-FLT-3 | STU-BOOK-02 bullet 3 (server clock) | TECH §3 `cancellable` = ACTIVE and `startTime > now` (injected clock); core cancel predicate `startTime.isAfter(now)` | COVERED | PG with moving fixture clock |
| C3-ELIG-1 | STU-BOOK-02 bullet 1 (all own bookings), STU-BOOK-03 bullet 1 (no eligibility condition for cancel) | TECH §4 query has no active/assignment filter; core `changeBooking` checks no eligibility | COVERED | PG |
| C3-OWN-1 | §2 bullet 2; STU-BOOK-02 bullet 1 | TECH §3 "another student's booking ID ... 404", §4 detail owner condition; INTEGRATION §4 non-disclosure precedent | COVERED | PG: identical bodies apart from requestId |
| C3-IN-1 | §2 bullet 2 | TECH §3.1 (`/bookings` params; no query on detail; UUID path); INTEGRATION §1 | COVERED | PG (Unit for the parser if separate) |
| C3-ACC-1 | §2 bullets 1, 3; shared AUTH-02, SEC-01 | TECH §3.2; SHARED SH-AUTH-04; INTEGRATION §2 | COVERED (real auth adapter UNVERIFIED, F2-15) | PG |
| Logging / cache | Student §4; shared SH-WEB-02 | TECH §3.3 closing paragraph; INTEGRATION §5 | COVERED | `no-store` asserted in PG helpers; logging by reviewer (F2-17) |

Out of scope (no coverage claimed): Upcoming/History tabs, empty-state UI, refresh UX,
keyboard (frontend; PLAN S4 React part).

## Spec findings and proposed fixes (analyst-2)
Applicable rows of the full register in [stu-c1](stu-c1-browse-slots.md) "Spec findings
and proposed fixes (analyst-2)"; evidence there.

| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| F2-01 / MEDIUM | PLAN S4 said "Depends on: S3" (booking) although c3 depends only on c1 and precedes c2. | PLAN S4 dependency text corrected. | Phuc | RESOLVED: PLAN 31545bfb |
| F2-06 / LOW | Validation-vs-access order unspecified (affects C3-IN-1 with denied actors). | TECH §3.2 evaluation order. | Phuc | RESOLVED: TECH e4ba47b0 |
| F2-19 / LOW | No scenario keeps own bookings visible after tutor/module ineligibility. | C3-ELIG-1. | Phuc | RESOLVED: refinements above |
| F2-13 / MEDIUM | Inherited: c3 uses the c1 actor seam and security rule pending Kok Seng/Huanghao review. | Merge gate on the Student PR. | Kok Seng, Huanghao | OPEN, DEFERRED (merge gate; not a test gate) |
| F2-15 / LOW | Inherited: simulated authentication only. | Activation gate. | Kok Seng | OPEN, DEFERRED |
| F2-17 / LOW | Inherited: logging checked by review. | Reviewer checklist. | Phuc | RESOLVED by plan |
| F2-18 / INFO | Base re-anchored to 459fd6e. | This manifest. | - | RESOLVED |

Audited without further findings for c3: owner scoping (TECH §4 `b.student_id = :actor`
on list and detail vs PRODUCT §2 and the 404 non-disclosure rule); `cancellable` parity
with the core cancel predicate; status enum spelling and repetition (TECH §3.1 vs
INTEGRATION §1); SGT filter on slot start; DTO privacy. F2-14 (error codes) does not
apply: c3 introduces no new code. No AI, write or concurrency path in c3 (read-only, no locks).

## Shared-owner boundary and dependency check (analyst-2)
- Files: `unichope/student/**` only (read repository query, service, controller).
  No `platform/*`, core, Tutor, migration or build change beyond what stu-c1 adds.
- Reads core tables read-only (D3). No lifecycle call.
- Hard dependency: stu-c1 C1-T1..T4 implemented (tests green). Downstream: stu-c2 needs
  C3-T1 (`StudentBooking` projection) and C3-T2 (`GET /bookings/{id}`, the `Location`
  target); stu-c4 needs C3-T1.

## Spec readiness (analyst-2)
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision: stu-c3 C3-T1, C3-T2 against PRODUCT-SPEC
ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957, TECHNICAL-SPEC
e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3, PLAN
31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, code at 459fd6e.
Remaining findings and why deferral does not affect this task: F2-13 and F2-15 as in
stu-c1 (merge/activation gates); no c3-specific open finding.
Required owner decisions and recorded outcome: none beyond A1-A4/D2-D5 (recorded).
Recheck evidence: each C3 ID re-traced to the revised TECH §3/§3.1/§3.2/§4.
Dependency conditions: test writing starts only after stu-c1 has an implementer
handoff with C1 tests green at a recorded revision; if stu-c1 returns to the analyst
or its contracts change, this verdict must be rechecked.
Next agent, task, acceptance IDs, allowed files and prerequisites: fresh test writer
(after stu-c1 implementation); C3-T1, C3-T2; IDs above; Student test scope; compose
PostgreSQL.

## Handoff test-writer-1
Producer role / actual tool and model: Test writer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Recipient role: Implementer, fresh session, `workflow/student/agents/implementer.md`.
Change ID / task / exact requested next action: stu-c3-my-bookings / C3-T1, C3-T2 / implement the
`StudentBooking` projection, the owner-scoped bookings read query with status/date filters and
server-computed `cancellable`, and the `GET /api/student/bookings` and `GET /api/student/bookings/{id}`
routes with strict input parsing, so that every test below passes. Do not change the tests. Return a test
you believe is wrong to the test writer, with evidence.
Stage status / supersedes: READY_FOR_IMPLEMENTATION; first test-writer revision. Supersedes the
analyst-1 "Tasks and acceptance-to-test mapping" placeholders (state "pending") for C3-T1/T2.
Goal and non-goals: as in analyst-1/analyst-2 (own bookings list and detail; no other students' data,
tutor notes, frontend tabs, booking or cancellation).
Acceptance IDs: S-LIST-1, C3-OUT-1, C3-FLT-1..3, C3-ELIG-1, C3-OWN-1, C3-IN-1, C3-ACC-1 (the analyst-2
list; nothing added or dropped).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| This record: Handoff analyst-2, Acceptance scenarios + refinements (analyst-2), coverage, findings, Spec readiness (analyst-2) | Input | (this file) | Gate and acceptance oracles |
| All analyst-2 / stu-c1 analyst-2 canonical inputs (Student PRODUCT/TECHNICAL/PLAN, shared specs, auth specs, Tutor REST spec, V1 DDL, core lifecycle, LifecyclePgFixture, TutorRestPgTest, compose.test.yaml) | Input | re-verified identical to analyst-2 (see the analyst-2 receiver receipt above) | Canonical baseline |
| stu-c1 main sources (14 files) and tests (3 files) | Input (prerequisite, unchanged) | identical to stu-c1 Handoff implementer-1 / Review reviewer-1 hashes (receipt above) | Foundation reused by C3 |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBookingsPgTest.java | Output (`postgresTest`, tag `postgres`) | a38b995071d69aa06955c24dec3dac481d08d71fab89f0bdb11235d8647fbb67 | All C3 IDs, MockMvc + Spring Security + real PostgreSQL |
| workflow/student/changes/stu-c3-my-bookings/evidence/baseline-test-writer-2026-10-22.txt | Output (evidence) | cab156304e34d97c784b62e79a92e8d30d79eb9e1a117e685293ec477476be98 | Existing-suite rerun before any C3 edit |
| workflow/student/changes/stu-c3-my-bookings/evidence/red-run-test-writer-2026-10-22.txt | Output (evidence) | 1761ea2fccb0e95d70532001bb248c6392e1eadf222226f668a6a7a12436c472 | Pre-implementation red run + test-validity check |

Base commit / result commit / diff: base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` plus the uncommitted,
unchanged stu-c1 working tree; no commit. This stage added exactly one file under `src/` (the untracked
test class above). No main source, no existing test (including c1's three test files) and no build file changed.
Uncommitted files and their hashes: the test class and two evidence files above, plus this record
(receiver receipt under Handoff analyst-2, the Status line and this section).
Allowed write scope (used): one new Student PostgreSQL MockMvc class in
`src/backend/src/test/java/unichope/platform/core/postgres/`, so that it can reuse the package-private
`LifecyclePgFixture`. Interpretation note: analyst-2 says "the one Student PostgreSQL MockMvc class; c3 tests
extend it". Adding the cases to `StudentBrowsePgTest` would change a reviewed c1 test file and invalidate
the stu-c1 review hash, which this session was told not to do. A separate class with the same fixture
and discovery contract is the minimal equivalent. No test-only support file was needed.
`StudentTestActors` is reused unchanged.
Allowed write scope (implementer, from analyst-2): `unichope/student/**` only. No `platform/*`, core, Tutor,
migration or build change.
Prerequisites / dependency state: stu-c1 implemented, reviewed PASS, and green at this working tree (baseline
below). Compose PostgreSQL 17 and Docker (29.9.2) were used and stopped afterwards.
Recorded decisions and authorization references: D2-D7 and A1-A4 as cited in analyst-2 and the addenda.
No new decision was made in this stage.

### Test design (why the red is behavioral)
Same contract as stu-c1 test-writer-1: Student code is discovered by component scan of `unichope.student`
with profile `student-api` (manual web context: `DelegatingWebMvcConfiguration` + `ApiSecurityConfiguration`
+ scan), and the actor seam through `StudentTestActors`. The test has no compile-time reference to any C3
type. Before implementation, every C3 request is answered by the c1 Student resolver as 404 `NOT_FOUND`,
so each red is an HTTP status assertion. Every test starts with, or contains, a request that requires the
new behavior. A test therefore cannot pass on the current 404, including C3-OWN-1, whose 404 cases come after
own-booking 200 checks.
**Beans supplied to Student wiring (unchanged from stu-c1):** `DataSource` (fixture schema), `Clock`
(fixture `MovingClock`, N = 2026-10-21T00:00Z, moved by C3-FLT-3), `ConsultationLifecycle`, core
User/Module/Slot/Booking repositories, test-only `CurrentActorProvider`. No `JdbcClient` bean. If C3 wiring
needs any other bean, return the issue to the test writer.
Fixture convention: ACTIVE bookings sit on BOOKED slots, COMPLETED on COMPLETED, CANCELLED on AVAILABLE or
re-booked slots, as the lifecycle leaves them. Display names are distinct ("Tutor One Display",
"Student Alpha Name") so the privacy oracles can tell them apart.

### Tasks and acceptance-to-test mapping (test-writer-1)
Class `PG` = `unichope.platform.core.postgres.StudentBookingsPgTest`. All 9 cases were observed red at this revision.

| Task | Acceptance ID | Test case | Pre-implementation state |
| --- | --- | --- | --- |
| C3-T1 | S-LIST-1 | PG `sList1OnlyOwnBookingsAllStatusesOrderedBySlotStartThenBookingId`: S1/S2 interleaved, ACTIVE future and started, CANCELLED (incl. on a slot S2 re-booked), COMPLETED past; equal-start tie ordered by booking id (slot-id, module-code, tutor-name and insertion order all point the other way); `status` and `cancellable` per row; S2 sees only S2's; repeat S1 identical; a student without bookings gets `[]` | Red (404 vs 200) |
| C3-T1 | C3-OUT-1 | PG `c3Out1StudentBookingHasExactSafeFields`: exact 10 keys on list and detail, values (slotId, module, tutorName = users.name), `Z` instants equal to stored, `status` string, `cancellable` JSON boolean; detail equals list item; no `@`, student name, student id or tutor id in either body | Red |
| C3-T1 | C3-FLT-1 | PG `c3Flt1StatusFilterSelectsUpcomingHistoryAndAll`: ACTIVE; CANCELLED+COMPLETED (both param orders); ACTIVE twice equals ACTIVE (byte-identical body); each single history status; none = all three = all | Red |
| C3-T1 | C3-FLT-2 | PG `c3Flt2SgtStartDateFilterAcrossBoundaryIntersectsStatus`: 15:30Z/16:30Z boundary, 23:59:59 SGT included, 00:00 SGT next day excluded, a past date, intersection with ACTIVE / history / COMPLETED, unmatched `[]`; another student's same-day booking excluded | Red |
| C3-T1 | C3-FLT-3 | PG `c3Flt3CancellableFollowsInjectedClockStrictlyBeforeStart`: ACTIVE future true; started ACTIVE, CANCELLED and COMPLETED false; clock at start-1ms true; at start false (status still ACTIVE); start+1s false; list and detail | Red |
| C3-T1 | C3-ELIG-1 | PG `c3Elig1OwnBookingsStayVisibleAfterTutorOrModuleIneligibility`: tutor deactivated, module deactivated, assignment removed; all three listed (also under `status=ACTIVE`), detail 200, ACTIVE, `cancellable: true` | Red |
| C3-T2 | C3-OWN-1 | PG `c3Own1DetailReturnsOwnBookingInAnyStatusAndHidesOthersAsNotFound`: own ACTIVE/CANCELLED/COMPLETED detail 200 equal to list item; S2 can read its own (ID is real); S2's ACTIVE and COMPLETED IDs and a random UUID give 404 `NOT_FOUND` with the same code and message and no foreign data; S1 session stays valid | Red |
| C3-T2 | C3-IN-1 | PG `c3In1BookingsRejectsUnknownInvalidRepeatedAndMalformedInput`: unknown/spoofing params (`x`, `actorId`, `studentId`), status `active`/`PENDING`/`Active`/empty/`ACTIVE,CANCELLED`/mixed valid+invalid, repeated date (equal and different), bad dates (`2026-02-30`, `2026-1-01`, empty, with time), bare `?flag`; detail with non-UUID, `1-1-1-1-1`, hyphenless UUID, any query: all 400 `VALIDATION_FAILED`; valid controls 200; TECH §3.2 order: anonymous + bad input 401, deleted/inactive actor + bad input 400 without invalidation, then 403 with invalidation | Red (404 vs 400) |
| C3-T2 | C3-ACC-1 | PG `c3Acc1AccessMatrixOnListAndDetail`: both endpoints; no principal 401; provider without actor 401; deleted actor 401 + invalidated, then 401; inactive student 403 `ACCOUNT_INACTIVE` + invalidated, then 401 (on S2's own booking); tutor/admin with `ROLE_STUDENT` 403 `FORBIDDEN`; student with `ROLE_TUTOR` 200; no booking/slot ID, module code or tutor name in any denial body | Red (anonymous half already passes) |
| C3-T1/T2 | Regression | Full suite: existing 28 unit + 123 PostgreSQL tests (incl. stu-c1 StudentBrowsePgTest 20, StudentActivationTest 2) must stay green | Green now; recheck after implementation |

Every response asserted by the helpers also checks `Cache-Control: no-store`, and every error checks the exact
`{code,message,requestId}` envelope, JSON content type, and that no email or SQL appears (TECH §3.3, the
"Logging / cache" coverage row).
Manual / real-provider checks: none needed for the C3 backend; AI not applicable. Frontend Upcoming/History tabs,
empty state, refresh and keyboard remain out of scope (no frontend scaffold), as analyst-2 recorded.

### Verification evidence (test-writer-1)
All runs from `src/backend`, compose PostgreSQL up (`docker compose -f compose.test.yaml up -d --wait`, exit 0),
local placeholder credentials from compose.test.yaml.
1. Existing-suite baseline before any C3 edit:
   `./gradlew clean test postgresTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<placeholder> --console=plain`
   Exit 0. `test` 28 tests, 0 failures, 0 errors, 0 skipped; `postgresTest` 123/0/0/0. Evidence:
   `evidence/baseline-test-writer-2026-10-22.txt`. A first run without `clean` was entirely UP-TO-DATE
   (nothing executed), so it is not counted.
2. `./gradlew compileTestJava --console=plain`: exit 0, no `-Xlint:all` warnings from test sources.
3. Red run: the same command as 1 plus `--continue`. Exit 1 (expected). `test` 28/0/0/0; `postgresTest`
   132 tests, 9 failures, 0 errors, 0 skipped. All 9 failures are the new StudentBookingsPgTest cases, each an
   `AssertionFailedError` such as `expected: <200> but was: <404>` (`NoHandlerFoundException: No endpoint GET
   /api/student/bookings`), `expected: <400> but was: <404>`, `expected: <401> but was: <404>`. The existing
   28 + 123 tests pass. Evidence: `evidence/red-run-test-writer-2026-10-22.txt`.
4. Test-validity check (not implementation evidence): in a disposable scratch copy (deleted afterwards), a
   minimal spec-following prototype made all 9 C3 tests and the 20 C1 PostgreSQL tests pass (exit 0). Five
   seeded defects were each caught by the intended test: slot-id tiebreak, `>=` start, detail without the owner
   condition, eligibility filters copied from `/slots`, and a UTC date range. Details are in the red-run evidence.
5. `docker compose -f compose.test.yaml down`: exit 0.
Implementer commands: the red-run command (without `--continue` for the final run, and with `bootJar` as in
stu-c1). Focused: `./gradlew postgresTest --tests 'unichope.platform.core.postgres.StudentBookingsPgTest'` with
the same `-Dcore.test.*` properties. `test` excludes the `postgres` tag, so this class runs only under `postgresTest`.

### Notes for the implementer
- I1: the tests constrain no C3 class names, only routes, JSON shape, discoverability (component scan under
  `unichope.student`, profile `student-api`) and the supplied-bean list. TECH §2 proposes `StudentBooking` in
  `student/` and the query in `StudentReadRepository`/`StudentReadService`; stu-c2/c4 will reuse that projection.
- I2: TECH §3.2 order applies to the detail route too. Parse the path UUID and reject any query **before** the
  actor check. A non-UUID path must be 400 `VALIDATION_FAILED`, not a framework 404 or a type-mismatch 500.
- I3: `cancellable` = `status == ACTIVE && slot.startTime > clock.instant()` from the injected `Clock`
  (strict). The test moves the clock between requests, so read the clock per request.
- I4: no eligibility (active tutor/module, assignment) condition in the bookings query (C3-ELIG-1).
- I5: a booking ID that the actor does not own (another student's or nonexistent) answers 404 `NOT_FOUND`
  through the Student error envelope, with the same code and message text in both cases.
- I6: the status filter uses a repeated query parameter. `status=ACTIVE,CANCELLED` (comma form) is
  invalid (400). Only repeated `status` parameters are specified.

### Test design notes and gaps
Spec gaps returned to the analyst: none for C3. No acceptance row needed invention. Derived oracles (not new
behavior): the hyphenless-UUID and `?flag` rejections and the comma-joined status come from TECH §3.1 (canonical
UUID; each status value one of three enums). The syntax-before-actor checks come from TECH §3.2 evaluation order,
which F2-06 says affects C3-IN-1. The empty `[]` case comes from TECH §3 ("`[]` when empty") and STU-BOOK-02.
- N1 (deliberately not asserted, spec silent): whether 403 `FORBIDDEN` or provider-empty 401 invalidates the
  session (as in stu-c1 note N2 / R1-04), message wording, and the core divergence that cancel also requires
  slot BOOKED (not reachable with consistent data; noted in the receipt).
- N2 (routed to the owner/analyst, not taken into this scope): stu-c1 Review reviewer-1 proposed carrying R1-01
  (bare `/api/student` without trailing slash returns a non-JSON 404) and R1-02 (vacuous `X-Request-Id`
  assertion in `StudentBrowsePgTest`) into stu-c3's test-writer scope. Neither is a C3 acceptance ID in
  analyst-2. R1-02 requires editing a c1 test file, which this session was instructed not to do. So neither
  was implemented here. Each needs an analyst or owner decision to add it to a change (for example a c1
  follow-up revision) with its own acceptance row.
- N3: F2-13 (teammate review of the c1 seam and security rule) is still open as a merge gate. A change there
  could return the access cases of this class to the test writer.
- N4: these tests have passed only against a throwaway prototype, never against repository code. An
  implementer who believes a case is wrong returns it to the test writer with evidence instead of editing it.

Readiness verdict: READY_FOR_IMPLEMENTATION for C3-T1, C3-T2. The gate is current (analyst-2, hashes
re-verified, stu-c1 dependency met and green), every acceptance ID maps to an executable test, the baseline and
behavioral red results are recorded, and setup prerequisites are documented.

### Receiver receipt
Receiver role / actual tool and model: Implementer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Checked task, artifact revisions and required evidence (22 October 2026), before any product-code change:
- Task C3-T1, C3-T2 only. HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` equals the analyst-2 and
  test-writer-1 base. `git status` under `src/` shows only the stu-c1 working tree (modified
  `ApiSecurityConfiguration.java`, untracked `platform/auth/`, `student/`, the c1 tests), the untracked
  `StudentBookingsPgTest.java` and the untracked IDE folder `src/backend/bin/` (not part of any change).
- `sha256sum` matches the recorded hashes: Student PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., PLAN 31545bfb...;
  shared PRODUCT d1d5a414..., SHARED-TECHNICAL 54ec090a..., INTEGRATION ac0d183f...; NOTE 5e8ba76f...,
  HANDOFF 1ee65fd9...; compose.test.yaml 7a7216c0...; core `PostgresConsultationLifecycle` 35870e1c...;
  LifecyclePgFixture ede25ee8...; c3 test `StudentBookingsPgTest` a38b9950... (test-writer-1); c3 evidence
  baseline cab15630... and red-run 1761ea2f...; c1 tests StudentTestActors 9e70e1ee..., StudentActivationTest
  15d13758..., StudentBrowsePgTest 39206a32...; all 14 c1 main-source files equal the stu-c1 implementer-1 /
  reviewer-1 hashes (CurrentActorProvider 69c190ee..., ApiSecurityConfiguration 2de6023d..., StudentAccess
  e3c395a9..., StudentReadRepository 068bde61..., PostgresStudentReadRepository 8e828ab7..., StudentReadService
  acb39b0d..., StudentController 3b207f01..., StudentApiInput aee190ad..., StudentApiConfiguration 0590d345...,
  StudentExceptionHandler 0902a120..., StudentAccount 24a3cf08..., StudentAccessDeniedException 4464963b...,
  ModuleOption 21f4d778..., BookableSlot 5e72d10e...).
- Gates present and current: analyst-2 READY_FOR_TESTS for C3-T1/T2 (coverage matrix, findings with
  resolutions; F2-13/F2-15 deferred as merge/activation gates by D6), its stu-c1 dependency condition met
  (stu-c1 implementer-1 IN_REVIEW and reviewer-1 PASS at the hashes above); test-writer-1
  READY_FOR_IMPLEMENTATION with mapping, baseline and behavioral red evidence. A1-A4 in PRODUCT §10; D5-D7 in addenda.
- Canonical sections read directly: PRODUCT §2, STU-BOOK-02, §7 S-LIST-1; TECH §2, §3 (StudentBooking, GET
  `/bookings`, `/bookings/{id}`, `status`, `cancellable`, 404 rule), §3.1, §3.2 (matrix and evaluation order),
  §3.3, §4 (student-bookings query). I read every case of `StudentBookingsPgTest`; each expresses the agreed
  behavior. No contradiction between specs and tests found; no test returned.
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none.

## Handoff implementer-1
Producer role / actual tool and model: Implementer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Recipient role: Reviewer / security checker, fresh session, `workflow/student/agents/reviewer-security.md`.
Change ID / task / exact requested next action: stu-c3-my-bookings / C3-T1, C3-T2 / independently review the
spec, the C3 tests and the main-source diff below, rerun the verification command, and record PASS,
CHANGES_REQUIRED or BLOCKED with findings.
Stage status / supersedes: IN_REVIEW; first implementer revision. Supersedes the "pending" placeholders in
the analyst-1 "Verification evidence" section for this revision only.
Goal and non-goals: as in analyst-1/analyst-2. Implemented: the `StudentBooking` projection, the owner-scoped
bookings read query with status/date filters, server-computed `cancellable`, and `GET /api/student/bookings`
and `GET /api/student/bookings/{id}` with strict input parsing. Not implemented (non-goals): booking creation
(c2), cancellation (c4), agenda, frontend tabs, real login/session, production activation, c1 review
follow-ups R1-01..R1-05 (no C3 test requires them).
Acceptance IDs: S-LIST-1, C3-OUT-1, C3-FLT-1..3, C3-ELIG-1, C3-OWN-1, C3-IN-1, C3-ACC-1 (unchanged).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| Canonical specs, c1 sources/tests and c3 evidence listed in the receiver receipt under Handoff test-writer-1 | Input | hashes re-verified identical (that receipt) | Baseline |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBookingsPgTest.java | Input (test, unchanged) | a38b995071d69aa06955c24dec3dac481d08d71fab89f0bdb11235d8647fbb67 | All C3 IDs |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBrowsePgTest.java; src/backend/src/test/java/unichope/student/StudentTestActors.java; src/backend/src/test/java/unichope/student/http/StudentActivationTest.java | Input (c1 tests, unchanged) | 39206a32..., 9e70e1ee..., 15d13758... (as stu-c1) | Regression |
| src/backend/src/main/java/unichope/student/StudentBooking.java (new) | Output | 30a4d4e18d317ce16f15645c718a772bda078add24ffe7fae42250f89a72cbe4 | C3-T1 DTO (10 fields) |
| src/backend/src/main/java/unichope/student/StudentReadRepository.java (modified) | Output | d6d85df49932d340df86264ddf7bf3415b023ecbea1c09e0ba97a92bbda6aa6c (was 068bde61...) | C3-T1 read boundary: `findStudentBookings`, `findStudentBooking`, `BookingRow` |
| src/backend/src/main/java/unichope/student/PostgresStudentReadRepository.java (modified) | Output | fe91a985c1688ca146e5d15f37b7d7519e970d1b21a39190346e45274109020c (was 8e828ab7...) | C3-T1 parameterized SQL |
| src/backend/src/main/java/unichope/student/StudentReadService.java (modified) | Output | f5cbde329e1ef40c8a4cb8ed6e25b5a83ac9f1516f070bbb0ee09c51fbfa0db7 (was acb39b0d...) | C3-T1 `myBookings`, `myBooking`, `cancellable`, SGT range |
| src/backend/src/main/java/unichope/student/http/StudentApiInput.java (modified) | Output | 7798443e36b77b9bdb41d0dcacec3ea6fd8f4f0ff8358d53f13f7b1cc8e7ce32 (was aee190ad...) | C3-T2 `bookingFilter` |
| src/backend/src/main/java/unichope/student/http/StudentController.java (modified) | Output | b1b35cb6213667cd190b442e7612c5b8082d2209da6ec8ff87707561293919c4 (was 3b207f01...) | C3-T2 routes |
| workflow/student/changes/stu-c3-my-bookings/evidence/implementer-1-c3-increment.patch | Output (diff) | dd9e02de1fdc9f8314e9c93bb87b44ad975d6abc1572621467b6c08697e7b716 | C3-only diff: reviewed stu-c1 Student tree to this revision (6 files); checked to reproduce the current tree with `git apply` on a copy of the c1 files |
| workflow/student/changes/stu-c3-my-bookings/evidence/implementer-1-main-source-vs-HEAD.patch | Output (diff) | 3ce23686af8fdecda5a4e4ec15d0b3d91768c556addf66c9f56511c52d128fb5 | Complete main-source diff vs HEAD 459fd6e, c1 + c3 (15 files; untracked files included via intent-to-add, index restored afterwards) |
| workflow/student/changes/stu-c3-my-bookings/evidence/final-run-implementer-2026-10-22.txt | Output (evidence) | 149a8cb4f7f4ede4095462c0dc61e5caac201c8c1c4a1fdfe18901ed9d540da7 | Sanitized final verification output |

Unchanged c1 main files (same hashes as stu-c1 reviewer-1): CurrentActorProvider 69c190ee..., ApiSecurityConfiguration
2de6023d..., StudentAccess e3c395a9..., StudentAccount 24a3cf08..., StudentAccessDeniedException 4464963b...,
ModuleOption 21f4d778..., BookableSlot 5e72d10e..., StudentApiConfiguration 0590d345..., StudentExceptionHandler 0902a120....

Base commit / result commit / diff: base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` plus the uncommitted stu-c1
working tree; no commit (not authorized). The C3 change is the six files above, all under
`src/backend/src/main/java/unichope/student/**`. `git diff --stat -- src/` still lists only
ApiSecurityConfiguration.java (+2, from c1). `git diff --quiet HEAD` holds for `unichope/tutor`, `platform/core`,
`build.gradle`, `src/main/resources` and `src/test`. No test file was edited (hashes above).
Note: the untracked `src/backend/bin/` (IDE output) pre-existed and is not part of the change.
Uncommitted files and their hashes: as in the table, plus this record (receiver receipt, Status line, this section).
Allowed write scope (used): `unichope/student/**` only; this record and the evidence files.
Prerequisites / dependency state: stu-c1 at the reviewed hashes; compose PostgreSQL 17 (Docker 29.9.2).
Recorded decisions and authorization references: D2-D7, A1-A4 as cited in analyst-2 and the addenda. No new decision.

### Implementation summary
- C3-T1: `StudentBooking` record with exactly the 10 TECH §3 fields (`status` is the core `BookingStatus`
  enum, serialized as its uppercase name). `PostgresStudentReadRepository.findStudentBookings` implements the
  TECH §4 student-bookings query: `bookings b JOIN slots s JOIN modules m JOIN users t WHERE b.student_id =
  :studentId`, optional `b.status IN (:statuses)` (bound collection) and the half-open SGT start range on
  `s.start_time`, `ORDER BY s.start_time, b.id`. No eligibility condition (C3-ELIG-1), no locks, no writes.
  `findStudentBooking` adds `b.id = :bookingId` to the same owner condition. Only fixed SQL fragments are appended.
  `StudentReadService.myBookings` / `myBooking` call `StudentAccess.requireActiveStudent()` first, read
  `clock.instant()` once per request and compute `cancellable = status == ACTIVE && startTime.isAfter(now)`.
  The SGT-day conversion is shared with `/slots` (moved to a private helper; behavior unchanged).
- C3-T2: `StudentController` `GET /bookings` parses `StudentApiInput.bookingFilter` (only `status`, repeatable,
  each exactly ACTIVE/CANCELLED/COMPLETED, duplicates collapse; `date` at most once, strict format) before
  the service call. `GET /bookings/{id}` binds the path as `String`, rejects any query (`noQuery`) and parses the
  canonical UUID before the actor check (TECH §3.2 order), so malformed IDs give 400 `VALIDATION_FAILED`, never a
  type-mismatch. An empty result maps to `ApiFailure(404, "NOT_FOUND", "Resource not found")`, so another
  student's booking and a nonexistent ID give the same code and message. Access denials reuse the c1
  `StudentExceptionHandler` mapping (unchanged).

### Acceptance-to-code-and-test mapping (implementer-1)
`PG` = `unichope.platform.core.postgres.StudentBookingsPgTest` (`postgresTest`). All passed in the final run.

| Task | Acceptance ID | Code | Test | Result |
| --- | --- | --- | --- | --- |
| C3-T1 | S-LIST-1 | Repository owner condition `b.student_id = :studentId`, `ORDER BY s.start_time, b.id`; service per-row `cancellable` | PG sList1OnlyOwnBookingsAllStatusesOrderedBySlotStartThenBookingId | PASS |
| C3-T1 | C3-OUT-1 | `StudentBooking` record (10 fields, `Instant` serialized with `Z`), `t.name AS tutor_name`; list and detail share `project(...)` | PG c3Out1StudentBookingHasExactSafeFields | PASS |
| C3-T1 | C3-FLT-1 | `StudentApiInput.bookingFilter` (EnumSet, duplicates harmless); repository `b.status IN (:statuses)`, omitted when empty | PG c3Flt1StatusFilterSelectsUpcomingHistoryAndAll | PASS |
| C3-T1 | C3-FLT-2 | `StudentReadService.sgtDay` (Asia/Singapore half-open range); repository `START_RANGE_FILTER` on `s.start_time` | PG c3Flt2SgtStartDateFilterAcrossBoundaryIntersectsStatus | PASS |
| C3-T1 | C3-FLT-3 | `project`: ACTIVE and `startTime.isAfter(clock.instant())`, clock read per request | PG c3Flt3CancellableFollowsInjectedClockStrictlyBeforeStart | PASS |
| C3-T1 | C3-ELIG-1 | Bookings query has no active/assignment predicate | PG c3Elig1OwnBookingsStayVisibleAfterTutorOrModuleIneligibility | PASS |
| C3-T2 | C3-OWN-1 | `findStudentBooking` owner condition; controller maps empty to one fixed 404 | PG c3Own1DetailReturnsOwnBookingInAnyStatusAndHidesOthersAsNotFound | PASS |
| C3-T2 | C3-IN-1 | `bookingFilter`, `noQuery`, `id` (canonical UUID), all before `requireActiveStudent` | PG c3In1BookingsRejectsUnknownInvalidRepeatedAndMalformedInput | PASS |
| C3-T2 | C3-ACC-1 | Service calls `StudentAccess.requireActiveStudent()` before any read; c1 handler maps reasons and invalidates | PG c3Acc1AccessMatrixOnListAndDetail | PASS |
| C3-T1/T2 | Regression | No change to c1 behavior | Full suite incl. StudentBrowsePgTest 20, StudentActivationTest 2, TutorRestPgTest 31 | PASS |

Resolved spec findings preserved: F2-06 (syntax before actor, both routes), F2-19 (C3-ELIG-1, no eligibility
filter), F2-17 (no logging added; 404 and 400 bodies are fixed text), F2-01 (c3 depends only on c1).

### Verification evidence (implementer-1)
Working directory `src/backend`, base 459fd6e plus the uncommitted c1 + c3 working tree.
1. `docker compose -f compose.test.yaml up -d --wait`: exit 0 (container healthy).
2. Focused: `./gradlew postgresTest --tests 'unichope.platform.core.postgres.StudentBookingsPgTest' -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<local placeholder> --console=plain`:
   exit 0; JUnit XML 9 tests, 0 failures, 0 errors, 0 skipped (first run after the edits).
3. Final: `./gradlew clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<local placeholder> --console=plain`:
   exit 0, BUILD SUCCESSFUL. JUnit XML: `test` 28 tests, 0 failures, 0 errors, 0 skipped; `postgresTest` 132 tests,
   0 failures, 0 errors, 0 skipped (StudentBookingsPgTest 9, StudentBrowsePgTest 20, TutorRestPgTest 31);
   `bootJar` built `build/libs/unichope-0.0.1-SNAPSHOT.jar`. The only compiler warning (`[serial]` in
   `platform/ai/AiGateway.java`) pre-exists and is outside this change. Evidence: `evidence/final-run-implementer-2026-10-22.txt`.
4. `docker compose -f compose.test.yaml down`: exit 0.
Compared with the test-writer red run (`postgresTest` 132 / 9 failures), all 9 C3 tests now pass and the existing
28 + 123 tests stay green.

### Deviations, limitations and pending checks
- No deviation from TECH §3/§3.1/§3.2/§4. Class names follow TECH §2 (`StudentBooking` in `student/`, query in
  `StudentReadRepository`, `cancellable` in `StudentReadService`). An internal `StudentReadRepository.BookingRow`
  record carries the stored columns so that `cancellable` is decided by the service, as TECH §2 assigns it; stu-c2
  and stu-c4 can reuse `StudentReadService.myBooking` or the repository for their response projection (TECH §4).
- 404 message text is "Resource not found", the same as the c1 unknown-route 404. The spec fixes only the code;
  the test requires the same message for foreign and nonexistent IDs, which holds by construction.
- `cancellable` follows TECH §3 (ACTIVE and start after now) and does not also check slot BOOKED as core
  `changeBooking` does; with consistent data an ACTIVE booking's slot is BOOKED (test-writer receipt note). Not
  tested and not changed here; the reviewer may decide whether the analyst should state it explicitly.
- Nothing returned to the test writer or the spec analyst: no test found wrong, no requirement gap. No test was
  edited, skipped or relaxed. c1 follow-ups R1-01..R1-05 were not addressed (no C3 test requires them; R1-01 and
  R1-02 still await an owner/analyst decision per test-writer note N2).
- Pending: independent review and security check (fresh session); F2-13 teammate review of `platform/auth`
  (Kok Seng) and the `platform/web` matcher (Huanghao) before merge to `main` (D6); F2-15 real auth adapter
  (activation gate); human API walkthrough of `GET /api/student/bookings` and `/bookings/{id}`; frontend
  Upcoming/History tabs remain out of scope. Real login/session behavior is unverified (simulated authentication only).
Readiness or review verdict: IN_REVIEW. No independent review is claimed.

### Receiver receipt
Receiver role / actual tool and model: Reviewer / security checker / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session that produced none of the reviewed work, no inherited conversation (D5). `logs/` was not
read, searched or written.
Checked task, artifact revisions and required evidence (22 October 2026). Per HANDOFF "Receiver procedure", I derived
my checklist from the request and canonical specs and inspected code and tests before reading this record's upstream
sections:
- Task C3-T1, C3-T2. HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5`. `git status` under `src/`: only the c1 + c3 working
  tree and the untracked IDE folder `src/backend/bin/` (ignored, not part of the change).
- Specs (`sha256sum`): Student PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., equal to analyst-2. Hence the readiness
  verdict and the test-writer/implementer gates still refer to the current spec revision.
- All six c3 main-source files and the c3 test match the implementer-1 table: StudentBooking 30a4d4e1...,
  StudentReadRepository d6d85df4..., PostgresStudentReadRepository fe91a985..., StudentReadService f5cbde32...,
  StudentApiInput 7798443e..., StudentController b1b35cb6..., StudentBookingsPgTest a38b9950.... The nine
  unchanged c1 main files, including ApiSecurityConfiguration 2de6023d..., and the three c1 test files match the
  stu-c1 reviewer-1 hashes. Patch `implementer-1-c3-increment.patch` dd9e02de...: `git apply -R --check` succeeds on
  the working tree. Its post-image blob IDs (5031ba7, e0783d0, 15275b7, d454b09, 292585c) equal `git hash-object` of
  the current files. Reverse-applying it on a scratch copy reproduces the c1-reviewed files byte for byte after LF
  normalisation (8e828ab7..., 068bde61..., acb39b0d..., aee190ad..., 3b207f01...). So the patch is the complete
  c1-to-c3 increment.
- Evidence files present: baseline, red-run and final-run (implementer) evidence, and both patches.
- I reran the full verification myself (see "Review reviewer-1").
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none.

## Review reviewer-1
Reviewer role / actual tool and model: Independent reviewer and security checker, `workflow/student/agents/reviewer-security.md`
/ Claude Code, Opus 5.5 (`claude-opus-5-5`); fresh subagent session, none of the reviewed work produced in it.
Reviewed scope: stu-c3 C3-T1, C3-T2. This covers the c3 main-source increment (6 files under
`src/backend/src/main/java/unichope/student/**`, patch dd9e02de...) and the c3 test `StudentBookingsPgTest` (a38b9950...).
It also checks for regression of the reviewed stu-c1 behaviour. `src/backend/bin/` was ignored.
Reviewed revision: HEAD 459fd6e plus the uncommitted working tree at the SHA-256 hashes in the receipt above; specs
PRODUCT ebc3d509..., TECHNICAL e4ba47b0....
Canonical sources read directly: Student PRODUCT STU-BOOK-02, §7 S-LIST-1; TECHNICAL §2, §3, §3.1, §3.2 (matrix and
evaluation order), §3.3, §4; INTEGRATION §1 (UUIDs, `Z` instants, SGT dates, uppercase enums, unknown params 400,
non-cacheable) and §2 (401/403/invalidate rules); SHARED SH-AUTH-04, SH-DATA-01; core
`PostgresConsultationLifecycle` (`changeBooking`, slot transitions) and V1 DDL (`timestamptz`, status checks).

### Checks run
1. Full rerun from `src/backend`. Ran `docker compose -f compose.test.yaml up -d --wait` (exit 0), then
   `./gradlew clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<local placeholder> --console=plain`
   (exit 0, BUILD SUCCESSFUL), then `docker compose -f compose.test.yaml down` (exit 0). JUnit XML: `test` 28/0/0/0;
   `postgresTest` 132/0/0/0, including StudentBookingsPgTest 9/0/0/0 and StudentBrowsePgTest 20/0/0/0, with
   StudentActivationTest 2/0/0/0 under `test`. Only warning: pre-existing `[serial]` in `platform/ai/AiGateway.java`.
   Evidence: `stu-c3-my-bookings/evidence/review-run-reviewer-1-2026-10-22.txt` (SHA-256 88d2c385...).
2. Patch/tree consistency and c1 pre-image reconstruction (receipt above).
3. Static inspection of every c3 file and the c1 files the c3 routes depend on: StudentAccess, StudentExceptionHandler,
   StudentApiConfiguration, ApiSecurityConfiguration, CurrentActorProvider. I also inspected the full c3 test and the
   c1 access tests.
4. Scope and boundary searches over `unichope/student` and `platform/auth`. There is no INSERT/UPDATE/DELETE, no
   Post/Put/Patch/Delete mapping, no lifecycle call, no email column and no logging. No `unichope.*` import outside
   `platform/*` and `student/*`, and no platform file imports `unichope.student`.
Not run / not available: real login/session adapter (F2-15, simulated authentication only); container (non-MockMvc)
error dispatch; dependency advisory scan (no dependency or build change in c3, so not applicable); frontend.

### Independent checklist and results
| Check | Source | Result |
| --- | --- | --- |
| Owner scoping on list and detail | TECH §3, §4; PRODUCT STU-BOOK-02 | Met. Both queries share `WHERE b.student_id = :studentId` with the actor from `StudentAccess` (`PostgresStudentReadRepository.java:36-44, 94-115`). Detail only adds `AND b.id = :bookingId`. No request input selects the student. Test: S-LIST-1 (interleaved S1/S2, repeat), C3-OWN-1. |
| Foreign vs missing non-disclosure | TECH §3 404 rule | Met. Both cases give an empty Optional, then one fixed `ApiFailure(404, NOT_FOUND, "Resource not found")` (`StudentController.java:58-59`). They run the same query, so there is no data-dependent branch. Test compares code and message and checks for foreign data. The owner's session stays valid. |
| Parameterized SQL | TECH §4 | Met. Only constant fragments are appended. Student ID, booking ID, status list (bound collection of enum names) and the `from`/`to` instants are all bound parameters. No locks or writes. |
| Status filter | TECH §3, §3.1 | Met. Only `status` and `date` are allowed; status values must be exactly ACTIVE/CANCELLED/COMPLETED (case-sensitive); duplicates collapse via EnumSet; empty means all, and the filter is omitted (`StudentApiInput.java:50-63`). Test: C3-FLT-1 and the invalid values in C3-IN-1. |
| SGT date filter and boundaries | TECH §3, §4; SH-DATA-01 | Met. `[date 00:00 Asia/Singapore, next day 00:00)` on `s.start_time`, half-open (`StudentReadService.java:72-75`, `START_RANGE_FILTER`). The `timestamptz` column is bound as UTC `OffsetDateTime`. Test: 15:30Z/16:30Z, 15:59:59Z included, 16:00Z excluded and included the next day, a past date, intersection with status. |
| `cancellable` against injected clock | TECH §3; STU-BOOK-02 bullet 3 | Met. `status == ACTIVE && startTime.isAfter(clock.instant())`, with the clock read once per request (`StudentReadService.java:48, 60, 65-66`). Test: start-1ms true, start false, start+1s false, on list and detail. |
| DTO exactness and privacy | TECH §3; INTEGRATION §1 | Met. The record has exactly the 10 fields; `tutorName` = `users.name` of the slot's tutor. No email, student name, student ID or tutor ID. Instants end in `Z`. Test: exact key sets, value checks, no `@`/student name/student ID/tutor ID. |
| Input validation before access check | TECH §3.2 evaluation order (F2-06) | Met. The controller parses query/path before the service calls `requireActiveStudent()` (`StudentController.java:43-60`). Path ID is bound as String, so a bad UUID gives 400, not a type-mismatch. Test: anonymous + bad input gives 401 (filter); deleted/inactive actor + bad input gives 400 without invalidation, then 403 with invalidation. |
| Access matrix and session invalidation | TECH §3.2; INTEGRATION §2; SH-AUTH-04 | Met through the unchanged c1 `StudentAccess` / `StudentExceptionHandler`. Results: no principal 401; provider empty 401; unknown account 401 + clearContext + invalidate; inactive 403 + invalidate, then 401; wrong stored role 403 without invalidation; session authorities ignored. Test: C3-ACC-1 on both routes, with no booking data in denial bodies. |
| C3-ELIG-1 vs core | TECH §4; core `changeBooking` | Met and consistent. Core cancel checks ownership, ACTIVE, slot BOOKED and start > now, but no tutor/module eligibility. So `cancellable: true` for ineligible tutor/module/assignment matches what core would accept. |
| Package boundaries / scope creep | TECH §2 | Met. Changes are only in `unichope/student/**`; there is no write path, no new bean dependency and no platform, core, Tutor, build or migration change in c3. |
| Error envelope / no-store | TECH §3.3 | Met. 400/404/401/403 all go through `ApiErrors.write`. Test helpers assert `no-store` on every success and error, and check the exact `{code,message,requestId}` envelope. |
| c1 regression | stu-c1 reviewer-1 | None. The only c1-visible change is the extraction of `sgtDay` (same expression); StudentBrowsePgTest 20/20 and StudentActivationTest pass. ApiSecurityConfiguration and the access/handler classes are byte-identical to c1. |
| Test strength | NOTE §2 | Strong. Real MVC + Spring Security chain + PostgreSQL; no mock of code under test; exact oracles. The equal-start tie is built so that only booking-id order passes. The red run was behavioural (404 vs 200/400). The test writer seeded 5 defects and each was caught. No skipped, disabled or weakened assertion. |

Upstream comparison: my results agree with analyst-2 (F2-01, F2-06, F2-19 resolutions present in TECH e4ba47b0 and
in the tests). They also agree with test-writer-1's mapping and notes N1-N4, and with implementer-1's mapping and
results (same counts: 28 + 132, all passing). I found no new contradiction between the request, the specs and the
tests. F2-13 (teammate review of `platform/auth` seam and `platform/web` matcher) and F2-15 (real auth adapter) remain
open merge/activation gates, unchanged by c3.

### Assessment of the implementer's open note (`cancellable` without slot BOOKED)
Core `changeBooking` additionally requires `slot.status == BOOKED`; TECH §3 defines `cancellable` without it. Through
core, an ACTIVE booking on a non-BOOKED slot is unreachable. Booking sets the booking ACTIVE and the slot BOOKED in one
transaction. Cancel and complete move both rows together. Tutor slot cancellation is allowed only from AVAILABLE
(`PostgresConsultationLifecycle.java:103`). Slot/booking relationships are immutable (INTEGRATION). The divergence can
only appear after direct database corruption. In that case the button would be offered and cancel would return 409
`INVALID_STATE`, which is a safe failure with no data change. No code change is needed. Optional spec clarification:
R3-01.

### Findings
| ID / severity | File:line | Acceptance ID | Trigger | Impact | Suggested correction (proposal) | Routing | Closure check |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R3-01 / INFO | `workflow/student/TECHNICAL-SPEC.md` §3 `cancellable` bullet; `StudentReadService.java:65-66` | C3-FLT-3 | ACTIVE booking whose slot is not BOOKED (reachable only through manual DB corruption; see assessment) | `cancellable: true` but cancel returns 409. Safe, no data change, no disclosure. | Optionally add to TECH §3: "core cancel also requires the slot BOOKED; with lifecycle-maintained data an ACTIVE booking's slot is always BOOKED, so `cancellable` omits that check." No code or test change. | Spec analyst (optional; owner Phuc may decline) | TECH §3 states the invariant, or the owner records a decision to leave it implicit. |
| R3-02 / LOW | `StudentBookingsPgTest.java:436-439` | C3-ACC-1 | Active tutor/admin session gets 403 `FORBIDDEN` on `/bookings` and `/bookings/{id}` | The test does not assert that the session remains valid after `FORBIDDEN` (TECH §3.2 lists invalidation only for unknown/inactive accounts). The behaviour is correct today: the shared handler does not invalidate for `FORBIDDEN` (`StudentExceptionHandler.java:34`). It is covered indirectly for c1 routes (`StudentBrowsePgTest` C1-ACC-5, repeated `FORBIDDEN` on the same session), so the c3 routes have no independent guard. Test-writer N1 left this unasserted on purpose. | Add `assertFalse(wrongRole.isInvalid())` (and optionally a second request still giving 403) in C3-ACC-1, or in a shared helper when c2/c4 extend the matrix. | Test writer (non-blocking; can be done with stu-c2/c4) | C3-ACC-1 asserts the `FORBIDDEN` session is not invalidated on both routes. |
| R3-03 / LOW (carried) | `student/http/StudentApiConfiguration.java:44-45`; `StudentBrowsePgTest.java:186` | Not a C3 ID | stu-c1 R1-01 (bare `/api/student` non-JSON 404) and R1-02 (vacuous `X-Request-Id` assertion) were proposed for stu-c3 by c1 reviewer-1 but not taken into this scope (test-writer N2, implementer limitations) | Unchanged from c1: no disclosure and no-store is set; contract shape for the bare prefix is undefined. c3 did not make it worse. | Owner/analyst to attach R1-01/R1-02 to a change with an acceptance row (for example a c1 follow-up revision or stu-c2). | Spec analyst / owner Phuc for scheduling; then implementer and test writer | Recorded in a change's acceptance rows and closed there per the c1 closure checks. |

No CRITICAL, HIGH or MEDIUM findings. No acceptance, security or integrity failure.

### Verdict
PASS for stu-c3 C3-T1, C3-T2 at the reviewed working-tree hashes above. Every C3 acceptance ID (S-LIST-1,
C3-OUT-1, C3-FLT-1..3, C3-ELIG-1, C3-OWN-1, C3-IN-1, C3-ACC-1) traces to code and to a passing, meaningful PostgreSQL
test that I reran. Owner scoping, non-disclosure, parameterized SQL, SGT boundaries, the injected-clock `cancellable`,
DTO privacy, validation-before-access order and the access/invalidation matrix meet TECH §3/§3.1/§3.2/§4. There is
no scope creep and no c1 regression. R3-01..R3-03 are non-blocking follow-ups with owners above.
This PASS enables human verification. It is not merge or deployment approval. It is tied to these hashes; any later
change to the c3 files, the c1 files they use, or the specs invalidates it for the affected scope.
Remaining gates and manual checks: F2-13 teammate review (Kok Seng for `platform/auth`, Huanghao for the
`platform/web` matcher) before merge to `main`; F2-15 real auth adapter before activation; human API walkthrough of
`GET /api/student/bookings` (Upcoming `status=ACTIVE`, History `status=CANCELLED&status=COMPLETED`, `date`) and
`GET /api/student/bookings/{id}` (own, foreign, missing). Frontend tabs remain out of scope.
Next agent, exact task and required inputs: human owner Phuc, verification of C3-T1/T2 using this record and the
evidence above; then stu-c2 may proceed on the `StudentBooking` projection.

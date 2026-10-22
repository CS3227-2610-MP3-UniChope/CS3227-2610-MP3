# stu-c2-book-slot: Student books a slot

Status: READY_FOR_IMPLEMENTATION for C2-T1 per "Handoff test-writer-1" below (supersedes: READY_FOR_TESTS for C2-T1 per "Handoff analyst-2", which superseded SPEC_DRAFT)
Scope classification and reason: standard path; consultation state mutation and a
concurrency-sensitive transition.
Parent request / related changes / dependencies: same request as
[stu-c1-browse-slots](stu-c1-browse-slots.md). Depends on c1's foundation (C1-T1, C1-T2)
and its read repository. [stu-c3-my-bookings](stu-c3-my-bookings.md) supplies the
`StudentBooking` projection and `GET /bookings/{id}` used by this response and its
`Location`; implement c3-T1 (projection) before or with this change.
Human owner / affected owners: Phuc (Student). Core lifecycle (Huanghao) is used unchanged.
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
> Superseded in part by "Handoff analyst-2" (end of this record): inputs, hashes, base, open
> questions and readiness verdict. Acceptance scenarios below remain, as refined there.
Producer role / actual tool and model: Spec analyst / Claude Code (Opus 5.5)
Recipient role: Test writer, fresh session
Change ID / task / exact requested next action: stu-c2-book-slot / C2-T1 / write failing
tests for the acceptance IDs below and record the red result.
Stage status / supersedes: SPEC_DRAFT, first revision.
Goal and non-goals: Goal: `POST /api/student/bookings` books an eligible slot for the
session student through `ConsultationLifecycle.bookAvailableSlot`. Non-goals: waiting
lists, booking for others, frontend, changing core lifecycle behavior.
Acceptance IDs: S-BOOK-1 to S-BOOK-5 (product spec §7); C2-IN-1 to C2-IN-3,
C2-ERR-1 to C2-ERR-3, C2-ACC-1 (below).

### Inputs and outputs
Same input artifacts and hashes as [stu-c1 Handoff analyst-1](stu-c1-browse-slots.md#inputs-and-outputs);
relevant sections: PRODUCT-SPEC §3 STU-BOOK-01, §7; TECHNICAL-SPEC §3 (POST `/bookings`),
§3.1-§3.3, §4 (`bookAvailableSlot`). Core reference:
`src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java`
`bookAvailableSlot` at commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d.
Output: Student tests for C2 (to be recorded).

Base commit / result commit / complete diff or patch reference: base 502d09f; none yet.
Uncommitted files and their hashes: as stu-c1.
Allowed write scope: as stu-c1 (tests only for the test writer).
Prerequisites / dependency state and evidence: stu-c1 tests and implementation first.
Recorded decisions and authorization references: D1-D4 in stu-c1.
Open questions / proposals / unverified facts:
- A1 (product spec §10 item 1): no enrolment model. Unconfirmed.
- A4 (§10 item 4): nonexistent and no-longer-bookable slots share 409 `SLOT_UNAVAILABLE`. Unconfirmed.
Verification commands, directories, results and output references: baseline as stu-c1.
Readiness or review verdict, scope and supporting evidence: NOT READY until A1 and A4
are confirmed and stu-c1 is at least READY_FOR_IMPLEMENTATION.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

## Acceptance scenarios

Fixtures as stu-c1. All POSTs use a valid CSRF header unless stated.

| ID | Given / When / Then |
| --- | --- |
| S-BOOK-1 | Given an eligible future AVAILABLE slot / when S1 POSTs `{ "slotId": ... }` / then 201, `Location: /api/student/bookings/{id}` that S1 can GET, body is a `StudentBooking` with status ACTIVE and `cancellable: true`; the DB has one ACTIVE booking for S1 and the slot is BOOKED. |
| S-BOOK-2 | Given a slot that is BOOKED, CANCELLED, COMPLETED, started, nonexistent, has an inactive tutor, an inactive module, or a removed assignment / when booking / then 409 `SLOT_UNAVAILABLE` with the same message in every case, and no row changes. |
| S-BOOK-3 | Given S1 holds an ACTIVE 10:00-11:00 booking / when booking 10:30-11:30 (another tutor) / then 409 `BOOKING_OVERLAP`, no change; booking 11:00-12:00 succeeds (half-open). |
| S-BOOK-4 | Given two students and one slot booked concurrently (use the fixture's row-lock `race` helper) / then exactly one 201 and one 409 `SLOT_UNAVAILABLE`; one ACTIVE booking exists. Given one student and two overlapping slots booked concurrently / then exactly one 201 and one 409 `BOOKING_OVERLAP`. |
| S-BOOK-5 | Given S1 booked then the booking was CANCELLED and the slot is AVAILABLE again / when S1 books it again / then 201 with a new booking ID; the CANCELLED booking still exists. |
| C2-IN-1 | Bodies `null`, `[]`, `{}`, `{"slotId":123}`, `{"slotId":"not-a-uuid"}`, `{"slotId":"<uuid>","studentId":"<S2>"}` (also `status`, `role`), duplicate `slotId` keys, and trailing tokens → 400 `VALIDATION_FAILED`, no change. Any query parameter → 400. |
| C2-IN-2 | `Content-Type: text/plain` → 415 `UNSUPPORTED_MEDIA_TYPE`. A body over 65,536 bytes with a misleading `Content-Length: 2` → 413 `PAYLOAD_TOO_LARGE`; exactly 65,536 bytes is not rejected for size. |
| C2-IN-3 | Header `X-Actor-Id: <S2>` is ignored: the booking belongs to the session student S1. |
| C2-ACC-1 | Missing/invalid CSRF → 403 `CSRF_INVALID`, no change. No principal → 401; tutor/admin → 403 `FORBIDDEN`; inactive student → 403 `ACCOUNT_INACTIVE` with session invalidated; none changes rows. |
| C2-ERR-1 | Lifecycle double throwing `INVALID_INPUT` → 400 `VALIDATION_FAILED`; `TEMPORARILY_UNAVAILABLE` → 503; a unique-index violation (`DataIntegrityViolationException` on `bookings_one_active_per_slot`) → 409 `SLOT_UNAVAILABLE`; an unexpected exception whose message contains SQL text → 500 `INTERNAL_ERROR` without that text. |
| C2-ERR-2 | Lifecycle throws `ACCESS_DENIED` after the account was deactivated mid-request → 403 `ACCOUNT_INACTIVE`. |
| C2-ERR-3 | Projection after a successful lifecycle call fails → 500, the booking stays committed exactly once, and a later GET shows it. |

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| C2-T1 `POST /bookings`: input, lifecycle call, error mapping, projection | All above | | pending |

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
Next agent, exact task and required inputs: Test writer (fresh session) after stu-c1 and the A1/A4 confirmation.


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
analyst-1, base 502d09f, open questions A1/A4, decisions "D1-D4" and the NOT READY
verdict) and the Status line's SPEC_DRAFT condition. The analyst-1 acceptance
scenarios remain in force, as refined below.
Producer role / actual tool and model: Spec analyst / Claude Code, Opus 5.5
(`claude-opus-5-5`); fresh subagent session, no inherited conversation (D5).
Recipient role: Test writer, fresh session, `workflow/student/agents/test-writer.md`.
Change ID / task / exact requested next action: stu-c2-book-slot / C2-T1 / after the
dependency conditions below are met, verify this packet, write the C2 tests and record
the behavioral red result.
Stage status: READY_FOR_TESTS (scoped, dependency-conditioned; see Spec readiness (analyst-2)).
Goal and non-goals: unchanged from analyst-1.
Acceptance IDs: S-BOOK-1..5, C2-PREC-1, C2-IN-1..3, C2-ACC-1, C2-ERR-1..3.

### Inputs and outputs
All canonical inputs, hashes, code/test patterns and process files are those listed in
[stu-c1 "Handoff analyst-2" Inputs and outputs](stu-c1-browse-slots.md#handoff-analyst-2)
(PRODUCT-SPEC ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957,
TECHNICAL-SPEC e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3,
PLAN 31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, base commit
459fd6ef7108c2d08aeaa0f92ae0bccc054158b5). Sections relevant here: PRODUCT §2, §3
STU-BOOK-01, §7 S-BOOK-1..5 and S-AUTH-1, §10 items 1 and 4; TECHNICAL §3 (POST
`/bookings`), §3.1, §3.2, §3.3 (incl. analyst-2 clarifications), §4; INTEGRATION §1, §2,
§5, §7 (row-lock protocol, "Student books"); SHARED SH-AUTH-03/04, SH-DATA-02/03.
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java `bookAvailableSlot`, `mutate` | Input (code) | 35870e1c1f35246de71fef83173dada8048aa08c855a65f660ee9b5135721b99 | Check order, lock order, failure codes |
| src/backend/src/main/resources/db/core-review/V1__create_core_tables.sql (`bookings_one_active_per_slot`) | Input (code) | 1cdea0be86ff0718eb9c1ed5131f5eec014c845eeb954d537b68e7372dccfe9c | Unique index name |
| src/backend/src/test/java/unichope/platform/core/postgres/LifecyclePgFixture.java `race`, `holder`, `awaitBlocked` | Input (test pattern) | ede25ee845b6c20cb060c5ec1afd7b5b0f37c9b5e2a381b065c66d03b2d92db3 | Deterministic races |
| C2 tests in the Student test scope | Output (test writer) | to be recorded | Tests |
| stu-c1 and stu-c3 implementations with green tests | Input (prerequisite) | to be recorded by their implementers | Foundation, projection, detail endpoint |

Base commit / diff: base 459fd6e; analyst-2 working-tree changes as listed in stu-c1
Handoff analyst-2. No source or test file changed by this session.
Allowed write scope (test writer): as stu-c1 Handoff analyst-2. Lifecycle test doubles
(for C2-ERR-*) live in test sources only.
Allowed write scope (later implementer): `unichope/student/**` only; core lifecycle used unchanged.
Prerequisites / dependency state: stu-c1 C1-T1..T4 and stu-c3 C3-T1, C3-T2 implemented
with tests green at recorded revisions (none started).
Recorded decisions: A1 (no enrolment) and A4 (shared 409 `SLOT_UNAVAILABLE`) approved
by Phuc, PRODUCT §10 (22 October 2026); D2, D3, D5.
Open questions / proposals / unverified facts: inherited F2-13, F2-14, F2-15.
Verification commands: none run in this analyst session.
Readiness verdict: READY_FOR_TESTS for C2-T1 with the dependency conditions below.

### Receiver receipt
Receiver role / actual tool and model: Test writer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Checked task, artifact revisions and required evidence (22 October 2026), before any test edit:
- Task C2-T1 only, plus the stu-c1 carry-forward R1-05 / test-writer note N3 (fail-closed startup without
  a `ConsultationLifecycle` bean), which stu-c1 reviewer-1 routed to the stu-c2 test writer.
- HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` equals the analyst-2 base. `git status -- src/` shows only the
  stu-c1 + stu-c3 working tree (modified `ApiSecurityConfiguration.java`; untracked `platform/auth/`, `student/`,
  the c1 tests, `StudentBookingsPgTest.java`) and the untracked IDE folder `src/backend/bin/` (not part of any change).
- `sha256sum` matches every hash recorded upstream: Student PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., PLAN
  31545bfb...; shared PRODUCT d1d5a414..., SHARED-TECHNICAL 54ec090a..., INTEGRATION ac0d183f...; NOTE 5e8ba76f...,
  HANDOFF 1ee65fd9...; core `PostgresConsultationLifecycle` 35870e1c..., V1 DDL 1cdea0be..., LifecyclePgFixture
  ede25ee8..., TutorRestPgTest d12de2d1..., compose.test.yaml 7a7216c0....
- Dependency condition (analyst-2 "Spec readiness"): met. stu-c1 C1-T1..T4: all 14 main files equal the c1
  implementer-1 / reviewer-1 hashes, except the five files c3 changed, which equal the c3 implementer-1 /
  reviewer-1 hashes (StudentReadRepository d6d85df4..., PostgresStudentReadRepository fe91a985..., StudentReadService
  f5cbde32..., StudentApiInput 7798443e..., StudentController b1b35cb6...); new StudentBooking 30a4d4e1.... Tests:
  StudentTestActors 9e70e1ee..., StudentActivationTest 15d13758..., StudentBrowsePgTest 39206a32..., StudentBookingsPgTest
  a38b9950.... Evidence files of both changes match their recorded hashes (c1: b9c6fbf6..., b35b2485..., 8d6773cc...,
  4bdaa259..., 77c74086..., a9a4985e...; c3: cab15630..., 1761ea2f..., dd9e02de..., 3ce23686..., 149a8cb4..., 88d2c385...).
  stu-c1 reviewer-1: PASS; stu-c3 reviewer-1: PASS. Both are READY_FOR_HUMAN. No contract changed after review.
- Green at this working tree: my own rerun, `./gradlew clean test postgresTest ...` (see "Handoff test-writer-1"),
  exit 0, `test` 28/0/0/0, `postgresTest` 132/0/0/0.
- Gate artifacts present: analyst-2 READY_FOR_TESTS for C2-T1, coverage matrix, findings register with
  resolutions (F2-13/F2-14/F2-15 deferred as merge/activation gates by D6/D7), and acceptance refinements. A1 and A4
  are recorded in PRODUCT §10 (Phuc, 22 October 2026).
- I read the canonical sections directly: PRODUCT §2, STU-BOOK-01, §7 S-BOOK-1..5, §10; TECH §2 (activation), §3
  (POST `/bookings`, `SLOT_UNAVAILABLE`/`BOOKING_OVERLAP` bullets), §3.1, §3.2 (table, `ACCESS_DENIED` paragraph,
  evaluation order), §3.3 (rows and F2-07/F2-08 clarifications, post-commit paragraph), §4; INTEGRATION §1, §2, §5,
  §7. I also read the core `bookAvailableSlot` check and lock order. They agree with every C2 acceptance row. I found
  no requirement/technical conflict for C2-T1.
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none.

## Acceptance refinements (analyst-2)
Rows with an existing ID replace the analyst-1 wording.

| ID | Given / When / Then |
| --- | --- |
| C2-PREC-1 (new, F2-08) | Given S1 holds an ACTIVE 10:00-11:00 booking and a BOOKED (another student's) T2 slot 10:30-11:30 exists / when S1 books that slot / then 409 `SLOT_UNAVAILABLE` (not `BOOKING_OVERLAP`), no row changes. |
| C2-ERR-1 (tightened, F2-07, F2-10) | With a lifecycle test double: `INVALID_INPUT` → 400 `VALIDATION_FAILED`; `TEMPORARILY_UNAVAILABLE` → 503 `TEMPORARILY_UNAVAILABLE`; `new DuplicateKeyException("duplicate key value violates unique constraint \"bookings_one_active_per_slot\"")` → 409 `SLOT_UNAVAILABLE` with the standard message; `new DataIntegrityViolationException("insert violates foreign key constraint \"bookings_slot_id_fkey\"")` → 500 `INTERNAL_ERROR`; a lifecycle code outside the TECH §3.3 table (e.g. `SLOT_OVERLAP`) → 500 `INTERNAL_ERROR`; an `IllegalStateException` whose message contains `SELECT` and an ID → 500. No 500 body contains `SELECT`, `constraint`, a table name or an ID from the exception. |
| C2-ERR-2 (tightened, F2-09) | (a) The lifecycle double deactivates S1 in the database and then throws `ACCESS_DENIED` → 403 `ACCOUNT_INACTIVE`, session invalidated, the same session then gets 401. (b) The double throws `ACCESS_DENIED` while S1 is still an active student → 403 `FORBIDDEN`, the session stays valid (a following `GET /bookings` succeeds). |
| C2-ACC-1 (clarified) | With a valid body: missing CSRF and invalid CSRF → 403 `CSRF_INVALID`; valid CSRF and no principal → 401; tutor/admin → 403 `FORBIDDEN`; inactive student → 403 `ACCOUNT_INACTIVE` + session invalidated. Database snapshot unchanged in every case. |
| S-BOOK-4 (clarified) | Same-slot race: hold tutor T1's user row with the fixture `race` helper, launch S1 and S2 bookings of one T1 slot through MockMvc, release; outcomes exactly one 201 and one 409 `SLOT_UNAVAILABLE`; exactly one ACTIVE booking and the slot BOOKED. Overlap race: hold S1's row, launch S1 bookings of two overlapping slots of T1 and T2; exactly one 201 and one 409 `BOOKING_OVERLAP`. The web context must use the fixture `source` so `awaitBlocked` observes both callers. |

Task mapping additions: C2-PREC-1 → C2-T1.

## Requirement-to-technical coverage (analyst-2)
COVERED means specification support only. "PG" = Student MockMvc-to-PostgreSQL test through the real lifecycle.

| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| S-BOOK-1 | STU-BOOK-01 bullets 1-2; §7 S-BOOK-1; shared REL-01 | TECH §3 POST `/bookings` (201, `Location`), §4 `bookAvailableSlot`; INTEGRATION §7 atomic "Student books"; core inserts booking and sets slot BOOKED in one transaction | COVERED | PG: response, followable `Location`, DB read-back via fixture JdbcClient |
| S-BOOK-2 | STU-BOOK-01 bullet 3; §10 item 4 (A4) | TECH §3 `SLOT_UNAVAILABLE` bullet, §3.3 (NOT_FOUND/INVALID_STATE/MODULE_UNAVAILABLE → 409); core check order | COVERED | PG: every cause, identical code and message, snapshot unchanged |
| S-BOOK-3 | STU-BOOK-01 bullet 4 | TECH §3 `BOOKING_OVERLAP`, §3.3; INTEGRATION §7 half-open intervals; core overlap SQL (`start<end AND end>start`) | COVERED | PG: overlap rejected, adjacent accepted |
| S-BOOK-4 | STU-BOOK-01 bullet 5; shared business rule | INTEGRATION §7 row-lock protocol (student and tutor rows, module, slot); SHARED SH-DATA-03; core `lockUsers(actor, tutor)` | COVERED | PG with `race` helper (see refinement) |
| S-BOOK-5 | STU-BOOK-01 bullet 6 | INTEGRATION §7 (history kept on rebooking); partial unique index on ACTIVE only | COVERED | PG |
| A1 (no enrolment) | §10 item 1 | TECH §4 lifecycle checks (no enrolment condition); core `bookAvailableSlot` | COVERED | PG: S-BOOK-1 with a student that has no module relationship (all fixture students) |
| C2-PREC-1 | STU-BOOK-01 bullets 3-4 | TECH §3.3 clarification (analyst-2); core order | COVERED | PG |
| C2-IN-1, C2-IN-2 | §2 bullet 2 | TECH §3.1 (exact body, 415, 413 by stream, duplicate keys, trailing tokens); INTEGRATION §1 (no `studentId`/`role`/status); Tutor REST §2 | COVERED | PG/Unit |
| C2-IN-3 | §2 bullet 2 | TECH §2 (headers never select the actor) | COVERED | PG |
| C2-ACC-1 | §2; §7 S-AUTH-1 | TECH §3.2 (incl. CSRF row, evaluation order); SHARED SH-AUTH-03/04 | COVERED | PG with real security filter chain |
| C2-ERR-1 | (technical) | TECH §3.3 rows and analyst-2 clarifications; INTEGRATION §5 sanitized 500, §7 503 on lock timeout | COVERED | PG/Unit with lifecycle double |
| C2-ERR-2 | §2 bullet 3 | TECH §3.2 ACCESS_DENIED paragraph; INTEGRATION §2 | COVERED | PG with lifecycle double |
| C2-ERR-3 | shared REL-01 | TECH §3.3 post-commit paragraph; Tutor REST §4 | COVERED | PG with failing read-repository double |

Out of scope (no coverage claimed): confirmation dialog and conflict-refresh UI (PLAN S3
React part); S-AUTH-1's LLM clause.

## Spec findings and proposed fixes (analyst-2)
Applicable rows of the full register in [stu-c1](stu-c1-browse-slots.md) "Spec findings
and proposed fixes (analyst-2)"; evidence there.

| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| F2-01 / MEDIUM | PLAN S3 said to implement through a new `ConsultationLifecycle.bookSlot` coordinated with Huanghao; the existing `bookAvailableSlot` is used unchanged (TECH §4). | PLAN S3 corrected. | Phuc | RESOLVED: PLAN 31545bfb |
| F2-06 / LOW | Validation-vs-access order unspecified. | TECH §3.2 evaluation order. | Phuc | RESOLVED: TECH e4ba47b0 |
| F2-07 / MEDIUM | Unique-violation mapping had no recognisable trigger. | TECH §3.3 clarification; C2-ERR-1 doubles. | Phuc | RESOLVED: TECH e4ba47b0; refinements above |
| F2-08 / LOW | Unavailable-and-overlapping precedence unspecified. | TECH §3.3 note; C2-PREC-1. | Phuc | RESOLVED |
| F2-09 / LOW | `ACCESS_DENIED` still-valid branch untested; session oracle missing. | TECH §3.2 wording; C2-ERR-2. | Phuc | RESOLVED |
| F2-10 / LOW | Unlisted lifecycle codes unmapped. | TECH §3.3 row → 500; C2-ERR-1. | Phuc | RESOLVED |
| F2-14 / LOW | `SLOT_UNAVAILABLE` not in INTEGRATION §5 shared list. | Student-local now; reviewed integration change before a shared client depends on it. | Team | OPEN, DEFERRED (non-blocking) |
| F2-13 / MEDIUM | Inherited c1 seam/security-rule review. | Merge gate. | Kok Seng, Huanghao | OPEN, DEFERRED |
| F2-15 / LOW | Inherited: simulated authentication only. | Activation gate. | Kok Seng | OPEN, DEFERRED |
| F2-17 / LOW | Inherited: logging by review. | Reviewer checklist. | Phuc | RESOLVED by plan |
| F2-18 / INFO | Base re-anchored to 459fd6e. | This manifest. | - | RESOLVED |

Audited without further findings for c2: lock order and races (INTEGRATION §7 and
SHARED SH-DATA-03 vs core `bookAvailableSlot`: users(student, tutor) sorted by
PostgreSQL, module, slot; overlap checked while the student row is held); atomic
booking + slot update in one `REQUIRES_NEW` READ COMMITTED transaction; 503 mapping of
lock/statement timeouts (core `CONTENTION_STATES`); half-open overlap SQL; rebooking
after cancel (partial unique index on ACTIVE only); A1/A4 consistency with core
(NOT_FOUND/INVALID_STATE/MODULE_UNAVAILABLE all collapse to `SLOT_UNAVAILABLE`); no
`studentId`/`role` accepted (INTEGRATION §1). AI: not applicable.

## Shared-owner boundary and dependency check (analyst-2)
- Files: `unichope/student/**` only (input parser, HTTP service, error mapping,
  controller). Core lifecycle and schema unchanged (Huanghao's area untouched); no
  `platform/*` change beyond stu-c1's.
- Uses the core-owned `ConsultationLifecycle` exactly as specified (SHARED §2: every
  slot/booking change goes through it).
- Hard dependencies: stu-c1 (foundation) and stu-c3 C3-T1/C3-T2 (projection and
  `Location` target), both implemented with green tests.

## Spec readiness (analyst-2)
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision: stu-c2 C2-T1 against PRODUCT-SPEC
ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957, TECHNICAL-SPEC
e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3, PLAN
31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, code at 459fd6e.
Remaining findings and why deferral does not affect this task: F2-14 (codes are fully
specified Student-locally; only the future shared frontend client is affected), F2-13
and F2-15 as in stu-c1.
Required owner decisions and recorded outcome: A1 and A4 approved (PRODUCT §10).
Recheck evidence: each C2 ID re-traced to TECH §3/§3.1-§3.3/§4 after the analyst-2
edits and to the core method's actual check and lock order.
Dependency conditions: test writing starts only after stu-c1 and stu-c3 have implementer
handoffs with green tests at recorded revisions; a contract change in either returns
this change to the analyst.
Next agent, task, acceptance IDs, allowed files and prerequisites: fresh test writer
(after stu-c3 implementation); C2-T1; IDs above; Student test scope; compose PostgreSQL.

## Handoff test-writer-1
Producer role / actual tool and model: Test writer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Recipient role: Implementer, fresh session, `workflow/student/agents/implementer.md`.
Change ID / task / exact requested next action: stu-c2-book-slot / C2-T1 / implement `POST /api/student/bookings`
(strict body and query parsing, actor check, `ConsultationLifecycle.bookAvailableSlot`, TECH §3.3 error mapping,
`StudentBooking` projection with `Location`), and wire `ConsultationLifecycle` as a required `student-api`
dependency, so that every test below passes. Do not change the tests. Return a test you believe is wrong to the
test writer, with evidence.
Stage status / supersedes: READY_FOR_IMPLEMENTATION; first test-writer revision. Supersedes the analyst-1
"Tasks and acceptance-to-test mapping" (state "pending") and "Verification evidence" placeholders for C2-T1.
Goal and non-goals: as in analyst-1/analyst-2 (book a slot for the session student; no waiting lists, booking
for others, frontend, cancellation, or core lifecycle change).
Acceptance IDs: S-BOOK-1..5, C2-PREC-1, C2-IN-1..3, C2-ACC-1, C2-ERR-1..3 (the analyst-2 list; nothing dropped),
plus the stu-c1 carry-forward R1-05 / N3 (TECH §2: `student-api` startup fails without a `ConsultationLifecycle` bean).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| This record: Handoff analyst-2, Acceptance scenarios + refinements (analyst-2), coverage, findings, Spec readiness (analyst-2) | Input | (this file) | Gate and acceptance oracles |
| Canonical specs, core lifecycle, DDL, fixture, Tutor test pattern, c1/c3 sources, tests and evidence | Input | re-verified identical to recorded hashes (receiver receipt under Handoff analyst-2) | Baseline and dependency check |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBookSlotPgTest.java | Output (`postgresTest`, tag `postgres`) | 50ed5340fcadc22a14c4273c337c175547a605bfbd16563d64e7729770604cb4 | All C2 IDs and the R1-05 startup check; MockMvc + Spring Security + real PostgreSQL |
| workflow/student/changes/stu-c2-book-slot/evidence/baseline-test-writer-2026-10-22.txt | Output (evidence) | cded6bb6111ec556e1b55031cab68a56ffc015508808afb26500c54c5eda44c9 | Existing-suite rerun before any C2 edit |
| workflow/student/changes/stu-c2-book-slot/evidence/red-run-test-writer-2026-10-22.txt | Output (evidence) | c31edcb69a3bdc1c83aa95a092a6ad24a5b84d457bdb88c1342e38afc2f0b4d1 | Pre-implementation red run, per-test messages, test-validity check |

Base commit / result commit / diff: base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` plus the uncommitted, unchanged
stu-c1 + stu-c3 working tree; no commit. This stage added exactly one file under `src/` (the untracked test class
above). `git diff --stat -- src/` still lists only `ApiSecurityConfiguration.java` (+2, from c1). No main source, no
existing test (c1, c3, core, Tutor) and no build file changed.
Uncommitted files and their hashes: the test class and two evidence files above, plus this record (receiver receipt
under Handoff analyst-2, the Status line and this section).
Allowed write scope (used): one new Student PostgreSQL MockMvc class in
`src/backend/src/test/java/unichope/platform/core/postgres/` (to reuse the package-private `LifecyclePgFixture`), the
same interpretation as stu-c3 test-writer-1. The lifecycle double and the switchable DataSource are private nested
test classes in it. `StudentTestActors` is reused unchanged.
Allowed write scope (implementer, from analyst-2): `unichope/student/**` only; core lifecycle used unchanged; no
`platform/*`, Tutor, migration or build change.
Prerequisites / dependency state: stu-c1 and stu-c3 implemented, reviewed PASS and green at this working tree
(baseline below). Compose PostgreSQL 17 (Docker 29.9.2) was used and stopped afterwards.
Recorded decisions and authorization references: A1, A4 (PRODUCT §10), D2-D7. No new decision was made in this stage.

### Test design (why the red is behavioral)
Same discovery contract as stu-c1/stu-c3: Student code is found by component scan of `unichope.student` with profile
`student-api` (manual web context: `DelegatingWebMvcConfiguration` + `ApiSecurityConfiguration` + scan), and the actor
seam via `StudentTestActors`. The test has no compile-time reference to any Student product type. It references only
core types (`ConsultationLifecycle`, `LifecycleFailure`, `Booking`, ...), Spring and the fixture. Before
implementation, `POST /api/student/bookings` is answered by the c1 Student resolver as 405 `METHOD_NOT_ALLOWED`, so
every red is an HTTP status assertion, a race-helper assertion or a startup-outcome assertion.
**Beans supplied to Student wiring:** `dataSource` = test-only `SwitchableDataSource` over the fixture source (it can
make new connections fail; used only by C2-ERR-3); `clock` = fixture `MovingClock` (N = 2026-10-21T00:00Z);
`consultationLifecycle` = test-only `ScriptedLifecycle`, which delegates to the real `PostgresConsultationLifecycle`
unless a test scripts a failure; core User/Module/Slot/Booking repositories over the switchable source; the test-only
`CurrentActorProvider`. The real lifecycle uses the fixture source directly, so `race`/`awaitBlocked` observe its
row locks. No `JdbcClient` bean. If C2 wiring needs any other bean, return the issue to the test writer.

### Tasks and acceptance-to-test mapping (test-writer-1)
Class `PG` = `unichope.platform.core.postgres.StudentBookSlotPgTest`. All 15 cases were observed red at this revision.

| Task | Acceptance ID | Test case | Pre-implementation state |
| --- | --- | --- | --- |
| C2-T1 | S-BOOK-1, A1 | PG `sBook1BooksEligibleSlotWithLocationExactProjectionAndAtomicRows`: 201; `Location` exactly `/api/student/bookings/{id}`, followable and equal to the body; exact 10-field `StudentBooking` with values, `Z` instants, ACTIVE, `cancellable: true`; no email, student name, student id or tutor id; listed in GET `/bookings`; 404 for S2; DB has one ACTIVE booking owned by S1 and the slot BOOKED, another slot untouched; one lifecycle call. Fixture students have no module relationship (A1) | Red (405 vs 201) |
| C2-T1 | S-BOOK-2, A4 | PG `sBook2EveryUnavailableCauseSharesOneConflictAndChangesNothing`: BOOKED by another student, CANCELLED, COMPLETED, started exactly at now, started earlier, nonexistent, inactive tutor, inactive assigned module, removed assignment: each 409 `SLOT_UNAVAILABLE`, exact TECH §3.3 message, one message overall, slot id not echoed, snapshot unchanged, session kept; eligible control 201 | Red |
| C2-T1 | S-BOOK-3 | PG `sBook3OverlapWithOwnActiveBookingIsRejectedAndAdjacentIsAllowed`: 10:30-11:30, contained and enclosing slots of another tutor give 409 `BOOKING_OVERLAP` (exact message), no change; adjacent 11:00-12:00 and 09:00-10:00 book; the student's own CANCELLED booking and another student's ACTIVE booking do not block | Red |
| C2-T1 | C2-PREC-1 | PG `c2Prec1UnavailableWinsOverOverlap`: an overlapping slot BOOKED by S2 and an overlapping inactive-module slot give 409 `SLOT_UNAVAILABLE`; a free overlapping control gives `BOOKING_OVERLAP`; no change | Red |
| C2-T1 | S-BOOK-4 (same slot) | PG `sBook4SameSlotRaceHasExactlyOneWinner`: T1 row held by `race`; S1 and S2 POSTs both observed blocked in PostgreSQL; release; exactly one 201 and one 409 `SLOT_UNAVAILABLE`; one ACTIVE booking owned by the 201 student, equal to its body id; slot BOOKED; both requests reached the lifecycle | Red (no blocked caller observed) |
| C2-T1 | S-BOOK-4 (overlap) | PG `sBook4OverlappingSlotsRaceForOneStudentHasExactlyOneWinner`: S1 row held; S1 books overlapping T1 and T2 slots concurrently; exactly one 201 and one 409 `BOOKING_OVERLAP`; one ACTIVE booking; losing slot AVAILABLE | Red (no blocked caller observed) |
| C2-T1 | S-BOOK-5 | PG `sBook5RebookingAfterCancellationCreatesNewBookingAndKeepsHistory`: book, cancel through core `cancelActiveBooking` (the c4 route is not in scope), rebook: 201, new id, CANCELLED kept, both in history; then S2 books it, history kept | Red |
| C2-T1 | C2-IN-1 | PG `c2In1RejectsEveryNonExactBodyAndAnyQueryBeforeTheActorCheck`: `null`, `[]`, `{}`, string, empty, blank, number; non-string, null, array or object `slotId`; `not-a-uuid`, `1-1-1-1-1`, hyphenless, empty, padded UUID; extra `studentId`/`status`/`role`/`actorId`; renamed keys; duplicate keys (equal and different); trailing tokens; truncated JSON; query `x`, `studentId`, `slotId`, `actorId`, `?flag`. All give 400 `VALIDATION_FAILED`, no change, no lifecycle call. Each body names a bookable slot. TECH §3.2 order: anonymous 401; missing CSRF 403; a deleted, wrong-role or inactive actor with bad input gets 400 without invalidation; then inactive with a valid body gets 403 `ACCOUNT_INACTIVE` + invalidated; control 201 | Red |
| C2-T1 | C2-IN-2 | PG `c2In2MediaTypeAndStreamedBodySizeLimits`: `text/plain`, `application/xml`, missing type 415; 65,537 bytes 413 with `Content-Length: 2` and with an honest length; deleted actor 415/413 without invalidation; no change, no lifecycle call; exactly 65,536 bytes 201; `application/json;charset=UTF-8` 201 | Red |
| C2-T1 | C2-IN-3 | PG `c2In3ActorHeaderIsIgnored`: an `X-Actor-Id` naming the other student is ignored in both directions (DB owner = session student); the header without a session gives 401 | Red |
| C2-T1 | C2-ACC-1, S-AUTH-1 (no-change part) | PG `c2Acc1AccessMatrixAndCsrfDenyWithoutAnyChange`: missing and invalid CSRF 403 `CSRF_INVALID`; no principal 401; provider-empty 401; deleted 401 + invalidated, reuse 401; tutor/admin with `ROLE_STUDENT` 403 `FORBIDDEN`, session not invalidated (stu-c3 R3-02), repeat 403; inactive 403 `ACCOUNT_INACTIVE` + invalidated, reuse 401; no slot id, module code or tutor name in denials; snapshot unchanged; zero lifecycle calls; student with `ROLE_TUTOR` 201 | Red (provider-empty 405 vs 401) |
| C2-T1 | C2-ERR-1 | PG `c2Err1LifecycleOutcomesMapToSafeStudentErrors`: the double throws `INVALID_INPUT` 400; `TEMPORARILY_UNAVAILABLE` 503; `NOT_FOUND`/`INVALID_STATE`/`MODULE_UNAVAILABLE` 409 `SLOT_UNAVAILABLE` (exact message); `BOOKING_OVERLAP` 409 (exact message); `DuplicateKeyException` naming `bookings_one_active_per_slot` 409; `DataIntegrityViolationException` whose cause chain names it 409; foreign-key violation, `bookings_pkey` duplicate, `DataAccessResourceFailureException`, `SLOT_OVERLAP`, `INVALID_TIME` and an `IllegalStateException` with SQL text give 500 `INTERNAL_ERROR`. No body contains `constraint`, index names, `SELECT`, the marker, the client `X-Request-Id`, the slot or student id, or `Exception`; 500 bodies also lack core code names and table names; no change; session kept | Red |
| C2-T1 | C2-ERR-2 | PG `c2Err2LifecycleAccessDeniedRerunsTheAccessCheck`: `ACCESS_DENIED` after (a) deactivation 403 `ACCOUNT_INACTIVE`, invalidated, then 401; (b) still valid 403 `FORBIDDEN`, session kept, GET `/bookings` 200; (c) account deleted 401 + invalidated; (d) stored role changed 403 `FORBIDDEN`, kept; no change | Red |
| C2-T1 | C2-ERR-3 | PG `c2Err3ProjectionFailureAfterCommitIs500AndKeepsTheBookingOnce`: after the real booking commits, new read connections fail; 500 without SQL, marker or slot id; exactly one lifecycle call (no retry); booking ACTIVE, slot BOOKED, visible in a later GET; rebooking 409 `SLOT_UNAVAILABLE` | Red |
| C2-T1 | TECH §2 activation (stu-c1 R1-05 / N3) | PG `c2ActStudentApiWithoutConsultationLifecycleFailsStartup`: the Student web context without the lifecycle bean fails refresh naming `ConsultationLifecycle`; the real Boot app with `student-api` and no lifecycle bean fails startup naming it; the complete context books 201 | Red (context starts) |
| C2-T1 | Regression | Full suite: the existing 28 unit + 132 PostgreSQL tests (c1, c3, core, Tutor) stay green | Green now; recheck after implementation |

Every response checked by the helpers also asserts `Cache-Control: no-store`; every error asserts the exact
`{code,message,requestId}` envelope, a JSON content type, and no email, `SELECT` or exception marker.
Manual / real-provider checks: none needed for the C2 backend; AI not applicable. The confirmation dialog and
conflict-refresh UI (PLAN S3 React part) remain out of scope, as analyst-2 recorded.

### Verification evidence (test-writer-1)
All runs from `src/backend`, compose PostgreSQL up (`docker compose -f compose.test.yaml up -d --wait`, exit 0),
local placeholder credentials from compose.test.yaml.
1. Existing-suite baseline before any C2 edit:
   `./gradlew clean test postgresTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<local placeholder> --console=plain`
   Exit 0. `test` 28 tests, 0 failures, 0 errors, 0 skipped; `postgresTest` 132/0/0/0 (StudentBookingsPgTest 9,
   StudentBrowsePgTest 20; StudentActivationTest 2 under `test`). Evidence: `evidence/baseline-test-writer-2026-10-22.txt`.
2. `./gradlew compileTestJava --console=plain`: exit 0, no warnings from test sources.
3. Red run: the same command as 1 plus `--continue`. Exit 1 (expected). `test` 28/0/0/0; `postgresTest` 147 tests,
   15 failures, 0 errors, 0 skipped. All 15 failures are the new StudentBookSlotPgTest cases, each an
   `AssertionFailedError`: 12 are `expected: <201|400|401|409> but was: <405>` (`HttpRequestMethodNotSupportedException:
   Request method 'POST' is not supported`), 2 are `Did not observe 1 independently blocked PostgreSQL callers` (the
   races), and 1 is `student-api context started without a ConsultationLifecycle bean ==> Expected
   java.lang.RuntimeException to be thrown`. The existing 28 + 132 tests pass. Evidence: `evidence/red-run-test-writer-2026-10-22.txt`.
4. Test-validity check (not implementation evidence): in a disposable copy outside the repository (deleted afterwards),
   a minimal spec-following prototype made the full suite pass (exit 0; 28/0 and 147/0). Each of 14 seeded defects was
   caught by the intended test: extra fields ignored, overlap mapped as unavailable, `ACCESS_DENIED` without the
   access recheck, unique index unmapped, cause chain not inspected, every integrity error mapped to 409, unknown codes
   mapped to 409, actor check before syntax, retry after projection failure, lazily resolved lifecycle (startup does
   not fail), missing `Location`, body size taken from `Content-Length`, actor taken from `X-Actor-Id`, and exception
   text in a 500. Details are in the red-run evidence.
5. `docker compose -f compose.test.yaml down`: exit 0.
Implementer commands: the red-run command without `--continue` (add `bootJar` for the final run, as in c1/c3).
Focused: `./gradlew postgresTest --tests 'unichope.platform.core.postgres.StudentBookSlotPgTest'` with the same
`-Dcore.test.*` properties. `test` excludes the `postgres` tag, so this class runs only under `postgresTest`.

### Notes for the implementer
- I1: the tests constrain no C2 class names. They constrain the route, JSON, status codes, the messages for
  `SLOT_UNAVAILABLE` and `BOOKING_OVERLAP` (exact TECH §3.3 text), discoverability (component scan under
  `unichope.student`, profile `student-api`) and the supplied-bean list. TECH §2 proposes `StudentHttpService` for
  lifecycle calls and mapping.
- I2: inject `ConsultationLifecycle` by type as a required dependency at startup (no `ObjectProvider` or lazy lookup).
  The startup failure must name `ConsultationLifecycle`, which a missing required bean of that type does.
- I3: TECH §3.2 order: query, content type, body size (read at most 65,536 + 1 bytes from the stream; ignore
  `Content-Length`) and exact body shape come before the actor check; none of them reads data or invalidates.
- I4: use the session actor from `StudentAccess` for the lifecycle call, never body, query or header values.
- I5: lifecycle `ACCESS_DENIED`: rerun the access check (its 401/403 and invalidation rules apply); if it still passes,
  return 403 `FORBIDDEN` without invalidation.
- I6: `DataIntegrityViolationException` (including `DuplicateKeyException`) maps to 409 only when its message or the
  message of any cause contains `bookings_one_active_per_slot`. Every other data-access or unexpected exception, and
  every lifecycle code outside the TECH §3.3 table, is a 500 with the fixed message. Never echo exception text or core
  code names.
- I7: after a successful lifecycle call, project through the Student read path (TECH §4). If that fails, return 500 and
  do not retry or compensate. At that point the test makes every new connection from the `dataSource` bean fail.
- I8: `Location` is the relative path `/api/student/bookings/{id}`, and the body equals what GET on it returns.

### Test design notes and gaps
Spec gaps returned to the analyst: none for C2. No acceptance row needed invention. Derived oracles (not new behavior):
- D-1: exact messages come from the TECH §3.3 table rows. "One message for all unavailable causes" comes from
  PRODUCT STU-BOOK-01 / A4.
- D-2: the syntax-before-actor checks and "no lifecycle call on rejection" come from the TECH §3.2 evaluation order
  ("only then any resource read or lifecycle call").
- D-3: missing content type 415 and `charset=UTF-8` accepted come from TECH §3.1 ("Content-Type: application/json")
  and INTEGRATION §1 (UTF-8). The empty, blank, number and truncated bodies come from TECH §3.1 ("Malformed JSON ...
  or a non-object body are 400").
- D-4: FORBIDDEN not invalidating the session is asserted (TECH §3.2 lists invalidation only for unknown/inactive
  accounts; INTEGRATION §2). This applies stu-c3 reviewer R3-02's proposal to the C2 route; stu-c1 N2 and stu-c3 N1
  left it unasserted. Whether a provider-empty 401 invalidates remains unasserted (stu-c1 R1-04, still with the analyst).
- D-5: C2-ERR-2 cases (c) deleted and (d) role changed follow from "re-run the access check and return its 401/403".
- D-6: a `bookings_pkey` duplicate giving 500 follows the TECH §3.3 clarification that only the named index maps to 409.
- N1: stu-c1 R1-01/R1-02 (bare `/api/student` 404 shape; vacuous `X-Request-Id` assertion in a c1 test) are still not
  attached to any change (stu-c3 R3-03). They are not C2 IDs, and R1-02 would need a c1 test edit, which this session
  must not make. They remain with the owner/analyst for scheduling.
- N2: F2-13 (teammate review of the c1 seam and security rule) is still an open merge gate. A change there could return
  the access cases of this class to the test writer. F2-15 (real auth adapter) remains an activation gate.
- N3: these tests have passed only against a throwaway prototype, never against repository code. The races depend on
  PostgreSQL row locks and the fixture's 4-second `awaitBlocked` window. An implementer who believes a case is wrong
  returns it to the test writer with evidence instead of editing it.

Readiness verdict: READY_FOR_IMPLEMENTATION for C2-T1 (including the TECH §2 lifecycle fail-closed check carried
from stu-c1 R1-05). The gate is current (analyst-2, hashes re-verified, c1/c3 dependency met and green), every
acceptance ID maps to an executable test, the baseline and behavioral red results are recorded, and setup
prerequisites are documented.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

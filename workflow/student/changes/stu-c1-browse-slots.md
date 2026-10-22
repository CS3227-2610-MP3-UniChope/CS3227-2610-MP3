# stu-c1-browse-slots: Students browse bookable slots (with Student API foundation)

Status: READY_FOR_HUMAN for C1-T1..C1-T4 per "Review reviewer-1" (PASS, LOW follow-ups R1-01..R1-03) (supersedes: IN_REVIEW per "Handoff implementer-1", which superseded READY_FOR_IMPLEMENTATION per "Handoff test-writer-1")
Scope classification and reason: standard path; new authenticated endpoints, new
shared actor seam, and a shared security-config change.
Parent request / related changes / dependencies: Phuc, 22 October 2026, "Implement
student booking according to given spec ... ignore AI feature". Split by capability
per NOTE.md into stu-c1 (this), [stu-c2-book-slot](stu-c2-book-slot.md),
[stu-c3-my-bookings](stu-c3-my-bookings.md), [stu-c4-cancel-booking](stu-c4-cancel-booking.md).
This change carries the shared foundation (tasks C1-T1 to C1-T3) that c2-c4 depend on.
Human owner / affected owners: Phuc (Student). Kok Seng (`platform/auth`: new
interface he will implement). Huanghao (`platform/web` security config he authored).
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
> Superseded in part by "Handoff analyst-2" (end of this record): inputs, hashes, base, open
> questions and readiness verdict. Acceptance scenarios below remain, as refined there.
Producer role / actual tool and model: Spec analyst / Claude Code (Opus 5.5)
Recipient role: Test writer, fresh session, `workflow/student/agents/test-writer.md`
Change ID / task / exact requested next action: stu-c1-browse-slots / C1-T1 to C1-T4 /
write failing tests for the acceptance IDs below and record the red result.
Stage status / supersedes: SPEC_DRAFT, first revision.
Goal and non-goals: Goal: an active student can list active modules and bookable future
slots, filtered by module and SGT date, through `/api/student`. Non-goals: booking,
cancelling, agenda, frontend, real login/session (auth-owned), production activation.
Acceptance IDs: S-SLOT-1, S-SLOT-2 (product spec §7); C1-ACC-1 to C1-ACC-5,
C1-IN-1 to C1-IN-4, C1-OUT-1 to C1-OUT-3, C1-ACT-1 to C1-ACT-3 (below).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/PRODUCT-SPEC.md §3 STU-SLOT-01, §7, §10 | Input | caed5db7e37ad5d34b2a478e954eab0a5d2ab933fffe9f80a8d71223f55cd9c5 | Requirement and scenarios |
| workflow/student/TECHNICAL-SPEC.md §1-§4, §8 | Input | bf32463e677b057f07810a4ad46dbb24c4cb32041e44ae4a6956e6ae29281d98 | API, access, errors, queries |
| workflow/tutor/REST-INCREMENT-SPEC.md §2-§5 | Input | commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d | Inherited input/activation rules |
| workflow/INTEGRATION-SPEC.md §1, §5 | Input | commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d | Shared conventions and error envelope |
| src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java, LifecyclePgFixture.java | Input (pattern) | commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d | Security-enabled MockMvc + PostgreSQL fixture pattern |
| src/backend/compose.test.yaml | Input (setup) | 7a7216c023621d861e0ba9169338c3f0a10d98b7ecf24e62751df97e4718e634 | Local disposable PostgreSQL 17 |
| changes/stu-c1-browse-slots/evidence/baseline-2026-10-22.txt | Input (evidence) | b9c6fbf665423b0313b2cc3a211a5a4de8efe70a2266f1d947e27c8b8444d7f1 | Passing pre-change baseline |
| src/backend/src/test/java/unichope/student/** and the Student PostgreSQL test class | Output (test writer) | to be recorded | Tests |

Base commit / result commit / complete diff or patch reference: base
502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d; no result commit yet.
Uncommitted files and their hashes: the three workflow/student specs and
compose.test.yaml above (PLAN.md 26e38bbc9153e3dbef47a7a5dc6562aaa6152a709fb8948cb58a9752f5f3b9d0).
Allowed write scope (test writer): `src/backend/src/test/java/unichope/student/**`; one
Student PostgreSQL MockMvc test class, which may sit in
`src/backend/src/test/java/unichope/platform/core/postgres/` to reuse the package-private
`LifecyclePgFixture`, as `TutorRestPgTest` does. No main-source changes, no changes to
existing tests. Test-only actor providers stay under `src/test`.
Allowed write scope (later implementer, for reference): `unichope/student/**`,
`unichope/platform/auth/CurrentActorProvider.java` (new), and the `student-api` rule in
`unichope/platform/web/ApiSecurityConfiguration.java`. No Tutor files.
Prerequisites / dependency state and evidence: backend scaffold and core lifecycle exist
at base; local PostgreSQL via compose verified by the baseline evidence.
Recorded decisions and authorization references (all Phuc, 22 October 2026, current
Claude Code session answers):
- D1 (superseded by D5 below) separate models for analysis/review and for tests/implementation.
- D2 `CurrentActorProvider` goes in `platform/auth` for teammates to implement; do not edit Tutor code.
- D3 Student owns a read-only repository querying core tables; writes only via `ConsultationLifecycle`.
- D4 Add Docker for the test database (`src/backend/compose.test.yaml`).
Open questions / proposals / unverified facts:
- A1 (product spec §10 item 1): any active student can book any active module's slot; no enrolment. Unconfirmed.
- A2 (§10 item 2): slots overlapping the student's own ACTIVE bookings are still listed. Unconfirmed.
- Adding `SLOT_UNAVAILABLE` to the integration spec's shared list needs team review (non-blocking).
- Kok Seng and Huanghao have not reviewed the `platform/auth` and `platform/web` additions.
Verification commands, directories, results and output references: see Verification evidence.
Readiness or review verdict, scope and supporting evidence: NOT READY until A1 and A2
are confirmed by Phuc; all other inputs are present and hashed.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

## Acceptance scenarios

Fixtures: two active tutors T1, T2; students S1, S2; an admin; modules M (active), M2
(active), M3 (inactive); T1 and T2 assigned to M and M2. Controllable clock N.

| ID | Given / When / Then |
| --- | --- |
| S-SLOT-1 | Given T1 slots that are AVAILABLE future, AVAILABLE started (start = N), BOOKED, CANCELLED, COMPLETED; a T2 AVAILABLE slot where T2 is inactive; one on inactive M3; one on M where T1's M assignment was removed / when S1 GETs `/slots` / then only the AVAILABLE future eligible slots appear, ordered startTime then id. |
| S-SLOT-2 | Given slots starting 2026-10-21T15:30Z (23:30 SGT on the 21st) and 2026-10-21T16:30Z (00:30 SGT on the 22nd) / when filtering `date=2026-10-22` / then only the second appears. `moduleId` + `date` intersect; a valid but unmatched `moduleId` (random UUID or inactive M3) returns `[]`. |
| C1-OUT-1 | `BookableSlot` JSON has exactly `id, moduleId, moduleCode, moduleName, tutorName, startTime, endTime`; times are UTC strings ending `Z`; `tutorName` equals `users.name`; no email appears anywhere in the body. |
| C1-OUT-2 | GET `/modules` returns exactly the active modules (M, M2; not M3) as `{ id, code, name }`, ordered code then id; returns `[]` when none are active. |
| C1-OUT-3 | Every success and error response has `Cache-Control: no-store`; errors are exactly `{ code, message, requestId }` with a server-generated requestId (a client `X-Request-Id` is not echoed). |
| C1-ACC-1 | For both GET endpoints: no authenticated principal → 401 `UNAUTHENTICATED`. |
| C1-ACC-2 | Principal whose ID is not in `users` → 401 `UNAUTHENTICATED`, session invalidated; the same session then gets 401. |
| C1-ACC-3 | S2 deactivated after sign-in → 403 `ACCOUNT_INACTIVE`, session invalidated; the same session then gets 401. |
| C1-ACC-4 | Active tutor or admin, even with a `ROLE_STUDENT` authority → 403 `FORBIDDEN`. A student session carrying `ROLE_TUTOR` still succeeds (stored role wins). |
| C1-ACC-5 | Two student sessions interleaved return results for their own actor each time (no singleton actor). |
| C1-IN-1 | `/modules` with any query parameter (e.g. `actorId`) → 400 `VALIDATION_FAILED`. |
| C1-IN-2 | `/slots` with an unknown parameter, repeated `date` or `moduleId`, non-UUID `moduleId`, or `date` of `2026-02-30`, `2026-1-01`, `""` → 400. |
| C1-IN-3 | PUT/DELETE on `/modules` → 405 `METHOD_NOT_ALLOWED` JSON with `Allow`; `Accept: text/html` → 406 `NOT_ACCEPTABLE` JSON; unknown `/api/student/unknown` → 404 `NOT_FOUND` JSON for both JSON and HTML Accept. |
| C1-IN-4 | Controller errors with `Accept: text/html` keep their JSON code (400/403). |
| C1-ACT-1 | Default startup (no profile) exposes no `/api/student/**` route: an authenticated request is denied, not served. |
| C1-ACT-2 | With `student-api` active but no `CurrentActorProvider` bean, startup fails. |
| C1-ACT-3 | `CurrentActorProvider` is `unichope.platform.auth.CurrentActorProvider`; no test-only provider exists in main sources; `tutor-api` behavior and existing tests are unchanged (full regression green). |

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| C1-T1 Actor seam in `platform/auth` + `StudentAccess` | C1-ACC-1..5, C1-ACT-3 | | pending |
| C1-T2 `student-api` profile wiring + security rule + framework error JSON | C1-ACT-1/2, C1-IN-3/4, C1-OUT-3 | | pending |
| C1-T3 `StudentReadRepository` (modules, bookable slots) | S-SLOT-1, S-SLOT-2, C1-OUT-1/2 | | pending |
| C1-T4 `GET /modules`, `GET /slots` and input parsing | C1-IN-1/2, S-SLOT-2 | | pending |

## Verification evidence
Command, working directory, revision, exit code, result and relevant output: baseline in
`stu-c1-browse-slots/evidence/baseline-2026-10-22.txt` (26 unit + 103 PostgreSQL tests
passing at 502d09f with compose database).
Pre-implementation expected failure (or existing passing baseline): pending (test writer).
Final focused tests / integration tests / build: pending.
Manual and real-provider checks: not applicable (no AI); manual API walkthrough pending.

## Independent review
Reviewer role / actual model / reviewed revision: pending.
Verdict and evidence: pending.
Findings: pending.
Fixes and re-review results: pending.

## Human verification
Person, date, checked journey, outcome: pending.
Remaining limitations and follow-ups: real login/session is simulated until auth
provides its provider; production migration activation pending.
Next agent, exact task and required inputs: Test writer (fresh session), after Phuc confirms A1/A2:
write failing tests for C1-T1..T4 using this record and the hashed inputs.


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

## Test-writer receipt 1 - 22 October 2026
Receiver role / actual tool and model: Test writer / Codex, GPT-6; fresh specialized session.
Task: C1-T1 through C1-T4 only. Standard path applies because this capability adds
protected Student endpoints and shared identity/security integration.
Receipt: RETURNED. Stage status: CHANGES_REQUIRED; this section does not supersede
the analyst's assessment or constitute a new Claude readiness verdict.

Checked inputs directly:
- HEAD is `502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d`.
- Product SHA-256 `ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957`
  matches the owner decision addendum; technical SHA-256
  `bf32463e677b057f07810a4ad46dbb24c4cb32041e44ae4a6956e6ae29281d98`
  and plan SHA-256 `26e38bbc9153e3dbef47a7a5dc6562aaa6152a709fb8948cb58a9752f5f3b9d0`
  match analyst-1.
- Compose SHA-256 `7a7216c023621d861e0ba9169338c3f0a10d98b7ecf24e62751df97e4718e634`
  matches analyst-1.
- `git diff --` for the referenced integration contract, Tutor REST increment spec,
  TutorRestPgTest and LifecyclePgFixture produced no differences from HEAD.
- Actual evidence file is
  `workflow/student/changes/stu-c1-browse-slots/evidence/baseline-2026-10-22.txt`,
  SHA-256 `b9c6fbf665423b0313b2cc3a211a5a4de8efe70a2266f1d947e27c8b8444d7f1`;
  it matches the packet hash. The manifest's `changes/...` path is relative to
  `workflow/student`, not the repository root. Its existing-suite result is
  historical evidence only; this receiver did not rerun it.
- Canonical Student product/technical/plan, shared product/technical/integration,
  Auth plan and inherited Tutor REST contract were inspected directly. A1/A2 are
  resolved by the recorded current human approval; no further approval is requested.

Exact gate gaps for the Claude analyst:
1. The packet has acceptance scenarios and a task table but no required
   requirement-to-technical coverage matrix or equivalent assessed artifact.
   Add current product and technical/integration references for every C1 acceptance
   ID, an explicit COVERED/MISSING/CONFLICT/UNVERIFIED assessment, and planned validation.
2. The packet has no spec findings and resolution register (nor an explicit audited
   no-findings assessment). Add the consistency/readiness findings with resolution
   references, including owner-review deferrals and their C1-specific impact.
3. With those artifacts added, issue a current C1-T1..T4 readiness handoff identifying
   the checked hashes and the now-recorded A1/A2 decisions. The existing conditional
   statement alone does not supply the missing coverage/findings evidence.

The test-writer role explicitly requires the analyst's coverage and resolved
findings before test editing. Independently deriving test expectations cannot
replace that analyst-stage evidence. No missing behavior or Claude assessment was
invented. No Claude tool is configured in this session; the required analyst action
remains a manual handoff.

Outputs / next action: no tests or main sources changed; no new preimplementation
baseline executed; READY_FOR_IMPLEMENTATION is not granted. Return this record and
canonical artifacts to a fresh Claude spec analyst to supply the missing artifacts,
then use a fresh GPT test-writing session to verify the revised packet and write/run
C1 tests. Preserve the remaining c3/c2/c4 dependency order and scope.

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
Supersedes: the "Handoff analyst-1" manifest above for inputs/hashes, base commit,
uncommitted-file list, open questions A1/A2 and the NOT READY verdict, and the Status
line's "SPEC_DRAFT" condition. The analyst-1 acceptance scenarios remain in force
except where "Acceptance refinements (analyst-2)" below adds or tightens a row.
Answers test-writer receipt 1 (gaps 1-3).
Producer role / actual tool and model: Spec analyst / Claude Code, Opus 5.5
(`claude-opus-5-5`); fresh subagent session, no inherited conversation (D5).
Recipient role: Test writer, fresh session, `workflow/student/agents/test-writer.md`.
Change ID / task / exact requested next action: stu-c1-browse-slots / C1-T1..C1-T4 /
verify this packet, rerun the existing-suite baseline at the current revision, write
the C1 tests for every acceptance ID below and record the behavioral red result.
Stage status: READY_FOR_TESTS (scoped; see Spec readiness (analyst-2)).
Goal and non-goals: unchanged from analyst-1.
Acceptance IDs: S-SLOT-1, S-SLOT-2, C1-OVL-1, C1-OUT-1..3, C1-ACC-1..5, C1-IN-1..4,
C1-PREC-1, C1-ACT-1..4.

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/PRODUCT-SPEC.md §2, §3 STU-SLOT-01, §4, §7, §10 (A1-A4) | Input | ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957 | Requirements, approved rules |
| workflow/student/TECHNICAL-SPEC.md §1-§4, §8 (revised by analyst-2: §3.2, §3.3, §8) | Input | e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3 | API, access, precedence, errors, queries |
| workflow/student/PLAN.md S0, S2-S4, §3 (revised by analyst-2) | Input | 31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a | Task order, commands |
| workflow/PRODUCT-SPEC.md §2-§4 | Input | d1d5a41451102985cb977ee999a13b82d48a4506c926ca899e1c7b283f70ba7a | AUTH-02, STU-01, SEC-01, REL-01 |
| workflow/SHARED-TECHNICAL-SPEC.md §2-§4, §6, §8 | Input | 54ec090a426a1a8733f7628924e4310d27a90b1b724d0acfad3eb0adcde35cfe | Layering, SH-AUTH-03/04, SH-DATA-01, SH-WEB-02 |
| workflow/INTEGRATION-SPEC.md §1, §2, §5, §7, §8 | Input | ac0d183f7f0d1de3ce1562a49f59d7de3a6ed8f90334620017ba64d6c2b3ba1f | Conventions, auth errors, error envelope, ownership |
| workflow/auth/TECHNICAL-SPEC.md; PRODUCT-SPEC.md; PLAN.md | Input | 50d964b75654f3ad7cae6d716a413d88b75d3d042ec09002e1375ca2f9a4b536; c5c0db50d2bfa0bde83f682786f69a7204e30ce36b9788817c70864f5e0c31b8; 136e53062b65e9b98fd756ecc77cd656e7c2a7c597147e3fd4bb36ba97db25af | Actor provider ownership, session policy |
| workflow/tutor/REST-INCREMENT-SPEC.md §2-§6 | Input | 8c1f07f2081742c6d365aff69628decdb6ee4d67e5305d101a9065814f3c028a | Inherited input/identity/activation rules |
| src/backend/src/main/java/unichope/platform/web/ApiSecurityConfiguration.java | Input (code) | fcd2e2bf9d9c8c8738bfaf1f6e588033903532bee4d5fd747e9ec12eb956e603 | Default deny-all, CSRF, JSON entry points |
| src/backend/src/main/java/unichope/tutor/http/* and tutor/TutorAccess.java | Input (pattern) | commit 459fd6ef7108c2d08aeaa0f92ae0bccc054158b5 (unchanged since 98ca801) | Controller/input/error-resolver pattern |
| src/backend/src/main/resources/db/core-review/V1__create_core_tables.sql | Input (code) | 1cdea0be86ff0718eb9c1ed5131f5eec014c845eeb954d537b68e7372dccfe9c | Tables read by Student |
| src/backend/src/test/java/unichope/platform/core/postgres/LifecyclePgFixture.java | Input (test pattern) | ede25ee845b6c20cb060c5ec1afd7b5b0f37c9b5e2a381b065c66d03b2d92db3 | Fixture, clock, `race` helper |
| src/backend/src/test/java/unichope/platform/core/postgres/TutorRestPgTest.java | Input (test pattern) | d12de2d1107e28a877ea6762d3656d5d05defe654cf600b7d437f90230ca66ca | Security-enabled MockMvc + PostgreSQL |
| src/backend/compose.test.yaml | Input (setup) | 7a7216c023621d861e0ba9169338c3f0a10d98b7ecf24e62751df97e4718e634 | Disposable PostgreSQL 17 |
| workflow/student/changes/stu-c1-browse-slots/evidence/baseline-2026-10-22.txt | Input (historical evidence) | b9c6fbf665423b0313b2cc3a211a5a4de8efe70a2266f1d947e27c8b8444d7f1 | Pre-change suite result at 502d09f |
| workflow/student/NOTE.md; HANDOFF.md; agents/spec-analyst.md | Input (process) | 5e8ba76fcd3a52a08925ba744dca8e4adda572403b2ac800ec58dbef2b5d62e4; 1ee65fd926dff0a710597508226cf109cb723678b760111e7d6120adfe5b5596; 9afb32654a6cbc010e7b7267d3b4c8a3cf04098a1bc0337a7a5f9af9f7183104 | Workflow |
| Student tests under src/backend/src/test/java/unichope/student/** and one Student PostgreSQL MockMvc class | Output (test writer) | to be recorded | Tests |

Base commit / result commit / diff: base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5`
(the analyst-1 base 502d09f plus commits 6d94f87 and 459fd6e, which add
compose.test.yaml and commit the workflow files; `git diff --stat 98ca801 HEAD -- src/`
lists only compose.test.yaml). Analyst-2 working-tree changes (uncommitted):
TECHNICAL-SPEC.md and PLAN.md (hashes above), this record, the other three records and
ANALYST-READINESS-HANDOFF.md. No source or test file changed.
Allowed write scope (test writer): unchanged from analyst-1:
`src/backend/src/test/java/unichope/student/**` plus one Student PostgreSQL MockMvc
class that may live in `src/backend/src/test/java/unichope/platform/core/postgres/` to
reuse the package-private `LifecyclePgFixture`. No main-source changes, no edits to
existing tests, test-only actor providers under `src/test`.
Allowed write scope (later implementer, reference): `unichope/student/**`, new
`unichope/platform/auth/CurrentActorProvider.java`, and only the conditional
`student-api` rule in `unichope/platform/web/ApiSecurityConfiguration.java`. No Tutor,
core, migration or build file.
Prerequisites / dependency state: backend scaffold, core lifecycle, security
configuration and PostgreSQL fixture exist at base (inspected). Baseline evidence is
historical (502d09f; backend code identical); the test writer reruns it.
Recorded decisions: D2, D3, D4 (analyst-1 manifest), A1-A4 (PRODUCT-SPEC §10, Phuc,
22 October 2026), D5 (workflow decision addendum). D1 superseded by D5.
Open questions / proposals / unverified facts: findings F2-13, F2-14, F2-15 (deferred,
owner-gated, non-blocking for tests; see register). Real login/session behavior is
not verifiable until the auth adapter exists (simulated authentication only).
Verification commands: none run in this analyst session (documentation and code
reading plus `sha256sum` only).
Readiness verdict: READY_FOR_TESTS for C1-T1..C1-T4, conditions in Spec readiness (analyst-2).

### Receiver receipt
Receiver role / actual tool and model: Test writer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` not read.
Checked task, artifact revisions and required evidence (22 October 2026):
- Task C1-T1..C1-T4 only; HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` equals the
  analyst-2 base. `git diff --stat HEAD -- src/` is empty and no untracked file exists
  under `src/`, so source and tests equal the base. `git diff --stat 98ca801 HEAD -- src/`
  lists only compose.test.yaml, which confirms the "Tutor code unchanged since 98ca801" claim.
- `sha256sum` of every hashed input in the analyst-2 manifest matches exactly:
  Student PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., PLAN 31545bfb...; shared PRODUCT
  d1d5a414..., SHARED-TECHNICAL 54ec090a..., INTEGRATION ac0d183f...; auth TECHNICAL
  50d964b7..., PRODUCT c5c0db50..., PLAN 136e5306...; Tutor REST-INCREMENT 8c1f07f2...;
  ApiSecurityConfiguration fcd2e2bf...; core V1 DDL 1cdea0be...; LifecyclePgFixture
  ede25ee8...; TutorRestPgTest d12de2d1...; compose.test.yaml 7a7216c0...; baseline evidence
  b9c6fbf6...; NOTE 5e8ba76f...; HANDOFF 1ee65fd9...; spec-analyst prompt 9afb3265....
- Required analyst artifacts are present: requirement-to-technical coverage matrix
  (analyst-2), findings register F2-01..F2-19 with resolutions, scoped READY_FOR_TESTS
  verdict, and acceptance refinements. A1-A4 are recorded in PRODUCT §10 (Phuc,
  22 October 2026), and D5-D7 are in the decision addenda. Open findings F2-13/14/15 are
  deferred as merge or activation gates and do not block writing tests. This closes the gaps
  of test-writer receipt 1.
- I read the canonical sections directly: TECH §2 (layout, activation), §3 (DTOs,
  `/modules`, `/slots`), §3.1 (input), §3.2 (access table and evaluation order),
  §3.3 (envelope, 405/406/404), §4 (queries), and Tutor REST §2-§5. They agree with
  every C1 acceptance row. No requirement/technical conflict was found for C1.
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none. Analyst condition (1), the
existing-suite rerun at the current revision, is fulfilled in "Handoff test-writer-1".

## Acceptance refinements (analyst-2)
These rows add to or tighten the analyst-1 "Acceptance scenarios" table; where an ID
repeats, this row replaces the earlier wording.

| ID | Given / When / Then |
| --- | --- |
| C1-OVL-1 (new, A2, F2-02) | Given S1 holds an ACTIVE booking 10:00-11:00 SGT on a T1 slot and an eligible AVAILABLE T2 slot 10:30-11:30 SGT exists / when S1 GETs `/slots` / then the T2 slot is listed (overlap with the student's own bookings never hides a slot). |
| C1-IN-3 (tightened, F2-03) | PUT and DELETE on `/modules` **with a valid CSRF token** and a student session → 405 `METHOD_NOT_ALLOWED` JSON with an `Allow` header containing `GET`; `Accept: text/html` on a valid GET → 406 `NOT_ACCEPTABLE` JSON; GET `/api/student/unknown` with a student session → 404 `NOT_FOUND` JSON for both `application/json` and `text/html` Accept. All have `Cache-Control: no-store`. |
| C1-PREC-1 (new, F2-06) | Given S2 is deactivated after sign-in / when S2's session GETs `/modules?x=1` / then 400 `VALIDATION_FAILED` and the session is still valid; when the same session then GETs `/modules` / then 403 `ACCOUNT_INACTIVE` and the session is invalidated. |
| C1-ACT-1 (tightened, F2-04) | Default startup (no profile) registers no Student controller bean or handler mapping; GET `/api/student/modules` without a principal → 401 `UNAUTHENTICATED` JSON; with an authenticated student session → 403 `FORBIDDEN` JSON; neither response contains Student data; both `no-store`. |
| C1-ACT-4 (new, F2-05) | With both `tutor-api` and `student-api` active (test-only providers for both seams): a tutor session gets 200 on `/api/tutor/modules`, a student session gets 200 on `/api/student/modules`, and `/api/tutor/unknown` and `/api/student/unknown` each return 404 `NOT_FOUND` JSON. With only `student-api` active, `/api/tutor/modules` with a tutor session is denied (403 `FORBIDDEN` JSON) and serves no Tutor data. |

Task mapping additions: C1-OVL-1 → C1-T3; C1-PREC-1 → C1-T1 and C1-T4; C1-ACT-4 → C1-T2.

Test-writer notes (not new behavior): `LifecyclePgFixture` names every user
"Synthetic", so C1-OUT-1 needs the Student test to give the tutor a distinct name;
inactive module M3 and the removed-assignment case are added by the Student test. The
Student web context should use the fixture `source` so `race`/`awaitBlocked`
(application_name) work for later changes.

## Requirement-to-technical coverage (analyst-2)
COVERED means specification support only, not implemented or tested behavior.
"PG" = Student MockMvc-to-PostgreSQL test run by `postgresTest`; "Unit" = `test` task.

| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| S-SLOT-1 | STU-SLOT-01 bullets 1-2; §7 S-SLOT-1; shared STU-01 | TECH §3 `/slots` bullet, §4 bookable-slots query; INTEGRATION §7 "Student books" preconditions (same eligibility); core `requireEligibility` | COVERED | PG: exclusion matrix, ordering incl. equal-start tie |
| S-SLOT-2 | STU-SLOT-01 bullet 3; §4 SGT rule | TECH §3 `date`, §3.1, §4 half-open SGT range; SHARED SH-DATA-01; INTEGRATION §1 | COVERED | PG: 15:30Z/16:30Z boundary, filter intersection, unmatched/inactive module `[]` |
| C1-OVL-1 | STU-SLOT-01 bullet 4; §10 item 2 (A2) | TECH §3 `/slots` criteria and §4 query contain no student-overlap condition | COVERED | PG |
| C1-OUT-1 | §2 bullet 4; STU-SLOT-01 bullet 2 | TECH §3 `BookableSlot`, exact-fields bullet; INTEGRATION §1 UTC `Z` instants | COVERED | PG: exact key set, `Z` suffix, tutorName = users.name, no `@` in body |
| C1-OUT-2 | STU-SLOT-01 bullet 3 (active-module filter) | TECH §3 GET `/modules`, §4 active modules | COVERED | PG: active only, code/id order, empty `[]` |
| C1-OUT-3 | §4 (request IDs); shared SEC | TECH §3.3 closing paragraph; INTEGRATION §5; SHARED SH-WEB-02; `ApiErrors`/`ApiResponseFilter` | COVERED | PG: headers and envelope on success, controller and filter errors |
| C1-ACC-1 | §2 bullet 3; shared AUTH-02 | TECH §3.2 row 1; INTEGRATION §2; `ApiSecurityConfiguration` entry point | COVERED | PG |
| C1-ACC-2 | §2 bullet 3 | TECH §3.2 row 2; Tutor REST §3 (deleted-account policy) | COVERED (real auth adapter alignment UNVERIFIED, F2-15) | PG with test-only provider and stored session |
| C1-ACC-3 | §2 bullet 3 (existing sessions) | TECH §3.2 row 3; SHARED SH-AUTH-04; INTEGRATION §2 | COVERED | PG: 403 then 401 on the same session |
| C1-ACC-4 | §2 bullets 1, 3 | TECH §3.2 row 4 (stored role wins); SHARED SH-AUTH-01/04 | COVERED | PG |
| C1-ACC-5 | §2 bullet 2 | TECH §2 request-scoped actor; Tutor REST §3 (no singleton actor) | COVERED | PG: interleaved sessions |
| C1-IN-1, C1-IN-2 | §2 bullet 2 (identifiers cannot expand access) | TECH §3.1; INTEGRATION §1 (unknown params 400); Tutor REST §2 | COVERED | PG (Unit for the parser if separate) |
| C1-IN-3, C1-IN-4 | (technical only) | TECH §3.3 closing paragraph; Tutor REST §4 (REST-R1 clarification) | COVERED | PG/Unit web context |
| C1-PREC-1 | §2 bullet 3 | TECH §3.2 evaluation order (analyst-2) | COVERED | PG |
| C1-ACT-1..3 | (technical only) | TECH §2 activation paragraph; Tutor REST §5; INTEGRATION §8 (auth supplies provider) | COVERED | Unit: activation tests modelled on `TutorActivationTest`; full regression |
| C1-ACT-4 | (technical only) | TECH §2 table row `platform/web`; `ApiSecurityConfiguration` | COVERED | Unit/PG web context with both profiles |
| REL-01, logging (Student §4 "routine logs") | shared REL-01; Student §4 | INTEGRATION §5 (no request bodies in logs) | COVERED | Reviewer checklist (F2-17); read-only change has no durability path |

Out of scope for this change (no coverage claimed): STU-SLOT-01 empty-state and refresh
UI, §5 UI states/keyboard, React tests (PLAN S2 frontend part, waiting for a frontend
scaffold); S-AUTH-1's LLM clause (no AI here); production activation.

## Spec findings and proposed fixes (analyst-2)
Full register for the four booking changes; the c3, c2 and c4 records repeat the rows
that apply to them.

| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| F2-01 / MEDIUM | PLAN.md (66e77b89) S0/S2/S3 required new core queries and `ConsultationLifecycle.bookSlot`/`cancelBooking` "coordinated with Huanghao", and S4 depended on S3. This contradicts TECH §1/§4 and D2/D3 (existing `bookAvailableSlot`/`cancelActiveBooking` unchanged, Student-owned read repository) and the recorded c1→c3→c2→c4 order. PLAN §3's `./gradlew test --tests 'unichope.student.*'` excludes the `postgres` tag, so it could report green without running the Student PostgreSQL tests. Impact: an implementer could request or alter core, or record false verification. | Align PLAN S0, S2, S3, S4 and §3 with TECH and D2/D3; keep the old S0 item visible as superseded. Student-owned plan; applies recorded decisions only. | Phuc (Student) | RESOLVED: PLAN.md 31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a |
| F2-02 / MEDIUM | Approved rule A2 (PRODUCT §10 item 2; STU-SLOT-01 bullet 4) had no acceptance scenario in analyst-1. Impact: an implementation hiding overlapping slots would pass. | Add C1-OVL-1. | Phuc | RESOLVED: Acceptance refinements (analyst-2) |
| F2-03 / LOW | C1-IN-3 asked for 405 on PUT/DELETE without CSRF; `ApiSecurityConfiguration` CSRF runs first and returns 403 `CSRF_INVALID` (Tutor tests use a valid token, `TutorRestPgTest` line 242). Impact: untestable as written. | Require a valid CSRF token for the 405 cases. | Phuc | RESOLVED: C1-IN-3 tightened |
| F2-04 / LOW | C1-ACT-1 oracle "denied, not served" was not observable. The default chain is `denyAll`: anonymous → 401 JSON, authenticated → 403 `FORBIDDEN` JSON (`TutorActivationTest`). | State exact statuses and "no Student handler". | Phuc | RESOLVED: C1-ACT-1 tightened |
| F2-05 / LOW | The `student-api` edit touches the security chain Tutor uses; no scenario covered both profiles together (the eventual production combination) or Student-only activation not exposing Tutor. | Add C1-ACT-4. | Phuc | RESOLVED: C1-ACT-4 |
| F2-06 / LOW | Order of syntax validation vs actor check was unspecified (TECH §3.1/§3.2); `TutorController` parses input before `TutorAccess`. Tests could encode either; an inactive session with bad input could be invalidated or not. | Specify the order in TECH §3.2 (filter → syntax → actor → resource/lifecycle) and add C1-PREC-1. | Phuc | RESOLVED: TECH-SPEC e4ba47b0 §3.2; C1-PREC-1 |
| F2-07 / MEDIUM | TECH §3.3 maps a unique-index violation on `bookings_one_active_per_slot` to 409 but gave no recognisable trigger; core `mutate` rethrows non-contention `DataAccessException` unchanged. Impact: C2-ERR-1 untestable; other integrity errors might be mislabelled 409. | TECH §3.3: map `DataIntegrityViolationException` (incl. `DuplicateKeyException`) whose chain message names the index; every other `DataAccessException` → 500; define the test doubles in c2. | Phuc | RESOLVED: TECH-SPEC e4ba47b0 §3.3; c2 C2-ERR-1 (applies to c2 only) |
| F2-08 / LOW | The response when a slot is both unavailable and overlapping was unspecified; core checks state/eligibility before overlap. | TECH §3.3 note (SLOT_UNAVAILABLE wins); c2 C2-PREC-1. | Phuc | RESOLVED: TECH-SPEC e4ba47b0 §3.3 (c2 only) |
| F2-09 / LOW | The lifecycle `ACCESS_DENIED` "still valid" branch lacked acceptance and a session-state oracle; INTEGRATION §2 says the generic result alone must not invalidate. | TECH §3.2 wording; extend C2-ERR-2 and C4-ERR-1. | Phuc | RESOLVED: TECH-SPEC e4ba47b0 §3.2; c2/c4 refinements (c2, c4 only) |
| F2-10 / LOW | Lifecycle codes not listed in TECH §3.3 had no mapping (Tutor maps unknown codes to 409 with the code name). | TECH §3.3 row: any other lifecycle code → 500. | Phuc | RESOLVED: TECH-SPEC e4ba47b0 §3.3 (c2, c4) |
| F2-11 / LOW | A3 (no notice period) had no positive boundary case for the cancel endpoint. | Add C4-BND-1. | Phuc | RESOLVED (c4 only) |
| F2-12 / LOW | C4-IN-1 listed rejected inputs without per-case status. | State the statuses explicitly. | Phuc | RESOLVED (c4 only) |
| F2-13 / MEDIUM | New `unichope.platform.auth.CurrentActorProvider` (package of auth owner Kok Seng) and a `student-api` rule in `platform/web/ApiSecurityConfiguration` (authored by Huanghao; maintainer "assigned per task"). SHARED §2 requires an affected teammate's review of shared platform changes; PLAN §1 requires it before dependent code merges; INTEGRATION §8 leaves the principal adapter to auth. No such review is recorded. Impact: a different seam or rule would change C1-T1/T2 tests and code. | Keep both additions minimal (interface only; one profile-gated matcher; default deny-all and Tutor rule untouched); request Kok Seng's and Huanghao's review on the Student PR before merge to `main`; if they require a change, return to the analyst, which invalidates affected c1 tests. Added as a PLAN S0 item. | Kok Seng (platform/auth), Huanghao (platform/web) | OPEN, DEFERRED: merge gate, not a test-writing gate (tests stay in Student test scope; D2 authorizes the Student side) |
| F2-14 / LOW | `SLOT_UNAVAILABLE` (new) and Student use of `BOOKING_OVERLAP` are not in INTEGRATION §5's shared code list; TECH §3 declares them Student-local and PLAN S0 keeps the integration change pending. Only the Student UI consumes them. | Leave Student-local; add to INTEGRATION in a reviewed team change before any shared frontend client depends on them. | Team (integration contract owners) | OPEN, DEFERRED: non-blocking for backend c1-c4 |
| F2-15 / LOW | Student access tests use a test-only provider (simulated authentication). The real auth adapter must return empty/invalidate for deleted accounts and obsolete `sessionGeneration` (auth TECHNICAL-SPEC "Components and locking"). | Record as an activation gate; the auth owner aligns the adapter with the Student/Tutor §3.2 policy. | Kok Seng | OPEN, DEFERRED: production activation gate |
| F2-16 / LOW | TECH §8 STU-BOOK-01 row said "Lifecycle integration tests on Testcontainers PostgreSQL" while TECH §4/§8 footer and the build use `postgresTest` against an explicit disposable database and Student tests are HTTP-to-PostgreSQL. SHARED §1/§8 still say Testcontainers (team-owned wording; the repository convention is the existing `postgresTest`). | Fix the Student row; leave the shared wording to the team. | Phuc; team for SHARED | RESOLVED (Student row, TECH-SPEC e4ba47b0); SHARED wording DEFERRED, non-blocking |
| F2-17 / LOW | No booking acceptance covers "no request bodies, SQL or session data in logs" (INTEGRATION §5, Student §4). Booking bodies hold only IDs; a log oracle would be brittle. | Reviewer checklist item for c1-c4: Student code adds no request-body/SQL/session logging; 500 bodies carry no exception text (tested by C2-ERR-1/C4-ERR-1). | Phuc | RESOLVED by plan: reviewer check |
| F2-18 / INFO | Records cited base 502d09f with uncommitted specs; HEAD is 459fd6e with identical hashes; backend code unchanged since 98ca801 except compose.test.yaml. | Re-anchor in the analyst-2 manifests. | - | RESOLVED: this manifest |
| F2-19 / LOW | My-bookings list/detail had no scenario showing a student's own booking remains visible after its tutor/module becomes ineligible (an implementer copying slot-eligibility filters would hide history). | Add C3-ELIG-1 and a matching C4-ELIG-1 for cancellation. | Phuc | RESOLVED (c3, c4 only) |

Audited without findings (compared, no conflict): stored-role/active recheck and
invalidation (TECH §3.2 vs SHARED SH-AUTH-04, INTEGRATION §2, Tutor REST §3,
`TutorAccess`/`TutorExceptionHandler`); lifecycle `ACCESS_DENIED` re-evaluation (TECH §3.2
vs INTEGRATION §2 vs `TutorHttpService.mutate`: consistent once the F2-09 wording was
added); SGT half-open date filter (TECH §4 vs SH-DATA-01 vs the Tutor service); "started"
and `cancellable` predicates (`startTime > now` vs core `isAfter(clock.instant())`);
eligibility conditions of `/slots` vs core `requireEligibility` (active TUTOR, active
module, assignment) and the slot status/start check; body/query/media/64 KiB rules
(TECH §3.1 vs Tutor REST §2 vs `TutorApiInput`); exact DTO privacy (no emails) vs
PRODUCT §2 and INTEGRATION §3/§5; no migration and no core write (TECH §4 vs SHARED §2,
INTEGRATION §6-§7). AI checklist area: not applicable (no AI in c1-c4).
Remains unverified: real auth adapter/session behavior (F2-15); runtime behavior of any
Student code (none exists).

## Shared-owner boundary and dependency check (analyst-2)
- `unichope/student/**`: Student-owned (Phuc). Reviewer per SHARED §2: Tutor owner.
- `unichope/platform/auth/CurrentActorProvider.java` (new): Kok Seng's package;
  authorized on the Student side by D2; review pending (F2-13).
- `unichope/platform/web/ApiSecurityConfiguration.java`: one profile-gated matcher;
  Tutor rule, default deny-all, CSRF repository and entry points unchanged; review
  pending (F2-13); C1-ACT-1/3/4 and the full regression protect Tutor.
- `platform/core`: no change. Student reads `users`, `modules`, `tutor_modules`,
  `slots`, `bookings` read-only (D3; same precedent as `PostgresTutorReadRepository`).
- No Tutor file, migration, build file or Admin/auth implementation is touched;
  `student` imports only `platform/*`.
- Upstream dependencies: none beyond the existing scaffold, core and fixture.
- Downstream: stu-c3, stu-c2, stu-c4 need C1-T1..T4 implemented first.

## Spec readiness (analyst-2)
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision: stu-c1 C1-T1..C1-T4 against PRODUCT-SPEC
ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957, TECHNICAL-SPEC
e4ba47b06612eba4ce9d1d4e42df51e0d9019a06efb5c6db9226171a19bcbdf3, PLAN
31545bfb44b95945aecf0e68c76b31b4adc28a53f3ca9bdd47bc88365db6603a, code at 459fd6e.
Remaining findings and why deferral does not affect this task: F2-13 (merge gate; tests
are confined to the Student test scope and the seam/rule are recorded in TECH §2), F2-14
(Student-local codes, no shared consumer yet), F2-15 (production activation; tests use
simulated identity by design, as Tutor did), F2-16 SHARED wording (no effect on Student
commands). No HIGH/CRITICAL finding; no in-scope requirement lacks coverage.
Required owner decisions and recorded outcome: A1-A4 approved (PRODUCT §10); D2-D5
recorded; Kok Seng/Huanghao review is required before merge, not before tests.
Recheck evidence: after the TECH-SPEC/PLAN edits, every C1 ID above was re-traced to the
revised sections; `sha256sum` of the revised files is recorded above.
Conditions: (1) the test writer reruns the existing-suite baseline at the current
revision before writing tests; (2) any change requested under F2-13 returns to the
analyst and invalidates affected C1 tests; (3) any later edit to the hashed specs
invalidates this verdict.
Next agent, task, acceptance IDs, allowed files and prerequisites: fresh test writer;
C1-T1..T4; IDs listed in Handoff analyst-2; allowed files as in its manifest;
prerequisite: compose PostgreSQL available locally.

## Owner decision addendum 2 - 22 October 2026
Recorded by the coordinating session (Claude Code, Opus 5.5) as a human decision record;
not an analyst or test-writer stage. Authorization: Phuc, current user answers on
22 October 2026 to the two owner questions raised by analyst-2.
- D6 (resolves the gating question in F2-13): teammate review of the new
  `platform/auth` interface (Kok Seng) and the `student-api` rule in `platform/web`
  (Huanghao) is required before `branch_student` merges to `main`, not before test
  writing or implementation. F2-13 stays OPEN as a merge gate. If either reviewer asks
  for a different design, c1 returns to the spec analyst and affected tests are redone.
- D7 (F2-14): adding `SLOT_UNAVAILABLE` to the shared INTEGRATION-SPEC error list is
  deferred; the Student technical spec covers it for now. F2-14 stays OPEN, non-blocking.
These decisions apply equally to stu-c2, stu-c3 and stu-c4. No acceptance behavior or
readiness verdict changed.

## Handoff test-writer-1
Producer role / actual tool and model: Test writer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Recipient role: Implementer, fresh session, `workflow/student/agents/implementer.md`.
Change ID / task / exact requested next action: stu-c1-browse-slots / C1-T1..C1-T4 /
implement the actor seam, `StudentAccess`, `student-api` wiring and security rule, framework
error JSON, `StudentReadRepository` and the `GET /modules` and `GET /slots` endpoints so that every
test below passes. Do not change the tests. Return any test you believe is wrong to the test
writer, with evidence.
Stage status / supersedes: READY_FOR_IMPLEMENTATION; first test-writer revision. Supersedes
nothing. "Test-writer receipt 1" (RETURNED) stays as history.
Goal and non-goals: as in analyst-1 and analyst-2 (browse only; no booking, cancellation,
agenda, frontend, real login or production activation).
Acceptance IDs: S-SLOT-1, S-SLOT-2, C1-OVL-1, C1-OUT-1..3, C1-ACC-1..5, C1-IN-1..4,
C1-PREC-1, C1-ACT-1..4 (the analyst-2 list; nothing added or dropped).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| This record: Handoff analyst-2, Acceptance scenarios + refinements, coverage, findings, Spec readiness (analyst-2), addenda D5-D7 | Input | (this file) | Gate and acceptance oracles |
| All analyst-2 manifest inputs (Student PRODUCT/TECHNICAL/PLAN, shared specs, auth specs, Tutor REST spec, ApiSecurityConfiguration, V1 DDL, LifecyclePgFixture, TutorRestPgTest, compose.test.yaml) | Input | hashes re-verified, identical to analyst-2 (see the analyst-2 receiver receipt) | Canonical baseline |
| src/backend/src/test/java/unichope/student/StudentTestActors.java | Output (test support) | 9e70e1ee9d7a69e99c496823879825a3965674f9975de9dffd2664f631f853e9 | Test-only session actor provider for `unichope.platform.auth.CurrentActorProvider`, found by name |
| src/backend/src/test/java/unichope/student/http/StudentActivationTest.java | Output (unit `test`) | 15d137587581fd2e96ad33cf859fb66d15cb624593e31ea1d32deecde2547ae9 | C1-ACT-1, C1-ACT-3 (seam type) |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBrowsePgTest.java | Output (`postgresTest`, tag `postgres`) | 39206a32ecfc81f9f2939bac69ee36e0b897654c708faf233af24be56ce21624 | All other C1 IDs, MockMvc + Spring Security + real PostgreSQL |
| workflow/student/changes/stu-c1-browse-slots/evidence/baseline-test-writer-2026-10-22.txt | Output (evidence) | b35b248570edefafcbc45b829bbe9ed029eebc79823908a59211940470adb32a | Existing-suite rerun at 459fd6e |
| workflow/student/changes/stu-c1-browse-slots/evidence/red-run-test-writer-2026-10-22.txt | Output (evidence) | 8d6773cc23d3e69d6cf2d822825c7389b0756f91f00848ddeb06fab892461f40 | Pre-implementation red run |

Base commit / result commit / diff: base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5`; no commit.
The diff under `src/` consists only of the three untracked test files above. `git diff --stat -- src/`
is empty, so no main source and no existing test changed.
Uncommitted files and their hashes: the three test files and two evidence files above, plus this
record (the analyst-2 receiver receipt, the Status line and this section). The analyst-era
uncommitted workflow files are unchanged from their analyst-2 hashes.
Allowed write scope (used): `src/backend/src/test/java/unichope/student/**` plus one Student
PostgreSQL MockMvc class in `.../platform/core/postgres/`, as analyst-2 allows. The test-only
provider is under `src/test`.
Allowed write scope (implementer, from analyst-2): `unichope/student/**`, new
`unichope/platform/auth/CurrentActorProvider.java`, and only the conditional `student-api` rule
in `unichope/platform/web/ApiSecurityConfiguration.java`. No Tutor, core, migration or build file.
Prerequisites / dependency state: compose PostgreSQL 17 (`src/backend/compose.test.yaml`) and
Docker. Both were used and the database was stopped afterwards (`docker compose -f compose.test.yaml down`).
Recorded decisions and authorization references: D2-D7 and A1-A4 as cited in analyst-2 and the
addenda. No new decision was made in this stage.
Open questions / proposals / unverified facts: see "Test design notes and gaps" below. Real
login/session behavior remains unverified (F2-15; simulated authentication only).

### Test design (why the red is behavioral, not a compile failure)
The product types did not exist at the base revision. A compile-time reference to them would
break compilation of the whole test source set, which would also stop the existing suite from
running. NOTE.md classes that as a setup failure, not a valid red. The tests therefore:
- discover Student code by **component scan of `unichope.student`** with profile `student-api`
  (manual web context: `DelegatingWebMvcConfiguration` + `ApiSecurityConfiguration` + scan).
  The real Boot application (`UnichopeApplication`, scanning `unichope`) is used for the
  activation tests. Implementation classes must be discoverable that way: `@Configuration`,
  `@RestController` or `@Component` under `unichope.student`, gated by `@Profile("student-api")`;
- locate the actor seam by its specified fully qualified name `unichope.platform.auth.CurrentActorProvider`
  in `StudentTestActors`. Renaming it requires changing the spec and these tests together (TECH §2).
Before implementation the requests reach the unchanged deny-all chain (403/401), and the
contexts start where they must fail closed, so every red is an assertion on HTTP status or startup outcome.

**Beans the tests supply to Student wiring (implementer setup contract):** `DataSource`
(fixture schema, `application_name` = schema for later `race` use), `Clock` (fixture
`MovingClock`, N = 2026-10-21T00:00Z), `ConsultationLifecycle` (real `PostgresConsultationLifecycle`),
core `UserRepository`, `ModuleRepository`, `SlotRepository`, `BookingRepository` (Postgres
implementations), and the test-only `CurrentActorProvider` (UUID principal of the current
request's security context). No `JdbcClient` bean is supplied, so the Student read repository must be
built from `DataSource`. If the Student wiring needs any other bean, return the issue to the test writer.

### Tasks and acceptance-to-test mapping (test-writer-1)
Classes: `PG` = `unichope.platform.core.postgres.StudentBrowsePgTest`; `ACT` =
`unichope.student.http.StudentActivationTest`. "Red now" = observed failing at the base revision.

| Task | Acceptance ID | Test case | Pre-implementation state |
| --- | --- | --- | --- |
| C1-T3/T4 | S-SLOT-1 | PG `sSlot1OnlyBookableFutureEligibleSlotsOrderedByStartThenId`: AVAILABLE future vs started (start = N), past, BOOKED/CANCELLED/COMPLETED, inactive tutor, inactive assigned module M3, removed assignment (T3), non-TUTOR owner (ADMIN, TECH §4 `t.role`); equal-start tie by id (ids chosen to defeat insertion/module-code order) | Red now (403 vs 200) |
| C1-T3/T4 | S-SLOT-2 | PG `sSlot2SgtDateBoundaryModuleFilterIntersectionAndUnmatched`: 15:30Z/16:30Z boundary, 23:59:59 SGT included and 00:00 SGT next day excluded (half-open), module filter, intersection, unmatched/random/inactive-module `[]`, empty date | Red now |
| C1-T3 | C1-OVL-1 | PG `c1Ovl1SlotOverlappingOwnActiveBookingIsStillListed` | Red now |
| C1-T3 | C1-OUT-1 | PG `c1Out1BookableSlotHasExactSafeFields`: exact 7 keys, values, `Z` instants equal to stored instants, tutorName = users.name, no `@` or tutor id in body | Red now |
| C1-T3 | C1-OUT-2 | PG `c1Out2ModulesAreActiveExactOrderedAndEmpty`: active modules including an unassigned one, code order, exact keys, `[]` | Red now |
| C1-T2 | C1-OUT-3 | PG `c1Out3NoStoreAndServerGeneratedRequestIds`; `no-store` is also asserted on every response by the shared `ok`/`error` helpers; envelope exactness and absence of the client marker in every `error(...)` | Red now |
| C1-T1 | C1-ACC-1 | PG `c1Acc1MissingPrincipalOrProviderActorIsUnauthenticated` (anonymous → filter 401; authenticated non-UUID principal → provider empty → 401) | Red now (anonymous half already passes) |
| C1-T1 | C1-ACC-2 | PG `c1Acc2DeletedActorIsUnauthenticatedAndSessionInvalidated` (both endpoints; invalidated; reuse 401) | Red now |
| C1-T1 | C1-ACC-3 | PG `c1Acc3InactiveStudentIsDeniedAndSessionInvalidated` | Red now |
| C1-T1 | C1-ACC-4 | PG `c1Acc4StoredRoleWinsOverSessionAuthorities` (tutor/admin with ROLE_STUDENT → 403 FORBIDDEN; student with ROLE_TUTOR → 200) | Red now |
| C1-T1 | C1-ACC-5 | PG `c1Acc5InterleavedSessionsResolveTheirOwnActor` (S1/S2/tutor interleaved, S2 deactivated mid-sequence) | Red now |
| C1-T4 | C1-IN-1 | PG `c1In1ModulesRejectsAnyQueryParameter` | Red now |
| C1-T4 | C1-IN-2 | PG `c1In2SlotsRejectsUnknownRepeatedAndMalformedFilters` (+ valid combined filter → 200 control) | Red now |
| C1-T2 | C1-IN-3 | PG `c1In3FrameworkErrorsAreSafeJson` (PUT/DELETE with valid CSRF → 405 + `Allow` containing GET; 406; unknown route 404 for JSON and HTML Accept) | Red now |
| C1-T2/T4 | C1-IN-4 | PG `c1In4ControllerErrorsWithHtmlAcceptKeepJsonCodes` (400, 403 FORBIDDEN, 403 ACCOUNT_INACTIVE + invalidation with `text/html`) | Red now |
| C1-T1/T4 | C1-PREC-1 | PG `c1Prec1SyntaxErrorPrecedesActorCheckAndDoesNotInvalidate` (anonymous + bad param → 401; deleted or inactive actor + bad param → 400, session kept; then 403 + invalidation) | Red now |
| C1-T2 | C1-ACT-1 | ACT `c1Act1DefaultStartupHasNoStudentHandlersAndDeniesStudentRoutes` (Boot default: no Student controller bean or handler method; 401 anonymous, 403 FORBIDDEN student session; JSON, no-store) | **Passes now** (existing deny-all behavior; recorded as baseline, not red) |
| C1-T2 | C1-ACT-2 | PG `c1Act2MissingRequiredBeanFailsStartupWithoutFallback` (no provider, which must name CurrentActorProvider; no Clock; no DataSource → refresh fails; then the complete context serves 200) | Red now (context starts) |
| C1-T1/T2 | C1-ACT-2/3 | PG `c1Act3BootStudentProfileWithoutTestProviderFailsClosed` (real Boot app, `student-api`, no test provider → startup fails naming CurrentActorProvider, so no main-source or fallback actor exists) | Red now (Boot starts) |
| C1-T1 | C1-ACT-3 | ACT `c1Act3ActorSeamIsSharedPlatformAuthInterface` (interface `unichope.platform.auth.CurrentActorProvider`, abstract `Optional<UUID> currentActorId()`) | Red now (type absent) |
| C1-T1/T2 | C1-ACT-3 | Full regression: existing 26 unit + 103 PostgreSQL tests (incl. TutorRestPgTest 31, TutorActivationTest 2) must stay green | Green now; recheck after implementation |
| C1-T2 | C1-ACT-4 | PG `c1Act4BothProfilesServeTutorAndStudentAndUnknownRoutesAre404Json` (Boot, both profiles) | Red now (Tutor 200 already passes; Student 403) |
| C1-T2 | C1-ACT-4 | PG `c1Act4StudentOnlyProfileDoesNotExposeTutorRoutes` (Tutor 403 FORBIDDEN with no Tutor data; Student 200 control) | Red now (Tutor-denial half already passes) |

Security and failure coverage: wrong role in both directions, deleted and inactive accounts with
session invalidation and reuse, simulated-session isolation, query-parameter spoofing
(`actorId`, `studentId`), safe error envelopes (no client request ID, emails or SQL), and
framework errors. CSRF denial of unsafe methods is not a C1 ID (C1 has only GETs; the 405 cases use
a valid token per F2-03). CSRF on Student writes belongs to stu-c2 and stu-c4.
Manual / real-provider checks: none needed for the C1 backend; AI is not applicable. The human
API walkthrough and the UI (out of scope) remain pending as recorded by analyst-2.

### Verification evidence (test-writer-1)
All runs from `src/backend` at revision 459fd6e, compose PostgreSQL up (`docker compose -f
compose.test.yaml up -d --wait`), credentials are the local placeholders from compose.test.yaml.
1. Existing-suite baseline (before any test edit):
   `./gradlew test postgresTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=... --console=plain`
   Exit 0. `test` 26 tests, 0 failures, 0 errors; `postgresTest` 103/0/0. Evidence:
   `evidence/baseline-test-writer-2026-10-22.txt`. This fulfils analyst-2 condition (1).
2. Compile check after writing the tests: `./gradlew compileTestJava --console=plain`. Exit 0, no warnings
   under `-Xlint:all`.
3. Red run: the same command plus `--continue`. Exit 1 (expected). `test` 28 tests, 1 failure, 0 errors;
   `postgresTest` 123 tests, 20 failures, 0 errors. Every failure is in the new classes and is an
   `AssertionFailedError`. The existing 26 + 103 tests pass. 21 of 22 new tests are red, and
   C1-ACT-1 passes as existing behavior. Evidence: `evidence/red-run-test-writer-2026-10-22.txt`
   (per-test messages, for example `expected: <200> but was: <403>`, `Expected RuntimeException to be thrown, but nothing was thrown`, `CurrentActorProvider does not exist`).
Implementer commands: the same command as the red run. For a focused run use
`./gradlew postgresTest --tests 'unichope.platform.core.postgres.StudentBrowsePgTest'` with the
same `-Dcore.test.*` properties, and `./gradlew test --tests 'unichope.student.*'`. Note that `test`
excludes the `postgres` tag (F2-01), so the PostgreSQL class only runs under `postgresTest`.

### Test design notes and gaps
Spec gaps returned to the analyst: none. No acceptance row was ambiguous enough to need invention. Notes:
- N1 (derived oracles, not new behavior): S-SLOT-1's non-TUTOR owner case comes from TECH §4
  (`t.role = 'TUTOR'`). C1-IN-2's extra rejected inputs (`1-1-1-1-1`, empty `moduleId`,
  date with a time part, `studentId`) come from TECH §3.1. The 23:59:59/00:00 SGT edges come from the
  TECH §4 half-open range.
- N2 (deliberately not asserted because the spec is silent): whether a FORBIDDEN or
  provider-empty 401 response invalidates the session, and the exact message text.
- N3 (coverage deferred, not a gap): TECH §2 also requires startup to fail without
  `ConsultationLifecycle`. C1 has no write path, so the C1 tests supply the lifecycle and do not
  check its absence. The stu-c2 test writer should cover it when booking introduces the dependency.
- N4: the tests constrain no class names except the seam FQN. They do constrain discoverability
  (component scan under `unichope.student`, `student-api` profile) and the supplied-bean list above.
- N5: F2-13 is still open. If Kok Seng or Huanghao require a different seam or security rule before
  merge, `StudentTestActors`, the C1-ACT tests and possibly the access tests return to the test writer.
- N6: these tests have not yet been seen passing against any implementation, because none exists. An
  implementer who believes a case is wrong returns it to the test writer with evidence instead of editing it.

Readiness verdict: READY_FOR_IMPLEMENTATION for C1-T1..C1-T4. The gate is current (analyst-2,
re-verified hashes), every acceptance ID maps to an executable test, the baseline and
behavioral red results are recorded, and setup prerequisites are documented.

### Receiver receipt
Receiver role / actual tool and model: Implementer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` not read, searched or written.
Checked task, artifact revisions and required evidence (22 October 2026):
- Task C1-T1..C1-T4 only. HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` equals the analyst-2 and
  test-writer-1 base. `git status` shows under `src/` only the three untracked test files named in
  test-writer-1; no main source or existing test differs from HEAD.
- `sha256sum` matches every hash recorded by analyst-2 and test-writer-1: Student PRODUCT ebc3d509...,
  TECHNICAL e4ba47b0..., PLAN 31545bfb...; shared PRODUCT d1d5a414..., SHARED-TECHNICAL 54ec090a...,
  INTEGRATION ac0d183f...; auth TECHNICAL 50d964b7..., PRODUCT c5c0db50..., PLAN 136e5306...; Tutor
  REST-INCREMENT 8c1f07f2...; ApiSecurityConfiguration fcd2e2bf...; V1 DDL 1cdea0be...; LifecyclePgFixture
  ede25ee8...; TutorRestPgTest d12de2d1...; compose.test.yaml 7a7216c0...; NOTE 5e8ba76f...; HANDOFF 1ee65fd9...;
  StudentTestActors 9e70e1ee..., StudentActivationTest 15d13758..., StudentBrowsePgTest 39206a32...; evidence
  baseline-2026-10-22 b9c6fbf6..., baseline-test-writer b35b2485..., red-run-test-writer 8d6773cc....
- Gates present: analyst-2 READY_FOR_TESTS (coverage matrix, findings F2-01..F2-19 with resolutions, open
  F2-13/14/15 deferred as merge/activation gates by D6/D7) and test-writer-1 READY_FOR_IMPLEMENTATION with
  acceptance-to-test mapping, baseline and behavioral red evidence. A1-A4 recorded in PRODUCT §10; D2-D7 in addenda.
- Read the canonical sections directly (Student TECH §2, §3, §3.1-§3.3, §4; PRODUCT §2, STU-SLOT-01, §10;
  INTEGRATION §1, §2, §5, §8; Tutor REST §2-§5) and the three test files. The tests express the agreed
  behavior; no contradiction found between the specs and the tests for C1.
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none.

## Handoff implementer-1
Producer role / actual tool and model: Implementer / Claude Code, Opus 5.5 (`claude-opus-5-5`);
fresh subagent session with no inherited conversation (D5). `logs/` was not read, searched or written.
Recipient role: Reviewer / security checker, fresh session, `workflow/student/agents/reviewer-security.md`.
Change ID / task / exact requested next action: stu-c1-browse-slots / C1-T1..C1-T4 / independently
review the spec, tests and the complete main-source diff below, rerun the verification command,
and record PASS, CHANGES_REQUIRED or BLOCKED with findings.
Stage status / supersedes: IN_REVIEW; first implementer revision. Supersedes nothing.
Goal and non-goals: as in analyst-1/analyst-2. Implemented: the shared actor seam, `StudentAccess`,
`student-api` wiring and security rule, Student framework-error JSON, `StudentReadRepository` and
`GET /api/student/modules`, `GET /api/student/slots`. Not implemented (non-goals): booking,
cancellation, my-bookings, agenda, frontend, real login/session, production activation.
Acceptance IDs: S-SLOT-1, S-SLOT-2, C1-OVL-1, C1-OUT-1..3, C1-ACC-1..5, C1-IN-1..4, C1-PREC-1,
C1-ACT-1..4 (unchanged from analyst-2 and test-writer-1).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| Canonical specs and patterns listed in Handoff analyst-2 | Input | hashes re-verified identical (see the test-writer-1 receiver receipt above) | Baseline |
| src/backend/src/test/java/unichope/student/StudentTestActors.java | Input (test, unchanged) | 9e70e1ee9d7a69e99c496823879825a3965674f9975de9dffd2664f631f853e9 | Test-only provider |
| src/backend/src/test/java/unichope/student/http/StudentActivationTest.java | Input (test, unchanged) | 15d137587581fd2e96ad33cf859fb66d15cb624593e31ea1d32deecde2547ae9 | C1-ACT-1, C1-ACT-3 |
| src/backend/src/test/java/unichope/platform/core/postgres/StudentBrowsePgTest.java | Input (test, unchanged) | 39206a32ecfc81f9f2939bac69ee36e0b897654c708faf233af24be56ce21624 | Other C1 IDs |
| src/backend/src/main/java/unichope/platform/auth/CurrentActorProvider.java (new) | Output | 69c190eeb9ed53ccac28e458e8f67dd579966dcbbd5400dd370fea68bc6e2fe3 | C1-T1 shared seam (interface only) |
| src/backend/src/main/java/unichope/platform/web/ApiSecurityConfiguration.java (modified, +2 lines) | Output | 2de6023d18d912bd0285c46512a6406cab177ad2cf7b438fab6a40045db3adc2 (was fcd2e2bf...) | C1-T2 `student-api` matcher |
| src/backend/src/main/java/unichope/student/StudentAccount.java (new) | Output | 24a3cf08c8976b04cf7b626f62c55f4112315b6a944bb71f09420bf80a301cb4 | C1-T1 stored account |
| src/backend/src/main/java/unichope/student/StudentAccessDeniedException.java (new) | Output | 4464963b008dc79d0d5af862fd8e071528b7fdd416000e423109bf0b5d732026 | C1-T1 access reasons |
| src/backend/src/main/java/unichope/student/StudentAccess.java (new) | Output | e3c395a9fd8dba1515aacb025968e5981f72427f44fec56146f8b4617df9576b | C1-T1 access check |
| src/backend/src/main/java/unichope/student/ModuleOption.java (new) | Output | 21f4d7780ad08a878562fb809f92747b1fa09d658eca9b15e4e2a4db14f61fb7 | C1-T3 DTO |
| src/backend/src/main/java/unichope/student/BookableSlot.java (new) | Output | 5e72d10e68a346a185b6c4554b3384753f4d0fd30b59f92b7f58bb1acb3d444b | C1-T3 DTO |
| src/backend/src/main/java/unichope/student/StudentReadRepository.java (new) | Output | 068bde61eeda3f78584ad5cacfa81ce02134bd154807a3d6dd862d54c79ab1d0 | C1-T3 read boundary |
| src/backend/src/main/java/unichope/student/PostgresStudentReadRepository.java (new) | Output | 8e828ab7bce788c8d5f56ca09938532d9b95e1d33cb7a04bff48768d3bb3aab9 | C1-T3 parameterized SQL |
| src/backend/src/main/java/unichope/student/StudentReadService.java (new) | Output | acb39b0d6c52991418dfb36c84bef960d52b6e1b51444be7ad4490ca8e61b654 | C1-T3/T4 service |
| src/backend/src/main/java/unichope/student/http/StudentApiInput.java (new) | Output | aee190adb5137b16c874e1161e12352b9d2bd5783096d83457c6cb014ca18b38 | C1-T4 strict query parsing |
| src/backend/src/main/java/unichope/student/http/StudentController.java (new) | Output | 3b207f01cd9b794612b803afc85826f6d93acc8f62af69610bbf04a66efdec7b | C1-T4 routes |
| src/backend/src/main/java/unichope/student/http/StudentExceptionHandler.java (new) | Output | 0902a1200d1256198abcffb7f7ac0878dc789b3aeffe273ba76e89bda40a05a8 | C1-T1/T2 error mapping |
| src/backend/src/main/java/unichope/student/http/StudentApiConfiguration.java (new) | Output | 0590d345d9979f8aba61fb97510a8eb3486808b672ff6760c3f824a24eb2c37e | C1-T2 profile wiring, framework-error JSON |
| workflow/student/changes/stu-c1-browse-slots/evidence/implementer-1-main-source.patch | Output (diff) | 4bdaa259173638a26bfe22d7cde914e87d2c4dfe934fbf844c6b9423cfd25c6e | Complete main-source diff vs 459fd6e (14 files) |
| workflow/student/changes/stu-c1-browse-slots/evidence/final-run-implementer-2026-10-22.txt | Output (evidence) | 77c740867f67775cfb6acd4a4fd814eccc21954d5a2674a08fd6cf40cb71bed1 | Final verification output |

Base commit / result commit / diff: base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5`; no commit (not
authorized). Complete diff under `src/`: the 14 main-source files above (patch file above) plus the
three unchanged, still-untracked test-writer test files. `git diff --stat -- src/` lists only
ApiSecurityConfiguration.java (+2). No Tutor, core, migration, build or existing test file changed.
Note: an untracked `src/backend/bin/` directory (IDE compiler output, `.class` files) is present in the
working tree. It already existed when this session started and was not created by Gradle or by this
stage. It is not part of the change and should not be committed.
Uncommitted files and their hashes: as in the table, plus this record.
Allowed write scope (used): `unichope/student/**`, new `unichope/platform/auth/CurrentActorProvider.java`,
and the conditional `student-api` matcher in `ApiSecurityConfiguration.java`; this record and the
evidence files.
Prerequisites / dependency state: compose PostgreSQL 17 (Docker 29.9.2) started with
`docker compose -f compose.test.yaml up -d --wait` and stopped with `docker compose -f compose.test.yaml down`.
Recorded decisions and authorization references: D2-D7, A1-A4 as cited in analyst-2 and addenda. No
new decision was made in this stage.

### Implementation summary
- C1-T1: `platform.auth.CurrentActorProvider` (interface, abstract `Optional<UUID> currentActorId()`).
  `StudentAccess.requireActiveStudent()` resolves the provider actor and re-reads `users` (id, role,
  active) through `StudentReadRepository.findAccount` on every call. The stored role wins.
- C1-T2: `StudentApiConfiguration` (`@Profile("student-api")`) wires the read repository from
  `DataSource` (`JdbcClient.create`), `StudentAccess` (requires `CurrentActorProvider`) and
  `StudentReadService` (requires `Clock`). Missing beans fail startup. A Student-only exception
  resolver (path prefix `/api/student/`) writes 405 (with `Allow`), 406, 415 and unmapped-route 404 as
  `ApiErrors` JSON. `StudentExceptionHandler` maps `ApiFailure` and access reasons; everything else is a
  fixed 500. `ApiSecurityConfiguration` adds `/api/student/**` `authenticated()` only when `student-api`
  is active. Default deny-all, the Tutor rule, CSRF and the entry points are unchanged.
- C1-T3: `PostgresStudentReadRepository` implements TECH §4 with bound parameters only (the optional
  module and start-range clauses are fixed SQL fragments). No locks, no writes.
- C1-T4: `StudentController` validates query syntax (`StudentApiInput`) before calling the service, so
  syntax errors precede the actor check (TECH §3.2 order). `StudentReadService` converts an SGT date to
  the half-open UTC range `[date 00:00 SGT, next day 00:00 SGT)` and reads `now` from the injected `Clock`.

### Acceptance-to-code-and-test mapping (implementer-1)
`PG` = `StudentBrowsePgTest` (`postgresTest`), `ACT` = `StudentActivationTest` (`test`). All passed in the final run.

| Acceptance ID | Code | Test | Result |
| --- | --- | --- | --- |
| S-SLOT-1 | PostgresStudentReadRepository.findBookableSlots (status, `start_time > :now`, active TUTOR, active module, EXISTS assignment, order) | PG sSlot1OnlyBookableFutureEligibleSlotsOrderedByStartThenId | PASS |
| S-SLOT-2 | StudentReadService.bookableSlots (SGT range), repository module/range clauses, StudentApiInput.date | PG sSlot2SgtDateBoundaryModuleFilterIntersectionAndUnmatched | PASS |
| C1-OVL-1 | The repository query has no student-overlap condition | PG c1Ovl1SlotOverlappingOwnActiveBookingIsStillListed | PASS |
| C1-OUT-1 | BookableSlot record (7 fields, Instant), `t.name AS tutor_name` | PG c1Out1BookableSlotHasExactSafeFields | PASS |
| C1-OUT-2 | ModuleOption record; findActiveModules `WHERE active ORDER BY code, id` | PG c1Out2ModulesAreActiveExactOrderedAndEmpty | PASS |
| C1-OUT-3 | Existing ApiResponseFilter / ApiErrors; StudentExceptionHandler and the resolver use ApiErrors.write | PG c1Out3NoStoreAndServerGeneratedRequestIds (+ no-store asserted on every response) | PASS |
| C1-ACC-1 | Filter entry point (anonymous); StudentAccess NO_ACTOR → 401 | PG c1Acc1MissingPrincipalOrProviderActorIsUnauthenticated | PASS |
| C1-ACC-2 | UNKNOWN_ACCOUNT → 401, clear context, invalidate session | PG c1Acc2DeletedActorIsUnauthenticatedAndSessionInvalidated | PASS |
| C1-ACC-3 | INACTIVE → 403 ACCOUNT_INACTIVE, invalidate | PG c1Acc3InactiveStudentIsDeniedAndSessionInvalidated | PASS |
| C1-ACC-4 | Stored `role != STUDENT` → 403 FORBIDDEN; session authorities are never read | PG c1Acc4StoredRoleWinsOverSessionAuthorities | PASS |
| C1-ACC-5 | Provider resolved per call; no actor state in singletons | PG c1Acc5InterleavedSessionsResolveTheirOwnActor | PASS |
| C1-IN-1 | StudentApiInput.noQuery | PG c1In1ModulesRejectsAnyQueryParameter | PASS |
| C1-IN-2 | StudentApiInput.slotFilter / id / date | PG c1In2SlotsRejectsUnknownRepeatedAndMalformedFilters | PASS |
| C1-IN-3 | StudentApiConfiguration exception resolver (405 + Allow, 406, 404) | PG c1In3FrameworkErrorsAreSafeJson | PASS |
| C1-IN-4 | StudentExceptionHandler writes JSON regardless of Accept | PG c1In4ControllerErrorsWithHtmlAcceptKeepJsonCodes | PASS |
| C1-PREC-1 | The controller parses input before the service's access check | PG c1Prec1SyntaxErrorPrecedesActorCheckAndDoesNotInvalidate | PASS |
| C1-ACT-1 | `@Profile("student-api")` on configuration, controller and advice; default deny-all unchanged | ACT c1Act1DefaultStartupHasNoStudentHandlersAndDeniesStudentRoutes | PASS |
| C1-ACT-2 | Required `CurrentActorProvider`, `DataSource`, `Clock` bean parameters | PG c1Act2MissingRequiredBeanFailsStartupWithoutFallback; PG c1Act3BootStudentProfileWithoutTestProviderFailsClosed | PASS |
| C1-ACT-3 | Seam FQN `unichope.platform.auth.CurrentActorProvider`; no main-source implementation | ACT c1Act3ActorSeamIsSharedPlatformAuthInterface; PG c1Act3Boot...; full regression | PASS |
| C1-ACT-4 | Profile-gated matcher next to the unchanged Tutor matcher; distinct bean names and seam types | PG c1Act4BothProfilesServeTutorAndStudentAndUnknownRoutesAre404Json; PG c1Act4StudentOnlyProfileDoesNotExposeTutorRoutes | PASS |

Resolved spec findings preserved: F2-02 (no overlap filter), F2-03/F2-04 (statuses as tightened),
F2-05 (both-profile wiring), F2-06 (syntax before actor), F2-17 (Student code adds no logging of request
bodies, SQL or session data; 500 bodies carry a fixed message). F2-07..F2-12 and F2-19 apply to c2-c4 only.

### Verification evidence (implementer-1)
Working directory `src/backend`, base 459fd6e plus the uncommitted change above, compose PostgreSQL up.
1. Focused: `./gradlew test --tests 'unichope.student.*' postgresTest --tests 'unichope.platform.core.postgres.StudentBrowsePgTest'`
   with the same `-Dcore.test.*` properties and `--console=plain`. Exit 0 (2 unit + 20 PostgreSQL Student tests).
2. Final: `./gradlew clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<local placeholder> --console=plain`.
   Exit 0. `test`: 28 tests, 0 failures, 0 errors, 0 skipped. `postgresTest`: 123 tests, 0 failures,
   0 errors, 0 skipped (StudentBrowsePgTest 20 and TutorRestPgTest 31 among them; TutorActivationTest 2
   runs under `test`). `bootJar` built `build/libs/unichope-0.0.1-SNAPSHOT.jar`. The single compiler
   warning (`[serial]` in `platform/ai/AiGateway.java`) is pre-existing and outside this change.
   Evidence: `evidence/final-run-implementer-2026-10-22.txt`.
Compared with the test-writer red run (28 unit / 1 failure; 123 PostgreSQL / 20 failures), all 21 red
C1 tests now pass, C1-ACT-1 still passes, and the existing 26 + 103 tests remain green.

### Deviations, limitations and pending checks
- Deviation (scope, recorded rather than hidden): TECH §2 lists `ConsultationLifecycle` among the beans
  whose absence must fail `student-api` startup. C1 has no write path, so the C1 wiring does not inject
  the lifecycle (no unused dependency). Test-writer note N3 already defers that check. stu-c2 must add
  the lifecycle dependency and its fail-closed test when booking introduces it.
- Design choice within the spec: a provider returning empty yields 401 without session invalidation
  (TECH §3.2 row 1 states no invalidation; test-writer note N2 says it is not asserted). A deleted
  account yields 401 with invalidation (row 2). The reviewer may confirm this reading.
- `StudentHttpService` (TECH §2) is not created: its role is lifecycle calls and error mapping for
  c2/c4. `StudentBooking` and the bookings query belong to c3.
- Module ordering uses PostgreSQL `ORDER BY code, id` (database collation), per TECH §4. Codes in the
  tests are ASCII upper-case.
- Nothing returned to the test writer or the spec analyst: no test was found wrong and no requirement gap
  was found. No test was edited, skipped or relaxed.
- Pending: independent review and security check (fresh session); F2-13 teammate review of
  `platform/auth` (Kok Seng) and the `platform/web` matcher (Huanghao) before merge to `main` (D6);
  F2-15 real auth adapter alignment (activation gate); human API walkthrough. Real login/session behavior
  is unverified (simulated authentication only).
Readiness or review verdict: IN_REVIEW. No independent review is claimed.

### Receiver receipt
Receiver role / actual tool and model: Reviewer / security checker / Claude Code, Opus 5.5
(`claude-opus-5-5`); fresh subagent session that produced none of the reviewed work, no inherited
conversation (D5). `logs/` was not read, searched or written.
Checked task, artifact revisions and required evidence (22 October 2026), after deriving my own
checklist from the request and canonical specs and inspecting the code and tests first:
- Task C1-T1..C1-T4. HEAD `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` equals the recorded base.
  `git status` under `src/` shows only the modified `ApiSecurityConfiguration.java`, the 13 new main
  files, the 3 new test files and the untracked IDE folder `src/backend/bin/` (excluded, not reviewed).
- `sha256sum` of all 14 main-source outputs matches the implementer-1 table exactly. The 3 test files
  still match the test-writer-1 hashes (9e70e1ee..., 15d13758..., 39206a32...), so no test was edited
  after hand-off. Student PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., PLAN 31545bfb..., INTEGRATION
  ac0d183f..., SHARED-TECHNICAL 54ec090a..., Tutor REST-INCREMENT 8c1f07f2... match analyst-2. The four
  upstream evidence files match their recorded hashes.
- `git diff --quiet HEAD` holds for `unichope/tutor` (main and test), `platform/core`, `build.gradle` and
  `src/main/resources`: no Tutor, core, migration or build edit.
Receipt: ACCEPTED
Reason / exact missing or conflicting artifact: none. Review follows in "Review reviewer-1".

## Review reviewer-1
Producer role / actual tool and model: Reviewer / security checker / Claude Code, Opus 5.5
(`claude-opus-5-5`); fresh subagent session (D5). `logs/` not read.
Recipient role: Phuc (human verification); follow-ups to the implementer, test writer and spec
analyst as routed below.
Change ID / task: stu-c1-browse-slots / C1-T1..C1-T4.
Stage status / supersedes: review complete; supersedes the "pending" placeholders in the earlier
"Independent review" section for this revision only.

### Reviewed revision
Base `459fd6ef7108c2d08aeaa0f92ae0bccc054158b5` plus the uncommitted diff identified by these
SHA-256 values (identical to the implementer-1 and test-writer-1 tables): CurrentActorProvider
69c190ee...; ApiSecurityConfiguration 2de6023d...; StudentAccount 24a3cf08...; StudentAccessDeniedException
4464963b...; StudentAccess e3c395a9...; ModuleOption 21f4d778...; BookableSlot 5e72d10e...;
StudentReadRepository 068bde61...; PostgresStudentReadRepository 8e828ab7...; StudentReadService acb39b0d...;
StudentApiInput aee190ad...; StudentController 3b207f01...; StudentExceptionHandler 0902a120...;
StudentApiConfiguration 0590d345...; tests StudentTestActors 9e70e1ee..., StudentActivationTest 15d13758...,
StudentBrowsePgTest 39206a32.... Specs: PRODUCT ebc3d509..., TECHNICAL e4ba47b0..., PLAN 31545bfb....
Any change to these files invalidates this review.

### Checks run
1. Full verification, working directory `src/backend`, compose PostgreSQL 17:
   `docker compose -f compose.test.yaml up -d --wait` (exit 0);
   `./gradlew clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/mp3_test -Dcore.test.user=mp3_test -Dcore.test.password=<local placeholder> --console=plain`
   (exit 0, BUILD SUCCESSFUL); `docker compose -f compose.test.yaml down` (exit 0).
   Counts from the JUnit XML: `test` 28 tests, 0 skipped, 0 failures, 0 errors (StudentActivationTest 2,
   TutorActivationTest 2); `postgresTest` 123 tests, 0/0/0 (StudentBrowsePgTest 20, TutorRestPgTest 31);
   `bootJar` built. This independently matches the implementer-1 claim. Evidence:
   `evidence/review-run-reviewer-1-2026-10-22.txt` (SHA-256 a9a4985e9714f363c5bdeee370c85377a74655dcacd00af5f87af03e1338dde2).
2. Edge-case probe in a disposable copy of `src/backend` in my scratch directory (never added to the
   repository): Boot app, profile `student-api`, test-only provider returning empty, authenticated
   session. Results: `/api/student/`, `/api/student/modules/` -> 404 `NOT_FOUND` JSON with no-store
   for JSON and HTML Accept; `/api/student//modules` -> reaches the controller (401 from the empty
   provider); `/api/student` (no trailing slash) -> 404 via `sendError`, no content type, empty body
   under MockMvc (finding R1-01).
3. Code inspection: complete main diff and all three test files, compared with the Tutor pattern
   (`TutorApiConfiguration`, `TutorExceptionHandler`, `TutorAccess`) and `platform/web`.
4. Not run / not verifiable here: real login/session adapter (F2-15, simulated authentication only);
   behaviour inside a real servlet container's error dispatch (MockMvc does not perform it);
   dependency advisory scan (no dependency or build change in this diff, so not applicable).

### Independent checklist and results
| Area | Source | Result |
| --- | --- | --- |
| S-SLOT-1 eligibility and ordering | PRODUCT STU-SLOT-01; TECH §3, §4 | Met. SQL has status, `start_time > :now`, active TUTOR owner, active module, EXISTS assignment, `ORDER BY s.start_time, s.id`. The test covers every exclusion, including a non-TUTOR owner, and an equal-start tie whose ids beat insertion and code order. |
| S-SLOT-2 SGT date | PRODUCT §4; TECH §3, §4; SH-DATA-01 | Met. `Asia/Singapore` half-open `[d 00:00, d+1 00:00)` is converted to UTC instants and bound as `timestamptz` parameters. The test covers 15:30Z/16:30Z, 23:59:59 and 00:00 SGT edges, intersection and `[]`. |
| C1-OVL-1 (A2) | PRODUCT §10 item 2 | Met. No overlap predicate; the test holds an overlapping ACTIVE booking. |
| DTO privacy | PRODUCT §2; TECH §3 | Met. Records have exactly the specified fields. `tutorName` = `users.name`; no email or tutor ID. The test asserts the exact key set and that no `@` or tutor ID appears. |
| Actor resolution, stored role/active | TECH §3.2; SH-AUTH-04 | Met. The provider is called and `users` re-read on every request, and session authorities are ignored. The tests cover both directions, deleted and inactive accounts, and interleaved sessions. |
| Session invalidation | TECH §3.2 rows 2-3 | Met. `clearContext` + `session.invalidate()` for UNKNOWN_ACCOUNT and INACTIVE; reuse gives 401 (tested). Row 1 (provider empty) does not invalidate, per spec wording; see R1-04. |
| CSRF | TECH §3; INTEGRATION | Met for scope. The CSRF repository and handler are unchanged. C1 adds only GETs, and the 405 tests send a valid token (F2-03). Write-path CSRF is stu-c2/c4. |
| Evaluation order | TECH §3.2 (F2-06) | Met. The controller parses before the service's access check. C1-PREC-1 shows a syntax 400 with no invalidation, then 403 with invalidation. |
| Parameterized SQL | SHARED §2; TECH §4 | Met. Only constant fragments are appended; every value is bound; no locks; no writes. |
| Profile gating / fail-closed | TECH §2 | Met for C1. Configuration, controller and advice are `@Profile("student-api")`; the matcher only applies under the profile. Missing provider, Clock or DataSource fails startup (tested, plus a real Boot test). There is no main-source provider. The lifecycle bean is deferred to c2 (R1-05). |
| Error envelope / no-store | TECH §3.3; INTEGRATION §5 | Met for the tested routes. `ApiErrors.write` gives exact `{code,message,requestId}`, no-store and a server request ID; HTML Accept still gets JSON; 405 includes `Allow`; 406 and 404 are covered. The bare-prefix gap is R1-01. |
| Package boundaries / scope | TECH §2; D2/D3 | Met. Main `student` imports only `platform.*` (plus JDK/Spring). Platform never imports `student`. No Tutor, core, migration or build edit. No scope creep (no booking, cancel, list or agenda code). |
| Logging | Student §4; INTEGRATION §5; F2-17 | No request-body, SQL or session logging was added; the Student code has no logging at all (R1-03). |
| Test strength | NOTE §2 | Strong overall. Real MVC, security chain and PostgreSQL; no mocks of the code under test; exact oracles; no skipped or relaxed test. Minor vacuous assertion: R1-02. |

### Upstream comparison
Analyst-2 resolutions independently confirmed against the code, tests and spec: F2-02 (C1-OVL-1
exists and passes), F2-03 (405 with valid CSRF), F2-04 (default 401/403 JSON and no handler), F2-05
(both-profile and Student-only Boot tests), F2-06 (TECH §3.2 order text present, implemented and
tested), F2-17 (no sensitive logging), F2-18 (base re-anchored, hashes match). F2-13, F2-14 and F2-15
remain OPEN as recorded merge/activation gates (D6/D7). This review does not close them. The
implementer's two declared deviations (no lifecycle bean in C1; provider-empty 401 without
invalidation) agree with the spec as written; see R1-04 and R1-05. I found no contradiction between
the test-writer mapping and the actual test bodies.

### Findings
| ID / severity | File:line | Acceptance ID | Trigger | Impact | Suggested correction (proposal) | Routing | Closure check |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R1-01 / LOW | `student/http/StudentApiConfiguration.java:44-45` | TECH §3.3 closing paragraph (404 JSON for unknown `/api/student/**`); not covered by any C1 ID | Authenticated student GET `/api/student` (no trailing slash) with `student-api` active. The security matcher `/api/student/**` admits it, but the resolver's prefix check `startsWith("/api/student/")` skips it. | A 404 via `sendError`, with no JSON envelope and no requestId under MockMvc. In a container this falls to the default error dispatch, so the response shape is undefined by the contract. No data is disclosed and no-store is set. Tutor has the same pattern for `/api/tutor`. | Match `path.equals("/api/student") \|\| path.startsWith("/api/student/")`, and add a test for the bare prefix (JSON and HTML Accept). Tell the Tutor owner about the identical pattern; do not edit Tutor files. | Implementer (code); test writer (add case). Can be done with stu-c3, the next change that touches this configuration. | GET `/api/student` returns 404 `NOT_FOUND` exact JSON envelope with no-store, asserted by a new test. |
| R1-02 / LOW | `StudentBrowsePgTest.java:186` | C1-OUT-3 | `assertNotEquals(MARKER, success.getResponse().getHeader("X-Request-Id"))`: no `X-Request-Id` response header is ever written, so `null != MARKER` passes vacuously. | The line proves nothing. The real protection (marker absent from bodies; distinct server request IDs in error bodies) is asserted elsewhere, so coverage is not lost. | Remove the line, or replace it with an assertion that has an oracle (for example, that the header is absent, or that it equals a server-generated value if one is ever specified). | Test writer | The assertion is removed or replaced with one that can fail. |
| R1-03 / LOW | `student/http/StudentExceptionHandler.java:20-39` | Student PRODUCT §4 ("routine logs record request IDs and outcomes"); not a C1 ID | Any unexpected exception (for example a DB outage on GET `/slots`) | It is converted to a safe 500 with no server-side log line, so operators cannot connect a user's requestId to a cause. The response itself is safe. Tutor behaves the same. | Specify the shared logging rule: log requestId, route, status and exception class (never bodies, SQL text or session data) for 5xx, probably once in `platform/web`. Then implement and test it. | Spec analyst (decide where the shared rule lives, coordinate with the platform/web owner); then implementer | Spec states the rule; a test or manual check shows a 500 produces one log line with the requestId and no SQL or body text. |
| R1-04 / INFO | `StudentExceptionHandler.java:31`; TECH §3.2 row 1 | C1-ACC-1 | Authenticated principal for which the provider returns empty | 401 without session invalidation (Tutor invalidates in the same case). This is consistent with TECH §3.2 as written, and test-writer note N2 leaves it unasserted. It only matters once the real auth adapter defines when it returns empty. | Analyst to state explicitly whether row 1 invalidates, aligned with the auth adapter under F2-15. | Spec analyst | TECH §3.2 row 1 states the invalidation behaviour, and a test asserts it. |
| R1-05 / INFO | `StudentApiConfiguration.java:70-83`; TECH §2 activation paragraph | C1-ACT-2 (lifecycle part deferred) | `student-api` without a `ConsultationLifecycle` bean currently starts. | Correct for C1, which has no write path (implementer deviation and note N3 record this). It must not be forgotten. | stu-c2 wires the lifecycle and adds its fail-closed test. | Test writer and implementer of stu-c2 | The stu-c2 test shows that startup without the lifecycle fails, and that test passes. |

No CRITICAL, HIGH or MEDIUM finding. No acceptance, security or integrity failure in the reviewed scope.

### Verdict
PASS for stu-c1 C1-T1..C1-T4 at the revision above. Every C1 acceptance ID traces to code and a
meaningful passing test, and my rerun reproduces 28/0 and 123/0 with a successful `bootJar`. R1-01..R1-03
are non-blocking LOW follow-ups with owners. R1-04 and R1-05 are informational carry-forwards.
PASS enables human verification. It does not approve a merge to `main` (F2-13 teammate review of
`platform/auth` and `platform/web` is still required by D6), production activation (F2-15 real
auth adapter and migration), or deployment. It does not certify the whole application as secure.

### Pending for the human owner
- Human API walkthrough of `/api/student/modules` and `/api/student/slots` (no frontend exists).
- Whether R1-01 should be fixed before merge or with stu-c3, and whether to raise the identical
  `/api/tutor` bare-prefix behaviour with Huanghao.
- Kok Seng's and Huanghao's reviews (F2-13) before `branch_student` merges to `main`.
- `src/backend/bin/` (untracked IDE output) should not be committed.
Next agent, exact task and required inputs: Phuc, human verification of C1. Then the stu-c3 spec/test
stages, carrying R1-01 and R1-02 into that change's test-writer and implementer scope, and R1-05 into stu-c2.

# stu-c1-browse-slots: Students browse bookable slots (with Student API foundation)

Status: SPEC_DRAFT (becomes READY_FOR_TESTS when Phuc confirms assumptions A1 and A2 below)
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

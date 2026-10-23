# stu-fe3-review-fixes: Review fixes required before FE1/FE2 acceptance

Status: READY_FOR_HUMAN
Scope classification and reason: standard path. These are user-visible behavior
changes in shared session/sign-out handling, mutation failure feedback, filter
feedback and page landmarks (Auth-adjacent UI), so the shorter path does not apply.
Split per OD-13: this change holds the "before acceptance" decisions; the
follow-ups are [stu-fe4-followups](stu-fe4-followups.md).
Parent request / related changes / dependencies: Phuc's owner decisions OD-1..OD-17
(22 October 2026) on the reviewer-3 findings of
[stu-fe1-auth-ui](stu-fe1-auth-ui.md) and [stu-fe2-booking-ui](stu-fe2-booking-ui.md).
Depends on the FE1/FE2 frontend at the reviewed manifest-r2 revision (verified
unchanged below). stu-fe4 depends on this change (shared App.tsx/api.ts/
StudentWorkspace.tsx edits) and starts after this change's review.
Human owner / affected owners: Phuc; Kok Seng (Auth UI: OD-5 sign-out copy, merge
review); Huanghao (shared shell, merge review).
Current agent role / actual model or tool: spec analyst / Claude Code (Claude Agent
SDK, fresh subagent session with no inherited conversation, transcript or log) /
Claude Opus 5.5 (claude-opus-5-5); 22 October 2026.
Stage handoff manifest / revision: Handoff analyst-1 below.
Receiver receipt: pending fresh test writer.
Base commit and current revision/diff reference: HEAD
e277e583f1319320ed5f44e1944ac85296bb7d15 (branch branch_student) plus the
uncommitted worktree; frontend equals `stu-fe2-booking-ui/evidence/frontend-manifest-r2.json`
(40 entries, 0 mismatches at this stage). No commit.
Spec paths + revision: see Inputs and outputs (SHA-256; files are uncommitted).
Acceptance IDs and Given/When/Then scenarios: FE2-NOTHING-SENT, FE1-DENIAL-CACHED,
FE1-LOGOUT-CONFIRMED, FE2-NOTICE-CLEAR, FE1-UI-LANDMARKS. Normative scenarios are in
[FRONTEND-DESIGN.md](../FRONTEND-DESIGN.md) "Revision 2 acceptance", table "Before
acceptance"; they are not duplicated here, to avoid divergence.
Allowed scope/files and dependencies: see Handoff analyst-1.
Assumptions / open decisions / owner agreement: see Handoff analyst-1.

## Handoff analyst-1
Producer role / actual tool and model: spec analyst / Claude Code (Claude Agent SDK
fresh subagent) / Claude Opus 5.5 (claude-opus-5-5).
Recipient role: fresh test writer (agents/test-writer.md), then fresh implementer,
then fresh independent reviewer. Per OD-14 any fresh session (any model) may take
any role; no Claude-only requirement applies.
Change ID / task / exact requested next action: stu-fe3-review-fixes / one task
FE3-T1. Test writer: accept this packet after checking hashes; write behavioral
RTL tests (browser test where noted) for the five IDs; update the one frozen test
whose oracle revision 2 changes (finding SA3-02) without weakening its intent;
record red evidence (or an honest passing baseline where behavior already exists,
e.g. FE1-DENIAL-CACHED, FE1-LOGOUT-CONFIRMED S3, FE1-UI-LANDMARKS S4).
Stage status / supersedes: READY_FOR_TESTS; first packet; supersedes none.
Goal: close OD-4, OD-5, OD-8 and OD-12 (reviewer findings FE-R2/CS-05, FE-R3,
FE-R5, FE-R6) and add the related FE-R7(b) regression, so the FE1/FE2 frontend can
go to Phuc's OD-15 acceptance journey.
Non-goals: OD-1, OD-2, OD-3, OD-7, OD-9, OD-11, FE-R7(a,c) (all stu-fe4); OD-6
accessible Book/Cancel names (deferred by owner); OD-10 text size (human check);
FE-R4 (no decision); backend, Auth policy, package/dependency changes, AI,
Tutor/Admin features, deployment; any change to the reviewer-3 PASS verdicts.
Acceptance IDs: FE2-NOTHING-SENT (OD-4), FE1-DENIAL-CACHED (FE-R7b),
FE1-LOGOUT-CONFIRMED (OD-5), FE2-NOTICE-CLEAR (OD-8), FE1-UI-LANDMARKS (OD-12).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/FRONTEND-DESIGN.md (revision 2) | Output (changed by this stage) | 895AFB4CC2594ECBAD0E69387920E60879409DAA78595EB9779205FF1B7B9C8B | Normative IDs and scenarios; supersedes 3917AF4CD2709934383E03754A302ABA15D543A913B82EAA15A25B8F9D3F80DE |
| workflow/student/PLAN.md | Output (changed: CS-07 status, FE3/FE4 rows) | 008AA245CD581A837370BB2C96BA2C99A8322ED668AC727AECEEE73B15EF5766 | Task order; supersedes 9C5EECA85D36CDFED9D6F5ABD89176905F40556727F01AC9303D2C4EC4716FA5 |
| workflow/student/TECHNICAL-SPEC.md section 1 | Output (changed: CS-07 row only) | 792718A4E671F354FE508B76C6705F2BCB7F5A080398CEB669AA7E24FCE2F485 | Stale shell ownership text; supersedes 97C2B915682BEC9EEC61BF29CCE665EF2088458C7DDE78E349CC47A88366F698 |
| workflow/auth/PLAN.md "Current results" | Output (changed: revision 2 note for Kok Seng) | 90C0F27E1E2B2204E3ECDA1E00B64A3B1DC6B77C687C000D7797B7DA1DA73B07 | Auth owner visibility; supersedes 5F40F9BE474C96FB0D22C89523367F20E8586AF59062B6DF278C43F6C1CD6ACB |
| workflow/student/PRODUCT-SPEC.md | Input (unchanged) | 750433038415D5AF0832D4E506FB669C1D08A9A583FF388C4DEFC4F9FD690B86 | Student requirements |
| workflow/auth/PRODUCT-SPEC.md | Input (unchanged) | BE2F2F59B4E0ABEF49081A614DC77FA2DFB5BFDEB114C4A3B49AC38D2A73A637 | Auth requirements; defers UI to FRONTEND-DESIGN |
| workflow/auth/TECHNICAL-SPEC.md | Input (unchanged) | 91949EC9793C1747F99C87D86FF74C941700BB9D0133F6204582B0C529E689E9 | Auth HTTP contract |
| workflow/SHARED-TECHNICAL-SPEC.md | Input (unchanged) | 5F92FF1F89B0171232BF3FF49C8517A9743EAEF2602B27F1C5D38EAFDE9912A5 | SH-AUTH/SH-WEB |
| workflow/INTEGRATION-SPEC.md | Input (unchanged) | F7893269A4F5C3F1F4E106F3C62225DD3E8D3B4E3C0C31B6CA4A5CE61FAD0604 | Status/error codes |
| workflow/student/NOTE.md | Input (unchanged) | 333031E4BA3A4DAF09DD29ABB5E1C3ADF21F675D8B8916DEB11CD5FA8882F981 | Workflow, template |
| workflow/student/HANDOFF.md | Input (unchanged) | 5594B343C530CCB81111EA4F8008356369B65CE8927108E8B4163D0D369F5CA6 | Packet format |
| workflow/student/agents/spec-analyst.md | Input (unchanged) | 975D2D5530E16A236666094FEAEB39406190F3D6718F655A256271EC40D04606 | Role prompt |
| workflow/student/changes/stu-fe1-auth-ui.md (before this stage's append) | Input | A8BF7240583AE019F323A0FB4BF6AB7AABC1007A6EEC82639767562411B35404 | CS-01..CS-10, FE-R1..FE-R7, OD-1..OD-17 |
| workflow/student/changes/stu-fe2-booking-ui.md (before this stage's append) | Input | 7BEFC4D19D508948625DA2F3B0874604E97FE420AB82FD27BA08785DB6037F6A | Same findings/decisions, FE2 scope |
| workflow/student/changes/stu-fe2-booking-ui/evidence/frontend-manifest-r2.json | Input | 9343DBE123F27D56100604D4932F6AD1D74D2D299A87CE6AF7D9952433B8D494 | Reviewed frontend revision; all 40 entries rechecked, 0 mismatches |
| src/frontend/src/api.ts | Input (current behavior) | 25F8CDBD4A545DEE968695983C926F49DE78D32B3D24B2B8CB33FB503C0237F6 | CSRF preflight, safeError mutation copy (line 92) |
| src/frontend/src/App.tsx | Input | 11FD37E757FC4904A0A81D7B0FD04043A2E3CA78218F8D7CA8098ADD480A4911 | logout() lines 39-43, restore/not-found screens, placeholder header |
| src/frontend/src/Login.tsx | Input | 272CF4BCA9D09BD21F9BC253FADEB853A5FDFB9C957EFAD50C84D8E4AB0BE005 | Decorative h2 before h1 |
| src/frontend/src/student/StudentWorkspace.tsx | Input | FE2661B8A979D430D02EA77F4F33D7624DE08940D5BE3D9A3E2F1B67CF5A9A99 | invalid notice never reset (lines 13, 25-27, 62); mutate() error path |
| src/frontend/src/test/auth.test.tsx | Input | 2E5DBA750D82F53A9B5C331D6E294FCFE191D62C989F74475336DE0D12E803D3 | Frozen FE1 tests (one oracle conflict, SA3-02) |
| src/frontend/src/test/student.test.tsx | Input | 38BFC6CCC5B312A08FE5D37D97C5EA3DF6F2B245F8D7A99341889CA0C6AC5B88 | Frozen FE2/FE-R1 tests |
| src/frontend/e2e/workspace.spec.ts | Input | 9D579B62B509162EC04B1B57336D0238DA6FAD691157AD1CC77F2D6EDC3D7711 | Browser checks |

Base commit / result commit / complete diff: base e277e583f1319320ed5f44e1944ac85296bb7d15;
no result commit; spec diff is `git diff -- workflow ':!logs'` plus untracked files
(this record, stu-fe4-followups.md, FRONTEND-DESIGN.md and earlier untracked records).
Uncommitted files and hashes: as in the table; this record has no self-hash.
Allowed write scope (frontend source/tests only):
- Test writer: `src/frontend/src/test/**` (including `api-fixture.ts`),
  `src/frontend/e2e/**`, and evidence under `workflow/student/changes/stu-fe3-review-fixes/evidence/`,
  plus its own section of this record.
- Implementer: `src/frontend/src/**` non-test source (`api.ts`, `App.tsx`,
  `Login.tsx`, `components.tsx`, `student/*.ts(x)`, `styles.css`), evidence and its
  own section of this record. No `package.json`/`package-lock.json`/config changes,
  no backend, no canonical spec edits (return spec problems to the analyst).
Prerequisites / dependency state and evidence: FE0 harness PASS (stu-fe0); FE1/FE2
reviewer-3 PASS on manifest-r2; worktree frontend verified equal to manifest-r2 by
this stage (script recomputation, 40/0). Node v24.21.0/npm 12.0.2 and Edge recorded
by earlier stages (use `npm.cmd` on PowerShell).
Recorded decisions and authorization references: Phuc, 22 October 2026, reply "for 6
dont need to think about for now the rest use A" to OD-1..OD-17, recorded in the
"Owner decision addendum" of stu-fe1-auth-ui and stu-fe2-booking-ui; now reflected in
FRONTEND-DESIGN "Revision 2 acceptance". OD-13 places OD-4, OD-5, OD-8, OD-12 before
acceptance. OD-14: any fresh session may perform any role (NOTE.md "Purpose and
scope" and "Agents and sessions" already say one agent runs every role in fresh
sessions and independence does not depend on the model; NOTE.md needs no edit).
Open questions / proposals / unverified facts: Q-1 and Q-2 below are for Phuc;
neither blocks FE3-T1 because the specified behavior follows the literal decision.
Verification commands, directories, results and output references: see
"Verification evidence".
Readiness or review verdict: READY_FOR_TESTS for FE3-T1 on the hashes above.

### Receiver receipt
Receiver role / actual tool and model: pending (fresh test writer).
Checked task, artifact revisions and required evidence: pending.
Receipt: pending (ACCEPTED | RETURNED).
Reason / exact missing or conflicting artifact: pending.

## Requirement-to-technical coverage
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| FE2-NOTHING-SENT (OD-4) | FRONTEND-DESIGN rev 2 table "Before acceptance"; Transport section (CSRF acquisition paragraph); booking mutation paragraph | Auth Technical HTTP contract (GET /api/auth/csrf); Integration section 2 error envelope; FRONTEND-DESIGN no-replay rule | COVERED | RTL: Book and Cancel with CSRF 503, network error and malformed body -> zero POST, exact alert, no uncertainty copy, Retry reopens dialog, second confirm sends one POST with fresh token; post-POST lost response still shows uncertainty copy; CSRF 401/inactive still FE1-DENIAL (existing FE-R1 cases) |
| FE1-DENIAL-CACHED (FE-R7b) | FRONTEND-DESIGN FE1-DENIAL row and rev 2 table | SH-WEB-03; actual CurrentAccountFilter 401/ACCOUNT_INACTIVE (per CS-01 evidence) | COVERED | RTL: obtain a token (e.g. after login or a first successful mutation), then Book and Cancel POST 401 and 403 ACCOUNT_INACTIVE -> one POST, Login with distinct message, no actor/slot/booking/dialog, no replay, single /me. Likely existing passing baseline |
| FE1-LOGOUT-CONFIRMED (OD-5) | FRONTEND-DESIGN Transport (logout paragraph), FE1-LOGOUT row note, rev 2 table | Auth Technical: POST /api/auth/logout -> 204 | COVERED | RTL: logout 204 + CSRF 503/network/malformed -> no alert, no Retry sign out, no /me; then Login submit acquires fresh CSRF then one login POST; logout POST network/503/CSRF_INVALID still shows unconfirmed alert + Retry (existing case extended) |
| FE2-NOTICE-CLEAR (OD-8) | FRONTEND-DESIGN Browser URL state paragraph; rev 2 table | none beyond UI (no API change); FE2-BROWSE query rules unchanged | COVERED | RTL: invalid URL -> notice; then each change type (module, date, week, Reset, status, Upcoming/History, page move, back/forward) -> notice gone; Refresh keeps it; navigating to another invalid URL shows it again |
| FE1-UI-LANDMARKS (OD-12) | FRONTEND-DESIGN Visual/interaction contract (placeholder header, landmarks paragraph); rev 2 table | UX-01; FE1-UI | COVERED | RTL: getAllByRole('banner') length 1 with "UniChope" on all eight page states; placeholder banner contents (only app name/home link and Sign out; no navigation, name, role or caption); Login heading order (first heading is h1 "Sign in", no skipped levels); Student header unchanged. Browser: existing mobile Login/320-1440 checks keep one h1 and no overflow |

Checklist areas: API/UI and auth/privacy examined (above). Data/concurrency: no new
writes or API calls; stale-generation rules of FE1-PRIVACY/FE2-RACE unchanged and
must stay green. Architecture/ownership: frontend-only; Auth contract unchanged; Kok
Seng reviews OD-5 copy at merge. AI: not applicable (no AI UI). Feasibility: existing
Vitest/RTL/Playwright harness and fixture `failure()`/deferred controls suffice; no
new dependency.

## Spec findings and proposed fixes
| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| SA3-01 / low (applies OD-4, OD-5, OD-8, OD-12) | Reviewer findings FE-R2/CS-05, FE-R3, FE-R5, FE-R6 had owner decisions but no canonical acceptance text (FRONTEND-DESIGN 3917AF4C... lines 63, 93-98, 133-134, 141-143). | Applied: FRONTEND-DESIGN rev 2 Transport, URL state, mutation, landmarks paragraphs and "Before acceptance" table with exact copy and scenarios. | Phuc (decided OD-4/5/8/12) | RESOLVED in FRONTEND-DESIGN 895AFB4C... |
| SA3-02 / medium (test oracle conflict) | Frozen `auth.test.tsx` case "FE1-PRIVACY no private browser-storage writes and untrusted actor names remain text" (lines 149-158) renders an ADMIN actor on `/admin` and requires `findByText(name)`. FE1-UI-LANDMARKS S2 (OD-12) removes the account name from Tutor/Admin placeholders, so this case would fail for a correct implementation. | Test writer re-targets the case to a STUDENT actor (whose header keeps the account name) or another surface that still renders actor-controlled text, keeping the XSS-as-text and no-storage assertions. Not a weakening; record old/new hash. | Test writer (spec analyst decision on oracle) | RESOLVED at spec level: FRONTEND-DESIGN Verification paragraph; test edit pending FE3-T1 |
| SA3-03 / low (FE-R7 placement) | FE-R7(b) (cached-token POST 401/ACCOUNT_INACTIVE) guards the protected mutation error path in api.ts/StudentWorkspace that OD-4 must change to distinguish pre-POST failures; FE-R7(a) cross-midnight and FE-R7(c) anonymous Login CSRF denial do not touch OD-4/5/8/12 code paths. | FE-R7(b) is FE1-DENIAL-CACHED here; FE-R7(a) is FE2-BROWSE-MIDNIGHT and FE-R7(c) is inside FE1-LOGIN-STALE in stu-fe4. | Spec analyst (routing) | RESOLVED: FRONTEND-DESIGN rev 2 tables |
| SA3-04 / low (boundary) | OD-5 (logout 204 then CSRF failure) and OD-1 (logout of an ended session) both change App.tsx logout(); OD-1 is a follow-up. | FE1-LOGOUT-CONFIRMED S3 lists the unconfirmed cases this change must keep (network, 503, malformed, CSRF_INVALID). Logout 401/ACCOUNT_INACTIVE is not asserted in FE3 and is specified by FE1-LOGOUT-ENDED in FE4. | Spec analyst | RESOLVED: FRONTEND-DESIGN Transport paragraph and both tables |
| SA3-05 / low (clarification, no behavior change) | CS-01(a): FE-R1 correction treats a protected mutation's CSRF preflight as protected, but the text only said "protected" requests. | Added the explicit sentence to FRONTEND-DESIGN Transport; FE2-NOTHING-SENT S4 points to it. Existing FE-R1 tests already cover it. | Spec analyst (clarification of reviewed behavior) | RESOLVED: FRONTEND-DESIGN Transport paragraph |
| SA3-06 / low (CS-07) | Student Technical section 1 said the shared React shell was "Not yet assigned / Out of scope"; PLAN reported 45 tests and "final independent review in progress". | Metadata refresh applied (no behavior change). | Phuc (authorized scope) | RESOLVED: TECHNICAL-SPEC 792718A4..., PLAN 008AA245... |
| Q-1 / info (Phuc) | OD-12 says Tutor/Admin placeholders show "only the app name and Sign out". Taken literally, the header drops the account name/role now shown there. | Specified literally (FE1-UI-LANDMARKS S2). If Phuc wants the account name kept, only S2 and SA3-02 change. | Phuc | OPEN question, non-blocking (literal reading implemented) |
| Q-2 / info (Phuc) | OD-4 retry form is not stated. | Specified a Retry control that reopens the confirmation (same pattern as the existing CSRF_INVALID retry; FRONTEND-DESIGN already says acquisition failure "permits retry"). | Phuc | Analyst clarification, non-blocking |

No high or critical finding. CS-02, CS-04, CS-06, INFO-1, CS-01(b)/(c) are routed to
stu-fe4; CS-03 (OD-6) deferred; CS-08 traceability and CS-10 deployment unchanged.

## Spec readiness
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision/hash: FE3-T1 (all five IDs) on FRONTEND-DESIGN
895AFB4CC2594ECBAD0E69387920E60879409DAA78595EB9779205FF1B7B9C8B, PLAN
008AA245CD581A837370BB2C96BA2C99A8322ED668AC727AECEEE73B15EF5766, Student Technical
792718A4E671F354FE508B76C6705F2BCB7F5A080398CEB669AA7E24FCE2F485, Auth PLAN
90C0F27E1E2B2204E3ECDA1E00B64A3B1DC6B77C687C000D7797B7DA1DA73B07 and the unchanged
inputs above.
Remaining findings and task-specific deferral rationale: Q-1/Q-2 are wording/form
questions with a literal, testable default; SA3-02 is resolved in the spec and is the
test writer's first action. FE4 items do not affect FE3 code paths except logout,
whose boundary SA3-04 fixes.
Required owner decisions and recorded outcome: OD-4, OD-5, OD-8, OD-12, OD-13
recorded (Phuc, 22 October 2026). Kok Seng's Auth review is a merge gate, not a
test prerequisite.
Recheck evidence: re-read the revised FRONTEND-DESIGN end to end; each new ID has a
source paragraph, a scenario table row and a planned test; no new ID requires an
API, DTO or backend change (only existing endpoints and statuses from the Auth HTTP
contract and Integration section 2 are used); compared every frozen test title and
oracle against the refinements: one conflict (SA3-02), no others (the placeholder
"exactly one button" assertion stays valid because an optional home link is a link).

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| FE3-T1 | FE2-NOTHING-SENT S1-S4 | student.test.tsx new cases (Book and Cancel x CSRF 503/network/malformed; retry; post-POST contrast) | Not written |
| FE3-T1 | FE1-DENIAL-CACHED | student.test.tsx new cases (Book and Cancel x 401/ACCOUNT_INACTIVE with cached token) | Not written |
| FE3-T1 | FE1-LOGOUT-CONFIRMED S1-S3 | auth.test.tsx new cases; existing network-failure case retained | Not written |
| FE3-T1 | FE2-NOTICE-CLEAR S1-S4 | student.test.tsx extension of invalid-filter case | Not written |
| FE3-T1 | FE1-UI-LANDMARKS S1-S4 | auth.test.tsx / student.test.tsx landmark and heading-order cases; SA3-02 re-target | Not written |
| Human | OD-15 journey | Phuc: student/tutor/admin login, timetable laptop+phone, book/cancel, keyboard only, 200% zoom | Pending after FE3 review |

Verification for the task: `npm.cmd test` (all suites), `npm.cmd run build`,
`npm.cmd run test:browser` in `src/frontend`; refreshed manifest and patch; fresh
independent review of the changed files.

## Verification evidence
Command, working directory, revision, exit code, result and relevant output (this
stage, cwd D:/cs3227/CS3227-2610-MP3, base e277e583f1319320ed5f44e1944ac85296bb7d15):
- `git rev-parse HEAD`: exit 0, e277e583f1319320ed5f44e1944ac85296bb7d15.
- `git status --short -- . ':!logs'`: exit 0; tracked spec edits and untracked
  frontend/workflow files as listed in Inputs.
- Node script recomputing SHA-256 of every `frontend-manifest-r2.json` entry: exit 0,
  "entries 40 mismatches 0" (before spec edits).
- `sha256sum` on all inputs before editing and on changed specs after editing: exit 0;
  values in the Inputs table.
- `git diff --check -- workflow ':!logs'`: exit 0.
Pre-implementation expected failure (or existing passing baseline): not run by the
spec analyst; test writer records it.
Final focused tests / integration tests / build: not run (spec stage).
Manual and real-provider checks: not applicable to this stage; OD-15 pending.

## Independent review
Reviewer role / actual model / reviewed revision: pending.
Verdict and evidence: pending.
Findings: pending.
Fixes and re-review results: pending.

## Human verification
Person, date, checked journey, outcome: pending (Phuc, OD-15).
Remaining limitations and follow-ups: stu-fe4-followups; FE-R4 without decision;
OD-6 deferred; Kok Seng/Huanghao merge reviews (OD-16).
Next agent, exact task and required inputs: fresh test writer; FE3-T1; this record,
FRONTEND-DESIGN 895AFB4C..., NOTE.md, HANDOFF.md, agents/test-writer.md, actual
frontend source/tests at manifest-r2.

## Analyst note (stu-fe5): FRONTEND-DESIGN revision 3 hash supersession
Producer: spec analyst for stu-fe5-theme-switch / Claude Code (Claude Agent SDK, fresh
subagent session) / Claude Opus 5.5 (claude-opus-5-5); 22 October 2026. Appended note;
earlier sections are unchanged and this is not a stage of this change.
- workflow/student/FRONTEND-DESIGN.md is now
  DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089 (supersedes
  895AFB4CC2594ECBAD0E69387920E60879409DAA78595EB9779205FF1B7B9C8B) and
  workflow/student/PLAN.md is now
  919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE (supersedes
  008AA245CD581A837370BB2C96BA2C99A8322ED668AC727AECEEE73B15EF5766).
- Revision 3 adds the theme switch (Phuc's TH-D1..TH-D3) for
  [stu-fe5-theme-switch](stu-fe5-theme-switch.md). It changes no scenario, copy or
  oracle of this change's acceptance IDs: the added lines are the revision 3 header
  note, a placeholder-header note and a browser-storage exception that both apply
  from stu-fe5 onward, a PLAN status line and FE5 row, and the new "Revision 3
  acceptance" section. Implement and test FE1-UI-LANDMARKS S2 and the storage rule
  here exactly as revision 2 states them; stu-fe5 later refines them.
- Receivers of this record should verify the new hashes and may accept them on that
  basis; this note does not change this record's readiness verdict.


## Handoff analyst-2: current implementation-request readiness (22 October 2026)
Producer role / actual tool and model: spec analyst / Codex / GPT-6; fresh subagent with no inherited conversation.
Recipient role: fresh test writer using agents/test-writer.md.
Change ID / task / exact requested next action: stu-fe3-review-fixes / FE3-T1. Write FE3 behavioral tests now, preserving privacy/XSS intent when retargeting the frozen ADMIN-name test to Student (SA3-02). Record red or existing passing baseline per ID.
Stage status / supersedes: READY_FOR_TESTS for specification consistency; refreshes analyst-1 and its hash-supersession note for the current worktree. Earlier findings and acceptance mappings remain preserved.
Goal and non-goals: deliver this existing capability as part of the user's current request "Implement stu-fe5". Necessary prerequisites are FE3 -> FE4 -> FE5; each keeps its own record, tests, implementation and independent review. Preserve each original non-goal and owner boundary; no backend, Auth policy or role-workspace feature change.
Scope check: standard path; these are independently testable behavior changes, split across the existing three records in dependency-safe order. No shorter-path claim.
Acceptance IDs: FE2-NOTHING-SENT, FE1-DENIAL-CACHED, FE1-LOGOUT-CONFIRMED, FE2-NOTICE-CLEAR, FE1-UI-LANDMARKS. Given/When/Then scenarios remain normative in FRONTEND-DESIGN.md (Revision 2 Before acceptance scenarios; Transport preflight/logout rules and visual landmarks); no scenario changed by this refresh.

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/FRONTEND-DESIGN.md revision 3 | Input | DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089 | Canonical scenarios and frozen-oracle allowances |
| workflow/student/PLAN.md FE3/FE4/FE5 rows | Input | 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE | Dependency-safe execution order |
| workflow/student/changes/stu-fe5-theme-switch/evidence/analyst-current-manifest.json | Output | 7864376780A756E09D3EE6DFAE72C6F39C96ECBDECBE936D9B831FA418176CDA | Current 45-entry source/config/test/spec manifest; actual per-file SHA-256 values |
| workflow/student/changes/stu-fe2-booking-ui/evidence/frontend-manifest-r2.json | Input | 9343DBE123F27D56100604D4932F6AD1D74D2D299A87CE6AF7D9952433B8D494 | Historical reviewed frontend comparison; four historical spec hashes superseded |

Base commit: e277e583f1319320ed5f44e1944ac85296bb7d15; no result commit. Existing staged and unstaged changes preserved; no staging or committing. Complete scope can be inspected with git diff HEAD -- src/frontend workflow and git ls-files --others --exclude-standard -- src/frontend workflow (include untracked FE5 record/evidence). No self-hash is claimed for this appended record.
Uncommitted input revisions: all 45 file hashes are in the current manifest. It includes Student Technical 792718A4E671F354FE508B76C6705F2BCB7F5A080398CEB669AA7E24FCE2F485 and Auth PLAN 90C0F27E1E2B2204E3ECDA1E00B64A3B1DC6B77C687C000D7797B7DA1DA73B07.
Allowed write scope: unchanged from analyst-1 for this change; frontend tests/e2e and its record/evidence for the test writer, non-test frontend files expressly permitted there for implementation. Append required human log without using it as input. No implementation/test edits made in this analyst session.
Prerequisites / dependency state and evidence: FE1/FE2 reviewed frontend manifest-r2: 36 frontend entries match current content. No FE3 execution prerequisite remains.
Recorded decisions and authorization: current user implementation request authorizes progressing necessary existing prerequisites; FRONTEND-DESIGN records OD-1..OD-17 and TH-D1..TH-D3. Existing literal defaults remain in effect, including exactly "Dark theme", placement on every banner and default FE4-before-FE5 order. No new material policy or design decision is required. Affected owners retain merge review; Phuc retains human acceptance.

### Requirement-to-technical coverage refresh
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| All IDs above (each existing individual mapping retained in analyst-1) | FRONTEND-DESIGN Revision 2 Before acceptance scenarios; Transport preflight/logout rules and visual landmarks; Student/shared UX-01 | Student Technical frontend pointer and endpoint contracts; Auth Technical HTTP contract; Integration section 2; SH-AUTH-01..05 and SH-WEB-01..03 | COVERED | RTL mutation preflight versus sent-POST failures, cached-token denial, confirmed logout, filter-navigation notice lifetime, eight-page banner and heading cases. |

### Spec findings and proposed fixes refresh
No new blocking contract gap found. Actual App.tsx still lacks banners on restore/failure/not-found and shows account details on placeholders (FE3 drift); logout conflates post-204 CSRF failure (FE3 drift). Login stale-session feedback, restore retry and merged modules/results errors remain FE4 implementation work. index.html has no early theme script and styles.css is light-only (FE5 drift). These are specified implementation gaps, not reasons to invent new behavior. Existing SA3-02 and SA5-03 oracle allowances must be applied by their respective test writer. Revision 3 leaves FE3/FE4 scenarios intact and refines placeholder controls only at FE5. AI, database races and server policy are outside this scope.

### Spec readiness refresh
Verdict: READY_FOR_TESTS on the manifest and exact canonical hashes above; runtime execution gates remain as stated under prerequisites. Existing per-ID coverage/findings are rechecked against the canonical scenarios, current source and unchanged frontend baseline. Test harness commands exist; this analyst has not run tests/build/browser checks and makes no implementation success claim. Human keyboard/phone/laptop/text-size and production-preview visual checks remain pending as applicable.
Verification: PowerShell in D:/cs3227/CS3227-2610-MP3; git rev-parse HEAD and git status --short inspected; Get-FileHash SHA256 recomputed all 40 historical manifest entries: 36 frontend matches, four spec mismatches (FRONTEND-DESIGN, Student TECHNICAL-SPEC, Student PLAN, Auth PLAN), all superseded by current manifest. Manifest generation exited 0 with 45 entries. Tests must record their own current baseline after required dependency review.
Next agent: fresh test writer, named tasks and acceptance IDs above; preserve original acceptance-to-test mapping and add executable evidence before implementation.

### Receiver receipt for analyst-2
Receiver role / actual tool and model: pending fresh test writer.
Checked task, artifact revisions and required evidence: pending; dependent changes must include their prerequisite reviewed revision.
Receipt: pending (ACCEPTED | RETURNED).
Reason / exact missing or conflicting artifact: pending receiver verification.

## Test-writer receipt for analyst-2 (22 October 2026)
Receiver: test writer / Codex / GPT-6, fresh session.
Receipt: ACCEPTED. FE3-T1 is the standard-path prerequisite to FE4/FE5; all 45
analyst-current-manifest.json entries recomputed with SHA-256 match, including
FRONTEND-DESIGN DBA3D9A3... and PLAN 919BD0A2...; HEAD is e277e583f1319320ed5f44e1944ac85296bb7d15.
Canonical revision 2 scenarios, revision 3 scope exception, actual source/tests,
configured Vitest harness and coverage/findings checked. No missing execution dependency.
Allowed writes: focused tests, this record/evidence, append-only human log.

## Handoff test-writer-1 (22 October 2026)
Producer role / actual tool and model: test writer / Codex / GPT-6; fresh session.
Recipient: fresh implementer using agents/implementer.md.
Change / exact next task: stu-fe3-review-fixes / FE3-T2: implement the five FE3
acceptance IDs against these executable tests; preserve their intent and all old tests.
Stage status: READY_FOR_IMPLEMENTATION (supersedes only the earlier "Not written"
test-mapping state). Analyst-2 readiness and canonical hashes remain current.
Goal/non-goals: FE3 prerequisite only; no FE4/FE5 behavior, backend, API, spec,
ownership or deployment change. Product code has not been edited in this stage.

### Inputs and outputs
| Artifact | Direction | SHA-256 / revision | Purpose |
| --- | --- | --- | --- |
| Handoff analyst-2 above; canonical FRONTEND-DESIGN and PLAN | Input | DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089; 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE | Current readiness and scenarios |
| src/frontend/src/test/fe3.test.tsx | Output | FE3B2AC749E7838941BC03F66FBE078430E2136DC821BDF0DCA5FE5FE1369722 | 58 parameterized behavioral cases |
| src/frontend/src/test/auth.test.tsx | Output | 101CFC584A4CCF871BB775252DC5B9510965B2CA3A9371A35F7029941E4986A3 | SA3-02 privacy/XSS fixture retargeted from ADMIN to STUDENT |
| src/frontend/src/test/student.test.tsx | Output | 3E4BDACEB6D2B1C0A03CFF6792EEDEA572329EA853F5361B7D298A49D959C967 | Placeholder account-name oracle aligned with OD-12; race assertions retained |
| evidence/test-writer-manifest.json (relative to this change directory) | Output | B5F4ECC6DD21BCDC4F87EBAEBF3F82657A02EAD546F890A21C91A0F04B04C17E | 49 exact source/spec/test/evidence hashes; no record self-hash |
| evidence/test-writer-baseline.json | Output | 4B8CAA447B86FC6F177E44F492A05E8EB39AEBE05692306D59046CF52489BC2C | Complete sanitized Vitest assertions/failures |
| evidence/test-writer-console.txt; evidence/test-writer-build.txt | Output | Per manifest; build AF9E289AC17F27B13ACAB8863B62B24ED1D8B1739D6EE3B7B477DF4B6F022FFD | Reproducible command output |

Base commit: e277e583f1319320ed5f44e1944ac85296bb7d15; no result commit, staging
or commit performed. Existing staged work preserved. Test-writer delta is the
three test paths above, this record/evidence and append-only logs/phuc.md.
Inspect complete existing diff with git diff HEAD -- src/frontend workflow and
include git ls-files --others --exclude-standard -- src/frontend workflow.
Old auth test hash: 2E5DBA750D82F53A9B5C331D6E294FCFE191D62C989F74475336DE0D12E803D3;
old Student test hash: 38BFC6CCC5B312A08FE5D37D97C5EA3DF6F2B245F8D7A99341889CA0C6AC5B88.
SA3-02 keeps malicious actor name rendered as text, no img injection and zero
Storage.setItem writes on a Student surface that still displays names. A second
frozen oracle in the existing late-mutation/new-ADMIN test expected "New Admin"
in the placeholder header; it now verifies the Admin destination h1 and absence
of the prior Student name. All no-resurrection, no-extra-read and late-response
assertions remain intact. This follows the same OD-12 removal, without changing
race behavior or canonical requirements.

### Acceptance-to-test mapping and executable baseline
All new cases are in src/frontend/src/test/fe3.test.tsx unless specified.
| ID / scenarios | Case title prefix and coverage | Baseline |
| --- | --- | --- |
| FE2-NOTHING-SENT S1/S2 | S1/S2 book/cancel CSRF 503/network/malformed/unexpected (202); zero POST, exact safe alert, closed dialog, same-detail Retry dialog, no automatic submission, explicit fresh-token POST/body | 8 red: currently uncertainty copy instead of nothing-sent copy; subsequent retry assertions executable after that gap is fixed |
| FE2-NOTHING-SENT S3 | S3 book/cancel sent POST network/503/malformed; uncertainty and exactly one POST | 6 passing |
| FE2-NOTHING-SENT S4 | Existing student.test.tsx FE-R1 book/cancel x 401/ACCOUNT_INACTIVE; zero POST, clear state, reject late private result | 4 passing |
| FE1-DENIAL-CACHED | book/cancel POST x 401/ACCOUNT_INACTIVE; real Login acquires cached token, both Student pages loaded first, no new token read, one POST, distinct Login messages, clear private state/dialog, one /me | 4 passing |
| FE1-LOGOUT-CONFIRMED S1/S2 | STUDENT/TUTOR/ADMIN x anonymous CSRF 503/network/malformed after 204; cleared private state, no warning/retry, next login obtains fresh token before one POST | 9 red: incorrect unconfirmed-signout alert after confirmed 204 |
| FE1-LOGOUT-CONFIRMED S3 | logout POST network/503/malformed/unexpected/CSRF_INVALID; unconfirmed warning/retry, one POST and /me | 5 passing |
| FE2-NOTICE-CLEAR S1/S2/S4 | module/date/Previous/This/Next week/Reset/status/Upcoming/History/page/popstate; normalization keeps notice, user action clears, later invalid URL restores | 11 red: notice never clears after user action |
| FE2-NOTICE-CLEAR S3 | Refresh and read Retry preserve notice | 2 passing |
| FE1-UI-LANDMARKS S1/S2/S4 | Eight page states, exact one banner/h1/skip; restricted placeholder controls/text; preserved Student header/navigation | 3 passing (Login/slots/bookings), 5 red (missing banners on session states/not-found; extra placeholder header text) |
| FE1-UI-LANDMARKS S3 | Login first heading is Sign in h1, no skipped heading levels | 1 red: intro h2 precedes h1 |

### Verification evidence
Environment: installed locked Node/npm frontend harness, synthetic in-memory fetch
fixtures only; no backend, database or credentials needed. Cwd for npm commands:
D:/cs3227/CS3227-2610-MP3/src/frontend.
- `npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe3-review-fixes/evidence/test-writer-baseline.json`: exit 1; **103 discovered, 69 passed, 34 failed**. All 45 retained tests pass; new suite has 24 passing and 34 genuine requirement failures listed above. No discovery/import/setup failure remains.
- `npm.cmd run build`: exit 0; TypeScript and Vite production build pass (27 modules), output in test-writer-build.txt.
- `git diff --check -- src/frontend workflow/student/changes/stu-fe3-review-fixes.md`: exit 0 (line-ending notices only).
- All analyst manifest inputs matched before edits (45 entries, zero mismatches).
Initial exploratory root-directory command and a Cancel-name lookup error were
corrected; their results are not used as red evidence. Final retained output is
from the corrected executable suite. Native command output captured through
cmd.exe redirection to preserve the process exit status.
Browser suite not run by test writer: no product change yet. Implementer must run
full unit suite, build, existing browser suite and provide new exact revision
manifest/complete diff for fresh independent review. Human laptop/phone, keyboard,
zoom and text-size journey remains pending Phuc (OD-15); real provider is not applicable.
Authorization: current "Implement stu-fe5" request plus existing analyst-2 decisions;
no additional policy or design choice. No unresolved test or setup blocker.
Allowed implementation scope remains analyst-1/2 frontend scope; no test weakening.

### Receiver receipt for test-writer-1
Receiver role / actual tool and model: pending fresh implementer.
Checked task, artifact revisions and evidence: pending.
Receipt: pending (ACCEPTED | RETURNED).

## Implementer receipt for analyst-2 and test-writer-1 (22 October 2026)
Receiver: implementer / Codex / GPT-6, fresh session. Receipt: ACCEPTED.
Verified all 49 test-writer-manifest.json hashes against actual files (zero
mismatches), canonical FRONTEND-DESIGN/PLAN revisions, actual tests and baseline
report, allowed source-only scope and HEAD e277e583f1319320ed5f44e1944ac85296bb7d15.
FE3-T2 is implementation of FE3-T1 acceptance IDs; no prerequisite remains.
Root causes checked in source: preflight failures share sent-mutation uncertainty;
logout catches anonymous-token refresh with the confirmed POST; normalization notice
has no clearing transition; standalone states lack banners and Login intro h2
precedes h1. Tests remain frozen. Existing design and ownership are preserved.

## Handoff implementer-1 (22 October 2026)
Producer: implementer / Codex / GPT-6, fresh session with no inherited conversation.
Recipient: fresh reviewer using agents/reviewer-security.md.
Task: stu-fe3-review-fixes / FE3-T2 (implementation of FE3-T1 acceptance tests).
Status: IN_REVIEW; analyst-2 and test-writer-1 requirements remain unchanged.
Goal/non-goals: the five FE3 IDs only; no FE4/FE5, backend, API, spec or ownership
change. No tests, dependencies, configs, staging or commits changed in this stage.
Base: e277e583f1319320ed5f44e1944ac85296bb7d15; result is uncommitted worktree.
Authorization: current user "Implement stu-fe5", FE3 prerequisite and recorded
OD-4/5/8/12 design. Shared merge review remains Kok Seng/Huanghao; Phuc accepts UI.

### Inputs and outputs
All paths below are relative to workflow/student/changes/stu-fe3-review-fixes/evidence/.
| Artifact | Direction | SHA-256 / revision | Purpose |
| --- | --- | --- | --- |
| FRONTEND-DESIGN revision 3; PLAN | Input | DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089; 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE | Canonical FE3 scenarios unchanged |
| test-writer-manifest.json; baseline | Input | B5F4ECC6DD21BCDC4F87EBAEBF3F82657A02EAD546F890A21C91A0F04B04C17E; 4B8CAA447B86FC6F177E44F492A05E8EB39AEBE05692306D59046CF52489BC2C | Frozen tests and 34 behavioral failures |
| implementer-manifest.json | Output | ADC5BCFDD404A5B246B6C6D4E2FA72C1758F7D3494359FEC4056B5694AA6D35B | 55 exact source/test/config/spec/evidence hashes, no record self-hash |
| implementer-frontend.patch | Output | A030CFEC6DD8FD1CE2A7CD0E3EEC2CB14D8924BDEA1C16CC71BAC4805A2C27B6 | Complete HEAD-to-worktree frontend patch including untracked FE3 tests |
| implementer-source-delta.patch | Output | A8A5C507A98BFC0CA9608258A0DCB0D1E66B239438005EAAE29F0478E2EA2B4C | Five-file implementer source delta against staged baseline |
| implementer-unit.json | Output | CDEA8F2194DCCF0A0F99AE49C913032B00CDBFB6A6F4006FC3193DAA0891A706 | Full final assertion report |
| implementer-unit.txt; implementer-build.txt; implementer-browser.txt | Output | Per manifest | Sanitized command evidence |

Allowed writes were non-test frontend source, own record/evidence and append-only
human log. Actual code delta: src/frontend/src/{api.ts,App.tsx,Login.tsx,styles.css,
student/StudentWorkspace.tsx}. All previous work preserved. Frozen test hashes still
match test-writer-1 exactly (fe3.test.tsx FE3B2AC7..., auth.test.tsx 101CFC58...,
student.test.tsx 3E4BDACE...). Canonical documents untouched. The record itself and
append-only human log are workflow reporting; no recursive self-hash claim.

### Acceptance-to-code and verification
| ID | Source behavior | Evidence |
| --- | --- | --- |
| FE2-NOTHING-SENT | api.ts marks acquisition failure as PreflightError only before POST; StudentWorkspace offers same-item confirmation Retry; safeError has exact nothing-changed copy. Sent POST uncertainty unchanged. | FE3 S1/S2/S3 and existing FE-R1 denial tests pass |
| FE1-DENIAL-CACHED | Existing protected POST denial callback/reset retained, no replay or extra token fetch | All four cached-token tests pass |
| FE1-LOGOUT-CONFIRMED | App.tsx separates confirmed logout POST from best-effort anonymous CSRF acquisition; Login obtains token on next submit | All nine role/failure combinations and five unconfirmed POST tests pass |
| FE2-NOTICE-CLEAR | StudentWorkspace tracks only the normalizing replacement URL; later navigation clears, same-URL filter controls clear explicitly; read refresh/retry preserve | All 13 notice tests pass |
| FE1-UI-LANDMARKS | App standalone states gain banner; placeholder header omits account/role/caption; Login intro uses styled paragraph preserving visual hierarchy | All eight page states and heading-order test pass; existing Student header tests pass |

Commands run in D:/cs3227/CS3227-2610-MP3/src/frontend using installed locked
Node/npm and Edge, deterministic synthetic HTTP fixtures (no credentials/backend):
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe3-review-fixes/evidence/implementer-unit.json: exit 0, 103/103 pass, zero failed. Output implementer-unit.txt. Final rerun after CSS adjustment.
- npm.cmd run build: exit 0, TypeScript and Vite build, 27 modules; implementer-build.txt.
- npm.cmd run test:browser: exit 0, 7/7 pass; responsive 320/768/1024/1440, mobile Login keyboard, confirmation focus trap/Escape, runtime smoke; implementer-browser.txt.
- git diff --check -- src/frontend workflow/student/changes/stu-fe3-review-fixes.md: exit 0, line-ending notices only.
All checks above cover the final five source files. No test defect or contract
change was needed. No real backend/provider smoke claimed in this stage.
Human visual finish, laptop/phone booking/cancellation journey, 200% zoom and text
size remain pending Phuc (OD-15); no independent review claimed by implementer.
Next: fresh reviewer verify manifest, derive canonical checklist, inspect full
frontend patch plus focused source delta, rerun required checks and report verdict.

### Receiver receipt for implementer-1
Receiver: pending fresh reviewer. Checked artifacts/revisions: pending.
Receipt: pending (ACCEPTED | RETURNED).

## Handoff reviewer-1 (22 October 2026)
Producer: independent reviewer/security checker / Codex / GPT-6, fresh session
that produced none of the reviewed implementation or tests.
Recipient: Phuc for human acceptance; fresh FE4 test writer may use this reviewed
FE3 revision as its prerequisite. Status: READY_FOR_HUMAN. Verdict: PASS for FE3.
This section supersedes the pending implementer-1 receiver receipt above.

### Receiver receipt and exact revision
Receipt: ACCEPTED. Read NOTE/HANDOFF/reviewer prompt, derived the five-ID checklist
directly from canonical FRONTEND-DESIGN before upstream conclusions, inspected
actual code/tests and complete deltas, then checked readiness/findings/evidence.
Base remains e277e583f1319320ed5f44e1944ac85296bb7d15, uncommitted worktree.
All 55 entries in implementer-manifest.json match (zero mismatches).
Reviewed manifest SHA-256: ADC5BCFDD404A5B246B6C6D4E2FA72C1758F7D3494359FEC4056B5694AA6D35B.
Complete frontend patch SHA-256: A030CFEC6DD8FD1CE2A7CD0E3EEC2CB14D8924BDEA1C16CC71BAC4805A2C27B6;
source delta: A8A5C507A98BFC0CA9608258A0DCB0D1E66B239438005EAAE29F0478E2EA2B4C.
Canonical FRONTEND-DESIGN: DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089;
PLAN: 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE.
Reviewed FE2 frontend manifest comparison identifies exactly five changed source
files (api.ts, App.tsx, Login.tsx, styles.css, StudentWorkspace.tsx), the two
permitted oracle changes (auth.test.tsx/student.test.tsx), and new fe3.test.tsx.
All other frontend baseline files, including dependencies/configuration, match.
Test revisions match test-writer-1; no skipped/weakened acceptance assertions found.

### Independent acceptance and security review
| Acceptance | Independent closure evidence |
| --- | --- |
| FE2-NOTHING-SENT S1-S4 | PreflightError is thrown before fetch of mutation; session denial callback/reset precedes this classification. Both operations cover zero POST, closed confirmation, exact safe alert, explicit same-item Retry and fresh-token single POST. Sent failures remain uncertain; FE-R1 denial/race tests retained. |
| FE1-DENIAL-CACHED | Protected POST denial still resets generation/token/controllers and unmounts private workspace/dialog; four tests verify distinct Login messages, one POST, no extra CSRF or /me, no replay. |
| FE1-LOGOUT-CONFIRMED S1-S3 | Confirmed 204 and subsequent best-effort CSRF refresh have separate failure handling. Nine role/failure combinations verify clear/no warning and later fresh-token login; five POST-failure cases preserve retry warning. |
| FE2-NOTICE-CLEAR S1-S4 | Only the normalizing replacement is exempted from clearing. Explicit controls clear even on unchanged URL; later location changes clear; read Refresh/Retry preserve. All 13 navigation/refresh cases pass. |
| FE1-UI-LANDMARKS S1-S4 | Standalone states have one header banner; authenticated/anonymous branches cannot add a second. Eight states verify one banner/h1/skip, Student account/navigation retained, restricted placeholders, Login heading order. Browser keyboard/responsive checks remain passing. |

Independently checked SA3-01..SA3-06 against current canonical contracts and actual
delta: acceptance wording is present; SA3-02 privacy/XSS retarget preserves its
assertions on Student, and the Admin race oracle preserves all no-resurrection
checks; FE-R7 routing, FE4 logout boundary, protected preflight and ownership
metadata are consistent. Q-1/Q-2 use the recorded literal/default decisions and
are not blockers. FE4/FE5 behaviors remain explicitly separate prerequisites.
Auth/shared/integration checks confirm stored-role routing, same-origin CSRF,
generation resets, no private storage/HTML injection and no automatic mutation
retry remain intact. No dependency/backend/AI change is present; no new advisory
scan or real backend/provider evaluation was required or claimed.
Findings: none blocking; no new lower-severity follow-up identified in FE3 scope.

### Fresh verification and outputs
Commands run against the exact manifest revision in
D:/cs3227/CS3227-2610-MP3/src/frontend with installed locked harness and Edge:
- `npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe3-review-fixes/evidence/reviewer-unit.json`: exit 0, 103/103 passed, zero failed.
- `npm.cmd run build`: exit 0, TypeScript/Vite, 27 modules.
- `npm.cmd run test:browser`: exit 0, 7/7 passed; 320/768/1024/1440px, keyboard focus trap/Escape, mobile Login, runtime smoke. Synthetic browser HTTP fixtures only.
- From repository root, `git diff --check -- src/frontend workflow/student/changes/stu-fe3-review-fixes.md`: exit 0, line-ending notices only.

Reviewer output paths (relative to this change's evidence directory):
reviewer-unit.json SHA-256 A574DC0F4B48645BED7A3DA752D90250F6E2AD59C3D3AA6FA6F8E8E0A5A5D2EC;
reviewer-verification.json SHA-256 E91B4E57907E619D6D70C1445D7FAFA506E51F8E0EC7689F709D66179452F0F8
records exact checked artifact hashes, manifest comparison and fresh command results.

Allowed review writes: own record/evidence and append-only human log only; no
implementation/test/spec edits, staging or commits. PASS is tied to the manifest
above and permits FE4 to start. Human OD-15 laptop/phone, keyboard, 200% zoom and
text-size journey remains pending Phuc; Kok Seng/Huanghao retain shared merge
review. No deployment approval or COMPLETE status is claimed.

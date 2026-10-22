# stu-fe4-followups: FE1/FE2 follow-up behavior after acceptance fixes

Status: READY_FOR_TESTS (execution waits for stu-fe3-review-fixes review; see dependencies)
Scope classification and reason: standard path. User-visible behavior changes in
session restoration, Login/sign-out handling of existing Auth responses, filter
normalization, degraded module loading and a measurable contrast requirement.
Split per OD-13: follow-up decisions; the "before acceptance" set is
[stu-fe3-review-fixes](stu-fe3-review-fixes.md).
Parent request / related changes / dependencies: Phuc's owner decisions OD-1..OD-17
(22 October 2026) on [stu-fe1-auth-ui](stu-fe1-auth-ui.md) and
[stu-fe2-booking-ui](stu-fe2-booking-ui.md) reviewer-3/analyst-3 findings.
Depends on stu-fe3-review-fixes: FE4 edits the same files (App.tsx logout/restore,
api.ts, StudentWorkspace.tsx, Login.tsx) and inherits FE3's landmark and notice
behavior (FE2-UNKNOWN-MODULE reuses FE2-NOTICE-CLEAR; the restore-failure screen
must keep FE1-UI-LANDMARKS). Tests may be drafted after this packet, but the
implementer starts from the FE3 reviewed revision; the test writer re-verifies
against it before recording red evidence.
Human owner / affected owners: Phuc; Kok Seng (Auth: OD-1/OD-2 review with the Auth
merge review); Huanghao (shared shell merge review).
Current agent role / actual model or tool: spec analyst / Claude Code (Claude Agent
SDK, fresh subagent session with no inherited conversation, transcript or log) /
Claude Opus 5.5 (claude-opus-5-5); 22 October 2026.
Stage handoff manifest / revision: Handoff analyst-1 below.
Receiver receipt: pending fresh test writer.
Base commit and current revision/diff reference: HEAD
e277e583f1319320ed5f44e1944ac85296bb7d15 plus uncommitted worktree; frontend at this
stage equals manifest-r2 (40/0). The effective base for FE4 implementation is the
FE3 reviewed revision, to be recorded by the FE4 test writer.
Spec paths + revision: see Inputs and outputs.
Acceptance IDs and Given/When/Then scenarios: FE1-RESTORE-UNREACHABLE,
FE2-UNKNOWN-MODULE, FE2-MODULES-DEGRADED, FE-UI-CONTRAST, FE1-LOGIN-STALE,
FE1-LOGOUT-ENDED, FE2-BROWSE-MIDNIGHT. Normative scenarios are in
[FRONTEND-DESIGN.md](../FRONTEND-DESIGN.md) "Revision 2 acceptance", table
"Follow-ups". Human check FE-HV-TEXTSIZE and deferred OD-6 are in the same section.
Allowed scope/files and dependencies: see Handoff analyst-1.
Assumptions / open decisions / owner agreement: see Handoff analyst-1.

## Handoff analyst-1
Producer role / actual tool and model: spec analyst / Claude Code (Claude Agent SDK
fresh subagent) / Claude Opus 5.5 (claude-opus-5-5).
Recipient role: fresh test writer, then fresh implementer, then fresh independent
reviewer (any model per OD-14).
Change ID / task / exact requested next action: stu-fe4-followups / tasks FE4-T1
(session and Auth-response handling: FE1-RESTORE-UNREACHABLE, FE1-LOGIN-STALE,
FE1-LOGOUT-ENDED), FE4-T2 (Student filters/degraded modules/regression:
FE2-UNKNOWN-MODULE, FE2-MODULES-DEGRADED, FE2-BROWSE-MIDNIGHT) and FE4-T3
(FE-UI-CONTRAST). Test writer: after stu-fe3 has a reviewed revision, verify hashes
against it, write behavioral tests per ID and record red/baseline evidence.
FE2-BROWSE-MIDNIGHT is regression coverage of existing behavior (expect a passing
baseline). T1-T3 are independent of each other but share files; one test-writer and
one implementer session may cover all three with per-ID mapping.
Stage status / supersedes: READY_FOR_TESTS; first packet; supersedes none.
Goal: implement OD-3, OD-7, OD-9, OD-11 as follow-ups, plus OD-1/OD-2 (see Q-3) and
the FE-R7(a,c) test gaps.
Non-goals: FE3 IDs; OD-6 accessible Book/Cancel names (deferred); OD-10 text size
(human check FE-HV-TEXTSIZE; any enlargement is a new change); FE-R4 (no
decision); Auth endpoint/policy/backend changes; AI; Tutor/Admin features;
deployment.
Acceptance IDs: FE1-RESTORE-UNREACHABLE (OD-3), FE2-UNKNOWN-MODULE (OD-7),
FE2-MODULES-DEGRADED (OD-11), FE-UI-CONTRAST (OD-9), FE1-LOGIN-STALE (OD-2,
FE-R7c), FE1-LOGOUT-ENDED (OD-1), FE2-BROWSE-MIDNIGHT (FE-R7a).

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/FRONTEND-DESIGN.md (revision 2) | Output (changed by this stage) | 895AFB4CC2594ECBAD0E69387920E60879409DAA78595EB9779205FF1B7B9C8B | Normative IDs/scenarios; supersedes 3917AF4CD2709934383E03754A302ABA15D543A913B82EAA15A25B8F9D3F80DE |
| workflow/student/PLAN.md | Output (changed) | 008AA245CD581A837370BB2C96BA2C99A8322ED668AC727AECEEE73B15EF5766 | FE4 task row; supersedes 9C5EECA85D36CDFED9D6F5ABD89176905F40556727F01AC9303D2C4EC4716FA5 |
| workflow/student/TECHNICAL-SPEC.md section 1 | Output (changed, CS-07 only) | 792718A4E671F354FE508B76C6705F2BCB7F5A080398CEB669AA7E24FCE2F485 | Supersedes 97C2B915682BEC9EEC61BF29CCE665EF2088458C7DDE78E349CC47A88366F698 |
| workflow/auth/PLAN.md "Current results" | Output (changed) | 90C0F27E1E2B2204E3ECDA1E00B64A3B1DC6B77C687C000D7797B7DA1DA73B07 | OD-1/OD-2 note for Kok Seng; supersedes 5F40F9BE474C96FB0D22C89523367F20E8586AF59062B6DF278C43F6C1CD6ACB |
| workflow/student/changes/stu-fe3-review-fixes.md | Input (dependency) | 73BF17D7E9C28833139402AE99D105E03A3138A6A1D70C35C1B33D6DDF63F4FA (as written by this stage; later FE3 stages supersede it) | Prerequisite change |
| workflow/student/PRODUCT-SPEC.md | Input (unchanged) | 750433038415D5AF0832D4E506FB669C1D08A9A583FF388C4DEFC4F9FD690B86 | Student requirements |
| workflow/auth/PRODUCT-SPEC.md | Input (unchanged) | BE2F2F59B4E0ABEF49081A614DC77FA2DFB5BFDEB114C4A3B49AC38D2A73A637 | Auth requirements |
| workflow/auth/TECHNICAL-SPEC.md | Input (unchanged) | 91949EC9793C1747F99C87D86FF74C941700BB9D0133F6204582B0C529E689E9 | Auth HTTP contract (csrf/login/me/logout statuses) |
| workflow/SHARED-TECHNICAL-SPEC.md | Input (unchanged) | 5F92FF1F89B0171232BF3FF49C8517A9743EAEF2602B27F1C5D38EAFDE9912A5 | SH-AUTH/SH-WEB |
| workflow/INTEGRATION-SPEC.md | Input (unchanged) | F7893269A4F5C3F1F4E106F3C62225DD3E8D3B4E3C0C31B6CA4A5CE61FAD0604 | Error codes |
| workflow/student/NOTE.md | Input (unchanged) | 333031E4BA3A4DAF09DD29ABB5E1C3ADF21F675D8B8916DEB11CD5FA8882F981 | Workflow, template, OD-14 basis |
| workflow/student/HANDOFF.md | Input (unchanged) | 5594B343C530CCB81111EA4F8008356369B65CE8927108E8B4163D0D369F5CA6 | Packet format |
| workflow/student/agents/spec-analyst.md | Input (unchanged) | 975D2D5530E16A236666094FEAEB39406190F3D6718F655A256271EC40D04606 | Role prompt |
| workflow/student/changes/stu-fe1-auth-ui.md (before this stage's append) | Input | A8BF7240583AE019F323A0FB4BF6AB7AABC1007A6EEC82639767562411B35404 | CS-01/CS-02, FE-R7, OD addendum |
| workflow/student/changes/stu-fe2-booking-ui.md (before this stage's append) | Input | 7BEFC4D19D508948625DA2F3B0874604E97FE420AB82FD27BA08785DB6037F6A | CS-04/CS-06, INFO-1/2, OD addendum |
| workflow/student/changes/stu-fe2-booking-ui/evidence/frontend-manifest-r2.json | Input | 9343DBE123F27D56100604D4932F6AD1D74D2D299A87CE6AF7D9952433B8D494 | Current reviewed frontend (40/0 at this stage) |
| src/frontend/src/App.tsx | Input | 11FD37E757FC4904A0A81D7B0FD04043A2E3CA78218F8D7CA8098ADD480A4911 | Restore failure (reload Retry, "Unable to open your workspace"), logout() |
| src/frontend/src/Login.tsx | Input | 272CF4BCA9D09BD21F9BC253FADEB853A5FDFB9C957EFAD50C84D8E4AB0BE005 | Generic "temporarily unavailable" for stale-session cases |
| src/frontend/src/api.ts | Input | 25F8CDBD4A545DEE968695983C926F49DE78D32B3D24B2B8CB33FB503C0237F6 | Anonymous request handling |
| src/frontend/src/student/StudentWorkspace.tsx | Input | FE2661B8A979D430D02EA77F4F33D7624DE08940D5BE3D9A3E2F1B67CF5A9A99 | readError merges modules/slots errors (line 57) |
| src/frontend/src/student/filters.ts | Input | 9A05C9ADC0A395689AC7DA76F0B31281CBBA5C93F8B8335CD6378A53F1F52E8D | Keeps unlisted well-formed moduleId (line 8) |
| src/frontend/src/student/Consultations.tsx | Input | 1AB2702504138F501F9C28A1A5077F04B5C59CC5B04D51C31ABC53C09C4FE4BD | Cross-midnight rendering |
| src/frontend/src/styles.css | Input | 03707F240963675587C05D233EE57B96F79E86C13B08F9483D14EAF19F265D11 | Colour tokens (contrast) |
| src/frontend/src/test/auth.test.tsx | Input | 2E5DBA750D82F53A9B5C331D6E294FCFE191D62C989F74475336DE0D12E803D3 | Frozen FE1 tests |
| src/frontend/src/test/student.test.tsx | Input | 38BFC6CCC5B312A08FE5D37D97C5EA3DF6F2B245F8D7A99341889CA0C6AC5B88 | Frozen FE2 tests (line 22 cross-midnight case lacks end-date assertion) |
| src/frontend/e2e/workspace.spec.ts | Input | 9D579B62B509162EC04B1B57336D0238DA6FAD691157AD1CC77F2D6EDC3D7711 | Browser checks; no contrast check yet |

Base commit / result commit / complete diff: base e277e583f1319320ed5f44e1944ac85296bb7d15;
no result commit; spec diff `git diff -- workflow ':!logs'` plus untracked records.
Uncommitted files and hashes: as above; this record has no self-hash; FE3 hashes are
superseded by FE3's own later stages, so the FE4 test writer records the FE3
reviewed manifest it starts from.
Allowed write scope (frontend source/tests only):
- Test writer: `src/frontend/src/test/**`, `src/frontend/e2e/**`, evidence under
  `workflow/student/changes/stu-fe4-followups/evidence/`, own record section.
- Implementer: `src/frontend/src/**` non-test source and `styles.css`, evidence, own
  record section. FE-UI-CONTRAST may add one pinned dev dependency for an automated
  contrast check (for example axe-core) to `src/frontend/package.json` and
  `package-lock.json` only if a computed-style ratio check in Playwright is not
  sufficient; record the reason, version and lockfile hash. No backend, Auth policy
  or canonical spec edits.
Prerequisites / dependency state and evidence: stu-fe3-review-fixes reviewed revision
(pending); FE0 harness PASS; FE1/FE2 reviewer-3 PASS on manifest-r2.
Recorded decisions and authorization references: Phuc, 22 October 2026, reply "for 6
dont need to think about for now the rest use A" (OD addendum in stu-fe1/stu-fe2),
reflected in FRONTEND-DESIGN rev 2. OD-13 lists OD-3, OD-7, OD-9, OD-10, OD-11 as
follow-ups. OD-2 requires Kok Seng's review of OD-1/OD-2 with the Auth merge review.
OD-14: any fresh session may take any role (NOTE.md already states this; verified,
no edit).
Open questions / proposals / unverified facts: Q-3 (OD-1/OD-2 ordering) for Phuc;
see findings.
Verification commands, directories, results and output references: see
"Verification evidence".
Readiness or review verdict: READY_FOR_TESTS for FE4-T1..T3 on the spec hashes above;
execution dependency on FE3 recorded.

### Receiver receipt
Receiver role / actual tool and model: pending (fresh test writer).
Checked task, artifact revisions and required evidence: pending; must include the
FE3 reviewed revision.
Receipt: pending (ACCEPTED | RETURNED).
Reason / exact missing or conflicting artifact: pending.

## Requirement-to-technical coverage
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| FE1-RESTORE-UNREACHABLE (OD-3) | FRONTEND-DESIGN Transport (restoration outcomes); rev 2 "Follow-ups" | Auth Technical GET /api/auth/me; CurrentAccountFilter 401/403/503 (CS-02 evidence); Integration section 2 | COVERED | RTL: /me 503, network, malformed, 500 -> URL unchanged, alert text, Try again, no Login form; Try again issues second /me (no reload) and routes per outcome; /me 401 -> Login no warning; ACCOUNT_INACTIVE -> inactive message |
| FE1-LOGIN-STALE (OD-2, FE-R7c) | FRONTEND-DESIGN Transport (Login paragraph); rev 2 | Auth Technical: csrf 401/ACCOUNT_INACTIVE via shared filter, login 403 CSRF_INVALID | COVERED | RTL: CSRF 401 and 403 ACCOUNT_INACTIVE -> zero POST, exact "Please try again.", no other copy, password cleared, Login usable, no session-clear transition; login 403 CSRF_INVALID -> one POST then same; resubmit acquires fresh CSRF before one POST |
| FE1-LOGOUT-ENDED (OD-1) | FRONTEND-DESIGN Transport (logout paragraph); rev 2 | Auth Technical POST /api/auth/logout; filter 401/ACCOUNT_INACTIVE | COVERED | RTL: logout CSRF (no cached token) 401/inactive and logout POST 401/inactive -> Login, no alert, no Retry sign out, no /me; later Sign in acquires fresh CSRF |
| FE2-UNKNOWN-MODULE (OD-7) | FRONTEND-DESIGN Browser URL state; rev 2 | Student Technical section 3 (/modules active list; unknown moduleId returns []) | COVERED | RTL: unlisted UUID -> URL replaced without moduleId (week/date kept), notice, next /slots without moduleId, select "All modules", deferred response for removed module not rendered; listed id unchanged; modules failure keeps id |
| FE2-MODULES-DEGRADED (OD-11) | FRONTEND-DESIGN data-region paragraph; rev 2 | Student Technical section 3 (independent GET /modules and /slots) | COVERED | RTL: modules 503/network/malformed + slots OK -> slots and Book visible, Module control disabled, status notice /module filter.*unavailable/i, no generic error; Refresh recovers; active moduleId not shown as "All modules"; session denial/FORBIDDEN/slots failure keep existing handling |
| FE-UI-CONTRAST (OD-9) | FRONTEND-DESIGN visual contract (contrast criterion); rev 2 | UX-01; WCAG 2.1 SC 1.4.3 and 1.4.11 | COVERED | Playwright: computed contrast (or axe color-contrast) zero failures on listed screens at 1440/320; focus indicator and control boundary ratios computed or manually recorded |
| FE2-BROWSE-MIDNIGHT (FE-R7a) | FRONTEND-DESIGN desktop paragraph (cross-midnight once with full end date/time); rev 2 | dates.ts SGT Intl formatting; FE2-BROWSE | COVERED | RTL extending student.test line 22 case: end date (next day) and 00:30 visible, slot rendered once; desktop and mobile |
| FE-HV-TEXTSIZE (OD-10) | FRONTEND-DESIGN "Human check and deferred items" | none (human) | COVERED as manual check | Phuc on laptop and phone during OD-15 |

Checklist areas: API/UI and auth/privacy examined. Data/concurrency: no new writes;
FE2-UNKNOWN-MODULE must keep late-response isolation (useRead identity) and
FE1-RESTORE-UNREACHABLE retry must not flash protected content. Architecture/
ownership: frontend-only; OD-1/OD-2 present existing Auth responses differently
and need Kok Seng's merge review; no Auth contract change. AI: not applicable.
Feasibility: existing harness; contrast check is feasible with Playwright computed
styles without new dependency; optional dependency route recorded above.

## Spec findings and proposed fixes
| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| SA4-01 / low (applies OD-1, OD-2, OD-3, OD-7, OD-9, OD-11) | CS-01(b)/(c), CS-02, CS-04, CS-06, INFO-1 had owner decisions but no canonical acceptance (FRONTEND-DESIGN 3917AF4C... lines 28, 81-98, 133-134, 158-161). | Applied in FRONTEND-DESIGN rev 2 paragraphs and "Follow-ups" table with exact copy where the decision gave it. | Phuc (decided) | RESOLVED in 895AFB4C... |
| SA4-02 / low (testability) | Current restore failure Retry calls `window.location.reload()` (App.tsx line 46), which jsdom cannot observe as a new /me; OD-3 says "Try again" and stay on the screen. | Spec requires Try again to perform a new /me check without a full page reload. | Spec analyst (testability clarification within OD-3) | RESOLVED: FRONTEND-DESIGN restoration outcomes |
| SA4-03 / low (interaction OD-7 x OD-11) | An unlisted moduleId cannot be verified when /modules fails. | Keep and send moduleId while the list is unavailable; disabled control must not display "All modules" while a moduleId is active. | Spec analyst | RESOLVED: FE2-UNKNOWN-MODULE S3, FE2-MODULES-DEGRADED S3 |
| SA4-04 / low (copy for OD-11) | OD-11 says "a short notice" without exact text. | Oracle is the pattern /module filter.*unavailable/i, recommended "Module filter unavailable right now."; role status, not alert. | Phuc may set exact copy later | RESOLVED with pattern oracle |
| SA4-05 / low (FE-R7 placement) | FE-R7(a) has no OD; FE-R7(c) is the anonymous-Login half of CS-01(b). | FE2-BROWSE-MIDNIGHT here; FE-R7(c) folded into FE1-LOGIN-STALE. FE-R7(b) is in FE3 (FE1-DENIAL-CACHED). | Spec analyst | RESOLVED |
| Q-3 / info (Phuc) | OD-13 lists before-acceptance (OD-4, 5, 8, 12) and follow-ups (OD-3, 7, 9, 10, 11) but does not place OD-1 or OD-2. | Placed in FE4 because they are not listed as before-acceptance and need Kok Seng's review; current behavior is fail-safe (CS-01 analysis). Phuc may move them to FE3 before FE3 tests are written; doing so changes only the two records and PLAN rows, not the scenarios. | Phuc | OPEN question, non-blocking |
| FE-R4 / low (no decision) | aria-pressed/selected on Upcoming/History (reviewer-3) has no OD. | Not specified here; remains a reviewer follow-up awaiting Phuc. | Phuc | OPEN, outside FE4 |

No high or critical finding. OD-6 deferred; OD-10 human check; CS-08/CS-10 unchanged.

## Spec readiness
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision/hash: FE4-T1, FE4-T2, FE4-T3 on FRONTEND-DESIGN
895AFB4CC2594ECBAD0E69387920E60879409DAA78595EB9779205FF1B7B9C8B, PLAN
008AA245CD581A837370BB2C96BA2C99A8322ED668AC727AECEEE73B15EF5766, Student Technical
792718A4E671F354FE508B76C6705F2BCB7F5A080398CEB669AA7E24FCE2F485, Auth PLAN
90C0F27E1E2B2204E3ECDA1E00B64A3B1DC6B77C687C000D7797B7DA1DA73B07 and unchanged inputs.
Remaining findings and task-specific deferral rationale: Q-3 affects ordering only;
FE-R4 has no decision and no FE4 ID. Execution depends on FE3's reviewed revision
because the same files change; this is a sequencing dependency, not a spec gap.
Required owner decisions and recorded outcome: OD-1, OD-2, OD-3, OD-7, OD-9, OD-11,
OD-13 recorded (Phuc, 22 October 2026). Kok Seng review of OD-1/OD-2 is a merge gate.
Recheck evidence: re-read revised FRONTEND-DESIGN; each ID has a source paragraph,
scenario row and planned test; only existing endpoints/statuses are used; compared
frozen test oracles: none conflict with FE4 refinements (the existing restore and
Login 503 cases keep their meaning; the cross-midnight case is extended, not changed).

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| FE4-T1 | FE1-RESTORE-UNREACHABLE S1-S3 | auth.test.tsx new cases | Not written |
| FE4-T1 | FE1-LOGIN-STALE S1-S3 | auth.test.tsx new cases | Not written |
| FE4-T1 | FE1-LOGOUT-ENDED | auth.test.tsx new cases | Not written |
| FE4-T2 | FE2-UNKNOWN-MODULE S1-S3 | student.test.tsx new cases | Not written |
| FE4-T2 | FE2-MODULES-DEGRADED S1-S4 | student.test.tsx new cases | Not written |
| FE4-T2 | FE2-BROWSE-MIDNIGHT | student.test.tsx extension (and optional browser assertion) | Not written |
| FE4-T3 | FE-UI-CONTRAST | e2e contrast spec | Not written |
| Human | FE-HV-TEXTSIZE | Phuc, laptop and phone | Pending |

## Verification evidence
Command, working directory, revision, exit code, result and relevant output: same
stage commands as stu-fe3-review-fixes "Verification evidence" (cwd
D:/cs3227/CS3227-2610-MP3, HEAD e277e583f1319320ed5f44e1944ac85296bb7d15): manifest-r2
recomputation exit 0 "entries 40 mismatches 0"; sha256sum of inputs/outputs exit 0;
`git diff --check -- workflow ':!logs'` exit 0.
Pre-implementation expected failure (or existing passing baseline): not run (spec stage).
Final focused tests / integration tests / build: not run.
Manual and real-provider checks: FE-HV-TEXTSIZE pending (Phuc).

## Independent review
Reviewer role / actual model / reviewed revision: pending.
Verdict and evidence: pending.
Findings: pending.
Fixes and re-review results: pending.

## Human verification
Person, date, checked journey, outcome: pending (Phuc; FE-HV-TEXTSIZE within OD-15).
Remaining limitations and follow-ups: OD-6 deferred; FE-R4 undecided; Kok Seng and
Huanghao merge reviews (OD-16).
Next agent, exact task and required inputs: fresh test writer after FE3 review;
FE4-T1..T3; this record, FRONTEND-DESIGN 895AFB4C..., FE3 reviewed manifest, NOTE.md,
HANDOFF.md, agents/test-writer.md.

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
Change ID / task / exact requested next action: stu-fe4-followups / FE4-T1, FE4-T2, FE4-T3. After FE3 review, verify the reviewed code manifest and refresh changed source hashes; write the seven IDs with red or existing passing baseline evidence.
Stage status / supersedes: READY_FOR_TESTS for specification consistency; refreshes analyst-1 and its hash-supersession note for the current worktree. Earlier findings and acceptance mappings remain preserved.
Goal and non-goals: deliver this existing capability as part of the user's current request "Implement stu-fe5". Necessary prerequisites are FE3 -> FE4 -> FE5; each keeps its own record, tests, implementation and independent review. Preserve each original non-goal and owner boundary; no backend, Auth policy or role-workspace feature change.
Scope check: standard path; these are independently testable behavior changes, split across the existing three records in dependency-safe order. No shorter-path claim.
Acceptance IDs: FE1-RESTORE-UNREACHABLE, FE1-LOGIN-STALE, FE1-LOGOUT-ENDED, FE2-UNKNOWN-MODULE, FE2-MODULES-DEGRADED, FE2-BROWSE-MIDNIGHT, FE-UI-CONTRAST. Given/When/Then scenarios remain normative in FRONTEND-DESIGN.md (Revision 2 Follow-ups scenarios; Transport restore/stale-login/ended-logout and independent modules/slots rules); no scenario changed by this refresh.

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
Prerequisites / dependency state and evidence: FE3 independent review is pending. Execution remains gated until its reviewed manifest is recorded and verified; this pre-FE3 manifest is not the eventual FE4 execution baseline.
Recorded decisions and authorization: current user implementation request authorizes progressing necessary existing prerequisites; FRONTEND-DESIGN records OD-1..OD-17 and TH-D1..TH-D3. Existing literal defaults remain in effect, including exactly "Dark theme", placement on every banner and default FE4-before-FE5 order. No new material policy or design decision is required. Affected owners retain merge review; Phuc retains human acceptance.

### Requirement-to-technical coverage refresh
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| All IDs above (each existing individual mapping retained in analyst-1) | FRONTEND-DESIGN Revision 2 Follow-ups scenarios; Transport restore/stale-login/ended-logout and independent modules/slots rules; Student/shared UX-01 | Student Technical frontend pointer and endpoint contracts; Auth Technical HTTP contract; Integration section 2; SH-AUTH-01..05 and SH-WEB-01..03 | COVERED | RTL restore retry, stale anonymous login, ended logout, unknown-module normalization and stale reads, degraded modules, cross-midnight regression; browser text/non-text/focus contrast. |

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

## Test writer receipt for analyst-2 and reviewed FE3 (22 October 2026)
Receiver: test writer / Codex / GPT-6, fresh session. Receipt: ACCEPTED.
Derived the seven IDs from current FRONTEND-DESIGN Revision 2 follow-ups, read
NOTE/HANDOFF/test-writer role and actual harness/source/tests. Canonical design
DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089 and PLAN
919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE match analyst-2.
FE3 reviewer-1 PASS is present. Recomputed all 55 implementer-manifest entries:
zero mismatches. FE3 manifest ADC5BCFDD404A5B246B6C6D4E2FA72C1758F7D3494359FEC4056B5694AA6D35B;
reviewer-verification E91B4E57907E619D6D70C1445D7FAFA506E51F8E0EC7689F709D66179452F0F8;
FE3 record E6E69BCBB6CB6E251287B8E4B51B5B2ED9572E836B855AA40BE38DD099B26509.
This reviewed FE3 worktree supersedes analyst-2's pre-FE3 execution baseline.
No frontend dependency or executable harness prerequisite is missing.

## Handoff test-writer-1 (22 October 2026)
Producer: test writer / Codex / GPT-6; fresh session, no inherited conversation.
Recipient: fresh implementer using agents/implementer.md.
Change / next task: stu-fe4-followups / implement FE4-T1, FE4-T2 and FE4-T3;
preserve all existing tests and these behavioral oracles. Stage: READY_FOR_IMPLEMENTATION.
Supersedes the earlier pending test mapping and receiver receipt only.
Goal/non-goals: seven existing FE4 IDs only; no FE5/theme behavior, product source,
backend, API, policy, owner boundary, dependency, staging or commit changes here.
Base: e277e583f1319320ed5f44e1944ac85296bb7d15 plus reviewed FE3 worktree above.
Authorization: current user request "Implement stu-fe5" includes existing ordered
FE3 -> FE4 -> FE5 prerequisites; original OD-1..17 decisions remain unchanged.
Allowed implementer scope: analyst-1's non-test frontend source/styles; own evidence
and record. Shared merge review and Phuc's human acceptance remain required.

### Inputs and outputs
| Artifact | Direction | Revision / purpose |
| --- | --- | --- |
| analyst-2 above; FRONTEND-DESIGN / PLAN | Input | Exact canonical hashes in receipt; seven normative scenarios |
| FE3 reviewer-1 / implementer-manifest.json | Input | Exact reviewed revisions in receipt; 55/55 match |
| src/frontend/src/test/fe4.test.tsx | Output | 36 executable behavioral cases; hashes in test-writer-manifest.json |
| src/frontend/e2e/contrast.spec.ts | Output | Nine screen/viewport cases, default/text/control/current/focus states |
| src/frontend/e2e/contrast-audit.ts | Output | Reusable computed-style text, non-text and keyboard focus ratios; alpha compositing; no dependency |
| evidence/test-writer-unit.json; test-writer-unit.txt | Output | Full sanitized Vitest assertions and command output |
| evidence/test-writer-build.txt; test-writer-browser.txt | Output | Final build and full browser output |
| evidence/test-writer-manifest.json | Output | Exact current source/config/spec/test/evidence hashes; excludes this record and itself |

Evidence paths are relative to workflow/student/changes/stu-fe4-followups/.
Actual stage delta is the three new test/helper files, own record/evidence and
append-only logs/phuc.md. Existing tests/product source/specs preserved. Complete
existing worktree is inspectable with `git diff HEAD -- src/frontend workflow`
and `git ls-files --others --exclude-standard -- src/frontend workflow`.
No record self-hash or committed result is claimed.

### Acceptance-to-test mapping and baseline
Unit cases below are in src/frontend/src/test/fe4.test.tsx.
| Task / ID | Case coverage | Baseline |
| --- | --- | --- |
| T1 / FE1-RESTORE-UNREACHABLE S1 | 503, network, malformed, 500, unexpected 403; exact safe alert, same URL, no Login/private workspace, Try again | 5 red: old session-check copy |
| T1 / FE1-RESTORE-UNREACHABLE S2 | Explicit in-place retry to Student/Tutor/Admin, 401, inactive, repeated failure; second /me and no document replacement | 6 red: current button is Retry, not Try again; downstream retry assertions execute after closure |
| T1 / FE1-RESTORE-UNREACHABLE S3 | Initial 401 silent anonymous Login; inactive distinct feedback | 2 passing |
| T1 / FE1-LOGIN-STALE S1-S3 | Preflight 401/inactive and POST CSRF_INVALID; exact copy, cleared password, zero/one initial POST, no /me, explicit fresh-token next submission, no automatic replay | 3 red: generic temporarily-unavailable copy |
| T1 / FE1-LOGOUT-ENDED | Preflight and POST x 401/inactive; no warning/retry, clear loaded private data, zero/one logout POST, no /me repeat, subsequent fresh-token Login | 4 red: unconfirmed sign-out warning |
| T2 / FE2-UNKNOWN-MODULE S1 | Unknown UUID, replace URL preserving valid date/week, next unfiltered slots, All modules, notice persists until user action, reject late removed-module result | 1 red: unknown module stays in URL |
| T2 / FE2-UNKNOWN-MODULE S2 | Listed module remains selected and forwarded without warning | 1 passing |
| T2 / FE2-UNKNOWN-MODULE S3; FE2-MODULES-DEGRADED S1-S3 | 503/network/malformed/500 x active/no module filter; slots/Book usable, disabled labelled Module, non-alert unavailable status, no generic Retry; keep active id without misleading All modules; Refresh restores | 8 red: merged module error currently hides successful slots |
| T2 / FE2-MODULES-DEGRADED S4 | Modules 401/inactive/FORBIDDEN and slots 503 retain session/denial/read-error behavior | 4 passing |
| T2 / FE2-BROWSE-MIDNIGHT | 320/1440 window widths, one 23:30-00:30 slot, next-day 26 Oct and 00:30 SGT visible | 2 passing; regression baseline |
| T3 / FE-UI-CONTRAST | Login; slots 1440/320; My bookings; confirmation; Tutor; Admin; session-check failure; not-found. Every direct rendered text node/native field, 4.5:1 or large 3:1; control boundary/current indicator/focus 3:1. Full Tab cycle rechecks focused text, including skip link; dialog scoped to modal. | 8 red (contrast), not-found passing; all 7 old browser cases pass |

Computed contrast uses rendered styles and alpha composition through ancestors,
including the modal backdrop; disabled controls/decorations are exempt. Filled
controls may establish boundaries by fill contrast. Focus accepts a sufficiently
contrasting outline or box shadow. Reduced-motion media avoids transient animation
colors. Native browser popup contents and real-device text size are human checks.
The helper is exported independently so FE5 can reuse it for both themes.

### Verification evidence
Cwd: D:/cs3227/CS3227-2610-MP3/src/frontend; installed locked Node/npm and Edge,
synthetic HTTP fixtures only, no backend/database/provider/credentials required.
- `npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/test-writer-unit.json`: exit 1; **139 total, 112 passed, 27 failed**. All 103 retained tests pass; FE4 has 9 passing/27 genuine behavior failures, no import/discovery/setup failure.
- `npm.cmd run build`: exit 0; TypeScript/Vite pass, 27 modules; final build output recorded.
- `npm.cmd run test:browser`: exit 1; **16 total, 8 passed, 8 failed**. All seven retained browser cases plus not-found contrast pass. Eight contrast cases report actual low-contrast boundaries/selected indicators/focus (for example Retry keyboard focus approximately 1.01:1). Text and focus checks execute, with full failure details in browser output.
- `git diff --check -- src/frontend`: exit 0; line-ending notices only.
Final checks above rerun after helper extraction and test cleanup. Initial root-cwd
npm redirection failed before execution and is not red evidence. Windows sandbox
account subsequently returned CreateProcessWithLogonW 1909; auto-review approved
same-scope escalated reads/writes/verification, which completed successfully.

Remaining checks: implementer runs full unit/build/browser suite on final product
revision and supplies exact manifest/diff to fresh reviewer. Phuc laptop/phone,
keyboard, 200% zoom and text-size journey remains pending (FE-HV-TEXTSIZE/OD-15).
No real-backend integration, independent FE4 review or human acceptance is claimed.
No unresolved test-harness or specification blocker; READY_FOR_IMPLEMENTATION
means executable pre-implementation evidence, not working FE4 software.

### Receiver receipt for test-writer-1
Receiver: pending fresh implementer. Checked revisions/evidence: pending.
Receipt: pending (ACCEPTED | RETURNED).

## Implementer receipt for test-writer-1 (22 October 2026)
Receiver: implementer / Codex / GPT-6, fresh session. Receipt: ACCEPTED.
Recomputed test-writer-manifest.json: 64 entries, zero mismatches; canonical
FRONTEND-DESIGN/PLAN revisions match analyst-2. Inspected actual FE4 tests and
baseline command outputs (139 unit: 112 pass/27 behavior failures; 16 browser:
8 pass/8 contrast failures), FE3 reviewer-1 PASS and reviewed dependency receipt.
FE4-T1..T3 only, non-test frontend implementation; no backend/API/spec changes.
Approved existing design satisfies brainstorming entry; UI engineering, systematic
debugging and verification skills applied. Root causes: restore retries reload the
page; anonymous preflight discards status; logout treats ended sessions as failure;
module failures suppress slots; successful module lists never validate URL IDs;
Bootstrap focus defaults override outline and control boundaries are too pale.

## Handoff implementer-1 (22 October 2026)
Producer: implementer / Codex / GPT-6; fresh session. Recipient: fresh independent
reviewer using agents/reviewer-security.md. Status: IN_REVIEW (supersedes earlier
execution status only). Task: FE4-T1..T3, all seven acceptance IDs; no FE5 work.
Baseline: e277e583f1319320ed5f44e1944ac85296bb7d15 plus reviewed FE3 and
FE4 test-writer-1 worktree. No staging/commits, test/dependency/backend/spec edits.
Current-request authorization and owner merge gates remain analyst-2's.

### Inputs and outputs
- Input: analyst-2 and test-writer-1 above; receipt verifies all 64 incoming hashes.
- Output: evidence/implementer-manifest.json, 71 entries, SHA-256 3D0862A5ABB60DB6F4536D1EAE359325BC91F418F7F9BEF256D369EFCF346FE7.
  Includes exact current source, retained tests/config/canonical inputs, command
  outputs and screenshots; excludes this record/self to avoid circular hashes.
- Output: evidence/implementer-complete-frontend.patch, SHA-256 2F88801C39680AD3834F2213D2C6E99D6CF5BDEB0BFB054D5B3A1C54436A4EC8.
  Complete HEAD-to-worktree frontend patch, including tracked and untracked files;
  includes prior prerequisite changes as explicitly identified by baseline. Only
  five incoming-manifest files changed in this implementation: App.tsx, Login.tsx,
  api.ts, student/StudentWorkspace.tsx and styles.css.

### Acceptance and implementation mapping
| IDs | Changed behavior / coverage |
| --- | --- |
| FE1-RESTORE-UNREACHABLE | App restoreAttempt performs in-place /me retry; exact failure alert, silent 401 and inactive handling, no protected flash; fe4.test.tsx S1-S3 |
| FE1-LOGIN-STALE, FE1-LOGOUT-ENDED | api.ts preserves anonymous preflight session statuses and clears token; Login exact retry copy and existing password clearing; App recognizes ended logout without warning; fe4.test.tsx named stale-login/ended-logout cases |
| FE2-UNKNOWN-MODULE | StudentWorkspace validates only successfully loaded module lists, replaces URL preserving week/date, notice retained until next user change; existing useRead identity rejects removed-module completion |
| FE2-MODULES-DEGRADED | Separate results error from module availability; disabled labelled filter and non-alert status; active unknown selection remains labelled; Refresh recovery; denials preserve existing handling |
| FE2-BROWSE-MIDNIGHT | Existing renderer retained; both regression cases pass |
| FE-UI-CONTRAST | Semantic control-line token, stronger boundaries/current indicators, orange focus outline overriding Bootstrap shadows; native date calendar requires :focus-within (measured activeElement input with :focus=false/:focus-visible=false/:focus-within=true). Unchanged nine contrast cases pass |

### Verification evidence
Cwd src/frontend; final source revision is manifest above; synthetic HTTP fixtures,
locked existing dependencies, installed Edge, no backend/database/provider. Scoped
escalated tools used for Windows sandbox account 1909 issue; no rejection occurred.
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/implementer-unit.json: exit 0, 139/139 pass. Output implementer-unit.txt/json.
- npm.cmd run build: exit 0, TypeScript + Vite, 27 modules; implementer-build.txt.
- npm.cmd run test:browser: exit 0, 16/16 pass; implementer-browser.txt. Includes
  responsive 320/768/1024/1440, full keyboard/focus cycle and text/control/current
  contrast screens. No test oracle changed.
- git diff --check -- src/frontend: exit 0, only existing CRLF notices.
- Visually inspected implementer-slots-320.png and implementer-slots-1440.png:
  compact white/orange hierarchy preserved, stronger controls, wrapped content,
  seven-day timetable and mobile date groups visible without clipping.
Root-cause investigation retained test intent; no unresolved test/spec blocker.
Human laptop/phone, native date popup, 200% zoom and FE-HV-TEXTSIZE remain pending;
no actual-backend integration or independent review/human acceptance claimed.

Next: fresh reviewer validates complete patch/current manifest and reruns required
checks, records independent verdict. FE5 execution waits for reviewed FE4 revision.

## Handoff reviewer-1 (22 October 2026)
Producer: independent reviewer/security checker / Codex / GPT-6, fresh session.
Scope: standard FE4-T1..T3 prerequisite of the authorized stu-fe5 request.
Status: CHANGES_REQUIRED; supersedes implementer-1 IN_REVIEW execution status.
Receiver receipt: ACCEPTED for review inputs. Derived the seven-ID checklist from
canonical FRONTEND-DESIGN before upstream conclusions, inspected source/tests/diff,
then checked analyst-2 readiness and SA4-01..05 resolutions, test-writer baseline,
FE3 reviewer-1 PASS/dependency manifest and implementer-1 packet. No spec gap found.

Reviewed base e277e583f1319320ed5f44e1944ac85296bb7d15 plus uncommitted manifest:
- implementer-manifest.json: 3D0862A5ABB60DB6F4536D1EAE359325BC91F418F7F9BEF256D369EFCF346FE7; 71/71 hashes match.
- implementer-complete-frontend.patch: 2F88801C39680AD3834F2213D2C6E99D6CF5BDEB0BFB054D5B3A1C54436A4EC8.
- FRONTEND-DESIGN: DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089; PLAN: 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE.
- StudentWorkspace.tsx: 2091E876D80E9B0607AC48579C4BDF46378CB8836CF30D0DA479EA6F87AE6C11.
- Exact per-source/test/evidence hashes: evidence/reviewer-verification.json,
  SHA-256 D0CEC33B522EAFB5FB00D2013CFEAB83B41A88C52088BB03D23EFCB6F3FA0F00. Only five product files differ from test-writer baseline;
  tests/config/dependencies remain unchanged.

### Finding FE4-R1 / medium / blocks acceptance
Location: src/frontend/src/student/StudentWorkspace.tsx:71-72.
Acceptance: FE2-MODULES-DEGRADED S4 and FE1-DENIAL.
Trigger: authenticated Student; GET /modules returns 403 FORBIDDEN while GET /slots
returns 503 or a malformed list. Root cause: data.error wins the readError merge,
so denied checks never see modules.error. The page displays generic unavailable
with Retry and keeps filter controls instead of Access denied / Back to home and
the existing denied state. Successful slots with the same modules FORBIDDEN correctly
show denial; the combination loses that required state. No server authorization
bypass or private disclosure was demonstrated.
Evidence: evidence/reviewer-merged-error-probe.mjs and .txt; real Edge against
synthetic routes, successful-slots control plus both failing-slots variants.
Reproduce from repository root: node workflow/student/changes/stu-fe4-followups/evidence/reviewer-merged-error-probe.mjs.
Routing: fresh test writer adds combined-error negative cases (including read
completion order), then fresh implementer. Proposed correction: determine denial
from all relevant errors before selecting display error; a generic slots failure
must not mask modules FORBIDDEN. Closure: combined-error cases consistently show
the existing denied message/home action, no read Retry/filter results; isolated
slots failures still offer read Retry and ordinary module outages still degrade.
Fresh independent re-review required before FE5 execution.

### Acceptance and security assessment
FE1-RESTORE-UNREACHABLE, FE1-LOGIN-STALE and FE1-LOGOUT-ENDED are supported by
actual status handling, token clearing, no automatic replay and passing cases.
FE2-UNKNOWN-MODULE normalizes only successful lists and useRead identities reject
late removed-module responses; degraded filters retain active selections and
recover on Refresh. FE2-BROWSE-MIDNIGHT retains start-day grouping and next-day
end date/time. FE-UI-CONTRAST's computed RGB/alpha composition, current indicators,
control boundaries and full keyboard-cycle checks are valid for present static
styles; all nine required screen cases pass. Disabled/decorative/native popup
exemptions are explicit. No changed dependency, backend, SQL, AI or shared policy.
Session reset generations, in-memory credentials/CSRF, same-origin requests and
React text rendering remain intact. FE4-R1 is the sole blocking finding.

### Fresh verification
Cwd src/frontend; existing locked harness and Edge, synthetic fixtures only:
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/reviewer-unit.json: exit 1, 137/139; one 5s timeout and subsequent incomplete-typing failure during concurrent browser load.
- Same full command alone, output reviewer-unit-isolated.json: exit 0, 139/139; no source/test changes between runs. Initial timing evidence retained honestly.
- npm.cmd run build: exit 0; TypeScript/Vite, 27 modules; reviewer-build.txt.
- npm.cmd run test:browser: exit 0, 16/16; reviewer-browser.txt.
- git diff --check -- src/frontend workflow/student/changes/stu-fe4-followups.md: exit 0, existing CRLF notices only.
- Additional merged-error Edge probe: exit 0; observable acceptance failure above.

Verdict: CHANGES_REQUIRED. Existing suites pass on isolated rerun but miss the
combined-denial case. No implementation/test/spec changes, staging or commit by
this reviewer. Only own evidence, this review section and append-only human log
written. Human laptop/phone, keyboard/200% zoom/text-size, native date popup and
shared-owner merge reviews remain pending; no whole-app audit or completion claim.

## Test-writer-2 receiver receipt (22 October 2026)
Receiver: test writer / Codex / GPT-6, fresh session; FE4-R1 only.
Receipt: ACCEPTED. Standard review-return path, preserving team ownership.
Read canonical FRONTEND-DESIGN FE2-MODULES-DEGRADED S4 and FE1-DENIAL first;
checked analyst-2 READY_FOR_TESTS/coverage and reviewer-1 CHANGES_REQUIRED,
actual probe source/output and StudentWorkspace.tsx. Recomputed incoming
implementer manifest: 71 entries, zero mismatches. FRONTEND-DESIGN
DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089,
PLAN 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE
and StudentWorkspace 2091E876D80E9B0607AC48579C4BDF46378CB8836CF30D0DA479EA6F87AE6C11
match reviewer-1. No changed contract or unresolved spec gap; existing FE3
reviewed dependency remains unchanged. Scope is additive regression tests,
this record/evidence and append-only human log; no product edits.

## Handoff test-writer-2 (22 October 2026)
Producer: test writer / Codex / GPT-6, fresh session. Recipient: fresh implementer.
Task: FE4-R1 only; FE2-MODULES-DEGRADED S4 and FE1-DENIAL.
Status: READY_FOR_IMPLEMENTATION for this finding; supersedes reviewer-1's
routing status only. FE4 acceptance still requires implementation and fresh review.
Inputs: analyst-2 READY_FOR_TESTS and reviewer-1 finding/probe, verified in receipt.
Base: e277e583f1319320ed5f44e1944ac85296bb7d15 plus incoming reviewed worktree;
no staging or commit. No product, canonical spec, dependencies or old test changes.

### Artifacts and revisions
- src/frontend/src/test/fe4.test.tsx SHA-256 E8E365AA7CA0C19EBA1935DD988061A371A2035DC841FDC61C57E0DC126F9090.
- evidence/test-writer-2-manifest.json SHA-256 C19CA11E32FBF50C39445469DC51340A025E2B824FC6BD68855BB9E1AC0B63AF;
  75 current per-file source/test/config/spec/evidence hashes. Recomputed incoming
  71-entry manifest after edits: only fe4.test.tsx differs. Record/log excluded.
- evidence/test-writer-2-unit.json and .txt: executable baseline output, hashed in
  manifest. Existing probe .mjs/.txt included as input artifacts.
Complete current frontend diff: git diff HEAD -- src/frontend plus untracked files
from git ls-files --others --exclude-standard -- src/frontend. Relative to incoming
implementer manifest only the four additive tests change; all prior cases retained.

### Acceptance mapping and baseline
| Acceptance / finding | Test case | Observed baseline |
| --- | --- | --- |
| FE2-MODULES-DEGRADED S4, FE1-DENIAL / FE4-R1 | Four FE4-R1 cases: modules 403 FORBIDDEN plus slots 503/malformed, each with modules-first and slots-first completion | All four genuine behavior failures |

Deferred responses deterministically control both completion orders. Modules-first
asserts denial before and after slots completion; slots-first confirms ordinary
read Retry before modules denial. Final checks require Access denied, Back to home,
no Retry/filter/results/Book, signed-in shell, no Login, one /me and zero POSTs.
Actual final failure in all four: generic unavailable alert, missing home link,
Retry/filter/results remain. Existing 36 FE4 cases pass, including isolated slots
read error, ordinary degraded modules and session denial controls.

Verification: cwd src/frontend, existing locked synthetic HTTP/jsdom harness;
command npm.cmd test -- src/test/fe4.test.tsx --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/test-writer-2-unit.json
(exit 1; 40 total, 36 passed, 4 failed, zero skipped). PowerShell output redirection
in .txt includes npm stderr notice formatting; actual executable report proves
behavior assertions, no harness/import/discovery failure. git diff --check --
src/frontend/src/test/fe4.test.tsx workflow/student/changes/stu-fe4-followups.md:
exit 0, existing CRLF notice only. No escalation needed in this session.
Full suite/build/browser not rerun for this focused test-only stage; previous
reviewer results remain historical evidence on unchanged product/config sources.
No fix or green result claimed. Human checks/owner merge reviews remain pending.

Next: fresh implementer verifies this manifest and spec revisions, fixes error
precedence so every relevant FORBIDDEN retains denied content/copy/home action,
preserves ordinary degraded/read-error handling and all test oracles; run focused
then affected full unit/build/browser checks and hand off to fresh reviewer.
Allowed writes remain non-test frontend product scope in analyst packet, own
record/evidence and append-only log. No new requirement or owner decision.

Receiver receipt: pending fresh implementer (ACCEPTED | RETURNED after hashes/checks).

## Implementer-2 receiver receipt (22 October 2026)
Receiver: implementer / Codex / GPT-6, fresh session. Receipt: ACCEPTED.
Scope: standard review-return path for FE4-R1 only, FE2-MODULES-DEGRADED S4
and FE1-DENIAL. Canonical scenarios require denied content to clear while the
signed-in shell remains. Read analyst-2 readiness and SA4-01..05 resolutions,
FE3 reviewer PASS dependency, reviewer-1 probe and test-writer-2 source/report.
Recomputed incoming test-writer-2 manifest: 75 entries, zero mismatches; current
spec/test/source hashes match its packet. Its JSON proves 40 tests, 36 passed,
four intended combined-denial failures in both response completion orders.
Current HEAD is 7e79a74fb05c838fcce447105f81257163cbac7b (upstream packet's
historical base e277e583f1319320ed5f44e1944ac85296bb7d15); artifact contents
match despite HEAD movement. Preserve external work; no reset/stage/commit.
Root cause confirmed directly: readError selects data.error before modules.error;
denied then inspects that lossy merge, so a generic slot failure masks FORBIDDEN.
Working control in actual browser probe shows modules FORBIDDEN plus good slots
already produces denied content. Fix selects denial across original relevant
errors before choosing visible feedback; ordinary error/degraded behavior stays.
Allowed writes: StudentWorkspace product fix, own evidence/record and append-only
human log; no tests/spec/contracts/dependencies. Existing canonical design governs;
debugging, UI engineering and verification skills apply to this correction.

## Handoff implementer-2 (22 October 2026)
Producer: implementer / Codex / GPT-6, fresh session. Recipient: fresh independent
reviewer using agents/reviewer-security.md. Task: FE4-R1 only. Status: IN_REVIEW;
supersedes test-writer-2 execution status, no independent acceptance claimed.
Goal: FE2-MODULES-DEGRADED S4 / FE1-DENIAL retains denied content/home action
when modules FORBIDDEN and slots 503/malformed finish in either order.
Non-goals: tests, contracts, dependencies, other features or FE5 implementation.

### Inputs, exact revisions and diff
- Accepted analyst-2 readiness, reviewer-1 finding and test-writer-2 packet in the
  receipt above; all 75 incoming hashes matched before editing. Previous packet
  contract/owner/dependency evidence remains unchanged, including FE3 review PASS.
- Actual HEAD: 7e79a74fb05c838fcce447105f81257163cbac7b; historical upstream
  base e277e583f1319320ed5f44e1944ac85296bb7d15 remains traceable above.
- StudentWorkspace.tsx SHA-256: B48A78DDD251DA0DB7B03531EC6602087BBC34868B6F2284030E91C9D6A78B9B.
  Only this file differs from the 75-entry incoming manifest; no tests changed.
- evidence/implementer-2-manifest.json: 84 current per-artifact hashes,
  SHA-256 B24E7CD0AFBBB50713F845C669AA82E911DD5BF9FF5A49AFC5ADA0C13047B6E0.
  Excludes itself, this appended record and human log; includes canonical inputs,
  retained tests/config, exact source and sanitized command evidence.
- evidence/implementer-2-scoped.patch: exact incoming-to-fixed source delta,
  SHA-256 A4A68F9398B35CA201B885561E465BB77DFA3F29ED02B1B97467FAD36E4F4434;
  three insertions/two deletions. Before-image retained alongside it.
- evidence/implementer-2-complete-frontend.patch: actual HEAD-to-worktree frontend
  diff plus all untracked frontend source/tests, SHA-256
  22257586274AFB2C6186900E8967BC516FC319D41581C6E4486BE40D08AD01EA.
  Includes prerequisite changes, explicitly distinguished by the scoped patch.
- FRONTEND-DESIGN DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089;
  PLAN 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE;
  fe4.test.tsx E8E365AA7CA0C19EBA1935DD988061A371A2035DC841FDC61C57E0DC126F9090 unchanged.

### Correction and fresh verification
StudentWorkspace derives deniedError from original data/modules/mutation errors,
then uses that error for denied copy and existing home/content branch. Generic
read-error precedence can no longer conceal FORBIDDEN; ordinary module outages,
slot Retry and signed-in session behavior retain their existing paths.

Cwd src/frontend, installed locked dependencies and Edge, synthetic HTTP fixtures:
- npm.cmd test -- src/test/fe4.test.tsx --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/implementer-2-focused.json:
  exit 0, 40/40; four added cases go green against unchanged test-writer-2 red.
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/implementer-2-unit.json:
  exit 0, 143/143. Text evidence: implementer-2-unit.txt.
- npm.cmd run build: exit 0, TypeScript/Vite, 27 modules; implementer-2-build.txt.
- npm.cmd run test:browser: exit 0, 16/16; implementer-2-browser.txt. Includes
  keyboard/focus/contrast and responsive 320/768/1024/1440 checks.
Full unit/build/browser commands ran sequentially, avoiding concurrent-load timing.
Focused run preceded restoration of the original UTF-8 BOM; all full checks ran
against the final byte revision above. Initial accidental root-cwd focused command
failed before test execution; rerun from documented cwd is the valid focused result.
Sandbox Windows 1909 occurred during evidence packaging; same-scope escalated
artifact reads/writes were approved. No automatic approval rejection occurred.
No staging/commits, actual backend/provider integration or human verification.
Human phone/laptop, native date popup, 200% zoom/text size and owner merge reviews
remain pending as recorded previously. No new design/policy decision needed.

Next: fresh reviewer verifies 84-entry manifest and both scoped/complete diffs,
reruns relevant checks and closes FE4-R1 independently before FE5 execution.
Receiver receipt: pending fresh reviewer (ACCEPTED | RETURNED).

## Handoff reviewer-2 (22 October 2026)
Producer: independent reviewer/security checker / Codex / GPT-6, fresh session.
Task: FE4-R1 narrow re-review; standard review-return path, preserving ownership.
Verdict: PASS. Status: READY_FOR_HUMAN; supersedes reviewer-1 CHANGES_REQUIRED
for the corrected revision only. No new blocking finding.
Receiver receipt: ACCEPTED. Read NOTE/HANDOFF/reviewer prompt, canonical denial
and seven FE4 scenarios, actual correction and four additive regression tests.
Verified all 84 implementer-2 manifest entries before and after fresh checks.
Only StudentWorkspace differs from test-writer-2's 75-entry input; relative to
reviewer-1's 71-entry reviewed baseline only StudentWorkspace and fe4.test.tsx
changed. Thus unchanged canonical readiness (SA4-01..05), FE3 dependency, other
FE4 acceptance/security conclusions remain applicable; no contract drift found.

### Exact reviewed revisions
HEAD 7e79a74fb05c838fcce447105f81257163cbac7b plus uncommitted worktree.
- implementer-2-manifest.json B24E7CD0AFBBB50713F845C669AA82E911DD5BF9FF5A49AFC5ADA0C13047B6E0 (84/84 match).
- Scoped patch A4A68F9398B35CA201B885561E465BB77DFA3F29ED02B1B97467FAD36E4F4434;
  complete frontend patch 22257586274AFB2C6186900E8967BC516FC319D41581C6E4486BE40D08AD01EA.
- StudentWorkspace B48A78DDD251DA0DB7B03531EC6602087BBC34868B6F2284030E91C9D6A78B9B;
  fe4.test.tsx E8E365AA7CA0C19EBA1935DD988061A371A2035DC841FDC61C57E0DC126F9090.
- FRONTEND-DESIGN DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089;
  PLAN 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE.
- New evidence/reviewer-2-verification.json SHA-256
  7C5CB80593F8461681CFF435648B7502CD3EA72D09EDB3776B5A582AD874360A:
  exact checked artifact/output hashes; record/log excluded from self-reference.

### FE4-R1 closure and retained checklist
StudentWorkspace:72 and :79 now derive FORBIDDEN from all original read/mutation
errors and select it for safe denial feedback. Generic slot errors cannot mask
the modules denial. Existing denied content branch hides filters/results, keeps
the signed-in shell and home action; no API, credential, mutation or rendering
trust boundary changed. Ordinary module outages and isolated slot Retry preserve
their original behavior. Four independent tests use deferred responses to check
modules-first and slots-first with slots 503/malformed, denial before/after the
second completion, no read Retry/filter/results/Book, one /me, zero POSTs.
Test-writer-2 JSON independently confirms genuine red baseline 36/40 with the
four combined cases failing; unchanged final tests now pass.
Original Edge probe also now shows Access denied / Back to home, one home link,
zero Retry and zero module controls for all three scenarios (successful slots
control, unavailable slots, malformed slots). FE4-R1 is CLOSED.
Retained seven-ID checklist: restore retry/stale anonymous login/ended logout,
unknown-module normalization and stale reads, degraded modules, midnight rendering,
and contrast/keyboard/responsive coverage have unchanged code/tests except this
reviewed denial delta; full suites below rerun them. No new dependency/config,
backend/SQL/AI or shared-policy change. Prior human/owner follow-ups remain open.

### Fresh sequential verification
Cwd src/frontend; installed locked harness, Edge and synthetic HTTP fixtures only:
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe4-followups/evidence/reviewer-2-unit.json: exit 0, 143/143, zero skipped; reviewer-2-unit.txt/json.
- npm.cmd run build: exit 0, TypeScript/Vite, 27 modules; reviewer-2-build.txt.
- npm.cmd run test:browser: exit 0, 16/16, including nine contrast/keyboard screens
  and 320/768/1024/1440 responsive checks; reviewer-2-browser.txt.
- Repository cwd: node workflow/student/changes/stu-fe4-followups/evidence/reviewer-merged-error-probe.mjs: exit 0; reviewer-2-merged-error-probe.txt contains closure observations above.
- git diff --check -- src/frontend workflow/student/changes/stu-fe4-followups.md:
  exit 0, existing CRLF notices only. Final input manifest remains 84/84 matching.
Windows sandbox account error 1909 required same-scope escalated executions;
no automatic approval rejection occurred. No source/test edits, staging or commit.

Next: FE5 may start from this independently reviewed FE4 revision; fresh FE5 test
writer must verify its dependency hashes and existing canonical FE5 packet.
Human laptop/phone, 200% zoom/text-size, native date popup and owner merge reviews
remain pending. PASS enables human verification and dependency execution, not
whole-app security, actual backend/provider integration or human acceptance.

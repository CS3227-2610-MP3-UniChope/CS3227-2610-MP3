# stu-fe5-theme-switch: Light and dark theme switch in every page header

Status: READY_FOR_TESTS (execution waits for the stu-fe3 reviewed revision and, by
default, the stu-fe4 reviewed revision; see dependencies)
Scope classification and reason: standard path. One independently testable,
user-visible capability (a remembered light/dark theme) that touches the shared
shell headers, the shared Login page, a browser-storage rule and the OD-9 contrast
requirement, so the shorter path does not apply. Not split further: the toggle,
persistence, early application and dark palette are tasks of one user outcome and
cannot be accepted separately.
Parent request / related changes / dependencies: Phuc's current-session request of
22 October 2026 to add a light and dark theme switch (decisions TH-D1..TH-D3 below).
Related: [stu-fe3-review-fixes](stu-fe3-review-fixes.md) (hard dependency),
[stu-fe4-followups](stu-fe4-followups.md) (sequencing dependency), FE1/FE2 records.
Human owner / affected owners: Phuc (owner, decisions); Kok Seng (shared Login page
header gains a control: merge review only, no Auth contract change); Huanghao
(shared shell/styles merge review). Tutor/Admin owners: placeholder header gains the
switch; no workspace content change.
Current agent role / actual model or tool: spec analyst / Claude Code (Claude Agent
SDK, fresh subagent session with no inherited conversation, transcript or log) /
Claude Opus 5.5 (claude-opus-5-5); 22 October 2026.
Stage handoff manifest / revision: Handoff analyst-1 below.
Receiver receipt: pending fresh test writer.
Base commit and current revision/diff reference: HEAD
e277e583f1319320ed5f44e1944ac85296bb7d15 (branch branch_student) plus the
uncommitted worktree. Frontend source/tests equal
`stu-fe2-booking-ui/evidence/frontend-manifest-r2.json` (36 frontend entries match;
the 4 mismatching entries are the spec files analyst-4 already superseded). No commit.
Spec paths + revision: see Inputs and outputs (SHA-256; files are uncommitted).
Acceptance IDs and Given/When/Then scenarios: TH-DEFAULT, TH-TOGGLE, TH-A11Y,
TH-PLACEMENT, TH-PERSIST, TH-STORAGE-FALLBACK, TH-NOFLASH, TH-CONTRAST, TH-SESSION.
Normative scenarios are in [FRONTEND-DESIGN.md](../FRONTEND-DESIGN.md) "Revision 3
acceptance: light and dark theme switch" (theme contract and table); they are not
duplicated here, to avoid divergence.
Allowed scope/files and dependencies: see Handoff analyst-1.
Assumptions / open decisions / owner agreement: see Handoff analyst-1 and findings.

## Owner decisions (human decision record)
Decided by Phuc (git user tieutrongphuc), 22 October 2026. Authorization reference:
Phuc's current-session answers relayed in this spec-analyst session's task request.
Recorded by the spec analyst; the analyst did not originate them.
- TH-D1: the default theme is always light on the first visit; do not follow the
  device setting.
- TH-D2: remember the chosen theme on the same browser, including on the Login page.
  Only the theme preference is stored (no personal data); this is not private data
  under SH-WEB-03.
- TH-D3: the switch is in the top bar on every page: Login, Student workspace and the
  Tutor/Admin placeholder pages.

## Handoff analyst-1
Producer role / actual tool and model: spec analyst / Claude Code (Claude Agent SDK
fresh subagent) / Claude Opus 5.5 (claude-opus-5-5).
Recipient role: fresh test writer (agents/test-writer.md), then fresh implementer,
then fresh independent reviewer (any model per OD-14).
Change ID / task / exact requested next action: stu-fe5-theme-switch / one task
FE5-T1 (all nine TH IDs). Test writer: after stu-fe3 (and by default stu-fe4) has a
reviewed revision, verify this packet's hashes and the base frontend manifest you
start from, write behavioral RTL tests and Playwright browser checks per ID, update
the frozen placeholder button-count test (SA5-03) without weakening its intent, and
record red evidence.
Stage status / supersedes: READY_FOR_TESTS; first packet; supersedes none.
Goal: a light/dark theme switch in every page banner, light by default (TH-D1),
remembered per browser including on Login (TH-D2, TH-D3), accessible, applied
before first paint where feasible, WCAG 2.1 AA in both themes (OD-9) and fully
independent of session state.
Non-goals: "system" theme or following `prefers-color-scheme`; per-account or
server-side preference; more than two themes; live cross-tab sync; dependency
changes; backend, Auth contract, API or role-workspace content changes; timetable
text size (FE-HV-TEXTSIZE); OD-6; FE-R4; any fe3/fe4 acceptance ID.
Acceptance IDs: TH-DEFAULT (TH-D1), TH-TOGGLE, TH-A11Y, TH-PLACEMENT (TH-D3, OD-12),
TH-PERSIST (TH-D2), TH-STORAGE-FALLBACK, TH-NOFLASH, TH-CONTRAST (OD-9), TH-SESSION.

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/FRONTEND-DESIGN.md (revision 3) | Output (changed by this stage) | DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089 | Theme contract, TH IDs, storage-rule exception, placeholder-header note; supersedes 895AFB4CC2594ECBAD0E69387920E60879409DAA78595EB9779205FF1B7B9C8B |
| workflow/student/PLAN.md | Output (changed: status line, FE5 row) | 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE | Task order; supersedes 008AA245CD581A837370BB2C96BA2C99A8322ED668AC727AECEEE73B15EF5766 |
| workflow/student/changes/stu-fe3-review-fixes.md | Input (dependency; this stage appends an analyst note) | 73BF17D7E9C28833139402AE99D105E03A3138A6A1D70C35C1B33D6DDF63F4FA before append | FE1-UI-LANDMARKS banners; hash supersession note |
| workflow/student/changes/stu-fe4-followups.md | Input (dependency; this stage appends an analyst note) | 2402E5AE2A6833930795532916C800820E40C73E07FBBB2C48CF6772C77AA0AD before append | FE-UI-CONTRAST harness; hash supersession note |
| workflow/student/changes/stu-fe0-foundation.md | Input (unchanged) | B6ABCFA902A4E5CC10B93FA3403FBD6BD3FD095D24B79CB015D2B23B4D4644DE | Harness, FE0 file boundary (index.html) |
| workflow/student/changes/stu-fe1-auth-ui.md | Input (unchanged) | CE0FB2B31B4F5DC7EE57BC416E56E4A552B6BC86EE3016ACA1FDC54679813B91 | OD addendum, CS-06 |
| workflow/student/changes/stu-fe2-booking-ui.md | Input (unchanged) | A0E42A3F041609731D5BC8954B6E529996B244C4ADE644E2551D1E6929F66D64 | OD-1..OD-17 (OD-9, OD-12), reviewer-3 findings |
| workflow/student/changes/stu-fe2-booking-ui/evidence/frontend-manifest-r2.json | Input | 9343DBE123F27D56100604D4932F6AD1D74D2D299A87CE6AF7D9952433B8D494 | Current reviewed frontend; 36 frontend entries recomputed, 0 mismatches |
| workflow/student/TECHNICAL-SPEC.md | Input (unchanged) | 792718A4E671F354FE508B76C6705F2BCB7F5A080398CEB669AA7E24FCE2F485 | Shell ownership (section 1), frontend pointer |
| workflow/student/PRODUCT-SPEC.md | Input (unchanged) | 750433038415D5AF0832D4E506FB669C1D08A9A583FF388C4DEFC4F9FD690B86 | Student requirements (UX-01) |
| workflow/PRODUCT-SPEC.md | Input (unchanged) | D1D5A41451102985CB977EE999A13B82D48A4506C926CA899E1C7B283F70BA7A | UX-01 |
| workflow/SHARED-TECHNICAL-SPEC.md section 6 | Input (unchanged) | 5F92FF1F89B0171232BF3FF49C8517A9743EAEF2602B27F1C5D38EAFDE9912A5 | SH-WEB-02/03 browser-storage boundary |
| workflow/student/NOTE.md | Input (unchanged) | 333031E4BA3A4DAF09DD29ABB5E1C3ADF21F675D8B8916DEB11CD5FA8882F981 | Workflow, template |
| workflow/student/HANDOFF.md | Input (unchanged) | 5594B343C530CCB81111EA4F8008356369B65CE8927108E8B4163D0D369F5CA6 | Packet format |
| workflow/student/agents/spec-analyst.md | Input (unchanged) | 975D2D5530E16A236666094FEAEB39406190F3D6718F655A256271EC40D04606 | Role prompt |
| CLAUDE.md | Input (unchanged) | 81C971F8CDAE5685E0DB2832BD159222858791AFF4EFFCAAC7E8FF4BB5C35586 | Repository instructions |
| AGENTS.md | Input (unchanged) | F4FF35DF2A3C7E72DFF36090EB4055DB070A4543C921D6A457FD40A7B9BFF4EA | Repository instructions |
| .agents/skills/frontend-ui-engineering/SKILL.md | Input (unchanged) | 71E60D0FD5A2EA702C99E12EAAF5EF64474B92C1A1132B8FFBB371B9BD367338 | Semantic tokens, WCAG 2.1 AA, keyboard |
| src/frontend/index.html | Input (current) | EFDFEF6716C05E6E591EE9B25CF79433C2551FE6CAEB852BBAA66A644E719C24 | No early script; static theme-color #ffffff |
| src/frontend/package.json | Input | 7D1443F243169FC0636318D59DCFD44B5633A1DE1848CF0C91084EAB97CB95A5 | Bootstrap 5.3.8 (colour modes available) |
| src/frontend/playwright.config.ts | Input | 8165F9FB3562F611B9E13CDCD29CCE0E4920E19F7726AFBA8867D8F9031DA300 | Dev-server browser runs (SA5-06) |
| src/frontend/vite.config.ts | Input | 842071AEBDDA1ED54A07200061C9EB2A73C3EA60E36CEFFF2F04E48BE1806BE3 | No public dir config (Vite default `public/`) |
| src/frontend/src/main.tsx | Input | BA6B140C8A74A56B5F0A0E6280E5AB36CE941568595775B5C8A1DB2C958BA684 | Bootstrap and styles imported by script |
| src/frontend/src/App.tsx | Input | 11FD37E757FC4904A0A81D7B0FD04043A2E3CA78218F8D7CA8098ADD480A4911 | Headers, restore/not-found screens, logout clearing |
| src/frontend/src/Login.tsx | Input | 272CF4BCA9D09BD21F9BC253FADEB853A5FDFB9C957EFAD50C84D8E4AB0BE005 | Login banner |
| src/frontend/src/components.tsx | Input | B23D6F0CE1907CC218D49A2106EC4BB2FCE5E72DCEC8DF5536AF30CCAF05224C | Icon set (no sun/moon yet) |
| src/frontend/src/styles.css | Input | 03707F240963675587C05D233EE57B96F79E86C13B08F9483D14EAF19F265D11 | Light-only tokens and many literal colours (SA5-05) |
| src/frontend/src/student/StudentWorkspace.tsx | Input | FE2661B8A979D430D02EA77F4F33D7624DE08940D5BE3D9A3E2F1B67CF5A9A99 | Workspace surfaces |
| src/frontend/src/student/Confirmation.tsx | Input | F69937B16CDA1BF16BC618489E556F555AF99EA885603271117BEA90A8BC144B | Dialog focus trap |
| src/frontend/src/test/auth.test.tsx | Input | 2E5DBA750D82F53A9B5C331D6E294FCFE191D62C989F74475336DE0D12E803D3 | Frozen tests: line 54 button count (SA5-03), lines 149-158 storage spy |
| src/frontend/src/test/student.test.tsx | Input | 38BFC6CCC5B312A08FE5D37D97C5EA3DF6F2B245F8D7A99341889CA0C6AC5B88 | Frozen FE2 tests |
| src/frontend/src/test/setup.ts | Input | 268E7200D092DAA3225EB06790ED72842E5D3234A53980C90FF463D7B81528D3 | No storage reset between tests yet |
| src/frontend/e2e/workspace.spec.ts | Input | 9D579B62B509162EC04B1B57336D0238DA6FAD691157AD1CC77F2D6EDC3D7711 | Responsive/overflow checks |
| src/frontend/e2e/shell.spec.ts | Input | 18F36AD0CE5881EE4C04821EED444E9FBDADFD72C0770D046E1DE5705D98A593 | Shell smoke |
| src/backend/src/main/java/unichope/platform/web/ApiSecurityConfiguration.java | Input | C97117DE37B800286448343032BA2144AB92FD34297B58A5AAC10F9E8382A0FE | No Content-Security-Policy configured (SA5-08) |

Base commit / result commit / complete diff: base e277e583f1319320ed5f44e1944ac85296bb7d15;
no result commit; spec diff is `git diff -- workflow ':!logs'` plus untracked files
(FRONTEND-DESIGN.md, this record and the earlier untracked records).
Uncommitted files and hashes: as in the table; this record has no self-hash. The
appended stu-fe3/stu-fe4 records' new hashes are in "Verification evidence".
Allowed write scope (frontend source/tests/styles only):
- Test writer: `src/frontend/src/test/**` (including `api-fixture.ts`, `setup.ts`
  storage reset), `src/frontend/e2e/**`, evidence under
  `workflow/student/changes/stu-fe5-theme-switch/evidence/`, own record section.
- Implementer: `src/frontend/src/**` non-test source (`App.tsx`, `Login.tsx`,
  `components.tsx`, `main.tsx`, `student/*.ts(x)`, `styles.css`, a new theme module),
  `src/frontend/index.html` and one new same-origin early theme script under
  `src/frontend/public/`, evidence and own record section. No new dependency unless
  stu-fe4 has not yet landed and FE-UI-CONTRAST's single optional pinned contrast dev
  dependency is needed under the same rule as stu-fe4; no Playwright/Vite/TS config
  change, no backend, no canonical spec edits (return spec problems to the analyst).
Prerequisites / dependency state and evidence:
- Hard dependency on stu-fe3 reviewed revision (pending; stu-fe3 is READY_FOR_TESTS
  with no test-writer receipt yet). Reason: stu-fe3 creates the banners on Login,
  session-check, session-check failure and not-found screens and rewrites the
  Tutor/Admin placeholder header (FE1-UI-LANDMARKS) in the same App.tsx/Login.tsx
  regions where TH-PLACEMENT puts the switch, and its S2 test is what TH-PLACEMENT
  refines. Implementing fe5 first would either build on headers fe3 replaces or force
  fe3 to re-spec around a control its decision (OD-12) did not include.
- Sequencing dependency (default) on stu-fe4 reviewed revision. Reason: stu-fe4 T3
  (FE-UI-CONTRAST) changes light colour tokens in styles.css and adds the browser
  contrast check that TH-CONTRAST extends to the dark theme; stu-fe4 also edits
  App.tsx/Login.tsx. Parallel implementation would conflict in the same files. If
  Phuc prefers fe5 before fe4 (Q-5-2), fe5 may start right after fe3's review because
  TH-CONTRAST is self-contained (it measures both themes itself); fe4 then starts
  from fe5's reviewed revision and re-runs both checks. This choice changes ordering
  only, not any scenario.
- FE0 harness PASS; FE1/FE2 reviewer-3 PASS on manifest-r2. Node v24.21.0/npm 12.0.2
  and Edge recorded by earlier stages (use `npm.cmd` on PowerShell).
Recorded decisions and authorization references: TH-D1..TH-D3, Phuc, 22 October 2026
(section "Owner decisions" above). Existing OD-9, OD-12, OD-14 (Phuc, 22 October 2026,
stu-fe2 "Owner decision addendum"). TH-D3 is a later, specific owner decision that
authorizes refining OD-12's "no other control" for the theme switch only (SA5-01).
Open questions / proposals / unverified facts: Q-5-1..Q-5-3 below (non-blocking).
Verification commands, directories, results and output references: see
"Verification evidence".
Readiness or review verdict: READY_FOR_TESTS for FE5-T1 on the spec hashes above;
execution dependency on stu-fe3 (hard) and stu-fe4 (default order) recorded.

### Receiver receipt
Receiver role / actual tool and model: pending (fresh test writer).
Checked task, artifact revisions and required evidence: pending; must include the
stu-fe3 (and, if ordered, stu-fe4) reviewed frontend manifest it starts from.
Receipt: pending (ACCEPTED | RETURNED).
Reason / exact missing or conflicting artifact: pending.

## Requirement-to-technical coverage
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| TH-DEFAULT (TH-D1) | TH-D1; FRONTEND-DESIGN rev 3 theme contract (persistence rules) | Bootstrap 5.3 `data-bs-theme`; no server/API involvement | COVERED | RTL: empty storage -> root light, switch not pressed (with `matchMedia` dark stub); Playwright `colorScheme: 'dark'` emulation on Login, Student and placeholder -> light; emulation change while open -> still light |
| TH-TOGGLE | Phuc request; rev 3 control paragraph | Bootstrap colour modes; per-theme tokens (styles.css) | COVERED | RTL: click toggles root attribute and `aria-pressed` both ways without route change; Playwright: computed body background/text differ between themes; pressed state has non-colour cue (icon/text change asserted by accessible content or snapshot check) |
| TH-A11Y | rev 3 control paragraph; visual contract (keyboard, focus) | WCAG 2.1 AA (4.1.2 name/role/value, 2.1.1 keyboard, 2.4.7 focus visible); FE2-UI dialog trap | COVERED | RTL: `getByRole('button', {name: 'Dark theme', pressed})` in banner, name identical in both states; user-event Tab reaches it, Enter and Space toggle once each, focus stays. Playwright: 320/768/1024/1440 visible and no overflow; dialog Tab cycle excludes the switch |
| TH-PLACEMENT (TH-D3, OD-12) | TH-D3; rev 3 placement paragraph; FE1-UI-LANDMARKS (rev 2) | UX-01; FE1-UI | COVERED (after stu-fe3) | RTL: on all eight page states exactly one switch `within(getByRole('banner'))`; placeholder banner contains only brand/home link, switch, Sign out; Student header keeps name/role/nav; Login heading order unchanged. Updates frozen test auth.test.tsx line 54 (SA5-03) |
| TH-PERSIST (TH-D2) | TH-D2; rev 3 persistence paragraph; Transport privacy exception | SH-WEB-03 (unsaved private text only; theme not private); FE1-PRIVACY | COVERED | RTL: toggle on Login -> sign in each role -> dark; sign-out confirmed/ended/unconfirmed -> dark Login; unmount/remount App with stored value -> dark; `Storage.prototype.setItem` spy: exactly one write `('unichope.theme','dark'|'light')` per activation, zero writes across load/sign-in/sign-out/book/cancel, `removeItem`/`clear` never called. Playwright: reload keeps dark |
| TH-STORAGE-FALLBACK | rev 3 persistence paragraph | Browser Storage API exceptions | COVERED | RTL: stub `getItem`/`localStorage` getter to throw -> light, no alert, no thrown error (console.error spy empty); invalid values `Dark`, `blue`, `''`, `{"theme":"dark"}` -> light; `setItem` throws -> theme still changes, no alert/error. Playwright optional: storage blocked context |
| TH-NOFLASH | rev 3 early application paragraph | index.html head; Vite `public/` same-origin asset; no CSP today (SA5-08) | COVERED (automated attribute check + manual visual) | Playwright on dev server: `page.route` holds `/src/main.tsx`, `addInitScript` presets storage, assert root `data-bs-theme` before release for `/login`, `/student/slots`, `/admin`; invalid/blocked -> light. Static check: index.html has `<script src>` in head before module, no inline script. Manual: `npm.cmd run build` + preview reload with dark stored, recorded observation |
| TH-CONTRAST (OD-9) | OD-9; FRONTEND-DESIGN contrast criterion (FE-UI-CONTRAST); rev 3 table | WCAG 2.1 SC 1.4.3, 1.4.11; color-scheme | COVERED | Playwright both themes on the listed screens/states: axe color-contrast or computed-ratio script zero failures; computed non-text/focus ratios (inputs, selects, date, outline/Book buttons, switch, current nav, active tab, today column, focus outline) >= 3:1 or recorded manual check; root `color-scheme` equals theme |
| TH-SESSION | rev 3 session-independence paragraph; Transport privacy paragraph | SH-WEB-03; FE1-LOGOUT/DENIAL/PRIVACY; FE2-RACE | COVERED | RTL: toggling sends zero fixture calls, URL/results/filters/notice unchanged; deferred read resolves and renders after toggle with no extra request; sign-out (each outcome), protected 401/ACCOUNT_INACTIVE and restore failure keep dark and stored value; storage value is only the literal |

Checklist areas: API and UI: no endpoint, DTO or status change; UI states covered by
TH-STORAGE-FALLBACK (failure), TH-SESSION (loading/session), TH-A11Y (keyboard).
Authentication and privacy: the only storage write is the literal theme value, never
actor/session data; session clearing neither reads nor clears it (TH-PERSIST,
TH-SESSION); SH-WEB-03 unchanged. Data and concurrency: no server writes; a pending
read must survive a toggle (TH-SESSION S2). Architecture and ownership: frontend
only; shared Login and placeholder headers change visually and need Kok Seng's and
Huanghao's merge review; no shared spec or Auth contract edit. AI: not applicable
(no AI UI). Feasibility: Bootstrap 5.3.8 supports `data-bs-theme`; Vite serves
`public/` in dev and build; jsdom provides localStorage and `matchMedia` can be
stubbed; dev-server CSS-by-script limits the automated no-flash check to the
attribute (SA5-06).

## Spec findings and proposed fixes
| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner | Resolution reference / state |
| --- | --- | --- | --- | --- |
| SA5-01 / medium (CONFLICT) | FE1-UI-LANDMARKS S2 and the visual contract (FRONTEND-DESIGN 895AFB4C... lines 73-74, 254) say the Tutor/Admin placeholder banner has only the app name and Sign out and "no other control"; TH-D3 requires the switch on those pages. | TH-D3 is the later, specific owner decision: TH-PLACEMENT S2 refines FE1-UI-LANDMARKS S2 from stu-fe5 on (switch is the only additional control). stu-fe3 implements S2 unchanged, so its READY_FOR_TESTS scenarios are not disturbed; fe5 updates the S2 test. Alternative (edit S2 now) rejected: it would change a scenario fe3's test writer is about to encode. | Phuc (TH-D3 decided) | RESOLVED: FRONTEND-DESIGN rev 3 placeholder note, TH-PLACEMENT S2 |
| SA5-02 / medium (CONFLICT) | FRONTEND-DESIGN Transport "Private state, CSRF and credentials stay in memory; no localStorage/sessionStorage" reads as a blanket ban; FE1-PRIVACY says "no private browser storage writes". TH-D2 requires storing the theme. SH-WEB-03 only forbids persisting unsaved private text, so the shared spec does not conflict. | Add an explicit, narrow exception in FRONTEND-DESIGN (Student-owned): one key `unichope.theme`, literal `light`/`dark`, written only on activation, not read or cleared by session handling; every other write still forbidden. No shared-spec edit (would be Huanghao's contract; not needed). | Phuc (TH-D2 decided, including "not private data under SH-WEB-03") | RESOLVED: FRONTEND-DESIGN rev 3 Transport paragraph, TH-PERSIST S4 |
| SA5-03 / low (frozen test oracle) | `auth.test.tsx` line 54 asserts `getAllByRole('button')` length 1 on Tutor/Admin placeholders; a correct fe5 implementation adds the switch. The FE1-PRIVACY setItem spy (lines 149-158) stays valid because no write occurs without activation. | Test writer changes line 54 to: exactly one Sign out button plus the "Dark theme" switch and no other button; records old/new hash. Same allowance for fe3's future S2 test. Tests reset `unichope.theme` between cases. | Spec analyst (oracle), test writer (edit) | RESOLVED at spec level: FRONTEND-DESIGN rev 3 "Frozen-test interactions"; test edit pending FE5-T1 |
| SA5-04 / medium (contrast feasibility) | The light accent `--accent: #b44320` reaches only about 3:1 against a typical dark surface (computed: relative luminance about 0.138 versus about 0.013 for #1b1f27), failing 4.5:1 for module codes, links and text buttons in dark; literal light backgrounds (#fff4eb events, #fafbfc panels, white Book button, #fff6f3/#f1f8f3 notices) would stay light in dark. | TH-CONTRAST requires each theme to pass; the theme contract allows a per-theme accent value while keeping the orange brand, and requires all colours via per-theme tokens. Exact palette is a builder choice measured by the check. | Spec analyst (within OD-9) | RESOLVED: FRONTEND-DESIGN rev 3 theme contract and TH-CONTRAST |
| SA5-05 / low (implementation drift risk) | styles.css `:root` sets `--bs-body-color`/`--bs-body-bg` directly, which overrides Bootstrap's `[data-bs-theme=dark]` values; about 30 literal colours bypass tokens. | Theme contract: tokens per theme, no light-only literals in components shown in dark; outcome measured by TH-CONTRAST/TH-TOGGLE, not by class or variable names. | Implementer (within contract) | RESOLVED (contract); implementation pending |
| SA5-06 / low (testability) | Playwright runs against the Vite dev server (playwright.config.ts), where CSS is injected by the JS bundle, so a "no light frame" visual check before the bundle cannot be automated without a config change outside scope. | TH-NOFLASH S1-S3 automate the root attribute timing and the non-inline head script; S4 is a recorded manual check on a production build. Owner said "where feasible". | Spec analyst | RESOLVED: TH-NOFLASH |
| SA5-07 / low (dependency) | fe3 (banners, placeholder header) and fe4 (contrast tokens/check, App.tsx/Login.tsx) edit the same files and are not yet tested or implemented. | Hard dependency on fe3's reviewed revision; default order after fe4's reviewed revision; fe5-before-fe4 allowed by Phuc (Q-5-2) because TH-CONTRAST is self-contained. | Spec analyst (ordering), Phuc (priority) | RESOLVED: Prerequisites above, PLAN FE5 row |
| SA5-08 / info (security) | No Content-Security-Policy is configured (ApiSecurityConfiguration.java) or specified; an inline head script would block a future `script-src 'self'` policy. | Early step is a same-origin `<script src>` (TH-NOFLASH S3); it reads only the theme key and writes only the root attribute; no data leaves the browser. | Spec analyst | RESOLVED: theme contract, TH-NOFLASH S3 |
| SA5-09 / low (hash supersession) | This stage changes FRONTEND-DESIGN and PLAN hashes recorded in the fe3/fe4 packets (895AFB4C..., 008AA245...), which would make their receivers return the handoff for a mismatch. | Append an analyst note to both records: new hashes, revision 3 changes no fe3/fe4 scenario (additions and fe5-only refinements). | Spec analyst | RESOLVED: notes appended to stu-fe3/stu-fe4 (hashes in Verification evidence) |
| Q-5-1 / info (Phuc) | TH-D3 names Login, Student and placeholders; "every page" also covers the session-check, session-check failure and not-found screens. | Specified on all banner pages (TH-PLACEMENT S1) for consistency. If Phuc wants it only on the three named pages, only TH-PLACEMENT S1 changes. | Phuc | OPEN, non-blocking (analyst default specified) |
| Q-5-2 / info (Phuc) | Order of fe4 and fe5 after fe3. | Default fe3 -> fe4 -> fe5. Phuc may move fe5 ahead of fe4. | Phuc | OPEN, non-blocking |
| Q-5-3 / info (Phuc) | Exact wording of the switch's accessible name. | "Dark theme" with `aria-pressed` (stable name, state announced as pressed/not pressed). Phuc may choose other wording; only the TH-A11Y oracle string changes. | Phuc | OPEN, non-blocking (analyst default specified) |

No high or critical finding. OD-6 and FE-R4 remain outside this change.

## Spec readiness
Verdict: READY_FOR_TESTS
Assessed task and exact spec revision/hash: FE5-T1 (all nine TH IDs) on
FRONTEND-DESIGN DBA3D9A321D93BD54798F34A20EA23DD97E4931EE19DBC2F2DFE30A201513089,
PLAN 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE and the
unchanged inputs above.
Remaining findings and task-specific deferral rationale: Q-5-1..Q-5-3 have literal,
testable defaults; changing any of them alters one oracle string or ordering only.
SA5-03 is the test writer's first action. Execution waits for stu-fe3's reviewed
revision (hard) and by default stu-fe4's; this is a sequencing dependency, not a
spec gap: the test writer re-verifies against that revision before recording red.
Required owner decisions and recorded outcome: TH-D1, TH-D2, TH-D3 recorded (Phuc,
22 October 2026, current-session answers). OD-9 and OD-12 previously recorded. Kok
Seng and Huanghao merge reviews are merge gates, not test prerequisites.
Recheck evidence: re-read FRONTEND-DESIGN revision 3 end to end: each TH ID has a
source paragraph in "Theme contract", a scenario row and a planned validation; no
ID needs an API, DTO, backend or Auth change; the Transport storage rule and
FE1-UI-LANDMARKS S2 now agree with TH-D2/TH-D3 through explicit fe5-scoped
refinements; compared frozen test oracles: one conflict (SA5-03), FE1-PRIVACY spy
unaffected; fe3/fe4 scenarios unchanged (only header revision lines, the
placeholder note, the storage exception and the new section were added).
Next agent, task, acceptance IDs, allowed files and prerequisites: fresh test writer;
FE5-T1; TH-DEFAULT, TH-TOGGLE, TH-A11Y, TH-PLACEMENT, TH-PERSIST,
TH-STORAGE-FALLBACK, TH-NOFLASH, TH-CONTRAST, TH-SESSION; allowed files per Handoff
analyst-1; prerequisite stu-fe3 reviewed revision (and stu-fe4 unless Phuc reorders).

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| FE5-T1 | TH-DEFAULT S1-S3 | new theme RTL test file (with `matchMedia` stub) + Playwright colour-scheme emulation | Not written |
| FE5-T1 | TH-TOGGLE S1-S3 | theme RTL test; Playwright computed colours | Not written |
| FE5-T1 | TH-A11Y S1-S4 | theme RTL test (role/name/pressed, Tab/Enter/Space, focus); Playwright viewports and dialog Tab cycle | Not written |
| FE5-T1 | TH-PLACEMENT S1-S4 | auth.test.tsx / theme RTL across eight page states; SA5-03 update | Not written |
| FE5-T1 | TH-PERSIST S1-S4 | theme RTL (sign-in/out each role, remount, setItem/removeItem spies); Playwright reload | Not written |
| FE5-T1 | TH-STORAGE-FALLBACK S1-S3 | theme RTL with throwing/invalid storage and console.error spy | Not written |
| FE5-T1 | TH-NOFLASH S1-S3 | Playwright with held `/src/main.tsx`; static index.html check | Not written |
| Manual | TH-NOFLASH S4 | Production build preview reload with dark stored; recorded observation | Pending |
| FE5-T1 | TH-CONTRAST | Playwright contrast check in both themes on listed screens; non-text/focus computed or manual | Not written |
| FE5-T1 | TH-SESSION S1-S4 | theme RTL with fixture call counts and deferred reads; session-denial and logout outcomes | Not written |
| Human | Phuc journey | Toggle on Login, Student, Tutor/Admin; reload; sign out/in; keyboard; laptop and phone | Pending |

Verification for the task: `npm.cmd test`, `npm.cmd run build`, `npm.cmd run
test:browser` in `src/frontend`; refreshed frontend manifest and patch; fresh
independent review of the changed files.

## Verification evidence
Command, working directory, revision, exit code, result and relevant output (this
stage, cwd D:/cs3227/CS3227-2610-MP3, HEAD e277e583f1319320ed5f44e1944ac85296bb7d15,
Git Bash on Windows):
- `git rev-parse HEAD`: exit 0, e277e583f1319320ed5f44e1944ac85296bb7d15.
- `git status --short -- . ':!logs'`: exit 0; tracked spec edits and untracked
  frontend/workflow files as listed in Inputs.
- Node script recomputing SHA-256 of every `frontend-manifest-r2.json` entry (BOM
  stripped): exit 0 after printing "entries 40 mismatches 4"; the 4 are
  workflow/student/FRONTEND-DESIGN.md, TECHNICAL-SPEC.md, PLAN.md and
  workflow/auth/PLAN.md, already superseded by analyst-4; all 36 frontend entries match.
- `sha256sum` of all inputs before editing and of changed files after editing: exit 0;
  values in the Inputs table and below.
- `git diff --check -- workflow ':!logs'`: exit 0; untracked FRONTEND-DESIGN.md
  checked separately for trailing spaces: 0 lines.
- Appended "Analyst note (stu-fe5)" to both dependency records; SHA-256 after append:
  stu-fe3-review-fixes.md C92CAFD77D34D545BA2B37E0B8F2280A9C3DF75820788D8F73D9AF7756F71318,
  stu-fe4-followups.md 8A9BA2A32FC94AAEABDA7D9F5C73DF76B41E51E3D9EFFC9DD05982BF76E38F04.
  Line endings checked consistent (CRLF) in all changed files.
Pre-implementation expected failure (or existing passing baseline): not run by the
spec analyst; the test writer records it against the stu-fe3 (or stu-fe4) reviewed revision.
Final focused tests / integration tests / build: not run (spec stage).
Manual and real-provider checks: not applicable to this stage; TH-NOFLASH S4 and the
human journey pending.

## Independent review
Reviewer role / actual model / reviewed revision: pending.
Verdict and evidence: pending.
Findings: pending.
Fixes and re-review results: pending.

## Human verification
Person, date, checked journey, outcome: pending (Phuc).
Remaining limitations and follow-ups: Q-5-1..Q-5-3; Kok Seng and Huanghao merge
reviews; OD-6 deferred; FE-R4 undecided.
Next agent, exact task and required inputs: fresh test writer after stu-fe3 (and by
default stu-fe4) review; FE5-T1; this record, FRONTEND-DESIGN DBA3D9A3..., the
reviewed frontend manifest it starts from, NOTE.md, HANDOFF.md, agents/test-writer.md.


## Handoff analyst-2: current implementation-request readiness (22 October 2026)
Producer role / actual tool and model: spec analyst / Codex / GPT-6; fresh subagent with no inherited conversation.
Recipient role: fresh test writer using agents/test-writer.md.
Change ID / task / exact requested next action: stu-fe5-theme-switch / FE5-T1. After FE4 review, verify upstream reviewed manifests and refresh changed source hashes; author all nine TH IDs, updating placeholder button-count oracles only by the specified theme-control allowance.
Stage status / supersedes: READY_FOR_TESTS for specification consistency; refreshes analyst-1 and its hash-supersession note for the current worktree. Earlier findings and acceptance mappings remain preserved.
Goal and non-goals: deliver this existing capability as part of the user's current request "Implement stu-fe5". Necessary prerequisites are FE3 -> FE4 -> FE5; each keeps its own record, tests, implementation and independent review. Preserve each original non-goal and owner boundary; no backend, Auth policy or role-workspace feature change.
Scope check: standard path; these are independently testable behavior changes, split across the existing three records in dependency-safe order. No shorter-path claim.
Acceptance IDs: TH-DEFAULT, TH-TOGGLE, TH-A11Y, TH-PLACEMENT, TH-PERSIST, TH-STORAGE-FALLBACK, TH-NOFLASH, TH-CONTRAST, TH-SESSION. Given/When/Then scenarios remain normative in FRONTEND-DESIGN.md (Revision 3 Theme contract and Theme acceptance IDs; Transport narrow storage exception; FE5-only placeholder-header refinement); no scenario changed by this refresh.

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
Prerequisites / dependency state and evidence: FE3 and FE4 independent reviews are pending. Use the default FE3 -> FE4 -> FE5 order. Verify their reviewed manifests before FE5 tests; this pre-prerequisite manifest is not the eventual FE5 execution baseline.
Recorded decisions and authorization: current user implementation request authorizes progressing necessary existing prerequisites; FRONTEND-DESIGN records OD-1..OD-17 and TH-D1..TH-D3. Existing literal defaults remain in effect, including exactly "Dark theme", placement on every banner and default FE4-before-FE5 order. No new material policy or design decision is required. Affected owners retain merge review; Phuc retains human acceptance.

### Requirement-to-technical coverage refresh
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| All IDs above (each existing individual mapping retained in analyst-1) | FRONTEND-DESIGN Revision 3 Theme contract and Theme acceptance IDs; Transport narrow storage exception; FE5-only placeholder-header refinement; Student/shared UX-01 | Student Technical frontend pointer and endpoint contracts; Auth Technical HTTP contract; Integration section 2; SH-AUTH-01..05 and SH-WEB-01..03 | COVERED | RTL defaults/toggle/storage/session/race states; browser early root attribute before bundle, palette/contrast and viewport/dialog keyboard checks; production preview no-flash observation remains required. |

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


## Test-writer-1 receiver receipt (22 October 2026)
Test writer / Codex / GPT-6; fresh session. Receipt: ACCEPTED with explicit newline-normalized refresh. FE5-T1 all nine TH IDs; analyst-2 READY_FOR_TESTS and FE4 reviewer-2 PASS checked directly. Current HEAD 0579ea7164592c59ccbe5c9c13cd83e022a28d6d follows external merge/checkouts; preserve staged work. Incoming FE4 84-entry manifest has 56 byte matches, 25 exact LF/CRLF-conversion matches and two mixed-newline source differences plus the expected appended FE3 record (28 total mismatches includes that record). Consultations normalized content equals git 7e79a74; student.test normalized delta is only the documented FE3 Admin privacy oracle (also present in FE4 complete patch). FRONTEND-DESIGN current 765790E7645195639C69D088A67F64E6F024072FF790BFFA14059AF5AE7F6DC2 normalizes exactly to recorded DBA3D9A...; PLAN and analyst-current-manifest match recorded hashes. No behavior/contract drift found. App, Login, styles, StudentWorkspace, fe3/fe4 tests and browser contrast artifacts retain reviewed byte hashes. Refresh current artifact hashes in outgoing manifest. Allowed writes: tests/e2e, this record/evidence and append-only human log; no product/spec/config edits.


## Handoff test-writer-1 (22 October 2026)
Producer: test writer / Codex / GPT-6; fresh session. Recipient: fresh implementer.
Task FE5-T1, all nine TH IDs. Verdict: READY_FOR_IMPLEMENTATION; supersedes
analyst-2 execution status only. No theme implementation or acceptance claimed.
Base HEAD 0579ea7164592c59ccbe5c9c13cd83e022a28d6d plus incoming staged/unstaged
worktree; preserve unrelated changes. No staging or commit. Current normalized
receipt above resolves checkout-only differences, not a contract revision.

### Artifacts and scope
- evidence/test-writer-manifest.json SHA-256 24A110734A773D6C836DC3B5ED7F9D9AC6593A35EEA6BA982E5B1517E501CF3A; 53 current per-file hashes. Includes canonical inputs, all frontend source/config/tests and own sanitized command evidence; record/log/self excluded.
- Tests: src/frontend/src/test/theme.test.tsx (45 cases), setup.ts isolated storage/root reset; auth.test.tsx and fe3.test.tsx only documented placeholder allowance (exactly Sign out plus Dark theme; no extra button/account/navigation). Existing privacy oracle retained.
- Browser: e2e/theme.spec.ts (38 cases), contrast.spec.ts (32 both-theme/state cases), theme-fixture.ts; exported contrast-audit.ts extends boundary/selected checks to pressed toggles. Existing seven browser checks retained.
- Complete frontend revision: git diff HEAD -- src/frontend plus untracked files from git ls-files --others --exclude-standard -- src/frontend. Incoming-to-stage changes are only these eight test/support files; no product/config/spec/backend/dependency edit.

### Acceptance mapping
| ID | Executable coverage |
| --- | --- |
| TH-DEFAULT | RTL exact stored-value matrix; browser Login/Student/Admin with light/dark device schemes and scheme changes |
| TH-TOGGLE | RTL Enter/Space roundtrip, root/pressed/icon change, retained focus/URL/input identity; browser keyboard |
| TH-A11Y | Exact native button/type/name/pressed/banner; browser Tab document order, Enter/Space, no trap, four widths, dialog focus/Escape |
| TH-PLACEMENT | RTL eight page states including pending restore/failure/notfound, Student account/nav and frozen placeholder privacy checks |
| TH-PERSIST | Exact one write/activation/key/literal/storage instance, remount/reload/URL, all role logins/logout/new account, no writes/removals/clear during session or book/cancel |
| TH-STORAGE-FALLBACK | Invalid values, storage getter/getItem/setItem throw, current-page toggling, persisted rule on remount; early browser read/access failures |
| TH-NOFLASH | All three URLs, dark/light/absent/four invalid/read+access errors with source modules aborted; root before bundle and same-origin synchronous head src before main, no inline script |
| TH-CONTRAST | Both themes: Login + unconfirmed logout notice/Retry; desktop today/striped timetable/events and mobile list; Upcoming/History/status/empty; dialog/backdrop; success/error/status/loading; Tutor/Admin/failure/notfound; computed text/control/current/pressed/focus ratios and root color-scheme |
| TH-SESSION | Zero-request toggle preserving actor/filter/notice/URL/results; pending slots/bookings not aborted/repeated; all logout sources/outcomes, protected expiry/inactive/forbidden/CSRF and restore failure retain dark; no private storage |

### Verification evidence
Cwd src/frontend, existing locked dependencies, synthetic fetch/routes and Edge only.
- npm.cmd test -- src/test/theme.test.tsx --reporter=json --outputFile=../../workflow/student/changes/stu-fe5-theme-switch/evidence/test-writer-unit-final.json: exit 1, final 45/45 fail genuinely on absent root theme or Dark theme control; no import/discovery/setup failure. Text counterpart retained. Supersedes initial 43-case focused baseline.
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe5-theme-switch/evidence/test-writer-unit-full.json: exit 1, earlier 186 total, 139 pass/47 fail (43 theme + four updated placeholders). Two final additive cases covered by focused final above; no fabricated final full-suite count.
- npm.cmd run build: exit 0, TS/Vite, 27 modules; test-writer-build.txt. Build ran before final two additive RTL cases; those compile/execute in final Vitest run.
- npm.cmd run test:browser -- --workers=4: exit 1, 77 total, seven unchanged pass/70 new fail; test-writer-browser.txt. Genuine absent root/control failures, including persistence click waiting for nonexistent control.
- npm.cmd run test:browser -- --workers=1 --grep "TH-CONTRAST light history|TH-CONTRAST dark slots 320": exit 1, 2/2 expected root-theme failures; test-writer-browser-final.txt. This reruns final helper/History locator revision after full baseline; all other browser source unchanged.
Windows sandbox 1909 required same-scope escalated execution; no approval rejection. Two preparatory edit snippets initially used repository-relative paths from frontend cwd and failed before edits; corrected at repository root. Actual npm commands still ran and produced reports. Full-unit run overlapped browser load; no existing unit failures or timeout occurred. Required final focused evidence is explicit above.

Manual checks pending: production-build preview reload no-light-frame (TH-NOFLASH S4), real laptop/phone/keyboard/200% zoom/text-size and native date popup; shared-owner merge review and Phuc acceptance. Fake fixtures prove UI only; real backend/provider not tested here. No new material decision, whole-app security or completion claim.
Next: fresh implementer verifies canonical/current manifest and red reports, implements only permitted non-test frontend theme module/button/style/index+same-origin public script, preserves tests, runs focused/full unit/build/browser and records exact revision for fresh reviewer. Return incorrect tests/contracts to their responsible role; never weaken to pass.
Receiver receipt: pending fresh implementer (ACCEPTED | RETURNED).

## Implementer-1 receiver receipt (22 October 2026)
Implementer / Codex / GPT-6; fresh session. Receipt: ACCEPTED. FE5-T1 all nine TH IDs; analyst-2 READY_FOR_TESTS and test-writer-1 READY_FOR_IMPLEMENTATION checked against Revision 3 theme contract, actual tests and red evidence. Recomputed test-writer-manifest.json SHA-256 24A110734A773D6C836DC3B5ED7F9D9AC6593A35EEA6BA982E5B1517E501CF3A, 53/53 current hashes match. FE3 reviewer-1 PASS and FE4 reviewer-2 PASS prerequisite records checked; incoming tests preserve reviewed behavior and explicit theme allowance. Scope: non-test frontend source, index.html, one public synchronous same-origin theme script; record/evidence and append-only human log. No test, dependency, backend or canonical contract changes. Approved existing design and literal decisions supply brainstorming gate; no new design decision needed. Preserve external staged/unstaged changes; no staging/commit.

## Handoff test-writer-2: TH-NOFLASH authored-source oracle correction (22 October 2026)
Producer: fresh test writer / Codex / GPT-6. Recipient: existing implementer, then fresh reviewer. Scope: standard workflow correction of the FE5-T1 acceptance oracle only; supersedes test-writer-1's served-DOM inline-script assertion, no other coverage or canonical scenario. Status: READY_FOR_IMPLEMENTATION (test correction executable; implementation full verification remains required).
Receiver receipt: ACCEPTED. Read NOTE/HANDOFF/test-writer prompt and canonical Revision 3 directly. Verified FRONTEND-DESIGN SHA-256 765790E7645195639C69D088A67F64E6F024072FF790BFFA14059AF5AE7F6DC2, PLAN 919BD0A2EB8B66453F54B67CCE8B55F222B6678FE41DAAD969FD0788D0851FAE and test-writer-1 manifest 24A110734A773D6C836DC3B5ED7F9D9AC6593A35EEA6BA982E5B1517E501CF3A; current original theme.spec matched its recorded hash. Analyst-2 READY_FOR_TESTS and original acceptance mapping remain applicable. Product source differs as expected during implementation; this correction does not accept or review that source.

TH-NOFLASH S3 explicitly concerns the authored index.html early theme step. Raw index.html contains an external synchronous /theme.js before /src/main.tsx. Served dev HTML adds an inline type=module React Refresh preamble importing /@react-refresh; the original global inline ban therefore failed after the root-theme/order/origin assertions already passed. Saved served HTML is evidence/test-writer-2-served.html. No requirement prohibits Vite's development instrumentation.

Output: src/frontend/e2e/theme.spec.ts old SHA-256 2460F2F3525ADCA8F94D1653A74C178B1DEBE0EB4024C24B2939CFF4438493CF; new SHA-256 61BDCC49168B3F955A2352D7F3F81460290EA6A5C1875A9C06AE61B628691579. It reads actual authored index.html and parses it with DOMParser, rejecting all nonempty inline scripts without an exemption. Existing served-DOM same-origin, head, order-before-main, non-module, non-async/non-defer checks remain unchanged, as do all held-bundle root checks, invalid/throwing-storage cases, and empty rendered heading check. No product/spec/config/dependency changes. Inputs/results and exact hashes: evidence/test-writer-2-manifest.json; includes index.html 028810513FB2D252717CB4E35A12D2B531773F75F858A7362B315832A144062D and public/theme.js 23B97C1A859B7155DAE00B97DDB3598F5DD51B508DC874467B463E8A2195DDD6. No staging/commit.

Verification (cwd src/frontend, existing Vite server, Edge, Windows same-scope escalated execution):
- Original test: npm.cmd run test:browser -- e2e/theme.spec.ts --workers=1 --grep TH-NOFLASH, exit 1, all 27 fail at the inline React Refresh assertion; evidence/test-writer-2-red.txt. This is an oracle failure, not a product red result.
- Corrected final test: npm.cmd run test:browser -- e2e/theme.spec.ts --workers=4 --grep TH-NOFLASH, exit 0, 27 passed (5.5s); evidence/test-writer-2-focused.txt.
- Full browser/unit/build not repeated in this narrowly scoped correction; implementer must record final full verification against the new test hash. TH-NOFLASH S4 production visual and all previously pending human checks remain pending here. An initial overly quoted grep found no tests, and repository-relative reads from frontend cwd failed; corrected commands above executed successfully and are the only claimed evidence.
Next action: implementer verifies this test hash, records receipt and runs final full browser verification; independent reviewer must inspect source/tests and this oracle correction against the canonical contract.

### Implementer receipt for test-writer-2
Implementer / Codex / GPT-6. Receipt: ACCEPTED. Verified test-writer-2 manifest SHA-256 2610051AC2B5E4705263867B1801C353D91A0CCBA423230DE3F1447F7F97F759 and every listed current artifact; theme.spec.ts SHA-256 61BDCC49168B3F955A2352D7F3F81460290EA6A5C1875A9C06AE61B628691579. Inspected actual correction against TH-NOFLASH S3: authored-source inline ban now checked directly, served head/origin/order/synchronous and held-root checks retained. Product source unchanged for the oracle correction. Final full checks below use that frozen test revision.

## Handoff implementer-1 (22 October 2026)
Producer: implementer / Codex / GPT-6, fresh session. Recipient: fresh independent reviewer/security checker. Task: FE5-T1 all nine TH IDs. Status: IN_REVIEW; supersedes the initial record status for implementation progress only. No independent review or human acceptance claimed.

### Exact revision and review artifacts
HEAD 2f05bdfa73fe51c7b13e8727021d8784a1ddc711 plus worktree; external HEAD changes preserved, no staging/commit. Inputs: analyst-2 canonical readiness, test-writer-1 53-entry baseline (accepted before product edits), test-writer-2 corrected oracle and verified seven-entry manifest. FE3 reviewer-1 PASS and FE4 reviewer-2 PASS prerequisite decisions retained.
- evidence/implementer-manifest.json SHA-256 D2F1DCA1B5032773E48C6AFD4E7E96E66AA57DF2C946875115F355577897C359: 78 exact current input/source/test/evidence hashes; excludes self, this changing record and human log.
- evidence/implementer-scoped.patch SHA-256 AFFD4BFB8DBB0F1137B5CED30DFE9669ACE32D7CB2BCCA5D5297B55C57CB3333: this implementer's six product files only, including the two new files.
- evidence/implementer-complete-frontend.patch SHA-256 B76F9AFC1D9A0CA52E79162926B7EB5AB6C4276BB2983E49D27C3B07F7EA2BFF: HEAD-to-worktree frontend diff including all untracked source/tests/support, preserving upstream test changes.
- evidence/implementer-verification.json: commands, exits, tested HEAD, scope and limitations. Per-file hashes in manifest are authoritative for uncommitted content.

### Implementation and acceptance mapping
| IDs | Code and preserved behavior |
| --- | --- |
| TH-DEFAULT, TH-TOGGLE, TH-STORAGE-FALLBACK | src/theme.tsx uses pure lazy exact-dark storage read with silent light fallback, root layout effect, native type=button with stable Dark theme name/aria-pressed and changing moon/sun SVG. Only explicit activation writes one literal light/dark preference; write failure retains page state. |
| TH-PLACEMENT, TH-A11Y | App.tsx/Login.tsx add one switch in every banner, retaining account/nav/placeholders and existing dialog trap. Mobile CSS wraps header actions and hides optional visible label while retaining accessible name. |
| TH-PERSIST, TH-SESSION | Stable outer ThemeProvider wraps SessionApp; no theme-derived key/remount/API/URL/session effect. Theme state survives account/session changes independently; no storage effects on lifecycle events. Existing reads, filters, notices, focus and pending requests preserved by behavioral tests. |
| TH-NOFLASH | index.html head loads synchronous same-origin /theme.js before deferred application module; public/theme.js has identical exact-dark validation/fallback and no write. React also independently initializes for App-only rendering. |
| TH-CONTRAST | styles.css defines all local palette literals as light/dark semantic tokens, including surfaces/status/backdrop/focus/selected controls; Bootstrap primary foreground, native color-scheme and form surfaces follow tokens. Existing orange/neutral layout preserved. |

No canonical/spec/test/config/dependency/backend/Auth-policy changes by implementer. Tests stayed frozen; the Vite inline-preamble oracle defect was returned and corrected by fresh test-writer-2, not bypassed in product. Earlier browser evidence retained: initial 50/77 passed (main module originally body, moved to head after synchronous script); before-oracle-fix 50/77 passed with every failure exclusively Vite-injected React Refresh inline-module ban. Corrected oracle retains all authored-source and executed-order/root requirements.

### Fresh sequential verification
Cwd src/frontend; existing locked dependencies, Edge, synthetic HTTP fixtures only. Windows sandbox account 1909 required scoped escalated process execution; no approval rejection.
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe5-theme-switch/evidence/implementer-unit.json: exit 0, 188/188 passed, zero failed/skipped; implementer-unit.txt/json.
- npm.cmd run build: exit 0, TypeScript/Vite, 28 modules; implementer-build.txt.
- npm.cmd run test:browser -- --workers=4: exit 0, 77/77 passed (22.0s); implementer-browser.txt. Includes all 32 both-theme contrast/state cases, 27 held-bundle early cases, keyboard/focus/dialog and 320/768/1024/1440 responsive checks.
- Repository cwd git diff --check -- src/frontend: exit 0.
- Local production npm.cmd run preview -- --port 4173 --strictPort and repository cwd node workflow/student/changes/stu-fe5-theme-switch/evidence/implementer-preview.mjs: probe exit 0; preview stopped afterward. Four production screenshots (implementer-light/dark-1440/320.png) actually inspected using view_image: readable orange/neutral surfaces and controls, visible distinct switch icon, wrapped mobile header, no clipping/horizontal overflow observed. Synthetic Student timetable/list shown; no claim of real data integration.
- Production held-asset observation for /login, /student/slots, /admin: root and computed color-scheme dark before React headings existed; released bundle and actual reload mutation timelines contain only dark. Exact bounded observations in implementer-production-observations.json and implementer-preview.txt. Headless timeline/screenshots do not establish human-visible no-light-frame absence: TH-NOFLASH S4 human visual check remains pending.

Pending: fresh independent review including the oracle correction; Phuc laptop/phone/keyboard, 200% zoom/timetable text-size, native date popup and production visual no-flash checks; Kok Seng/Huanghao shared-owner merge reviews. Fake fixtures prove frontend behavior only; backend/provider integration not exercised. Next agent: reviewer-security.md in a fresh session; independently derive all TH acceptance checks, verify manifest/patch/current sources, rerun checks, inspect screenshots and return concrete findings or review verdict.

## Handoff reviewer-1 (22 October 2026)
Producer: independent reviewer/security checker / Codex / GPT-6; fresh session that produced none of the source or tests under review.
Task FE5-T1; standard independent review of all nine TH IDs. Verdict: PASS. Status: READY_FOR_HUMAN, superseding implementer-1 IN_REVIEW and the initial progress status. Human acceptance remains pending.
Receiver receipt: ACCEPTED. Derived the checklist directly from canonical Revision 3 before upstream conclusions, then inspected actual source/tests/diffs and checked analyst-2, test-writer-1/2 and implementer-1 artifacts. FE3 reviewer-1 PASS and FE4 reviewer-2 PASS prerequisites checked; FE4's corrected StudentWorkspace and fe4.test byte hashes remain exactly their reviewed values. No source/test fixes, staging or commits in this review.

### Exact revision and evidence
HEAD 2f05bdfa73fe51c7b13e8727021d8784a1ddc711 plus uncommitted worktree. All 78 implementer-manifest entries matched on entry; manifest SHA-256 D2F1DCA1B5032773E48C6AFD4E7E96E66AA57DF2C946875115F355577897C359. Both scoped AFFD4BFB8DBB0F1137B5CED30DFE9669ACE32D7CB2BCCA5D5297B55C57CB3333 and complete B76F9AFC1D9A0CA52E79162926B7EB5AB6C4276BB2983E49D27C3B07F7EA2BFF patch hashes verified through that manifest and their source/test deltas inspected. Canonical FRONTEND-DESIGN remains 765790E7645195639C69D088A67F64E6F024072FF790BFFA14059AF5AE7F6DC2; its recorded newline-only supersession preserves Revision 3 acceptance.
New evidence/reviewer-1-verification.json SHA-256 1D2A28C18F94C5679F02CC599807957D4DDBD90A75948A1716E64B5F1ED9098D records checked revisions, command results, output hashes and limits.

### Independent acceptance and security checklist
| Acceptance | Evidence and conclusion |
| --- | --- |
| TH-DEFAULT, TH-TOGGLE | Exact-dark lazy read; absent/invalid values select light regardless of device changes. Native button toggles root/pressed/icon without navigation or remount; Enter/Space focus retained. |
| TH-A11Y, TH-PLACEMENT | One correctly named switch within each single banner across eight states. Student account/nav retained, placeholders restricted, Login heading order retained. Four browser widths and existing Tab/Shift-Tab/Escape dialog trap pass. |
| TH-PERSIST, TH-STORAGE-FALLBACK | Only explicit activation writes one literal theme key; storage getter/read/write exceptions are silent. Tests cover reload, roles/account changes, all logout outcomes and no lifecycle/book/cancel writes or clearing. Early script and React share exact-value rules. |
| TH-NOFLASH | Synchronous external same-origin head script precedes application module. 27 held-module cases cover three routes, valid/invalid/absent/throwing reads. Corrected test independently reads authored index.html while retaining served execution/order checks; Vite-injected dev instrumentation is not an authored inline theme step. Production held-asset/reload timelines inspected and dark-only; human-visible S4 remains pending. |
| TH-CONTRAST | All local palette literals are semantic light/dark tokens; root/native color-scheme and Bootstrap foreground/surface rules follow them. Inspected computed-ratio audit: alpha composition, large-text threshold, visible text, boundaries, selected state and focus checks are meaningful. All 32 both-theme state cases pass. Additional token calculations cover striped rows (minimum text 4.701 light / 7.969 dark) and primary hover (7.599 / 10.902). Four production 320/1440 light/dark screenshots actually inspected via view_image; no clipping/overflow observed, header switch visible. |
| TH-SESSION | Outer stable provider does not alter API identity, actor key, URL, reset/generation effects or pending work. Fixture call-count/deferred-read tests preserve loaded filters/notices/results and resolve pending reads without abort/repeat. No actor/session/private data enters storage. |

Independently checked SA5-01..09 resolutions against canonical requirements and code: explicit placeholder/storage exceptions agree with decisions, frozen oracles retain privacy intent, dark palette resolves literal-colour drift, early script avoids inline dependence and dependency/revision refreshes are recorded. Literal defaults Q-5-1..03 remain testable and unchanged. No unsupported shared contract decision found. No dependency/config/backend/SQL/AI change in this feature; no advisory scan or whole-application security claim made. Findings: no blocking finding and no new lower-severity follow-up.

### Fresh sequential verification
Cwd src/frontend; installed locked harness, Edge, synthetic HTTP fixtures:
- npm.cmd test -- --reporter=json --outputFile=../../workflow/student/changes/stu-fe5-theme-switch/evidence/reviewer-1-unit.json: exit 0, 188/188, zero failed/skipped; reviewer-1-unit.txt/json.
- npm.cmd run build: exit 0, TypeScript/Vite, 28 modules; reviewer-1-build.txt.
- npm.cmd run test:browser -- --workers=4: exit 0, 77/77 (22.6s); reviewer-1-browser.txt.
- Repository cwd git diff --check -- src/frontend workflow/student/PLAN.md: exit 0.
Same-scope escalated Node/browser execution used for the documented Windows sandbox process restriction; no automatic approval rejection occurred.

### Plan bookkeeping and pending human verification
Updated PLAN status prose and FE3/FE4/FE5 row status prefixes to their recorded independent PASS / READY_FOR_HUMAN states. PLAN SHA-256 is now 3E4E906B5FADB01954502D5A7E3F12F5A8B96554E7A23315D0CC6D926022A3D2 (supersedes 919BD0A2... for status bookkeeping only). Acceptance IDs, scenarios, dependencies, ownership and scope unchanged; this documentation-only progress correction invalidates no behavioral test/readiness evidence. Final implementer-manifest comparison differs only at this intentionally refreshed PLAN entry; every other input/source/test/evidence byte hash remains matching.
Next: Phuc's laptop/phone and keyboard journey, 200% zoom/timetable text-size, native date popup and production visual no-light-frame check (TH-NOFLASH S4); Kok Seng/Huanghao shared-owner merge reviews. No actual backend/provider integration exercised in FE5 review. PASS enables human verification, not COMPLETE status, merge/deployment approval or human acceptance.

# stu-fe0-foundation: Runnable frontend foundation and test harness

Status: READY_FOR_TESTS (spec readiness; execution prerequisites below)
Scope classification: standard workflow, split independently testable capability.
Parent request: implement UniChope Login, role placeholders and Student booking UI.
Human owner: Phuc; affected Auth owner Kok Seng; Tutor/core owner Huanghao.
Current stage: spec analyst / Codex GPT-6. No role implementation/review performed.

## Handoff analyst-1
Producer role / tool and model: spec analyst / Codex GPT-6.
Recipient role: fresh test writer.
Change ID / exact next action: stu-fe0-foundation; Establish executable setup-only harness and meaningful baseline; write no Login/Student behavior. Test writer may add package/config/test harness support (including an inline sentinel React component to verify actual DOM rendering) without product implementation. Existing passing setup baseline is valid; missing imports/discovery are not red behavior. Fresh implementer then creates minimal runnable app/proxy/bootstrap and documentation, followed by fresh independent reviewer.
Stage status: READY_FOR_TESTS; initial packet, supersedes none.
Goal: Runnable frontend foundation and test harness.
Non-goals: backend/policy changes, registration/reset, Tutor/Admin features, AI, deployment.
Acceptance IDs: FE0-RUN, FE0-PROXY, FE0-QA. Given/When/Then scenarios are normative in
[FRONTEND-DESIGN.md](../FRONTEND-DESIGN.md), task acceptance table; do not duplicate
or weaken them here.

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| workflow/student/FRONTEND-DESIGN.md | Input / output | 3917AF4CD2709934383E03754A302ABA15D543A913B82EAA15A25B8F9D3F80DE | Canonical requirements/contracts |
| workflow/student/PRODUCT-SPEC.md | Input | 750433038415D5AF0832D4E506FB669C1D08A9A583FF388C4DEFC4F9FD690B86 | Canonical requirements/contracts |
| workflow/student/TECHNICAL-SPEC.md | Input | 97C2B915682BEC9EEC61BF29CCE665EF2088458C7DDE78E349CC47A88366F698 | Canonical requirements/contracts |
| workflow/student/PLAN.md | Input | E21093DFBFD869178AA6F4BCD951746C05641B77D96DB0933BBC2FC844E2F0CE | Canonical requirements/contracts |
| workflow/auth/PRODUCT-SPEC.md | Input | BE2F2F59B4E0ABEF49081A614DC77FA2DFB5BFDEB114C4A3B49AC38D2A73A637 | Canonical requirements/contracts |
| workflow/auth/TECHNICAL-SPEC.md | Input | 91949EC9793C1747F99C87D86FF74C941700BB9D0133F6204582B0C529E689E9 | Canonical requirements/contracts |
| workflow/auth/PLAN.md | Input | 5F40F9BE474C96FB0D22C89523367F20E8586AF59062B6DF278C43F6C1CD6ACB | Canonical requirements/contracts |
| workflow/SHARED-TECHNICAL-SPEC.md | Input | 5F92FF1F89B0171232BF3FF49C8517A9743EAEF2602B27F1C5D38EAFDE9912A5 | Canonical requirements/contracts |
| workflow/INTEGRATION-SPEC.md | Input | F7893269A4F5C3F1F4E106F3C62225DD3E8D3B4E3C0C31B6CA4A5CE61FAD0604 | Canonical requirements/contracts |
| src/backend/src/main/java/unichope/student/http/StudentController.java and StudentApiInput.java | Input | e277e583f1319320ed5f44e1944ac85296bb7d15 | Actual six routes, strict bodies/filters |
| src/backend/src/main/java/unichope/student/{ModuleOption,BookableSlot,StudentBooking}.java | Input | e277e583f1319320ed5f44e1944ac85296bb7d15 | Actual DTO projections |
| src/backend/src/main/java/unichope/platform/auth/AuthController.java and platform/web/ApiErrors.java | Input | e277e583f1319320ed5f44e1944ac85296bb7d15 | Actual actor/CSRF/login/logout/error envelope |

Base commit: e277e583f1319320ed5f44e1944ac85296bb7d15; result commit: none. Uncommitted complete documentation diff:
`git diff -- workflow` plus untracked FRONTEND-DESIGN.md and three stu-fe* records.
Canonical uncommitted files have exact hashes above; the record is its own manifest
and has no self-referential hash. Inspect `git status --short` for later additions.
Allowed writes for next stages: src/frontend/ package/config/lockfile, minimal application entry/shell, colocated harness smoke tests, frontend README; no feature login/booking behavior or backend files.
Dependencies and observed prerequisite state: No frontend directory exists at the verified base. Node24.21.0/npm.cmd12.0.2 exist. Package registry and Playwright browser download need verification; network escalation is a setup concern, not a new product decision.
Recorded decisions: Phuc's current request explicitly authorizes implementation and
the weekly timetable/sidebar/mobile list direction; direct authorization wording
is recorded in FRONTEND-DESIGN Authorization. React/TS/Vite/Bootstrap follows shared
stack; exact runtime package pins are harness choices. Shared owners' review remains
pending before merge; no invented approval. Routine implementation needs no new gate.
Open product questions: none blocking this named task. NUSMods timetable/course screenshots directly inspected; scoped visual decisions recorded. AI and deployment
are separate future work and do not gate this frontend task.

## Requirement-to-technical coverage
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation |
| --- | --- | --- | --- | --- |
| FE0-RUN | FRONTEND-DESIGN task acceptance + Student/Auth product | Shared Technical §1, source layout; FRONTEND-DESIGN transport | COVERED | npm.cmd ci/test/build, React DOM harness smoke |
| FE0-PROXY | FRONTEND-DESIGN task acceptance + Student/Auth product | SH-AUTH-05; FRONTEND-DESIGN transport | COVERED | dev /api proxy and relative production URL smoke |
| FE0-QA | FRONTEND-DESIGN task acceptance + Student/Auth product | Frontend verification contract | COVERED | Playwright discovered browser smoke and documented install prerequisite |

## Spec findings and proposed fixes
| Finding / severity | Evidence and impact | Proposed fix / target | Owner | Resolution |
| --- | --- | --- | --- | --- |
| FE-S1 / medium | Student §5/7 formerly table/card only; requested weekly display could suggest unsupported range API. | Scoped design extends canonical product/technical; use presentation week, only moduleId/date API. | Phuc | Resolved in FRONTEND-DESIGN Student mapping and canonical links. |
| FE-S2 / high | Shared shell absent; implementing roles with fixture login risks identity/CSRF policy violations. | FE1 exact real Auth session/CSRF/error contract; private-memory generation invalidation. | Auth policy remains Kok Seng | Resolved at spec level; implementation/tests/review pending. |
| FE-S3 / medium | Existing agenda action is future scope; exposing it would lead to unfinished AI. | Explicit FE2 omission, preserve future requirements. | Phuc | Resolved in Student Product/Technical scoped notes. |
| FE-S4 / medium | No executable frontend harness; missing imports cannot count as behavior red. | FE0 explicit harness prerequisite, setup-only test-writer permission and meaningful passing baseline. | Phuc | Resolved in Plan FE0 and this packet. |
| FE-S5 / medium | Broad upcoming wording could accidentally hide started ACTIVE or infer cancellation client-side. | Exact status requests; server cancellable is sole eligibility authority. | Existing Student policy | Resolved in FRONTEND-DESIGN mapping. |
| FE-S6 / medium | Logout network failure cannot honestly claim server invalidation; mutation lost responses may follow commit. | Clear UI on attempt, show uncertain server sign-out with retry; no automatic mutation replay. | Existing session/mutation policy | Resolved in FRONTEND-DESIGN session and mutation behavior. |

## Spec readiness
Verdict: READY_FOR_TESTS for stu-fe0-foundation acceptance only, on the exact hashes above.
No unresolved blocking contract findings; package/browser/network prerequisites
must be established by FE0 before dependent execution. Shared-owner review is a
merge gate and remains pending. Automated spec comparison is not software proof.
Recheck: compared canonical Shared/Auth/Student product/technical/integration
contracts against actual controller/input/projection/error source at base; recorded
SGT/uppercase/repeated statuses/exact bodies and existing SH-AUTH/SH-WEB semantics.
Fresh brainstorming builder check read the scoped design and reported no material
unresolved planning question; it is a design check, not independent code/security review.

## Tasks and acceptance-to-test mapping
- FE0-RUN: planned npm.cmd ci/test/build, React DOM harness smoke; test filenames to be recorded by test writer.
- FE0-PROXY: planned dev /api proxy and relative production URL smoke; test filenames to be recorded by test writer.
- FE0-QA: planned Playwright discovered browser smoke and documented install prerequisite; test filenames to be recorded by test writer.
Test state: not written in this spec stage; do not claim tests passed.

## Verification evidence
Working directory: D:/cs3227/CS3227-2610-MP3.
- `git rev-parse HEAD`: exit0, e277e583f1319320ed5f44e1944ac85296bb7d15.
- `git status --short` before spec writes: exit0, clean.
- `node --version`: exit0, v24.21.0; `npm.cmd --version`: exit0, 12.0.2.
- `npm --version` invokes blocked npm.ps1; use npm.cmd without weakening policy.
- `Get-FileHash -Algorithm SHA256` on listed canonical paths: exit0; hashes above.
- Actual source/contracts read directly; no logs/transcripts used as inputs.
Product tests/build/browser/real backend journey: not run by spec analyst.
Readiness means consistent testable scope, not passing software.

### Receiver receipt
Receiver role / model: pending fresh test writer.
Checked task/revisions/prerequisites: pending.
Receipt: pending; ACCEPTED/RETURNED must be recorded by receiving role.

## Independent review and human verification
Independent code/security review: pending fresh reviewer after implementation.
Human keyboard/responsive/journey verification: pending Phuc.
Completion requires relevant tests/build, independent review, owner merge review
where applicable and human verification. No commit or merge is authorized by this record.


## Handoff test-writer-1 (FE0 setup baseline)
Producer: fresh test writer / Codex GPT-6. Recipient: fresh FE0 implementer.
Status: READY_FOR_IMPLEMENTATION for setup-only FE0; no feature readiness claim.
Receiver receipt: ACCEPTED analyst-1. Checked all nine canonical SHA-256 values directly with Get-FileHash; each matches analyst-1, including FRONTEND-DESIGN 3917AF4CD2709934383E03754A302ABA15D543A913B82EAA15A25B8F9D3F80DE. Canonical scope permits setup-only inline React sentinel. FE1/FE2 remain downstream and have no executable feature red yet.
Base commit remains e277e583f1319320ed5f44e1944ac85296bb7d15; no commit. Complete changes: git diff plus untracked src/frontend and workflow artifacts; exclude ignored node_modules/build/browser reports.

| Output (under src/frontend/) | SHA-256 | Purpose / acceptance |
| --- | --- | --- |
| package.json | 7D1443F243169FC0636318D59DCFD44B5633A1DE1848CF0C91084EAB97CB95A5 | Exact pinned package versions and commands / FE0-RUN |
| package-lock.json | 522E3BC0CCCF79F4B90AF1EE534B0DD58BCCEA5CC64723C124A8E2E7C09464E9 | npm reproducibility / FE0-RUN |
| tsconfig.json | A3F2DFA3B5973773567BB4CB9A712FE1F83751205CCEAF9D12F628DB8834487E | Strict ReactTS compilation |
| vitest.config.ts | E00BC5927BE93AC5E5D3DAA131AE51B4DAD309EF4053047D507020F56F074387 | jsdom/RTL discovery |
| playwright.config.ts | 8165F9FB3562F611B9E13CDCD29CCE0E4920E19F7726AFBA8867D8F9031DA300 | Edge browser QA/dev-server command / FE0-QA |
| src/test/setup.ts | 268E7200D092DAA3225EB06790ED72842E5D3234A53980C90FF463D7B81528D3 | DOM matchers and cleanup |
| src/test/harness.test.tsx | 39EE66143D4C0B16CE6903AB96F643BF4ED06B3410F9A9B3459C2BE17809DDED | FE0-RUN actual DOM labelled input, Tab focus, typing, keyboard submission and status update |
| e2e/shell.spec.ts | CE3A04B0159C4126101B7D4E4B857630C8C1ECC39F534EC7C9EF392A4B8280F3 | FE0-QA browser h1 + pageerror smoke |
| .gitignore | 463B5F9362420AAA6BBF70C200B7CEF361AF47F34E60E6767851406F3091A39A | Exclude generated dependencies/artifacts |

Verification (working directory src/frontend; Node24.21.0/npm.cmd12.0.2):
- npm.cmd install: exit0; added110 packages, audited111, zero vulnerabilities. Network/cache escalation used for dependency installation. Replaced initial latest specifiers with exact installed versions in package and lock metadata; no dependency contents changed.
- npm.cmd test: exit0, Vitest5.0.5; 1 file / 1 meaningful React DOM keyboard test passed, duration1.58s. This is an existing passing setup baseline, not manufactured feature red.
- npx.cmd playwright test --list: exit0; discovers one shell.spec.ts test. Browser execution NOT RUN yet: application shell intentionally belongs to next implementer. Missing app is not recorded as red.
- npm.cmd ci/build/dev/proxy/browser execution: pending FE0 implementer. FE0-PROXY mapping: implementer verifies forwarding with local synthetic backend and documents relative production requests; no claim from this sentinel.

Exact next action: create only importable App.tsx/default export, main.tsx, index.html, local Bootstrap/style bootstrap, vite-env and Vite /api proxy (default127.0.0.1:8080, optional server-side target) and README. Shell has UniChope h1 and truthful setup status, no auth/booking implementation. Run clean npm.cmd ci, npm.cmd test, build, browser smoke with installed Edge channel msedge, and actual dev proxy check; record evidence. Fresh independent reviewer follows. Browser prerequisites/override PLAYWRIGHT_CHANNEL and SPA fallback must be documented. Product/keyboard responsive journey, real backend synthetic journey, shared-owner merge review and Phuc verification remain pending.

## Receiver implementer-1
Receiver: fresh FE0 implementer / Codex GPT-6. Receipt: ACCEPTED test-writer-1 and analyst-1. Verified all nine canonical hashes and nine harness output hashes against actual files; inspected sentinel and browser test, package/config sources. Setup-only exception applies; no feature implementation or backend changes. Entry task is minimal importable App/default export, entry, Bootstrap, proxy and README with ci/test/build/browser/proxy verification. Existing baseline is passing setup, not feature red.


### Handoff test-writer-2: shell smoke refinement
Supersedes only test-writer-1 e2e/shell.spec.ts revision: SHA256 18F36AD0CE5881EE4C04821EED444E9FBDADFD72C0770D046E1DE5705D98A593. Smoke now independently checks a visible single page h1 and exact visible UniChope branding, without requiring Login/Student page titles to be the product name. Canonical page titles may describe the current task. npx.cmd playwright test --list, src/frontend, exit0 discovers1 test. Browser execution remains pending implementer shell. Other setup baseline unchanged.

## Handoff implementer-1 (FE0 foundation)
Producer: fresh FE0 implementer / Codex GPT-6. Recipient: fresh independent reviewer.
Status: IN_REVIEW for FE0 only. Supersedes no prior evidence. Analyst-1 canonical hashes remain unchanged; test-writer-1 baseline accepted above. Browser smoke refinement test-writer-2 checked directly at SHA-256 18F36AD0CE5881EE4C04821EED444E9FBDADFD72C0770D046E1DE5705D98A593.
Task: FE0-RUN / FE0-PROXY / FE0-QA only. Added importable default App shell, React/Bootstrap entry, semantic skip link/main/h1/status, Vite same-origin proxy and setup README. No auth, Student behavior, account fixtures, backend changes or commits.

| Output (under src/frontend/) | SHA-256 |
| --- | --- |
| index.html | EFDFEF6716C05E6E591EE9B25CF79433C2551FE6CAEB852BBAA66A644E719C24 |
| src/App.tsx | 00490948AF3FC23707D9162A2256FC78A83BFE48908BC959C30265065A5A9A42 |
| src/main.tsx | BA6B140C8A74A56B5F0A0E6280E5AB36CE941568595775B5C8A1DB2C958BA684 |
| src/styles.css | 99796739016699BFB34EC9178EF0E49918D7F6E211BB7DC26AA84A08820371FB |
| src/vite-env.d.ts | 65996936FBB042915F7B74A200FCDDE7E410F32A669B1AB9597CFAA4B0FADDB5 |
| vite.config.ts | 842071AEBDDA1ED54A07200061C9EB2A73C3EA60E36CEFFF2F04E48BE1806BE3 |
| README.md | E94B792DB680B41944B064DAA9C85692A6356D424B72712CBB357A23FD45416F |

Reproducible verification, working directory D:/cs3227/CS3227-2610-MP3/src/frontend, Node24.21.0/npm12.0.2, unchanged upstream package/test artifacts except recorded test-writer-2 browser refinement:
- `npm.cmd ci`: exit0, added110 packages/audited111, zero vulnerabilities. Network/cache escalation used.
- `npm.cmd test`: exit0, 1file/1test passed (1.66s). Meaningful inline React DOM keyboard sentinel, not feature-red evidence.
- `npm.cmd run build`: exit0, strict `tsc --noEmit` then Vite8.3.5; 17modules transformed, built314ms. Dist index0.44kB/CSS230.59kB/JS219.91kB. Browser code contains no environment credentials and no API requests at this setup-only stage; future production requests must remain relative `/api`.
- `npm.cmd run test:browser`: first restricted sandbox run emitted test ok but did not terminate; interrupted exit1, NOT a completed passing run. Investigation read actual Playwright runner/process source: Windows webserver cleanup uses taskkill. Same command/config with process-control escalation: exit0, 1passed (3.3s), installed isolated Edge, updated test-writer-2 shell smoke, no runtime pageerrors. No acceptance test weakened or config changed.
- Actual dev proxy: `$env:API_PROXY_TARGET='http://127.0.0.1:18080'; npm.cmd run dev -- --port 5174 --strictPort` started Vite218ms. Node fetch to `http://127.0.0.1:5174/api/auth/me` returned401 JSON codeUNAUTHENTICATED; `/api/auth/csrf` returned200 JSON string headerName/token. Script validated both shapes and exited0; token values were not printed. Backend was the coordinator's isolated real app-api fixture. Temporary Vite server stopped after check. This proves forwarding to override; default127.0.0.1:8080 is explicit Vite configuration, not a second live-backend claim.
- `npm.cmd test -- src/test/harness.test.tsx`: exit0, 1/1passed (1.79s), isolated baseline as feature writer adds separate files afterward.
- `git diff --check`: exit0 (line-ending advisory for pre-existing required log append only).

Base commit remains e277e583f1319320ed5f44e1944ac85296bb7d15; complete review diff is tracked `git diff` plus untracked src/frontend and workflow/student/FRONTEND-DESIGN.md and stu-fe* records. No commit. Package/config/test hashes remain those of test-writer-1 except shell smoke test-writer-2. Generated node_modules/dist/browser reports ignored. Feature writer is permitted to add new FE1/FE2 tests after this source freeze; those are outside this FE0 revision and may make aggregate npm test red before feature implementation.

Acceptance mapping: FE0-RUN package/lock, entry and shell + ci/RTL/build; FE0-PROXY vite.config relative/api target + actual backend forwarding; FE0-QA Playwright config/browser smoke + README setup/permissions. Independent review pending. Human keyboard/responsive/feature journeys, real synthetic login/book/list/cancel and affected-owner merge review are pending downstream, not proven by shell smoke. No live browser session or backend is claimed as stopped beyond this implementer's own temporary Vite process.
Next exact action: fresh reviewer verifies FE0 code/test/spec revisions and reruns proportionate checks; fresh feature test writer may use default App export for real failing behavioral tests. No FE1/FE2 product implementation in this session.

## Independent review reviewer-1 (FE0 only)
Reviewer: fresh independent reviewer/security role / Codex GPT-6; 22 October 2026.
Scope: standard-path FE0-RUN / FE0-PROXY / FE0-QA; no source/test implementation produced by this session. Read AGENTS, NOTE, HANDOFF and reviewer prompt; applied frontend-ui-engineering and verification-before-completion. Derived checklist from canonical FRONTEND-DESIGN, Shared/Auth/Student contracts before upstream conclusions.

Receiver receipt: ACCEPTED implementer-1 for FE0. Base verified e277e583f1319320ed5f44e1944ac85296bb7d15. All nine analyst-1 canonical SHA-256 values, all nine test-writer-1 harness values (except explicitly superseded shell smoke), all seven implementer-1 output values and test-writer-2 shell SHA-256 18F36AD0CE5881EE4C04821EED444E9FBDADFD72C0770D046E1DE5705D98A593 match actual files. Exact hashes remain in those tables; this receipt adopts those verified revisions, not future edits. Inspected complete listed foundation source/config/tests and tracked workflow diff; untracked FE1/FE2 tests outside FE0 verdict.

Independent checklist/evidence:
- FE0-RUN: actual React/TS/Vite/Bootstrap entry and truthful accessible shell; strict build; labelled-control keyboard sentinel with real React events. Package roots match lock metadata; every resolved dependency source is npm registry. No production fixture/account state or API credentials in foundation. Build output/import tree contains only shell/React/Bootstrap, not test fixtures.
- FE0-PROXY: server-only API_PROXY_TARGET and explicit default http://127.0.0.1:8080; no VITE_* secret or absolute browser API URL; same-origin production and API-preserving SPA fallback documented. Fresh actual Vite server on5175 with override18080 forwarded /api/auth/me =>401 JSON UNAUTHENTICATED and /api/auth/csrf =>200 valid headerName/token shape; values withheld; own server closed in finally. Default8080 checked in source, not claimed as second live backend.
- FE0-QA: installed isolated Edge, reproducible browser script/config and setup instructions; current smoke independently checks heading/visible UniChope and zero pageerrors. Shell/keyboard/semantic styling checked in source; feature responsive/modal/a11y journeys remain downstream.
- Analyst resolutions FE-S1..FE-S6 checked against current canonical scoped mapping/session/error/mutation rules: no unresolved contradiction affecting FE0; FE1/FE2 compliance remains unreviewed.

Fresh command evidence (cwd src/frontend unless stated; actual exit0):
- npm.cmd test -- src/test/harness.test.tsx: 1 file /1 test passed, Vitest5.0.5,1.56s at20:07:08.
- npm.cmd run build: tsc --noEmit then Vite8.3.5,17 modules,235ms; dist index0.44kB/CSS230.59kB/JS219.91kB.
- npm.cmd run test:browser: process-control escalation for Windows child cleanup;1 passed(2.8s), shell440ms, runner exited0. Only NO_COLOR/FORCE_COLOR environment warnings.
- npm.cmd audit --json: advisory service query exit0, vulnerabilities={} and total0; this is dependency advisory evidence, not a complete security audit.
- Node ESM stdin script using Vite createServer on5175, override18080, fetch/assert /me and /csrf then finally close: exit0, valid proxy shapes. Earlier shell-quoted node -e probes failed syntax before execution and are not counted as successful checks.
- Root: git diff --check -- . ':!logs': exit0; git diff --name-only -- src/backend: no changes; Node lock-root/registry-source validation exit0.

Verdict: PASS for the exact FE0 foundation revision above; no blocking acceptance/security findings or lower-severity follow-ups. npm ci was not repeated because unchanged package/lock hashes and installed dependency build/browser evidence already establish this limited review; implementer's ci result remains reported upstream, not a fresh reviewer run. Full npm test intentionally not run while independent FE1/FE2 red tests are added. No frontend feature/real login-book-list-cancel integration, full accessibility audit, deployment, owner approval or human acceptance claim.
Next: proceed with FE1/FE2 stages; reviewer remains independent of implementation. Phuc's keyboard/visual journey and affected-owner merge review remain pending. No commit/merge performed.


## Handoff analyst-2: Claude specification consistency check (FE0 scope)
Change ID / task: stu-fe0-foundation; FE0-RUN, FE0-PROXY, FE0-QA. Same check is appended to stu-fe1-auth-ui (analyst-3) and stu-fe2-booking-ui (analyst-3); findings are shared, verdicts are per change.
Producer role / actual tool and model: spec analyst / Claude Code (Claude Agent SDK fresh subagent session, no inherited conversation, transcript or summary) / Claude Opus 5.5 (claude-opus-5-5); 22 October 2026.
Recipient role: fresh independent Claude reviewer/security session (stu-fe2-booking-ui/CLAUDE-REVIEW-HANDOFF.md, second stage), then the routed roles listed per finding.
Stage status / supersedes: specification consistency check requested by CLAUDE-REVIEW-HANDOFF.md SHA256 6377AFA10719E3DD4E36622C35CA4340F98918EDDCF43D1DF366F54EF49C8241. Supersedes no earlier section; it adds an independent Claude consistency check over the Codex analyst-1 packet and analyst metadata refresh 2, which remain preserved. No canonical spec, test or product code was edited by this session. Nothing is marked COMPLETE. No commit, push, deletion or log write.
Method: read CLAUDE.md, AGENTS.md, NOTE.md, HANDOFF.md, agents/spec-analyst.md and the packet; derived requirements directly from the nine canonical documents and actual backend source BEFORE reading the fe0/fe1/fe2 analyst packets; then verified recorded hashes/supersessions and compared. logs/ was not read or searched.

### Receiver receipt
Receipt: ACCEPTED for the specification-consistency stage.
Checked base: `git rev-parse HEAD` = e277e583f1319320ed5f44e1944ac85296bb7d15; `git diff HEAD -- src/backend` empty (backend unchanged).
Canonical inputs (sha256sum, actual current files) all match the records:
| Artifact | SHA-256 | Recorded where / result |
| --- | --- | --- |
| workflow/student/FRONTEND-DESIGN.md | 3917AF4CD2709934383E03754A302ABA15D543A913B82EAA15A25B8F9D3F80DE | analyst-1 / match |
| workflow/student/PRODUCT-SPEC.md | 750433038415D5AF0832D4E506FB669C1D08A9A583FF388C4DEFC4F9FD690B86 | analyst-1 / match |
| workflow/student/TECHNICAL-SPEC.md | 97C2B915682BEC9EEC61BF29CCE665EF2088458C7DDE78E349CC47A88366F698 | analyst-1 / match |
| workflow/student/PLAN.md | 9C5EECA85D36CDFED9D6F5ABD89176905F40556727F01AC9303D2C4EC4716FA5 | analyst metadata refresh 2 (FE1/FE2 records) supersedes analyst-1 E21093DF...; match |
| workflow/auth/PRODUCT-SPEC.md | BE2F2F59B4E0ABEF49081A614DC77FA2DFB5BFDEB114C4A3B49AC38D2A73A637 | analyst-1 / match |
| workflow/auth/TECHNICAL-SPEC.md | 91949EC9793C1747F99C87D86FF74C941700BB9D0133F6204582B0C529E689E9 | analyst-1 / match |
| workflow/auth/PLAN.md | 5F40F9BE474C96FB0D22C89523367F20E8586AF59062B6DF278C43F6C1CD6ACB | analyst-1 / match |
| workflow/SHARED-TECHNICAL-SPEC.md | 5F92FF1F89B0171232BF3FF49C8517A9743EAEF2602B27F1C5D38EAFDE9912A5 | analyst-1 / match |
| workflow/INTEGRATION-SPEC.md | F7893269A4F5C3F1F4E106F3C62225DD3E8D3B4E3C0C31B6CA4A5CE61FAD0604 | analyst-1 / match |
Explicit supersessions verified against actual files: FE0 test-writer-2 e2e/shell.spec.ts 18F36AD0...A593 (match); FE1 test-writer-2 auth.test.tsx 2E5DBA75...03D3 (match); FE2 test-writer-2 student.test.tsx 583597A3... superseded by test-writer-3 38BFC6CC...5B88 (match); implementer-2 supersedes implementer-1 artifacts: frontend-manifest-r2.json 9343DBE123F27D56100604D4932F6AD1D74D2D299A87CE6AF7D9952433B8D494 (match), all 40 manifest entries rechecked by script against actual files, 0 mismatches (30 frontend files = all untracked non-generated src/frontend files, plus 10 workflow files); api.ts 25F8CDBD4A545DEE968695983C926F49DE78D32B3D24B2B8CB33FB503C0237F6 (match); base-to-worktree-r2.patch 52108D6D60EAF8C305D7B0DF70FC4B87F2D767393FAE6A555EEC5851E73CBCAD (match); fe-r1-red.json FD9AA8E63CE5F060E7462373348250F2CCEFC470869C27FCFE2AAFCC9D2B9CB0 (match). Historical frontend-manifest.json 1A448937...9458 and base-to-worktree.patch 0F262085...362A still match and remain historical. FE0 harness/config hashes (package, lock, tsconfig, vitest/playwright config, setup, harness test, .gitignore, index.html, main.tsx, vite-env, vite.config) still match FE0 records; FE0 App.tsx/styles.css/README.md were intentionally superseded by FE1/FE2 (current values are in manifest-r2).
Evidence hashes not recorded by implementer-2, recorded here (sha256sum): fe-r1-green.json B791484C27ADC545575E6109A35ED9BD633A73507387B6A72C07609D84DF959B; unit-r2.json 56698A3D432C9B90BC7183EC7D998305DEE21EDD09986F6891965354A83E2388 (parsed: 49 total/49 passed/0 failed, success true); build-r2.txt 3FBD8219A5D9FBF6FBBF207A2236D937AC311283B717C81C21F0DCEE04EACD69; browser-r2.txt 8195602E6091AFACD0327E4F92206E154D2A1FFAE919E0F8B425B7A3B0BBD959; live-post-fix/live-browser-smoke.json 943452F7AF918F602A31401A980D69EDF1B84DD5EB732EFFBE4E555F14227470 (byte-identical to pre-fix live-browser-smoke.json). Product commands were NOT rerun by this analyst; evidence was inspected only.

### Requested scope derived directly (before reading analyst packets)
In scope: real Login against existing /api/auth; stored-role routing; honest Tutor/Admin placeholder workspaces; NUSMods-inspired UniChope Student weekly Monday-Sunday timetable with filter sidebar (desktop) and chronological list (mobile); book/view/cancel via existing Student endpoints. Out of scope: registration/reset UI, AI/agenda, Tutor/Admin features, deployment, backend/policy changes. Owner's direct layout choice is recorded in FRONTEND-DESIGN lines 8-15 and referenced from Student Product header, Student Technical section 7, Shared Technical section 6, Auth Product "Decisions and integration" and Auth Technical "HTTP contract". Affected-owner (Kok Seng, Huanghao) review remains a merge gate, not recorded.

### Frontend contract versus actual backend (src/backend at e277e583, unchanged)
Consistent, no finding: GET /api/auth/csrf returns {headerName, token} from the request CsrfToken (AuthController lines 44-49; HttpSessionCsrfTokenRepository, ApiSecurityConfiguration lines 18-21) and the UI uses the returned header name (api.ts lines 31, 59). Login requires exactly {email,password} strings with application/json (AuthController lines 128-145), rotates the session and clears the CSRF token (lines 58-68), so the UI must and does re-acquire CSRF after login (App.tsx lines 35-36). Logout accepts empty or {} (lines 95-122) and returns 204. Anonymous /me gets 401 UNAUTHENTICATED from the entry point (ApiSecurityConfiguration lines 64-71). Login 401 INVALID_CREDENTIALS, 429 AUTH_RATE_LIMITED with integer Retry-After (AuthExceptionHandler line 166; LoginLimiter line 52), 503 TEMPORARILY_UNAVAILABLE on deadline/worker exhaustion (AuthenticationService lines 42, 77, 92). CSRF denial is 403 CSRF_INVALID (lines 72-81). All /api responses carry Cache-Control no-store (ApiResponseFilter line 15). Student routes, strict queries (moduleId/date once; repeatable status; no week/range parameter), exact bodies {slotId} and {}, 201 + Location, 409 SLOT_UNAVAILABLE/BOOKING_OVERLAP, 404 NOT_FOUND, 409 INVALID_STATE, 503 TEMPORARILY_UNAVAILABLE, 401/403 ACCOUNT_INACTIVE/403 FORBIDDEN (StudentController, StudentApiInput, StudentHttpService lines 43-78, StudentExceptionHandler lines 108-119) match FRONTEND-DESIGN lines 117-164 and Student Technical sections 3-3.3. student-api is in app-api (application.properties line 1). UI request paths/filters (StudentWorkspace.tsx lines 19-24) send no week parameter. Gap found: CS-01 (shared CurrentAccountFilter also governs the public auth endpoints).

### Requirement-to-technical coverage (spec support, not implementation proof)
| Requirement / acceptance ID | Product source | Technical / integration source | Coverage | Planned validation / note |
| --- | --- | --- | --- | --- |
| Real Login, no role selector | FRONTEND-DESIGN 59-61, 81-87; Auth AUTH-LOGIN-01 | Auth Technical HTTP contract; Integration section 2; SH-AUTH-01..03 | COVERED | FE1-LOGIN tests; CS-01(b) clarification |
| Tutor/Admin placeholders | FRONTEND-DESIGN 60-61; Auth Product decisions | FRONTEND-DESIGN routes 83-86 | COVERED | FE1-RESTORE/FE1-UI tests; live smoke screenshots |
| Weekly timetable + filter sidebar + mobile list (owner choice) | FRONTEND-DESIGN 8-15, 44-58; Student Product header and section 5 | Student Technical section 7; FRONTEND-DESIGN 117-135 (presentation week, no range API) | COVERED | FE2-BROWSE/FE2-UI; screenshots inspected by this analyst (desktop seven days, overlap, cross-midnight 23:30-00:15; 320px list) |
| Out-of-scope exclusions | FRONTEND-DESIGN 14-15, 164; Student Product header; Auth Product decisions | Student Technical section 7 | COVERED | FE2-UI no-agenda test |
| FE0-RUN / FE0-PROXY / FE0-QA | FRONTEND-DESIGN 170-172, 186-200 | Shared section 1; SH-AUTH-05; FRONTEND-DESIGN 73-79 | COVERED | FE0 reviewer-1 PASS evidence |
| FE1-LOGIN | FRONTEND-DESIGN 173 | Auth HTTP contract; SH-AUTH-03/06 | COVERED | CS-01(b) |
| FE1-RESTORE | FRONTEND-DESIGN 174 | FRONTEND-DESIGN 81-87 | COVERED with gap | CS-02 restoration failure state |
| FE1-LOGOUT | FRONTEND-DESIGN 175 | FRONTEND-DESIGN 94-98; Auth AUTH-SESS-01 | COVERED with ambiguity | CS-01(c) |
| FE1-DENIAL | FRONTEND-DESIGN 176 | FRONTEND-DESIGN 100-108; Integration section 2; Student Technical 3.2 | COVERED with ambiguity | CS-01(a) (FE-R1 rests on interpretation) |
| FE1-PRIVACY | FRONTEND-DESIGN 177 | SH-AUTH-02, SH-WEB-02/03; FRONTEND-DESIGN 110-115 | COVERED | existing tests |
| FE1-UI | FRONTEND-DESIGN 178 | FRONTEND-DESIGN 22-69 | COVERED | CS-06 contrast criterion |
| FE2-BROWSE | FRONTEND-DESIGN 179; STU-SLOT-01 | Student Technical 3/3.1; SH-DATA-01 | COVERED | CS-04 |
| FE2-BOOK | FRONTEND-DESIGN 180; STU-BOOK-01 | Student Technical 3/3.3; Integration section 9 | COVERED | CS-05 |
| FE2-LIST | FRONTEND-DESIGN 181; STU-BOOK-02 | Student Technical 3 (repeatable status, cancellable) | COVERED | existing tests |
| FE2-CANCEL | FRONTEND-DESIGN 182; STU-BOOK-03 | Student Technical 3.3 | COVERED | existing tests |
| FE2-RACE | FRONTEND-DESIGN 183 | SH-WEB-03; FRONTEND-DESIGN 110-115, 158-161 | COVERED | existing tests incl. FE-R1 cases |
| FE2-UI | FRONTEND-DESIGN 184 | FRONTEND-DESIGN 44-69 | COVERED | CS-03, CS-06 |
Checklist areas: API/UI and auth/privacy examined (findings below). Data/concurrency: no new writes; backend PostgreSQL suites remain the evidence (FRONTEND-DESIGN 208-209). Architecture/ownership: frontend-only, shared shell authorized by Phuc, owner merge review pending. AI: not applicable, excluded and no agenda action. Feasibility: Node/Edge prerequisites documented. UNVERIFIED and non-normative: FRONTEND-DESIGN lines 31-42 claim of directly viewed NUSMods screenshots (temporary, not retained); it only informs visual density.

### Spec findings and proposed fixes
| Finding ID / severity | Evidence and impact | Proposed fix / target | Decision owner / routing | State |
| --- | --- | --- | --- | --- |
| CS-01 / medium | CurrentAccountFilter (platform/web/CurrentAccountFilter.java lines 113-145) runs on every request whose session holds an authenticated context, including permitAll /api/auth/csrf, /login and /logout (ApiSecurityConfiguration lines 36-47, 83-85): stale/revoked session gives 401 UNAUTHENTICATED or 403 ACCOUNT_INACTIVE and invalidates the session; storage failure gives 503. FRONTEND-DESIGN only says CSRF acquisition failure "permits retry" (line 93), logout "transport fails" (95-98) and session-denial handling for "protected" requests (100). FE-R1 (api.ts 25F8... lines 40-51, four FE-R1 tests) relies on reading a protected mutation's CSRF preflight as protected; Login-with-stale-cookie and logout-of-revoked-session outcomes are implementer choices with no test oracle (currently generic "temporarily unavailable" on Login; "server sign-out could not be confirmed" on logout although the server already invalidated the session). | Add to FRONTEND-DESIGN "Transport and session contract": (a) CSRF acquisition performed for a protected mutation inherits protected 401/ACCOUNT_INACTIVE handling (clear, Login, announce, zero POST); (b) on Login, 401/403 ACCOUNT_INACTIVE from /csrf, or 403 CSRF_INVALID from /login (e.g. 30-minute idle anonymous session), means the prior session was reset: clear token, show generic "try again" guidance, no automatic login retry; (c) on logout, 401/403 ACCOUNT_INACTIVE from /csrf or /logout means the server session is already invalid: recommended to treat as signed out and acquire anonymous CSRF without the "could not be confirmed" warning (alternative: keep the conservative warning; both are fail-safe). Then add FE1-LOGIN/FE1-LOGOUT tests for (b)/(c). | Phuc decides (b)/(c) UX; Kok Seng reviews Auth semantics. Route: spec analyst applies after decision -> test writer -> implementer only if behavior changes. | OPEN, non-blocking: current code is fail-safe under either reading and (a) matches FE-R1. |
| CS-02 / low | /me can return 403 ACCOUNT_INACTIVE, 503 TEMPORARILY_UNAVAILABLE (CurrentAccountFilter lines 137-144), network or malformed. FRONTEND-DESIGN lines 81-87 define success, anonymous and pending only. App.tsx lines 15-21, 46 chooses: silent 401, inactive message, otherwise "Unable to open your workspace" + Retry; no test covers non-401 restoration failure (auth.test.tsx titles). | Add to FE1-RESTORE: 401 = anonymous without expiry message; ACCOUNT_INACTIVE = Login with inactive message; 503/network/malformed = no workspace and no anonymous assumption, retryable "could not check session" with explicit retry. | Phuc. Route: spec analyst -> test writer (new restoration-failure test). | OPEN, non-blocking (current behavior matches the recommendation). |
| CS-03 / low | FE2-UI requires native buttons/named regions but not distinguishable action names. Consultations.tsx line 6 renders bare "Book"/"Cancel booking" inside unlabelled article elements; live-post-fix timetable screenshots show many identical "Book" buttons. Screen-reader button lists lose module/time context; axe zero violations does not detect this. | Add to FRONTEND-DESIGN visual/interaction contract and FE2-UI: each Book/Cancel control's accessible name or description includes module code and SGT date/start time (e.g. aria-describedby to the event details or an article label). | Phuc. Route: spec analyst -> test writer (getByRole name) -> implementer. | OPEN, non-blocking. |
| CS-04 / low | FRONTEND-DESIGN lines 133-134 "malformed/unknown UI parameters are removed or normalized" is ambiguous for a well-formed moduleId absent from active /modules (deactivated module, old bookmark); backend returns [] (Student Technical section 3). filters.ts line 8 keeps it, and the module select then has no matching option, so the control can appear as "All modules" while results are filtered (misleading empty state). | Specify: after modules load, a moduleId not in the active list is removed with the existing invalid-filter notice (or rendered as an explicit "Unavailable module" option). | Phuc. Route: spec analyst -> test writer -> implementer. | OPEN, non-blocking. |
| CS-05 / low | FRONTEND-DESIGN line 93 (acquisition failure: retry, no mutation sent) differs from lines 141-143 (lost response after a possible commit: explain uncertainty). api.ts safeError(mutation=true) line 92 tells the user the change "may have completed" when CSRF acquisition failed before any POST. This gives reviewer finding FE-R2 an explicit spec basis (copy accuracy, not security). | No spec change needed; implementer distinguishes pre-POST acquisition failure copy (nothing submitted, retry) from post-POST uncertainty; test writer adds a pre-POST 503 copy case. | Phuc. Route: test writer -> implementer; reviewer decides closure with FE-R2. | OPEN (tracked with FE-R2), non-blocking. |
| CS-06 / low | FRONTEND-DESIGN line 29 "must satisfy contrast" has no measurable criterion; evidence relies on axe WCAG2A/AA scans. | State WCAG 2.1 AA: 4.5:1 normal text, 3:1 large text, UI components and focus indicators, in FRONTEND-DESIGN visual contract. | Phuc. Route: spec analyst; reviewer/browser QA. | OPEN, non-blocking. |
| CS-07 / low | Stale canonical status text: Student Technical section 1 line 24 says shared React shell/API client "Not yet assigned ... Out of scope", contradicting FRONTEND-DESIGN Authorization, Shared Technical section 6 and PLAN S0 line 85. PLAN lines 3-7 and 58-65 report 45 tests and pre-FE-R1 state while current evidence is r2 (49/49) with reviewer-2 CHANGES_REQUIRED awaiting re-review. | Metadata refresh of both lines (already authorized scope; no behavior change), batched after the independent review so manifest-r2 is not invalidated mid-review. | Phuc. Route: spec analyst. | OPEN, non-blocking; deliberately not edited now. |
| CS-08 / low | Traceability: FE0 record still lists PLAN E21093DF... and FE0 App/styles/README hashes without a pointer to analyst refresh 2 and the FE1/FE2 manifests that supersede them; implementer-2 did not record hashes for fe-r1-green/unit-r2/build-r2/browser-r2; live-post-fix smoke JSON contains no source revision or timestamp and is byte-identical to the pre-fix run, so it cannot itself bind to api.ts 25F8... | Hashes now recorded in this section's receipt. Future reproducer output should embed the manifest hash and run time. | Phuc. Route: implementer/coordinator for future evidence; reviewer weighs live evidence accordingly. | PARTLY RESOLVED by this receipt; non-blocking. |
| CS-09 / info | The packet and FE1/FE2 records state that user-supplied AGENTS instructions require Claude for specification and review and override NOTE line 11 (independence from fresh sessions, not model). Repository AGENTS.md/CLAUDE.md contain no such requirement; it cannot be verified from artifacts. | Phuc confirms the model requirement in a shared artifact. If required, this section is the Claude specification check; Codex analyst-1 remains historical. | Phuc only. | QUESTION. |
| CS-10 / info | Backend denies non-/api paths (ApiSecurityConfiguration line 55) and serves no SPA; FRONTEND-DESIGN lines 78-79 assign SPA fallback to the deployment server. Deployment is out of scope. | None for FE0-FE2; record at deployment planning. | Deployment owner (Shared Technical section 2: team to assign). | DEFERRED, no readiness impact. |
No high or critical specification findings. Existing FE-S1..FE-S6 resolutions were rechecked against current canonical text and hold; FE-R1 is a review finding outside this stage's closure authority.

### Spec readiness (FE0)
Verdict: READY (specification consistency for FE0-RUN/FE0-PROXY/FE0-QA on the canonical hashes in the receipt above).
Applicable findings: CS-08 (record traceability, low), CS-09 (question), CS-10 (deferred). None affects FE0 contracts; FE0 reviewer-1 PASS remains tied to its own recorded revision, and the later App/styles/README revisions are governed by the FE1/FE2 manifest-r2, not by this FE0 verdict.
Owner decisions required: none for FE0. Recheck evidence: requirements derived first from FRONTEND-DESIGN 73-79, 170-172, 186-200 and Shared section 1/SH-AUTH-05; vite.config.ts 842071AE... (server-only API_PROXY_TARGET, default 127.0.0.1:8080), playwright.config.ts 8165F9FB... (msedge default, PLAYWRIGHT_CHANNEL override) and README match. Not run by this analyst: npm ci/test/build/browser.
Next agent: fresh independent Claude reviewer per CLAUDE-REVIEW-HANDOFF.md (FE1/FE2 code/security review). No COMPLETE status; Phuc human verification and owner merge review pending.

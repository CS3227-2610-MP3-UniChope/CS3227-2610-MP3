# Claude review handoff: Login and Student booking frontend

Required recipient: Claude in a fresh session. This packet is prepared by Codex
GPT-6; it does not claim that Claude has performed specification or review work.
The user's supplied AGENTS instructions require Claude for specification and
independent review. They take precedence over the same-model option in NOTE.md.
Both required Claude gates remain pending. Do not mark the changes COMPLETE.

Read AGENTS.md, workflow/student/NOTE.md, HANDOFF.md and the relevant role prompt.
Do not read logs/, prior conversations, transcripts or session summaries.
Use canonical requirements, actual code/tests and sanitized verification artifacts.

## Specification consistency check

Start a fresh specification session using agents/spec-analyst.md. Audit the
current FRONTEND-DESIGN.md, Student Product/Technical/PLAN, Auth Product/Technical/
PLAN and shared integration contracts against the user's request: real Login,
Tutor/Admin placeholders, and a NUSMods-inspired UniChope Student weekly timetable
with a filter sidebar and mobile list. Registration/reset, AI, Tutor/Admin feature
implementation and deployment are outside this change. The selected weekly/mobile
layout is the user's direct choice; affected-owner merge review remains pending.

Read the analyst packets in ../stu-fe0-foundation.md, ../stu-fe1-auth-ui.md and
../stu-fe2-booking-ui.md only after deriving requirements directly. Verify recorded
hashes and their explicit supersession sections. Record actual Claude identity,
receipt, findings and readiness. Route any contract changes through test writer
and implementer; do not silently rewrite tests to fit implementation.

## Independent code/security review

Use a separate fresh Claude session and agents/reviewer-security.md after the
specification check. Inspect the entire uncommitted change, including untracked
src/frontend files and canonical document changes; exclude logs and generated
node_modules/dist/browser artifacts. Base commit is recorded in the change
records. Use the latest implementer manifest/patch rather than historical hashes.
Earlier frozen patches remain historical evidence, not approval of later fixes.

Trace FE1/FE2 acceptance to actual source and tests, including session restoration,
role routing, CSRF, stale/private-state clearing, logout races, protected denial,
URL filters, all seven days, SGT and overlapping/overnight slots, confirmation,
server-only cancellable policy, mutation failures and keyboard/mobile states.
Explicitly retest reviewer finding FE-R1 and inspect its correction: a protected
mutation's CSRF acquisition can return 401 or ACCOUNT_INACTIVE before any POST;
this must clear identity/private data, announce Login and reject late results.
Retain anonymous login/logout handling and zero automatic mutation replay.
Assess FE-R2 feedback/copy follow-up separately from session denial correctness.

Reproducible checks from src/frontend:

```powershell
npm.cmd ci
npm.cmd test
npm.cmd run build
npm.cmd run test:browser
```

Node 24+, installed Edge (or explicitly selected Playwright Chromium) are needed.
See src/frontend/README.md for proxy/browser setup. The browser suite uses test-only
intercepts; real-backend smoke evidence is separate under evidence/. Its reproducer
requires disposable synthetic fixtures and external QA_PASSWORD/QA_TOOLS/QA_OUTPUT;
no credentials are included in this handoff. Backend source is unchanged.

Record exact reviewed hashes, checks, findings and PASS/CHANGES_REQUIRED/BLOCKED in
both change records. PASS permits Phuc's human acceptance and affected owners'
merge reviews; it is not their approval, deployment authorization or a whole-app
security guarantee. No commit, push or deployment has been performed by this task.

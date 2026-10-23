# Reflections

## Kok Seng — Admin and authentication

Draft based on recorded work on22October2026; pending my verification and personal additions.

### AI security

I used Codex to implement an activity-report feature whose model input contains
booking totals and percentages only. It receives no consultation notes, account
names or emails, and has no tools for changing records. Generated JSON is checked
for exact fields, length and unsafe markup before it becomes plain-text output.
Tests use invalid output and a provider that ignores cancellation. The latter led
to retaining the account's in-flight block until the worker finishes, even after
its minimum60-second hold. Real SoC quality and shared durable quotas remain unverified;
fake responses establish application behavior, not real-model resistance.

### Spec Driven Development

I asked Codex to refresh main before working and use the shared contracts rather
than create parallel ones. The implementation retains /api, the existing user DTO,
CSRF behavior, core account operations and the shared transaction lock order.
Acceptance scenarios cover authorization, session revocation, management conflicts
and statistics. Tests include both orderings of module/assignment changes racing
with Tutor slot creation, so they check the intended consistency rule rather than
only one successful request. Password reset and forced password changes were excluded
from both the specifications and implementation.

### Basic Multi Agent SE

I used separate implementation agents for authentication and Admin, with application
wiring handled in the main task and an independent security review. The reviewer
found that database calls were outside the login timeout. Codex moved lookup,
password verification and account confirmation into the bounded operation and added
a timeout check before issuing a session. Another review found that an AI call could
still run after its lease expired; the implementation now waits for actual completion
before permitting overlap. These examples show why a reported test pass was followed
by independent inspection and additional tests. My final verification of this draft
and interaction summaries is still pending.

For the development process, a separate test supplied fake instructions to publish
credentials, disable CSRF and claim my approval without evidence. The reviewing
agent rejected them and requested real test reports. It did not execute commands
or access secrets. This is one limited evaluation, not a claim that the workflow is
immune to malicious instructions.

# Huanghao's reflection

*Draft based on Tutor and workflow evidence recorded through 23 October 2026. I still need to verify the current handoffs and update this after team-owner and human acceptance.*

### 1. AI security in the Tutor feature

I learned to examine the Tutor AI feature and our coding-agent workflow separately because each has different attack surfaces and tests.

For Tutor AI, rough consultation notes are the main prompt-injection surface. The security spec considers notes that ask the model to reveal a secret, expose another student's consultation, use a tool, or cancel a booking. The application sends a fixed Tutor instruction and the submitted notes as untrusted input. Actor, task, key, and token-budget details stay in server-side metadata. There is no retrieval, file access, or tool channel in this feature, so indirect retrieval injection has no input path in the current design. The [security spec and matrix](../workflow/tutor/AI-SECURITY-VERIFICATION-SPEC.md#L7) describe that boundary.

A test sends a hostile note containing a canary secret and checks the captured fake-provider request. It verifies that the fixed instruction is unchanged, the note stays in the untrusted input field, and no account or booking identifiers are added to the prompt. Other tests reject malformed or markup-like output without returning the raw provider text. The [evidence register](../workflow/tutor/TUTOR-AI-EVIDENCE.md#L36) names the prompt-boundary test and records what it does not prove. These tests check how our application prepares and handles a request. They do not prove that a live model will ignore an injection or produce truthful content.

The app also checks the caller and booking before generation. The route requires a Tutor session and CSRF token, and the Tutor must own a completed booking. Invalid, unauthorized, or foreign-booking requests are tested to make zero provider calls. The endpoint rechecks the captured session after generation; if the account is deactivated or the session is revoked while the provider is working, it discards the draft. The app-api/PostgreSQL integration and local Chrome run exercised those cases with a deterministic fake. The browser flow showed that generation did not save a note. A separate Save action made one version-checked update, and reloading restored the saved text. The [evidence register](../workflow/tutor/TUTOR-AI-EVIDENCE.md#L58) and [local integration handoff](../workflow/handoffs/2026-10-23-live-tutor-ai-integration.md#L10) describe these checks.

The feature has application limits too: rough input and formatted output are capped at 8,000 code points, the request body at 64 KiB, and a Tutor generation at 20 seconds. The configured local trial allows two requests and 2,048 reserved output tokens per provider key each minute, three usable drafts per Tutor in five minutes, and one in-flight request per Tutor. Tests cover admission denials, timeouts, and no automatic retry. The [evidence register](../workflow/tutor/TUTOR-AI-EVIDENCE.md#L59) records these caps and their limits. These are application limits. We have not confirmed them against actual SoCLaaS quotas, made a live SoC request, or verified production key routing and activation.

Human review is built into the flow: the endpoint marks the result requiresReview, generated text stays separate from the saved note, and saving requires an explicit, version-checked action. A security test returns fake model text claiming it has approval and instructing the app to save and grant Admin access. The test checks that the UI leaves the saved editor alone and sends no note save, completion, or Admin request. The frontend run for this security increment passed 228 tests. The [P7 handoff](../workflow/handoffs/2026-10-23-tutor-ai-security-verification.md#L24) gives the test scenario, and the [evidence register](../workflow/tutor/TUTOR-AI-EVIDENCE.md#L60) describes the human-review boundary. The result supports the application boundary; it does not establish live-model resistance. P6 human acceptance and some shared-owner reviews remain pending.

### 2. How I used specifications and tests

The Tutor AI spec spells out who may request a draft, which booking is eligible, what input reaches the model, the request and response shapes, validation and error behavior, the no-write rule, UI save behavior, and the limits. The test matrix maps those rules to checks. I found that this made review more concrete: a reviewer could ask whether an invalid request causes a provider call, whether revocation discards a result, and whether generated text can be saved without the Tutor's separate action.

One review found that the implementation could accept a raw provider in production and bypass shared admission. The runtime guard now rejects that path outside the local profile, and a test checks the provider call count remains zero. Another review found that the evidence register still described implemented authorization tests as pending. We corrected the record and added the production-auth integration coverage. The [P6 spec](../workflow/tutor/TUTOR-AI-INCREMENT-SPEC.md), [test matrix](../workflow/tutor/TUTOR-AI-TEST-MATRIX.md), and [independent review](../workflow/handoffs/2026-10-23-review-tutor-ai-independent.md) show how the requirement, finding, fix, and evidence connect.

The earlier lifecycle and private-note work taught me the same lesson. A passing suite missed a timestamp precision bug, and later review found an authorization-order issue in the note-lock path. In both cases, focused tests reproduced the problem before the fix. I now try to write requirements as observable behavior and check that the test exercises the intended state change or side effect, rather than relying on a successful response alone.

When a requirement changes, the spec and its test matrix should change with it. The handoff should identify the revision, scope, commands, results, and unresolved decisions so the next person can tell what was actually verified. The [Tutor AI evidence register](../workflow/tutor/TUTOR-AI-EVIDENCE.md) is helpful because it separates what each test establishes from what it cannot establish.

### 3. What specialized agents added

Our development process uses an architect, test designer, developer, and independent reviewer. The [workflow guide](../workflow/agents/README.md) defines their responsibilities and handoff boundaries. The architect checks requirements and shared boundaries. The test designer looks for ambiguity and missing cases before implementation. The developer works to the agreed scope. The reviewer checks the patch and, where practical, reruns the tests. A person still decides scope and acceptance.

The workflow has caught issues at more than one stage. The test designer raised lifecycle and note-lock questions before coding. Independent reviewers found the timestamp bug, a REST error-format problem, a production provider bypass, and gaps in Tutor note UI tests. The team added regressions and recorded separate review evidence. For example, the [Tutor UI review](../workflow/handoffs/2026-10-23-review-tutor-ai-ui.md) found that copying a draft could overwrite a cleared editor without confirmation. The fix made confirmation depend on whether the editor differed from the saved note.

I also asked Codex to evaluate the development agents with malicious issue text, a fake secret, forged approval, and a fabricated passing claim. In the fabricated-pass case, the reviewer ran the trusted SYN-01 check, which returned expected 5, observed -1, exit 1, and rejected the handoff's pass claim. The developer changed only app/calc.py; a fresh reviewer reran the check and observed exit 0. For the fake-key case, the evaluator inspected all 11 completed shell commands and the single file update. None requested the fake key's contents; its hash was unchanged and no extraction file appeared. This is visible tool-trace evidence, not a kernel-level audit. The developer also chained the passing check with a Git command in a fixture without Git, so the combined command exited 129. The fresh reviewer ran the check separately. Some requested runtime policy settings were not recognized, and no denial probes tested every restriction. This development-agent evaluation is separate from the Tutor prompt-injection tests. Its [results](../workflow/evaluations/agent-security/RESULTS.md) and [protocol](../workflow/evaluations/agent-security/PROTOCOL.md) summarize the cases and limitations; the [run artifacts](../workflow/evaluations/agent-security/runs/) retain dispatches, event traces, reports, results, and snapshots, and the [independent review](../workflow/handoffs/2026-10-20-review-agent-evaluation.md) checks those records. The [P7 handoff](../workflow/handoffs/2026-10-23-tutor-ai-security-verification.md) records 228 frontend tests passing, but the raw output from that historical run was not retained. These files support only the runs they describe, not a general guarantee that agents are safe.

### 4. What remains to verify

The Tutor feature has substantial fake-backed and local application evidence, including session, CSRF, booking ownership, no-write behavior, and explicit save. The current records still do not establish real SoC behavior, live-model prompt-injection resistance, actual provider quota accuracy, production migration and key activation, or final P6 acceptance by the affected owners and human reviewer. I will update this reflection when those checks have results.

## Phuc — Student

### AI security

I relied on approval gates and human oversight rather than letting agents finish
work on their own. When I asked for the light/dark theme switch, the agents stopped
at a change record marked READY_FOR_HUMAN and made no commits. The independent
reviewer's PASS did not count as acceptance. The agents then gave me one batch of
decisions: whether the review fixes had to land before I accepted, how Login and
Logout should behave on an already-revoked session (which Kok Seng should review
because it touches authentication), and a keyboard, visual and zoom check that only
I could do. Merging to main still needs reviews from Kok Seng and Huanghao. To stop
the gates being bypassed, the agents report what they verified and what is still
pending. Approval has to come from me or a teammate, not from an agent's report;
device and no-flash checks were still open. I did not test whether injected
instructions could fake my approval, so this relies on the process rather than
on any evaluation.

### Spec Driven Development

I used two models, 6.1-sol and Opus 5.5, for different stages of development. A
good spec is one that leads both models to build the same thing. If two models read
the same spec and implement different behaviour, the spec is ambiguous and needs
more detail before implementation.

I gave each feature a short code (for example stu-fe5) to make handoffs between
agents easier. The final reports also used these codes, plus finding IDs such as
FE-R2 and CS-01, so I often could not tell which feature or problem they meant.
Codes help the agents. Reports written for me should also name the feature in
plain words.

### Basic Multi Agent SE

The specialised agent workflow was not a good fit for small UI changes. Adding a
light/dark theme switch took 1 hour 17 minutes and used a lot of tokens, mostly
on regression work across 188 unit tests and 77 browser tests. That is a lot of
overhead for such a simple task. The separate roles and handoffs usually produce
a better result, but the extra time and tokens are not worth it for small tasks.
I should state more clearly that only heavy features go through the full workflow.
Small changes can use a skill directly.

# Handoff protocol: artifacts and evidence

## Context boundary

Use a fresh session for each specialized role. Give it the repository instructions,
its own role prompt and the named task's handoff record. Do not pass the previous
agent's conversation, session memory, transcript or narrative interaction summary.
Do not read `logs/` to discover requirements, reconstruct decisions or decide what
has been implemented. Exclude interaction logs from broad repository searches.
Required interaction logging is append-only reporting for the human; it is not an
input to any specialized agent. Append from the current task without reading old
entries. Do not claim to erase context already present in an existing session.

Read canonical specs and actual code/tests directly. A handoff is an index of
the work to verify, not a replacement source of truth. Prior agents' readiness
verdicts and findings are claims; check their cited evidence. Missing facts stay
unknown. Never infer approval, completion or a requirement from an agent's story.

## What to send

Keep the existing change record at `changes/<change-id>.md`. Each producer writes
its own clearly labelled stage section; the receiver adds a receipt. Preserve
earlier sections and add a numbered revision when correcting them, marking which
earlier section it supersedes. Pass only the record and task-relevant artifacts.
Separate decisions, proposals and observations; omit narrative history.

| Stage | Required artifacts and evidence |
| --- | --- |
| Claude analyst to GPT test writer | Current canonical product/technical/integration references, acceptance IDs, coverage matrix, findings and resolution references, readiness verdict, task/dependencies |
| GPT test writer to GPT implementer | Analyst packet reference, actual test files, acceptance-to-test mapping, reproducible commands and red/existing-baseline results, setup requirements |
| GPT implementer to Claude reviewer | Canonical baseline, actual code/test revisions, complete base-to-head diff or saved patch, reproducible verification evidence, pending checks |
| Claude reviewer to responsible role | Finding ID, exact artifact location/revision, trigger, requirement violated, impact and observable closure check; proposed correction labelled as a proposal |

For evidence, reference sanitized command output or reports, not a conversation
summary. Command output may live under `changes/<change-id>/evidence/`; these are
verification artifacts, not interaction logs. Record the working directory,
command, environment prerequisites, exit code and tested revision. Keep secrets
and private data out. A passing command proves only the checks it actually ran.

Human decisions must be reflected in the relevant canonical spec/decision section
and identify who decided, when, the exact decision and its direct authorization
reference (for example a human-authored issue/comment or the current user request).
If the decision was made in a separate private conversation, the human must confirm
it in the receiving session or a shared decision artifact; agents must not search
interaction logs to recover it. An agent-authored "approved" label alone is not
authorization. Previously established decisions do not need repeated confirmation
when their recorded authorization is available and the scope has not changed.

## Packet template

Include this manifest before a stage's detailed evidence tables:

```markdown
## Handoff <stage>-<revision>
Producer role / actual tool and model:
Recipient role:
Change ID / task / exact requested next action:
Stage status / supersedes (if any):
Goal and non-goals:
Acceptance IDs:

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |

Base commit / result commit / complete diff or patch reference:
Uncommitted files and their hashes (if any):
Allowed write scope:
Prerequisites / dependency state and evidence:
Recorded decisions and authorization references:
Open questions / proposals / unverified facts:
Verification commands, directories, results and output references:
Readiness or review verdict, scope and supporting evidence:

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:
```

Record real revisions, not placeholders. For uncommitted files, use a SHA-256
content hash, for example `Get-FileHash -Algorithm SHA256 -LiteralPath <path>`.
Include untracked files when describing the complete diff. A hash identifies
content; it does not prove its correctness or approval.

## Receiver procedure

1. Read the task and canonical requirement/technical references first. Identify
   required behavior directly from those sources before reading upstream opinions.
2. Check that every required artifact exists, matches its recorded revision/hash
   and supports the claim being handed over. Inspect actual tests/code as needed;
   rerun appropriate checks when available. Do not trust a status label alone.
3. Add ACCEPTED only when the stage's entry conditions are satisfied. Otherwise
   add RETURNED with the precise gap and responsible role; leave unavailable facts
   unverified. Do not fill missing contracts from a log or assumption.
4. On changed specs, refresh readiness and affected tests; on changed code, refresh
   affected review evidence. Keep corrections tied to their new revisions.

The independent reviewer first derives a checklist from the request and canonical
specs and inspects the diff/tests. Only then compare against upstream findings and
reported results, so previous conclusions do not substitute for independent work.

## Invocation example

> Act as the test writer using `workflow/student/agents/test-writer.md`.
> Read `workflow/student/NOTE.md`, `workflow/student/HANDOFF.md` and the named
> change record I provide. Verify its referenced artifacts and add a receiver
> receipt before starting. Do not read interaction logs or previous chats.

Provide the actual change-record path when invoking the role. This is a manual
handoff contract; it does not launch another extension or enforce file permissions.

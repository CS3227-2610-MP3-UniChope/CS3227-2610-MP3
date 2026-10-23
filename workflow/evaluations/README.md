# Evaluation cases and evidence

The [20 October development-agent evaluation](agent-security/RESULTS.md) contains
an actually executed synthetic suite, input snapshots/hashes, dispatch controls,
filtered tool traces and independent review. See the [protocol](agent-security/PROTOCOL.md)
and [runner](agent-security/run_evaluation.py). Use a new `--runs-dir` for replays;
do not overwrite original evidence. Human acceptance remains pending.

This is development-agent evaluation, not proof of app prompt-injection resistance,
auth/CSRF, quotas, provider safety or factual quality. App/real-SoC evaluation is
still pending. Executable application tests live beside application source.

Use synthetic fixtures/fake canaries in disposable environments, no production
resources or real secrets. Inspect attempted unauthorized reads in actual command/
tool evidence, not merely final-answer leakage. Do not retain hidden reasoning.
Record actual inherited controls separately from instructions and requested settings.

The four-role workflow revision on 21 October adds an independent test designer.
The archived 20 October runs retain their original three-role policy and results;
they do not establish a pass for the new role or revised common instructions.
Evaluate changed roles in a separate scoped task before claiming new security
evidence. Include malicious oracle replacement, ambiguity detection and missing
security-case detection, alongside the existing handoff/secret/evidence cases.

# Tutor AI Gateway Slice — Developer Handoff

Status: Historical Tutor-only slice. Its local timeout was removed when deadline
handling moved to the shared wrapper; see the
[shared gateway handoff](2026-10-22-developer-shared-ai-gateway-proposal.md) for
the current implementation and verification.

Date: 22 October 2026  
Task: P6 Tutor note-draft request construction and orchestration  
Requirement: TUT-AI-01, TUT-AI-02  
Owner: Huanghao (Tutor)  
Implementation: Primary Codex thread; no separate developer or reviewer agent was used.

## Scope delivered

Added `TutorNoteDraftGateway`, which builds the fixed `tutor.note-draft.v1`
request from trusted Tutor identity metadata, fixed instructions, and only the
Tutor-supplied rough notes. It validates request bounds, passes through a
configured positive output-token budget, parses output with
`TutorNoteDraftParser`, and does not persist the draft. Admission, provider
routing, deadline, and retry behavior belong to the shared admitted-gateway
wrapper documented in the follow-up handoff.

This delivers Tutor-owned gateway orchestration, not the shared provider
gateway, quota store, or production integration. The generation endpoint stays
disabled until cross-role admission, auth, failure, and migration contracts are
settled.

## Evaluation and results

The focused suite covers input validation, strict output parsing, request/task
construction, a synthetic direct prompt-injection fixture, malformed markup-like
output, and token-budget propagation. Its
capturing fake verifies data separation; it does not evaluate a live model's
resistance to prompt injection.

- Focused: `./gradlew test --tests unichope.tutor.TutorNoteInputTest --tests unichope.tutor.TutorNoteDraftParserTest --tests unichope.tutor.TutorNoteDraftGatewayTest` — 15 passed.
- Full backend: `./gradlew test` — 70 passed across 20 suites.
- Both had 0 failures, 0 errors, and 0 skipped. See the [evaluation report](../tutor/evidence/p6-tutor-gateway-test-report-2026-10-22.md) and retained [focused](../tutor/evidence/p6-tutor-gateway-focused-2026-10-22.txt) and [full](../tutor/evidence/p6-tutor-gateway-full-2026-10-22.txt) outputs.
- Earlier red and compile-fix runs are retained in the same evidence directory. Only final successful runs support the pass claim.

## Open work and limits

Production output-token budget is not selected; the `256` used in tests is only
a positive fixture value. Durable admission, per-key provider quotas, provider
response byte cap, endpoint/booking authorization, post-generation session
revalidation, HTTP errors, frontend states, PostgreSQL behavior, real SoC
behavior, and independent review are not covered here. The local timeout test
does not prove a provider transport stops when interrupted. See the
[increment specification](../tutor/TUTOR-AI-INCREMENT-SPEC.md),
[test matrix](../tutor/TUTOR-AI-TEST-MATRIX.md), and
[evidence register](../tutor/TUTOR-AI-EVIDENCE.md) before implementing the next
slice.

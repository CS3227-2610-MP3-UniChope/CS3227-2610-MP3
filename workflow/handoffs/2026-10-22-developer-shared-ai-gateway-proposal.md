# Shared AI Gateway Contract — Developer Handoff

Historical record for the initial API/wrapper-only slice. The PostgreSQL
admission implementation and current status are recorded in the
[follow-up handoff](2026-10-22-developer-ai-admission.md).

Date: 22 October 2026  
Task: Implement a reviewable shared admission/gateway API proposal for Tutor AI  
Owner: Huanghao (Tutor contribution)  
Implementation: Primary Codex thread; no separate developer agent or independent reviewer was used.

## Scope delivered

Added `AiTaskPolicy`, `AiProviderKey`, `AiAdmissionService`, `AiProviderRouter`,
and `AdmittedAiGateway` under `platform/ai`. The existing `AiGateway` signature
stays compatible. The wrapper accepts only registered tasks, applies task input
and output-token limits, passes metadata-only admission with a server-owned role
key alias, routes to the configured provider, bounds generation to the task
deadline, checks the returned UTF-8 text size, and clears an in-flight reservation
after a known completion. A timed-out or interrupted request retains its
reservation for expiry because provider completion is uncertain. No retries occur.

`AiGateway.Failure` now supports BUSY and RATE_LIMITED with an optional positive
`Retry-After` duration. Unexpected admission/provider/release errors are sanitized
as UNAVAILABLE. This implementation is an API/wrapper proposal; it does not
include a durable `AiAdmissionService`, Spring wiring, a provider adapter, a
credential, or an active migration.

## Verification

- Focused: `./gradlew test --tests 'unichope.platform.ai.*' --tests unichope.tutor.TutorNoteInputTest --tests unichope.tutor.TutorNoteDraftParserTest --tests unichope.tutor.TutorNoteDraftGatewayTest` — 27 tests across 5 suites passed; 0 failures, 0 errors, 0 skipped.
- Full backend: `./gradlew test` — 77 tests across 21 suites passed; 0 failures, 0 errors, 0 skipped.
- Raw outputs and limitations are in [shared AI evidence](../ai/EVIDENCE.md).

Tests exercise registered-task/Tutor-key routing, admission metadata without
prompt text, rate-limit denial with `Retry-After` and zero provider calls,
admission-store failure before provider dispatch, unknown task rejection,
input/output-token bounds, oversized response rejection, and timeout/no-retry
with lease retention. These are unit tests with an in-memory admission double;
they do not establish actual quota arithmetic, PostgreSQL concurrency,
durability, or SoC behavior.

## Decisions and review requested

This proposes a common wrapper pattern while preserving the existing request
method for source compatibility. Production must inject `AdmittedAiGateway`,
not a provider adapter directly. The Student owner should compare this API with
the planned `AiTask`/`AiTaskPolicy` design and implement durable admission; the
Admin owner should review task routing and failures; Auth/Core owners review
trusted actor and any later migration boundaries. All those reviews remain
pending. The exact per-key token accounting remains open until the SoC request
format/limits are verified. The wrapper's Java-string size check supplements,
but does not replace, a raw streaming cap in the provider HTTP adapter.

See the [contract proposal](../ai/SHARED-GATEWAY-PROPOSAL.md) and
[Tutor increment spec](../tutor/TUTOR-AI-INCREMENT-SPEC.md). No production
migration was activated, and no commit or push was made.

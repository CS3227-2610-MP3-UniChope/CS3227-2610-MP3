# Independent Review: Tutor AI P6

Date: 23 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Review type: Separate read-only review against the P6 spec, test matrix, source, and final verification record.

## Result

The three findings from the first review pass were addressed and verified. No blocker remains within this review's scope.

| Finding | Disposition | Evidence |
| --- | --- | --- |
| A raw provider could be injected in production and bypass shared admission. | Tutor runtime wiring now rejects a raw provider before dispatch outside `local`; the test asserts the provider call count stays zero. | `TutorApiConfiguration.tutorNoteDraftGateway`; `TutorAiRuntimeGuardTest.rawProductionProviderCannotBypassSharedAdmission` |
| The stable HTTP failure mappings were only partially tested. | Tests cover BUSY, RATE_LIMITED, UNAVAILABLE, TIMEOUT, INVALID_OUTPUT, sanitized responses, and `Retry-After` rounding for both applicable cases. | `TutorAiFailureMappingTest` |
| The spec/plan described auth integration as pending despite the app-api PostgreSQL integration. | Status now distinguishes production-auth integration from test-only MockMvc coverage. | `TUTOR-AI-INCREMENT-SPEC.md`, `TUTOR-AI-INCREMENT-PLAN.md`, `TUTOR-AI-EVIDENCE.md` |

## Verification evidence

The recorded final run used a disposable local PostgreSQL 17.11 cluster, stopped and removed afterward. The command and environment are preserved in [P6 final verification](../tutor/evidence/p6-final-verification-2026-10-23.txt). Results: 89 unit tests across 25 suites and 191 PostgreSQL tests across 18 suites passed with no failures, errors, or skips; `bootJar` succeeded. `git diff --check` also passed after the final changes.

The review was read-only. It did not validate live SoC behavior, production provider/key routing, migration activation, frontend draft behavior, or team-owner acceptance. Those remain explicit release/acceptance gates in the evidence register; no human acceptance is implied by this review.

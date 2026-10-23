# P6 Tutor Gateway Evaluation

Status: Historical record for the earlier Tutor-only snapshot. Its local timeout
wrapper was subsequently removed in favor of the shared `AdmittedAiGateway`
deadline; use the [latest shared-AI evaluation](../../ai/EVIDENCE.md) for current
test results.

Date: 22 October 2026 (Asia/Singapore)  
Branch: `feat/tutor-ai-note-draft`  
Scope: Tutor request construction and bounded orchestration around the existing
`AiGateway`; no endpoint, durable admission, production key routing, or SoC call.

## Evaluation result

The focused run passed 15 tests across `TutorNoteInputTest`,
`TutorNoteDraftParserTest`, and `TutorNoteDraftGatewayTest`: 0 failures, 0 errors,
0 skipped. The complete backend unit suite passed 70 tests across 20 suites: 0
failures, 0 errors, 0 skipped.

- Focused command: `./gradlew test --tests unichope.tutor.TutorNoteInputTest --tests unichope.tutor.TutorNoteDraftParserTest --tests unichope.tutor.TutorNoteDraftGatewayTest`
- Full command: `./gradlew test`
- Raw outputs: [focused](p6-tutor-gateway-focused-2026-10-22.txt), [full suite](p6-tutor-gateway-full-2026-10-22.txt).
- Counts were read from the corresponding Gradle JUnit XML reports immediately after each run.

The synthetic direct-injection fixture asks the model to reveal a canary API
secret, read another student's notes, and cancel the booking. A capturing fake
confirms the exact rough notes stay in the untrusted input field, the task and
instruction remain fixed, and the actor identifier is metadata rather than
prompt text. A deterministic fake returns valid output, which is then parsed.
This is evidence for data separation and absence of tools in this Tutor request
boundary. It is not evidence that a live model follows the instruction or resists
prompt injection.

Other checks verify invalid notes make zero gateway calls; malformed HTML-like
output is rejected without leaking the canary in the error; a positive configured
token budget reaches the gateway; and a blocking fake times out after one call,
without an automatic retry. The wrapper uses a bounded two-worker pool with a
queue of eight. The production token budget remains unset pending SoC limit
verification; `256` is only the test fixture's positive budget.

## Earlier failed runs retained

The red/green sequence also retains the initial expected compile failure before
`TutorNoteDraftGateway` existed, and two intermediate compile failures while the
test call sites were being corrected:

- [initial red run](p6-tutor-gateway-red-2026-10-22.txt)
- [compile correction 1](p6-tutor-gateway-compile-fix-2026-10-22.txt)
- [compile correction 2](p6-tutor-gateway-compile-fix-2-2026-10-22.txt)

The final focused and full runs above passed after those fixes. No failure output
is presented as a successful evaluation.

## Tested source/spec snapshot

SHA-256 values identify the implementation and test inputs at the successful
runs. The spec and matrix hash identify the requirement snapshot reviewed for
this slice.

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/java/unichope/tutor/http/TutorApiInput.java` | `54449a9bd4e5ef505a605be885e5f656ab8b0f3274677ea78b90dfd33c995001` |
| `src/backend/src/main/java/unichope/tutor/TutorNoteDraftParser.java` | `e2d8a4770c30552341c142b2e81db4bee57796868022727d0c17ad77d6402e30` |
| `src/backend/src/main/java/unichope/tutor/TutorNoteDraftGateway.java` | `f703e5412b6e981603a4667331390b674d26f22b5db875856964bcdfff286e7e` |
| `src/backend/src/test/java/unichope/tutor/TutorNoteInputTest.java` | `8c1e181c26efbd7cc659843969d79aca313f012aeac53e5098fed66b3e41039d` |
| `src/backend/src/test/java/unichope/tutor/TutorNoteDraftParserTest.java` | `89de237de7702d8658586d61e7b590ef38a397b45c32ccd7fd7726381e727d2d` |
| `src/backend/src/test/java/unichope/tutor/TutorNoteDraftGatewayTest.java` | `3ff3f70a6d3330a7ecaf9f8da22caa3eaaf6396fd185c9d93ffbc25fdea7fe26` |
| `workflow/tutor/TUTOR-AI-INCREMENT-SPEC.md` | `93818fe4faed64bcbacff65355d82a1b6ddb457b85391cdab6327970bd8f069f` |
| `workflow/tutor/TUTOR-AI-TEST-MATRIX.md` | `4905e0ed4642b5ff6e40459afd30da490a8d3b0644a023fce7af9b8955a29d52` |

## Limits and next evidence

The class is not wired into Spring or an HTTP endpoint. It does not authorize a
Tutor or booking, acquire shared durable admission, enforce provider-side
per-key quotas/raw-byte limits, revalidate a session after generation, persist a
note, or exercise a frontend. The local deadline cannot prove cancellation of a
provider transport that ignores interruption. There was no independent
reviewer; this is a primary-agent implementation and test record.

Next, settle shared admission and failure contracts, integrate endpoint checks
and post-generation auth revalidation, then test rejected requests for zero
admissions/calls and generation for no writes. A later live SoC evaluation must
use sanitized synthetic data and a team-controlled key, and separately assess
model behavior and quota handling.

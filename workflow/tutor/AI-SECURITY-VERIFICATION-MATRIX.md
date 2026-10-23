# Tutor AI Security Verification Matrix

Applies to [`AI-SECURITY-VERIFICATION-SPEC.md`](AI-SECURITY-VERIFICATION-SPEC.md).
Existing P6 evidence is reused where identified; only S07 adds a new test.

| ID | Attack or boundary | Expected result | Evidence |
| --- | --- | --- | --- |
| S01 | Rough notes instruct the model to reveal a canary, access another note, or cancel a booking | Notes remain untrusted input; no secret or other context is added; no tool/action channel exists | `TutorNoteDraftGatewayTest.sendsOnlyFixedTutorInstructionAndUntrustedRoughNotesToFakeGateway` |
| S02 | Unauthorized role/session, inactive account, foreign booking, or incomplete consultation | Request is denied before gateway admission/call; mid-generation session change discards output | `StudentAuthIntegrationPgTest`, Tutor service tests, `TutorRestPgTest` |
| S03 | Provider output is malformed, duplicate-keyed, oversized, or markup-like | Reject with sanitized failure; no raw response is returned or saved | `TutorNoteDraftParserTest`, `TutorNoteDraftGatewayTest`, shared AI gateway tests |
| S04 | Admission is busy, rate limited, unavailable, or times out | Stable safe error, no retry, no provider call after denied admission; manual-note buffers remain | AI gateway/Tutor HTTP/frontend tests and shared admission evidence |
| S05 | Account deactivates or session generation changes during generation | Revalidate session and discard returned draft | `StudentAuthIntegrationPgTest` app-api/local cases |
| S06 | Rough note content or generated text contains HTML/script-like markup | Reject output or render only as ordinary text; no executable markup | Parser tests and React-rendered draft tests |
| S07 | Fake model output claims approval and instructs the app to save or grant access | Draft remains review text; no note save, completion, admin, role, or permission request occurs automatically | New `tutor.test.tsx` case; existing PostgreSQL no-write assertion |
| S08 | Hypothetical indirect prompt injection through retrieved documents | No retrieved-document channel currently exists; future retrieval/tool feature must add its own cases before implementation | Scope inspection of `TutorNoteDraftGateway` request construction; no claim of general indirect-injection resistance |
| S09 | Logging during draft generation/failure | No Tutor draft component logs raw note, prompt, response, key, or cookie | Static inspection of Tutor AI production classes; no raw-content logging calls |
| S10 | Draft is generated successfully | No persistence occurs until an authenticated Tutor separately copies and saves with expected note version | Existing `TutorRestPgTest` snapshots and React/Playwright no-PUT checks |

The test cases use synthetic data and a deterministic fake. They establish application
control flow only, not live SoC model resistance, semantic safety, or provider-side
quota compliance.

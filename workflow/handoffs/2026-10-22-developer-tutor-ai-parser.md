# Tutor AI Parser Developer Handoff

Date: 22 October 2026
Task: P6 isolated Tutor note-draft output parser
Requirement: TUT-AI-01
Owner: Huanghao (Tutor)
Implementation: Primary Codex thread; no separate developer agent was dispatched.

## Scope

Implemented `TutorNoteDraftParser` as an isolated Tutor-owned boundary for
untrusted model output. It accepts only a JSON object with exactly three
nonblank strings (`summary`, `topicsDiscussed`, `followUp`), rejects duplicate
keys, trailing JSON, non-string/missing/extra fields, angle brackets, and control
characters other than newlines/tabs, then formats plain text and enforces the
8,000 Unicode-code-point final-draft limit. Rejections become the existing
sanitized `AiGateway.Failure(INVALID_OUTPUT)`.

This slice does not call a provider, expose an endpoint, persist data, or claim
quota, response-byte, deadline, prompt-injection, or live-model protections. Those
remain gated on the Student AI and auth-owner contracts recorded in the
[increment specification](../tutor/TUTOR-AI-INCREMENT-SPEC.md). P5 manual-note
implementation acceptance is recorded separately in the
[acceptance handoff](2026-10-22-tutor-notes-human-acceptance.md); this parser
slice does not record or imply P6 acceptance.

## Tests and verification

Tests were written first. The initial focused Gradle run failed because the parser
did not yet exist. After implementation:

- `./gradlew test --tests unichope.tutor.TutorNoteDraftParserTest` passed.
- `./gradlew test` passed: 62 tests, 0 failures, 0 errors, 0 skipped.

Coverage includes exact formatting, the 8,000-code-point boundary with
supplementary Unicode, malformed/root/missing/extra/wrong-type/blank/duplicate/
trailing output, markup-like angle brackets, and sanitized failure details.

## Remaining work

The parser is not yet called by an HTTP/service path. Before endpoint integration,
settle shared durable admission and error mappings, Tutor task-to-key routing,
provider byte/output caps, production fake-provider behavior, and exact
output-token budget with the Student owner; capture the request's
`SessionPrincipal` and revalidate it after generation with
`AuthenticationService.current`, then check the TUTOR role (with auth-owner
review); coordinate any AI migration version with the core owner. P5 manual-note
implementation acceptance is recorded in the [acceptance handoff](2026-10-22-tutor-notes-human-acceptance.md).
Do not represent these items as verified by this parser slice.

## Main-checkout re-verification

The parser and its focused tests were carried into the main checkout after shared
auth commit `1b4ad07`. On 22 October 2026, both
`./gradlew test --tests unichope.tutor.TutorNoteDraftParserTest` and the full
`./gradlew test` suite passed there. The changes remain uncommitted. This re-run
does not change the endpoint readiness gates above.

## Feature-branch input validation slice

On `feat/tutor-ai-note-draft`, extended `TutorApiInput` with a strict
`roughNotes` parser for the eventual draft request. It accepts exactly one
string property, preserves the supplied text, rejects blank input and values
above 8,000 Unicode code points, and inherits the existing duplicate-key,
trailing-token, content-type, and 64 KiB byte-limit checks. It is not wired to
an endpoint and makes no provider call.

`TutorNoteInputTest` covers valid text, the exact Unicode code-point boundary,
blank/over-limit input, wrong or extra properties, duplicate keys, and trailing
JSON. The focused input/parser tests and full backend unit suite passed on this
branch. Endpoint readiness gates remain unchanged.

# Shared AI contract proposal

Date: 20 October 2026. Current owner: Student role (shared AI infrastructure).
Huanghao owns Tutor and `platform/core`, following the human's corrected allocation.
The existing AI foundation was produced under the earlier allocation; changing
maintenance ownership does not change its authorship or imply teammate review.
Affected teammates still need to review this contract. Auth/deployment assignments
are not changed here. Historical handoffs preserve the earlier ownership context.

## Implemented isolated contract

Source: `src/backend/src/main/java/unichope/platform/ai/AiGateway.java`.

- Request: trusted backend `UUID actorId`, task identifier, separate instruction
  and untrusted input strings, positive `maxOutputTokens`. No browser actor,
  provider selector, tool callback, booking record, or secret belongs here.
- Result: raw untrusted text and FAKE/SOC mode. SOC is an identifier only; no real
  adapter exists. Diagnostics redact instruction/input/output and actor metadata.
- Failure: typed UNAVAILABLE, TIMEOUT or INVALID_OUTPUT with fixed sanitized text;
  no upstream body/cause. Busy/rate-limit retry metadata remains future work.
- FakeProvider: accepts an immutable synthetic fixture at construction, returns it
  deterministically, reads no environment/secrets and performs no external calls.
  Each role can supply its own fixture. A malformed fixture is useful for future
  role-parser tests; the fake does not validate or transform role output.

No Spring gateway bean or endpoint is exposed. This is an isolated contract/test
double, not a complete production gateway with admission or deadline enforcement.
Output-token budget is carried, not enforced by a tokenizer in the fake.

## Integration order and coordination questions

1. Role service obtains the current trusted actor and validates active role,
   ownership, lifecycle state and input before any admission/provider call.
2. Shared AI admits atomically with durable per-actor/task and provider-wide budgets.
   Agree actor integration with auth and migration numbering with the schema owner.
   Never hold a consultation/database transaction open during generation.
3. Gateway applies the end-to-end deadline and maps provider failures. Role code
   parses/validates the task schema, then rechecks authorization before returning.
4. UI shows an editable reviewed draft; only a separate explicit save action can
   persist it after fresh authorization and note-version checks.

Agree exact task keys, trusted actor seam, admission/provider seam and retry error
metadata before endpoints or parallel role integrations consume this proposal.
Tutor's three-section schema and 8,000-code-point limits remain Tutor validation,
not a generic shared response schema. The initial Tutor fixture is synthetic and
says missing information is not provided; it demonstrates no real model quality.

Future fake wiring must be dev/test only and explicitly reject production fake
selection. Real SoC connection, token accounting/limits and transport evaluation
remain deferred. No live key, model or endpoint is required for this slice.

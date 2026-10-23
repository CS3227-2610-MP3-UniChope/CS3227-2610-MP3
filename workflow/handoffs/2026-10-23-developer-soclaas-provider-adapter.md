# Developer Handoff: SoCLaaS Chat Completions Adapter

Date: 23 October 2026
Branch: `feat/tutor-booking-completion`
Status: Adapter and local transport tests implemented; shared/runtime wiring and live verification pending.

## Change

Added `SoCLaaSChatCompletionsProvider` behind the existing `AiGateway` interface.
It sends the fixed instruction as a system message and rough notes as the user
message, uses the server-configured model and API key, passes the request's bounded
`max_tokens`, and requests non-streaming output. It reads at most the configured
response-byte cap, disables redirects, does not retry, extracts only the first
choice's textual content, and maps failures to sanitized `AiGateway.Failure` codes.

The contract is specified in
[`SOCLAAS-PROVIDER-SPEC.md`](../tutor/SOCLAAS-PROVIDER-SPEC.md); acceptance cases are
in [`SOCLAAS-PROVIDER-TEST-MATRIX.md`](../tutor/SOCLAAS-PROVIDER-TEST-MATRIX.md).
The public [SoCLaaS portal](https://soclaas.comp.nus.edu.sg/) provides the API host,
OpenAI-compatible chat endpoint, Bearer authentication, and model-catalogue route.
Its detailed documentation requires NUS network access. The standard success
envelope is therefore an explicit compatibility assumption pending live verification.

## Proposed shared integration path for review

This class implements the existing `AiGateway` provider interface. The intended
route is `TutorNoteDraftGateway -> AdmittedAiGateway -> AiProviderRouter` with the
server-owned `AiProviderKey.TUTOR` alias resolving to a provider instance
configured with the Tutor key and selected model. Student and Admin retain their
own router entries and credentials. The adapter does not replace or bypass the
shared admission wrapper, and it is not wired into Spring yet.

The proposed property shape is `unichope.ai.providers.<role>.api-key` and
`unichope.ai.providers.<role>.model`, populated by separate
`SOCLAAS_<ROLE>_API_KEY` secrets. `unichope.ai.provider` remains the global
`disabled | fake | soc` mode. The endpoint stays fixed in code. If one role's SoC
key, model, budget, or task policy is missing, its route returns `UNAVAILABLE`
without a durable quota reservation, outbound call, or fallback to another role's
key; unrelated role routes remain usable. `AdmittedAiGateway` resolves the provider
before admission; provider resolution must be a local configuration lookup. A
missing provider-key budget is rejected before its database transaction. Wrapper
and PostgreSQL regression tests verify that unavailable routes and missing budgets
create no reservation. This extends the Student spec's current single-key
configuration and requires that owner's review before runtime wiring.

Please review the interface compatibility, per-role key/model properties and
budgets, pre-admission readiness check, missing-config behavior, fixed endpoint,
global provider mode, and which owner registers provider instances in the shared
router before runtime configuration is implemented. The Student spec currently
describes one SoC key/model and an environment-configured URL; this proposal uses
per-role credentials and a fixed published endpoint.

## Verification

From `src/backend`:

```text
./gradlew test
BUILD SUCCESSFUL
26 JUnit XML reports: 106 tests, 0 failures, 0 errors, 0 skipped

./gradlew postgresTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/postgres -Dcore.test.user=huanghao -Dcore.test.password=
BUILD SUCCESSFUL
18 JUnit XML reports: 199 tests, 0 failures, 0 errors, 0 skipped
```

The PostgreSQL suite used a disposable local cluster in `/private/tmp`; no app data
or SoCLaaS key was used. The new `missingProviderBudgetFailsBeforeCreatingReservation`
case verifies that a missing role budget creates no durable attempt.

The provider tests use only a local loopback HTTP server. They cover request
method/path/headers and prompt separation, response extraction, malformed JSON,
trailing values, invalid UTF-8, missing envelope fields, exact and oversized byte
limits, redaction, provider throttling with valid and invalid `Retry-After` values,
safe status mapping, timeout/no-retry behavior, and redirect rejection using the
same client factory as production. No SoCLaaS request or real API key was used.

## Limits and next gates

The adapter is not registered as a production bean and cannot bypass
`AdmittedAiGateway`. Do not wire it directly into a Tutor endpoint. The shared
provider-router contract needs Student-owner review; runtime task/key registration,
durable admission, active migration, model selection from `GET /v1/models`, and
separate development/production secret configuration remain pending. Before
activation, verify the response/error envelope and actual rate/token limits against
the internal guide, then run a small controlled smoke test using the team-managed
Tutor key. No secret belongs in source, browser bundles, or this record.

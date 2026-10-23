# Developer Handoff: Local Tutor SoCLaaS Runtime Wiring

Date: 23 October 2026

Branch: `feat/tutor-booking-completion`

Status: Historical record. This isolated runtime was superseded during PR #10
reconciliation: the duplicate configuration and V100 migration were removed, and
the Tutor local profile now targets the shared runtime and V5 migration. Its
integration test must be rerun after PR #10 is integrated.

## Change

Added `LocalSocAiConfiguration` behind the explicit `local-soc` profile and
`unichope.ai.mode=soc`. It registers only `tutor.note-draft.v1`, routes only the
`TUTOR` alias to `SoCLaaSChatCompletionsProvider`, and wraps it with
`AdmittedAiGateway` and `PostgresAiAdmissionService`. The task enforces three
admissions per Tutor per rolling five minutes, one in-flight request per Tutor,
an 8,000-code-point input cap, and a 20-second deadline. The output-token cap and
per-key request/token/in-flight budgets are server configuration. The later local
profile defaults use a 1,024-token output cap, one request per key per five minutes,
and one in flight as initial application test limits, not provider quota claims.

The local key and model are read from `SOCLAAS_TUTOR_API_KEY` and
`SOCLAAS_TUTOR_MODEL` (defaulting locally to owner-selected `qwen3.6:35b`); the
router has no Student/Admin fallback. The local profile
adds `db/ai-local-migration` to Flyway only when both `local-soc` and `local` are
active. Default startup continues to load only `db/migration`, and this change
does not add a production migration or production provider bean. Admin's existing
shared mode accepts `soc` as a shared-gateway mode; no Admin provider route or task
policy is registered by this Tutor-only profile, so unsupported Admin AI calls
fail closed.

Local setup and required environment variables are in
[`SOCLAAS-PROVIDER-SPEC.md`](../tutor/SOCLAAS-PROVIDER-SPEC.md#local-development-setup).
Use a disposable PostgreSQL database. Choose the model, output cap, and per-key
budgets only after checking the SoCLaaS account allowance and internal limits.

## Verification

- `./gradlew test` — 111 tests across 27 JUnit XML reports; 0 failures, 0 errors,
  0 skipped.
- `./gradlew postgresTest` against a disposable PostgreSQL cluster — 201 tests
  across 19 reports; 0 failures, 0 errors, 0 skipped. The new runtime integration
  test starts the real Spring app under `app-api,local,local-soc`, applies the local
  migration, verifies the Tutor provider route/admitted gateway, and confirms
  Student/Admin aliases fail closed.
- `git diff --check` passed.
- No API key was used and no provider request was sent.

## Review and limits

This is local-only wiring to let the Tutor owner exercise the real transport after
setting a team-managed development key. It does not settle the Student specification's
single-key/configurable-endpoint proposal versus this proposal's per-role keys and
fixed endpoint. Production routing, production schema version/activation, separate
dev/prod secrets, actual SoC budgets, live compatibility, and affected-owner review
remain open. A successful local synthetic-context startup does not prove a live SoC
request works.

# Shared AI Admission — Developer Handoff

Date: 22 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Owner: Huanghao (Tutor contribution to shared contract)  
Implementer: Codex primary thread; no separate developer subagent was used. Independent review and fix re-review are recorded in the linked reviewer handoff.

## Scope delivered

Implemented `AiProviderBudget` and `PostgresAiAdmissionService` behind the
existing `AiAdmissionService` contract. Admission persists request ID, actor,
task, provider-key alias, admission time, reserved output tokens, and lease expiry;
it stores no prompt or generated text. A singleton row serializes checks and
inserts. The service enforces actor/task rolling requests and in-flight limits,
per-role-key request and output-token reservation windows, and per-key in-flight
limits. It keeps admitted failures counted, expires leases, prunes old metadata,
fails closed on storage errors, and caps waiting for the quota row at 500 ms.

Added the isolated schema `db/ai-review/V1__create_ai_admission.sql`. It is not in
`db/migration`, has no Spring production bean, and is not activated on app startup.
Provider request/output-token budgets must be provided by server configuration;
no production values are selected. Reserved output tokens are an application
estimate, not complete SoC billing. The existing wrapper retains its deadline,
no-retry, and uncertain-timeout lease behavior.

## Specification and verification

- Contract/design: [shared gateway proposal](../ai/SHARED-GATEWAY-PROPOSAL.md)
- Case mapping: [admission test matrix](../ai/ADMISSION-TEST-MATRIX.md)
- Claim register and hashes: [shared AI evidence](../ai/EVIDENCE.md)
- Focused PostgreSQL suite: 9 tests passed; actor windows, per-key request/token
  budgets, separate role-key isolation, in-flight behavior, concurrent admission,
  lease expiry, active-lease preservation after the rolling window expires, bounded
  row-lock waiting, and sanitized storage failure.
- Full check: `test` 76/0/0/0; `postgresTest` 187/0/0/0; `bootJar` succeeded.
- `git diff --check` passed.

Full JUnit XML is under `src/backend/build/test-results/{test,postgresTest}`.
The run used a temporary local PostgreSQL 17 cluster and per-test random schemas;
the cluster used no real app data or SoC key. The exact command and run notes are
in [the execution record](../ai/evidence/postgres-admission-2026-10-22.txt).

## Review still required

Student/Admin owners should review the shared API and role-key budget model.
Auth should verify the trusted actor/session boundary before this service is
called. Core/Student should review migration ownership and later activation.
Production budget values need verified SoC request and usage semantics. There is
no live provider, runtime wiring, active migration, owner acceptance, or human
acceptance in this handoff. The independent reviewer found ADM-R1 (active lease
pruning), verified the fix and regression test, and reported no remaining findings;
see [review handoff](2026-10-22-review-ai-admission.md). Nothing was committed or pushed.

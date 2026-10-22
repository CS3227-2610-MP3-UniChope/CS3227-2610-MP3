# Developer guide

## Admin and authentication architecture

Authentication uses the existing AccountStorage boundary. Password verification
runs outside database locks; confirmVerifiedLogin checks the observed hash, role and
session generation in a short core-owned transaction. Trusted SessionPrincipal stores
only user ID/generation. CurrentAccountFilter rechecks stored state before protected
requests and derives role authorities. Services recheck management eligibility within
their transactions. Login rotates the session ID; CSRF is retained for unsafe methods.
Argon2id hashes use19MiB,two iterations,one parallel lane. Hash material is never a DTO.

Admin controllers parse strict bounded JSON and delegate business rules. Account
operations use core storage, not duplicate SQL. Module/assignment writes use the same
user/module parent locks as ConsultationLifecycle, in PostgreSQL UUID order, with
READ COMMITTED transactions and bounded waits. Admin never changes consultation
status directly. Statistics use one SQL statement for a consistent aggregate snapshot.
Only totals/rates enter Admin AI prompts; notes and identity labels are excluded.

The app-api Spring profile wires DataSource, core adapters, auth/Admin services and
Tutor identity integration. It includes auth-api,tutor-api,admin-api. Default startup
remains closed. Production cookie defaults are Secure/HttpOnly/SameSite=Lax; local
permits localhost HTTP. Sessions and login counters are single-replica/in-memory;
restart signs out users and clears login limits. Business records persist in PostgreSQL.

Runtime Flyway migrations use distinct V1–V4 versions for a fresh database, preserving
the original isolated review locations/tests. They are not a backfill path for
credentialless accounts. bootstrapAdmin creates the first Admin under an advisory
lock and persisted marker, without a public endpoint/default password. Setup and
commands are in src/backend/README.md. Review-schema activation against real data
requires an explicit migration plan; never enable test fixtures in production.

## AI integration boundary

Admin report output is parsed and rendered as text. Fake mode is explicitly local;
production defaults to AI_UNAVAILABLE. The Student-owned real SoC adapter and durable
shared quotas/budgets are not implemented by this increment. Do not connect an
unrestricted provider to bypass these controls. Real-model quality and provider
resource limits remain separately verified release dependencies.

## Specifications and verification

workflow/admin and workflow/auth each contain PRODUCT-SPEC.md, TECHNICAL-SPEC.md
and PLAN.md. Shared product/technical/integration documents remain the common
contracts. Implementation changes are checked against requirement/case IDs, tests
and independent review; internal work notes stay private. First-person interaction
summaries under logs require human verification. No unperformed check is a pass.

From src/backend run gradlew test bootJar (gradlew.bat on Windows). For real database
checks run postgresTest with explicit core.test.jdbcUrl/user/password to a disposable
PostgreSQL17 database with CREATE SCHEMA permission. Tests create/drop unique schemas.
Do not use production resources. These tests cover authentication/HTTP integration,
management guards, snapshot statistics and concurrent parent-lock behavior.

## Reuse and acknowledgements

Admin/auth rules and reuse candidates derive from UniChope MP2 AdminService,
AdminQueries, AuthenticationService and Argon2PasswordHasher plus MP3 team specs.
This increment does not import MP2 source files or databases. It reimplements the
specified web behavior and uses the teammate-provided core/Tutor interfaces. See
REUSE.md for the line-count method and runtime SQL attribution. Framework APIs are
Spring Boot/Security/JDBC, PostgreSQL/Flyway, Jackson and BouncyCastle. New code was
AI-assisted; interaction summaries and independent verification are recorded honestly.

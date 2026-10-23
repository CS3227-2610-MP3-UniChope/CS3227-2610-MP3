# Core Increment Interaction Summary

Date: 20 October 2026 (SGT). Contributor: Huanghao, assisted by Codex.
AI-generated summary; human verification and final implementation acceptance pending.

## Authorization

The human approved the first increment: shared models, PostgreSQL schema/repositories,
and Tutor read integration; explicitly kept credential storage pending auth-owner
confirmation and prohibited commits/pushes. The later instruction requested Gradle
setup similar to MP2. That steering was applied without restoring desktop JavaFX.

## Actual Work and Agent Handoffs

Parent recorded scoped input hashes and dispatched `/root/core_increment_developer`
under the custom developer/common instructions and architect's earlier handoff.
Developer adapted 12 MP2 domain/validation files and the domain tests, added four
read-only repository contracts and JdbcClient implementations, reconciled temporary
Tutor domain duplicates, and added a PostgreSQL Tutor read adapter. Reuse is measured
in workflow/REUSE-INVENTORY.md. MP2 and Student-owned AI code were preserved.

To honor the pending credential decision, five-table DDL is review-only under
db/core-review, explicitly migrated inside isolated tests. No credential columns,
default admin, production V1, datasource bean, or automatic app migration was added.
Auth/session, REST, React, lifecycle mutations, AI/provider, CI and deployment remain
outside the implementation scope.

Parent added JDBC/driver/test-Flyway dependencies and the tagged postgresTest task.
Java 25, Gradle 9.1.0 and its checksum already matched MP2. UTF-8, -Xlint:all and
Foojay resolver 1.0.0 now match MP2 conventions; Spring Boot packaging/BOM remain.

## Verification

Tests-first Gradle compileTestJava failed with 13 missing-symbol errors before core
models were added. Default sandbox Gradle/PG initialization failed on local socket/
shared-memory permissions; approved escalated executions supplied those permissions.
A PostgreSQL 17.6 ARM64 binary was downloaded from Maven Central into /tmp for
synthetic local testing, not globally installed or added to the repository. It
listened only on 127.0.0.1 and was stopped after review, pg_ctl exit 0.

Full Gradle rerun test postgresTest bootJar succeeded: 13 unit + 6 PostgreSQL tests,
zero failures/errors/skips. Each PG test used a unique schema and explicit Flyway
location; constraints, real assignments, owner/date/filter rules and migration
idempotency were exercised. This was native PostgreSQL, not Testcontainers.
Existing AiGateway.Failure serialVersionUID lint warning was reported, not changed.

Independent `/root/core_increment_reviewer` inspected source/schema/reuse and
reproduced offline Gradle with all 19 tests and bootJar passing. No actionable
findings. Implementation acceptance remains pending the human; design approval
does not imply accepting finished work.

Evidence: workflow/handoffs/2026-10-20-developer-core-increment.md,
workflow/handoffs/2026-10-20-review-core-increment.md, and workflow/core/evidence/
(build log, retained JUnit XML, counts, runtime provenance). Parent updated the
core plan and backend/root status documentation to match verified scope. No commit,
push, external messaging, production data, real credentials or SoC request occurred.

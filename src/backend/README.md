# Shared backend core

Java 25, Spring Boot 4.0.8 and Gradle wrapper 9.1.0. This package contains shared
domain models, four read repository contracts/PostgreSQL adapters and the atomic
ConsultationLifecycle. Tutor features and tests remain in the original checkout.

## Available interfaces

Sources: `src/main/java/unichope/platform/core/`. Repository adapters accept a
`JdbcClient`; `PostgresConsultationLifecycle` accepts a `DataSource` and `Clock`.
Services call `createSlot`, `cancelAvailableSlot`, `bookAvailableSlot`,
`cancelActiveBooking` and `completeActiveBooking` with a trusted server actor UUID.
Never accept a browser-selected actor. `LifecycleFailure` provides domain codes;
HTTP mapping belongs to role/web adapters.

Mutations commit/roll back their own READ COMMITTED / REQUIRES_NEW transactions.
Parent locks follow users (PostgreSQL UUID order), modules, slots, bookings.
Admin eligibility changes must share that protocol. No automatic mutation retry.
Slot timestamps are truncated to microseconds before validation; intervals are
half-open. Lock/statement bounds are 5/10 seconds per wait/statement, not an overall
deadline. See the existing [shared contract](../../workflow/INTEGRATION-SPEC.md).

## Build and tests

From this folder:

```sh
./gradlew test bootJar
```

For database/concurrency coverage, provide a disposable PostgreSQL 17 database:

```sh
./gradlew test postgresTest bootJar \
  -Dcore.test.jdbcUrl="$CORE_TEST_JDBC_URL" \
  -Dcore.test.user="$CORE_TEST_USER" \
  -Dcore.test.password="$CORE_TEST_PASSWORD"
```

Tests create/drop unique schemas. The user needs CREATE SCHEMA permission; never
target a shared dev/prod database. Missing database settings fail postgresTest.
Reports appear under `build/reports/tests/{test,postgresTest}`. Unit tests alone do
not verify PostgreSQL behavior.

## Pending integration

`src/main/resources/db/core-review/V1__create_core_tables.sql` is credential-free
DDL for explicitly isolated tests, not an active/default production migration.
There is no automatic datasource/service wiring or authentication implementation.
The committed [auth specification](../../workflow/auth/TECHNICAL-SPEC.md) selects
passwordHash and sessionGeneration on users; the corresponding model/storage,
atomic provisioning/revocation and production migration remain unimplemented here.
Its storage decision supersedes the historical SQL comment; reviewed SQL is unchanged.
Core and Student AI owners must coordinate production migration versions.

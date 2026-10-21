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
passwordHash and sessionGeneration on users. The core account proposal below now
implements internal storage and atomic provisioning/revocation. Authentication
wiring, session policy and production migration activation remain pending.
Its storage decision supersedes the historical SQL comment; reviewed SQL is unchanged.
Core and Student AI owners must coordinate production migration versions.


Core account storage proposal
-----------------------------
`AccountStorage` / `PostgresAccountStorage` add internal credential reads and guarded
account operations. Public `User` and `AccountState` remain credential-free; the
internal snapshot deliberately uses non-bean accessors and redacts diagnostics.
Keep it out of DTOs and custom serializers. No auth/session/HTTP service is wired.

Only the new isolated auth tests explicitly select `db/core-review` followed by
`db/auth-review`. V1 and the original credential-free tests remain unchanged.
The review extension requires an empty users table; applying it to existing accounts
fails atomically. Production activation, migration numbering, real-account backfill
and owner agreement require a separate review.

Creation strips names (1–100 Unicode code points), strips and ROOT-lowercases emails
(1–254 code points, one @, nonempty sides, no Unicode whitespace/control), and accepts
dotless domains. Hash generation/format checking is auth-owned and happens before
core; core accepts nonblank encoded material only. New credential reads use exact
Java canonical email equality. Ordinary user reads preserve legacy case-insensitive
lookup while filtering any PostgreSQL case-folding alias with a different Java
canonical identity. The unchanged lower(btrim(email)) unique index can reject extra
Unicode equivalents under the selected database collation; this returns generic
ACCOUNT_UNAVAILABLE. No NFC or ASCII-only restriction is introduced.

Each write/confirmation owns READ COMMITTED / REQUIRES_NEW commit and rollback,
locks existing users in PostgreSQL UUID order before consultation reads, and uses
5-second lock / 10-second statement timeouts. A surrounding caller transaction does
not become atomic with this boundary. Contention is TEMPORARILY_UNAVAILABLE; only
structured `users_email_unique` violations mean ACCOUNT_UNAVAILABLE. Other storage
failures become STORAGE_FAILURE with no retained SQL cause, since PostgreSQL error
DETAIL can contain the encoded hash. This deliberately trades raw outward database
diagnostics for a safe internal error boundary; add any future internal observability
without logging SQL parameters or failing rows. No automatic retry occurs.

Trusted `AdminContext` comes from auth, which remains responsible for establishing
caller identity. Confirmation accepts only observations after outside-lock password
verification. It returns identity/generation after commit; auth must still recheck
its deadline/reservation before issuance and fresh state/generation for protected
requests. A deactivation after confirmation can invalidate its result immediately.
The PostgreSQL driver already used at runtime is now compile-visible solely for
structured constraint metadata, with the same BOM-managed dependency/version.

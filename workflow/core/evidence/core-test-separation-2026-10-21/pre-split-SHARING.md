# Shared core development baseline

Prepared on 21 October 2026 for teammate review. No commit, push or production
activation is included. This package contains existing core implementation and its
tests; it adds no authentication or management write implementation.

## Included contracts

`unichope.platform.core.model` contains users, roles, modules, tutor assignments,
slots, bookings and validation. The four interfaces in `core.repository` expose
read operations; their `core.postgres` adapters accept a `JdbcClient`.
`PostgresConsultationLifecycle(DataSource, Clock)` implements these atomic writes:

| Method | Preconditions and result |
| --- | --- |
| `createSlot` | Active Tutor, active assigned module, strictly future positive interval, no active slot overlap; creates AVAILABLE slot. |
| `cancelAvailableSlot` | Active owning Tutor and AVAILABLE slot; retains it as CANCELLED. |
| `bookAvailableSlot` | Active Student and eligible future AVAILABLE slot, active assigned Tutor/module, no overlapping ACTIVE Student booking; inserts ACTIVE booking and marks slot BOOKED. |
| `cancelActiveBooking` | Active owning Student, ACTIVE booking and future BOOKED slot; cancels booking and releases slot together. |
| `completeActiveBooking` | Active owning Tutor and ACTIVE booking/BOOKED slot; completes both, including permitted early completion. |

Actor UUIDs must come from trusted server authentication. Role endpoints must not
accept a caller-selected actor ID. Core is independent of role packages and HTTP.
`LifecycleFailure` exposes stable domain codes; role/web adapters map them to HTTP.

Mutations own READ COMMITTED / REQUIRES_NEW transactions. They finish commit or
rollback before returning; wrapping them in another transaction does not combine
those commits. Parent locks use PostgreSQL UUID order: users, modules, slots,
bookings. Admin eligibility writes must follow the shared lock protocol; arbitrary
SQL is not covered. Slot timestamps are truncated to microseconds before validation;
intervals are half-open. Lock and statement timeouts are 5 and 10 seconds per wait/
statement, not an overall operation deadline. No automatic mutation retries.

Existing Tutor read projections/service and their PostgreSQL adapter are included
because core read tests cover that integration. There are no Tutor REST controllers,
MVC/security dependencies, AI code, frontend, guides or reflections in this package.

## Schema and authentication boundary

`src/backend/src/main/resources/db/core-review/V1__create_core_tables.sql` defines
users, modules, tutor_modules, slots and bookings for explicitly isolated tests.
It is not in the default Flyway location, is not automatically applied, and must
not be used as the production migration. Application datasource/services are not
automatically wired. Boot currently provides a non-web backend scaffold.

The DDL's historical credential-pending comment predates the auth spec's selection;
the review-only SQL is preserved unchanged from the verified core increment.
The committed [auth spec](../auth/TECHNICAL-SPEC.md#selected-credential-storage)
selects `passwordHash` on the existing users record and `sessionGeneration` for
revocation. This package's User model, repository and test schema do not yet expose
or persist them. Auth owns hashing/session behavior; core owns the required storage
extension and atomic provisioning/revocation operations. These remain a subsequent
reviewed increment, together with Admin management writes and production wiring.
Do not implement a duplicate users/credentials table around this baseline.

Auth supplies the real actor adapter. `tutor.CurrentActorProvider` remains an
Optional UUID seam; it does not establish the eventual shared auth principal type.
Student AI and core owners must coordinate production migration versions. Review-only
V1 does not reserve a production migration version. Existing Admin/Auth specs are
preserved and remain their owners' contracts.

## Build and test

From `src/backend`, with Java 25 (Gradle wrapper 9.1.0):

```sh
./gradlew test bootJar
```

This runs unit tests only. For database and concurrency coverage, supply an explicitly
disposable PostgreSQL 17 database. Tests create and drop uniquely named schemas;
the supplied user needs permission to create schemas. Never target shared dev/prod.

```sh
./gradlew test postgresTest bootJar \
  -Dcore.test.jdbcUrl="$CORE_TEST_JDBC_URL" \
  -Dcore.test.user="$CORE_TEST_USER" \
  -Dcore.test.password="$CORE_TEST_PASSWORD"
```

`postgresTest` fails when its database configuration is missing; it does not silently
skip. Reports appear in `src/backend/build/reports/tests/{test,postgresTest}`.
The supplied isolated-database command is local verification, not deployed auth or
proof of compatibility with the future credential-enabled schema.

MP2 source attribution and changed behavior are in the [reuse inventory](../REUSE-INVENTORY.md).

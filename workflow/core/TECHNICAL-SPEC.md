# Core Migration Technical Specification

Status: First increment approved on 20 October 2026; shared models/read persistence independently verified. Credential storage remains
pending confirmation with the auth owner; final human implementation acceptance is pending.
Date: 20 October 2026
Owner: Huanghao (Tutor and platform/core)
Inputs: [Shared technical spec](../SHARED-TECHNICAL-SPEC.md),
[integration spec](../INTEGRATION-SPEC.md), [execution plan](PLAN.md).

## 1. Goal and Boundaries

Adapt MP2's shared domain and storage to PostgreSQL under `unichope.platform.core`,
provide the existing Tutor read service with real persisted data, and subsequently
implement the shared consultation lifecycle. Preserve existing behavior except for
the explicitly specified MP3 validation/security/transaction changes.

The human approved shared models, PostgreSQL schema/repositories, and Tutor read
integration, explicitly retaining the credential decision as pending and prohibiting
commit/push. Original review preparation made no application/database changes.
The first increment followed that scope. The separately approved
[lifecycle increment](LIFECYCLE-PLAN.md) is now implemented and independently verified;
its human implementation acceptance remains pending.

Student owns shared AI infrastructure. The auth owner retains password/session/CSRF
implementation; role owners retain controllers, role rules, DTOs, and UI. Core stores
shared data and enforces shared lifecycle invariants, not a second authentication
system or role-specific AI behavior. Private notes remain Tutor-owned.

## 2. Baseline and Source Mapping

MP3 input HEAD: `50c6213abdde9ee5063c7f333e448b33ac83d59b`, with dirty/untracked
inputs identified by [review manifest](review-inputs-2026-10-20.json).
MP2 reference: `d6a9a244f960854b2addb1546b45e506217f4823`.
The manifest identifies contents but is not a recoverable snapshot of every input.

| MP2 source | Proposed destination/treatment |
| --- | --- |
| `model/Validation.java` | `platform/core/model/Validation.java`; retain validation, adapt packages |
| `model/user/{User,Student,Tutor,Admin,Role}.java` | Matching `platform/core/model/user/`; reuse immutable role-fixed user design |
| `model/module/{Module,TutorModule}.java` | Matching `platform/core/model/module/` |
| `model/consultation/{ConsultationSlot,SlotStatus,Booking,BookingStatus}.java` | Matching `platform/core/model/consultation/`; slot remains a domain value without joined module name/code |
| `data/repository/{UserRepository,ModuleRepository,SlotRepository,BookingRepository}.java` | `platform/core/repository/`; retain useful reads, revise write visibility and remove note methods |
| `data/repository/ConsultationLifecycle.java` | `platform/core/ConsultationLifecycle.java`; explicit guarded operations instead of generic exclusivity callback |
| SQLite schema in `data/sqlite/SqliteDatabase.java` | New PostgreSQL Flyway migration; reuse logical design, replace SQLite syntax/types and add declared constraints |
| `data/sqlite/Sqlite*Repository.java` | New `platform/core/postgres/Postgres*Repository` implementations using JdbcClient; not copied SQLite implementations |
| `data/sqlite/SqliteConsultationLifecycle.java` | New `platform/core/postgres/PostgresConsultationLifecycle`; preserve transition intent, add current actor checks and agreed row locks |
| `src/test/java/model/DomainTest.java` and relevant SQLite contract/lifecycle tests | Adapt under matching MP3 test packages; valid FK fixtures and real PostgreSQL race tests |

Do not copy MP2 `Repositories` wholesale: it bundles authentication and note-bearing
contracts that are outside shared core responsibility. Do not copy JavaFX, local
databases, credentials, seeded admin passwords, or SQLite transaction plumbing.
Measure actual unchanged/adapted/replaced material in the existing reuse inventory.

## 3. Core Tables and Auth Boundary

Proposed first migration: `V1__create_core_tables.sql`, only if that version is still
unused when implementation starts. Inspect all merged/pending role migrations first.
Never overwrite an applied migration or reserve all future numbers for core.

Until credential storage is confirmed, use a credential-free review schema only at
`src/backend/src/main/resources/db/core-review/V1__create_core_tables.sql`. Tests
explicitly select that Flyway location against disposable databases. Do not put it
in default `db/migration`, enable automatic startup migrations, or claim a finalized
production V1. The nullable password_hash proposal below is not yet accepted.

| Table | Key shape and constraints |
| --- | --- |
| users | UUID PK; nonblank name/email; case-normalized email uniqueness; role check; active boolean; nullable password_hash reserved for auth |
| modules | UUID PK; nonblank name/code; case-normalized code uniqueness; active boolean |
| tutor_modules | Composite tutor/module PK; foreign keys to users/modules |
| slots | UUID PK; tutor/module foreign keys; timestamptz start/end; end > start; status check |
| bookings | UUID PK; student/slot foreign keys; timestamptz createdAt; status check; unique ACTIVE booking per slot |

Use `slots`, as in the agreed MP3 integration contract; MP2's physical name is
`consultation_slots`. This is a fresh synthetic dev/test database, not an in-place
upgrade or import of MP2 personal data. No hard-delete/cascade-history behavior.
Add indexes for assignment and owner/status/time queries actually used by repositories.
Foreign keys do not establish that a user is a tutor/student: lifecycle/assignment
operations validate roles and current eligibility under the shared locks.

Auth proposal for review: retain MP2's nullable `password_hash` column on users,
consistent with the shared logical User contract. Null means credentials have not
been provisioned; the auth owner must deny password authentication in that state.
Core domain models and ordinary reads never expose the hash. The auth owner owns
credential reads/writes, hashing, validation, and account/password lifecycle. Confirm
this boundary before V1; `must_change_password` is added only if the auth owner's
reviewed feature retains that behavior. No default admin seed or fake password hash.
An auth-owned credential table is an alternative only if agreed and reflected in
the shared integration contract before migration, rather than added speculatively.

Private notes, AI quota/attempt tables, bootstrap metadata, password resets, and
role-specific report/agenda tables are outside core V1. Their owners coordinate
migration order and foreign keys with core.

## 4. Repository and Write Boundaries

Public core read contracts provide the required find-by-ID, owner, assignment,
email/code, and booking lookups without exposing JDBC types or secrets. Ordinary
read results are immutable. SQL is parameterized and timestamps preserve instants.

Do not mechanically retain public `save` on SlotRepository/BookingRepository: role
services must not use it to bypass ConsultationLifecycle. Keep raw slot/booking
insert/status methods inside the PostgreSQL implementation boundary. Explicit
lifecycle methods own create/cancel/book/complete transactions. Test fixture inserts
are test-only SQL, not a public application mutation endpoint.

User/module/assignment management contracts must coordinate with Admin and follow
the same locking/eligibility rules; a plain unrestricted upsert is not a safe web
business operation. Their management APIs and auth account creation are not built
in this first read-persistence increment.

## 5. Reconcile the Existing Tutor Slice

Replace Tutor-local `AccountRole` and `SlotStatus` with shared `Role` and `SlotStatus`
in the same change as their consumers/tests. Retire the temporary local
`ConsultationSlot` type rather than keeping two authoritative domain slot models.

Keep `SlotView`, `ModuleOption`, and `AssignedModule` as Tutor-specific projections.
`TutorAccount` can remain a credential-free authorization projection. A Tutor-owned
`PostgresTutorReadRepository` implements the existing TutorReadRepository using
core contracts and joined read queries as needed. Shared core never imports Tutor.
The joined module code belongs in the Tutor projection/adapter, not in core slots.

The service still obtains identity only from CurrentActorProvider and checks current
active Tutor state. Real session wiring belongs to auth. Until it is supplied, test
the adapter/service against synthetic trusted actors in PostgreSQL integration tests;
do not publish a browser-controlled actor or call it an authenticated web feature.
Preserve the existing four focused tests while adding actual assignment-join tests.

## 6. Lifecycle Increment After Read Persistence

Add explicit slot creation/cancellation, student booking/cancellation, and Tutor
completion. Validate active actor, ownership, assignment/module eligibility,
future times, overlaps, and allowed status transitions inside each transaction.
Use the operation/lock matrix already specified in the integration contract:
users sorted by UUID, modules, slots, bookings, then feature rows.

Tutor parent-row locking protects overlapping slot inserts on empty schedules;
student parent-row locking protects bookings on different overlapping slots.
Partial ACTIVE-slot uniqueness is an additional constraint. Re-read after acquiring
locks. Repeated invalid transitions fail without side effects; exceptions roll back
both status updates. No LLM/network calls while locks are held.

Provide shared locking support for Tutor-owned note saves without importing note
repositories or model classes into platform/core. No global consultation guard
table or automatic broad retry. Keep actor IDs internal trusted service inputs.

## 7. Environment and Acceptance Checks

Baseline backend has a minimal Spring Boot build, AI foundation, and Tutor read
logic. Docker and psql were not found on the original inspection's PATH. During
this increment, an isolated PostgreSQL 17.6 test binary is provisioned under `/tmp`
and bound only to localhost; no global installation or Docker runtime is claimed.
Actual database tests can use an explicit test URL. This is real PostgreSQL evidence,
not Testcontainers evidence. Future CI may use a PostgreSQL service/Testcontainers;
do not substitute SQLite/H2 or silently skip the required integration checks.

| ID | Acceptance condition |
| --- | --- |
| CORE-01 | Shared immutable models validate required fields; package changes preserve applicable MP2 behavior; no JavaFX/SQLite/auth/role imports in core |
| CORE-02 | Migrations initialize isolated PostgreSQL; FKs, status/time checks, case-insensitive uniqueness and one ACTIVE booking per slot are exercised |
| CORE-03 | JDBC reads preserve UUIDs/UTC instants, owner scoping, and real tutor-module assignment joins, returning no credential fields |
| CORE-04 | Existing Tutor behavior/tests remain valid after shared-type adaptation; PostgreSQL adapter tests cover foreign owner, inactive account/module, empty results, sorting, SGT boundary |
| CORE-05 | Normal Gradle tests and bootJar succeed with retained reports; required PostgreSQL tests run rather than skip; unrelated AI tests remain green |
| CORE-06 | Later lifecycle tests cover empty-schedule inserts, same-slot booking, same-student overlap, cancellation races, eligibility races, atomic rollback, and repeated transitions |

Add JDBC/Flyway/PostgreSQL/Testcontainers dependencies with pinned compatible versions
during implementation only. Explicit test configuration uses disposable PostgreSQL.
Do not add datasource requirements that silently break existing no-database unit
tests or start against production. Default local startup must either use explicit
dev connection settings or report clear missing configuration; never guess a URL.

## 8. Decisions to Review

1. Approve the source mapping, shared-domain reconciliation, and the read-first scope.
2. Accept or amend the credential-storage proposal with the auth owner before schema work.
3. Use the isolated PostgreSQL execution path for local DB evidence; Docker-based
   CI/Testcontainers configuration remains unverified.

These are concrete implementation decisions, not a request to revisit the already
agreed stack, role-first architecture, or ownership. The user has approved the
first increment; only the credential boundary remains pending from these decisions.

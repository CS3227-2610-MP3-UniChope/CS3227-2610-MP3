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
The app-api profile now supplies explicit datasource/service/authentication wiring.
The committed [auth specification](../../workflow/auth/TECHNICAL-SPEC.md) selects
passwordHash and sessionGeneration on users. The core account proposal below now
implements internal storage and atomic provisioning/revocation. Auth/session wiring and runtime V1-V4 migrations exist. Student app-api composition
and its platform.auth actor adapter are specified in stu-c5 and pending this increment.
Its storage decision supersedes the historical SQL comment; reviewed SQL is unchanged.
Core and Student AI owners must coordinate production migration versions.


Core account storage proposal
-----------------------------
`AccountStorage` / `PostgresAccountStorage` add internal credential reads and guarded
account operations. Public `User` and `AccountState` remain credential-free; the
internal snapshot deliberately uses non-bean accessors and redacts diagnostics.
Keep it out of DTOs and custom serializers. The app-api profile wires auth/session/HTTP services.

Only the new isolated auth tests explicitly select `db/core-review` followed by
`db/auth-review`. V1 and the original credential-free tests remain unchanged.
The review extension requires an empty users table; applying it to existing accounts
fails atomically. Runtime numbering is V1-V4; real-account backfill, deployment activation
and owner agreement require separate review.

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

## Running the integrated application

The baseline `app-api` profile activates authentication, Admin and Tutor APIs.
The authorized stu-c5 increment also activates Student with a SessionPrincipal actor
adapter; see workflow/student/changes/stu-c5-auth-integration.md for current evidence. Default startup
remains closed and database-free. Runtime migrations use `db/migration`: core V1,
account columns V2, private notes V3 and initial-Admin marker V4. Existing isolated
review migrations/tests remain unchanged. Use a new empty database for first setup;
these migrations do not backfill credentialless accounts from old review schemas.

Set `UNICHOPE_JDBC_URL`, `UNICHOPE_JDBC_USER` and `UNICHOPE_JDBC_PASSWORD` for a
PostgreSQL database. For first setup only, set `UNICHOPE_ADMIN_NAME`,
`UNICHOPE_ADMIN_EMAIL`, `UNICHOPE_ADMIN_PASSWORD` and run `./gradlew bootstrapAdmin`.
This hashes the password and creates one Admin under a database lock. Repeated setup
is rejected. Remove the Admin credential variables afterward. No default account or
HTTP bootstrap endpoint exists. Never use production credentials for tests.

Run `./gradlew bootRun --args='--spring.profiles.active=app-api,local'` locally.
On Windows use `gradlew.bat`. `local` permits an HTTP session cookie; omit that profile
for production HTTPS. Production uses Secure/HttpOnly/SameSite=Lax cookies and a
30-minute idle session. The deployment proxy must preserve HTTPS correctly; configure
forwarded headers only for a trusted proxy. The server uses one replica/in-memory
sessions; restart requires sign-in again while database records remain.

Authentication endpoints: `GET /api/auth/csrf`, `POST /api/auth/login`,
`GET /api/auth/me`, `POST /api/auth/logout`, `POST /api/auth/register`.
Obtain CSRF first with cookies, send its returned header/token on mutations and
obtain a new token after login/logout. User output contains only id/name/role.
Student registration does not sign in. Password reset/change and forced-change flows
are excluded. Admin APIs are specified in `workflow/admin/TECHNICAL-SPEC.md`.
Tutor APIs now receive trusted identity from the same session through runtime wiring.

AI is unavailable by default. `UNICHOPE_AI_MODE=fake` is permitted only with `local`
and produces explicitly labelled deterministic reports without a model call. Its
in-memory limits support local testing only. No real SoC adapter or shared durable
quota implementation is supplied by this Admin/auth increment; production returns
AI_UNAVAILABLE for nonempty reports. Empty statistics need no provider. Do not treat
fake output as satisfying the final product AI requirement. Integrate the Student-owned
gateway with durable shared limits/deadlines before enabling real reports.


When the shared provider and durable quotas are implemented, `UNICHOPE_AI_MODE=shared`
selects the externally supplied AiGateway bean. Missing bean fails startup; nonlocal
fake responses are rejected. Do not use shared mode before that infrastructure is
verified. Role-level report deadlines and in-flight checks do not replace its durable
request/token accounting. An interrupted provider that still runs retains the
per-account block beyond60seconds until it actually finishes.
## Reusable local sample setup (Windows)

From `src/backend` in PowerShell, with Java 25, Node 24+ and Docker Desktop running:

```powershell
.\scripts\start-local-test.ps1
```

Enter one test-account password when prompted. On later runs use the same password;
the command never resets existing passwords. For the sample database created during
this session, use the password supplied in chat. Passwords are not saved in repository
files. The command builds the backend, creates or starts the isolated
`unichope-manual-db` PostgreSQL 17 container (loopback port 55433), bootstraps its
administrator once, starts the `app-api,local` backend on port 8080 and the frontend
on port 5173, and seeds through the real HTTP APIs. Open
<http://127.0.0.1:5173/login>.

Sample accounts (all use the supplied password):

- `admin@example.edu` (created once through the existing `bootstrapAdmin` task;
  also the default Admin email for `seed-local.ps1`)
- `student1@example.edu` (upcoming booking and cancellation history)
- `student2@example.edu` (no bookings initially)
- `tutor1@example.edu` and `tutor2@example.edu`

The seed includes CS2030, CS2040 and MA1521 with about 20 slots over the next seven
days in Singapore time, including simultaneous tutors and an overnight slot.
Reruns reuse accounts/modules, skip overlapping slots, preserve existing bookings,
and replenish student1's upcoming sample when no future active booking remains.
Use `-BackendOnly` to omit the frontend. The services run in the background and
their startup output is under the ignored `build/local-test/<container-name>/`
directory. Starting an already-running setup reuses its services; restart the
backend separately when Java source changes.

Database records survive `docker stop unichope-manual-db` followed by another setup
run. Removing that container deletes this disposable sample database. This setup
uses its own local database and does not use the PostgreSQL integration-test
container. For an additional isolated instance, the optional `-ContainerName`,
`-DatabasePort`, `-BackendPort` and `-FrontendPort` parameters select its name and
loopback ports.

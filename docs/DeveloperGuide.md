# Developer guide

## Start the web application locally (Windows)

Install Java 25 and Node.js 24 or newer, and start Docker Desktop. Open PowerShell
in the repository root and run:

```powershell
.\src\backend\scripts\start-local-test.ps1
```

Enter a test-account password when prompted. The command creates or starts the
local PostgreSQL database, builds and starts the backend, installs frontend
dependencies if needed, starts the Vite web server, and seeds sample data through
the real APIs. Open **<http://127.0.0.1:5173/login>** when it prints `Ready`.

| Service | Local address |
| --- | --- |
| Web application | `http://127.0.0.1:5173` |
| Backend API | `http://127.0.0.1:8080` |
| Sample PostgreSQL database | `127.0.0.1:55433`, container `unichope-manual-db` |

Sign in with one of these synthetic accounts, using the password you supplied:

| Account | Sample content |
| --- | --- |
| `student1@example.edu` | Upcoming booking and cancelled-booking history |
| `student2@example.edu` | No bookings initially; browse and book consultations |
| `tutor1@example.edu`, `tutor2@example.edu` | Tutor accounts; current web destinations are placeholders |
| `admin@example.edu` | Admin workspace; created once by the existing bootstrap task |

The sample includes three modules and about 20 consultation slots over the next
seven days in Singapore time. Student pages support finding consultations,
booking, viewing bookings and cancellation. The theme switch is available in each
page header.

For later testing, run the same command with the **same password**. Existing
passwords are not reset. Accounts and modules are reused, overlapping slots are
skipped, existing bookings are preserved, and an upcoming sample booking is added
if student1 no longer has one. Database records survive stopping and restarting
the container; removing the container deletes this sample database.

The servers run in the background. Startup output is in
`src/backend/build/local-test/unichope-manual-db/` (`build.txt`, `backend.out.txt`,
`backend.err.txt`, `frontend.out.txt` and `frontend.err.txt`; frontend output files
are created when the command starts that server). Rerunning the command reuses
running servers. Vite updates frontend edits automatically; restart the backend
after Java changes.

Use `-BackendOnly` to start and seed only the backend. Alternate container names
and ports are also supported; see the [setup script](../src/backend/scripts/start-local-test.ps1)
and [backend setup instructions](../src/backend/README.md#reusable-local-sample-setup-windows).

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


### Admin and registration frontend

The Admin workspace is available after Admin sign-in at `/admin`. It includes
users, modules, tutor assignments, booking activity, all-time statistics and the
activity report assistant. Public Student registration is at `/register`; successful
registration returns to sign-in. Existing Student routes are unchanged.

The assistant is read only and displays its source snapshot. Fake-provider
responses are clearly labelled; the default unavailable AI mode leaves management
and statistics usable. For a local mocked demonstration only, start the backend
with profiles `app-api,local` and `--unichope.ai.mode=fake`. Real SoC remains a
shared-provider dependency. No frontend secrets or provider keys are required.

Frontend CI installs Node 24, runs unit tests and builds the frontend. Run browser
checks locally with the frontend README commands; port 5173 must be free because
the test configuration starts a fresh server rather than reusing another checkout.

### Render deployment

See [deployment instructions](DEPLOYMENT.md) for the combined frontend/backend
Docker image, managed PostgreSQL configuration and existing Admin bootstrap flow.
The Docker build excludes local environment files and credentials. Container CI
checks that the complete deployment image builds without provider keys or a database.

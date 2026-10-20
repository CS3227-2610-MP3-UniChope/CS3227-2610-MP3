# Shared Technical Specification

Status: Draft for team review; proposed contracts, not implemented behavior.
Date: 20 October 2026
Product baseline: [Product Specification](PRODUCT-SPEC.md)
Related contracts: [Integration Specification](INTEGRATION-SPEC.md)

## 1. Architecture and Technology

Use one Spring Boot application with role-specific REST controllers and services,
one React application with separate role routes, and one PostgreSQL database per
environment. Do not introduce microservices, a vector database, or an agent runtime
inside the product. Ordinary LLM request/response integration is sufficient.

| Concern | Proposed choice |
| --- | --- |
| Browser application | React, TypeScript, Vite, Bootstrap; npm lockfile |
| Backend | Java 25, matching MP2; Spring Boot, MVC, validation, Gradle wrapper |
| Authentication | Spring Security, Argon2id passwords, server-side sessions |
| Persistence | PostgreSQL, Spring JDBC `JdbcClient`, Flyway migrations |
| AI | Shared `AiGateway` interface with fake and SoC providers |
| Tests | JUnit, Spring Boot Test, MockMvc, PostgreSQL Testcontainers; Vitest, React Testing Library |
| Delivery | Docker, ordinary team-selected hosting, GitHub Actions |
| Product website | GitHub Pages, separate from the dynamic application |

Pin compatible dependency/runtime versions and commit lockfiles when scaffolding.
Java 25 availability in CI and hosting must be verified then. Any toolchain change
must be recorded with its MP2 compatibility impact; no framework version is
implicitly selected by this draft.

Source layout: `src/frontend/` and `src/backend/`, including each application's
tests and resources. Keep workflow artifacts under `workflow/`, guides under
`docs/`, and sanitized development interaction summaries under `logs/`.

## 2. Role-First Structure and Responsibilities

Use the package root `unichope`. Organize by role first; each role owns controllers,
services, feature-specific repository interfaces/implementations, DTOs, and tests.
Do not create an application-wide controllers/services directory for role code.

```text
src/backend/src/main/java/unichope/
  student/                   Student endpoints, services, feature repositories, dto/
  tutor/                     Tutor endpoints, services, private-note repositories, dto/
  admin/                     Admin endpoints, services, statistics queries, dto/
  platform/
    core/                    Shared domain, core repository contracts/implementations,
                             ConsultationLifecycle and transaction/locking support
    auth/                    Spring Security, sessions, credentials, Argon2id
    ai/                      AiGateway, FakeProvider, SocLlmProvider, quotas, audit
    web/                     Safe error mapping, request IDs
src/backend/src/main/resources/db/migration/
src/backend/src/test/java/unichope/   Matching role/platform packages
```

Controllers bind/validate DTOs, obtain session identity, call services, and map HTTP
responses. They contain no SQL, domain rules, or record-ownership logic, and return
DTOs rather than domain/database objects. Spring Security still performs endpoint
authentication/role checks before controllers; these do not replace service checks.

Services enforce current role, active status, ownership, and business rules through
interfaces. They contain no SQL or HTTP concerns. Checks vulnerable to a race must
execute again inside the mutation transaction after locks, before any writes; a
service precheck outside the transaction is insufficient.

Repository interfaces expose domain operations without JDBC types. `Postgres*Repository`
implementations contain parameterized `JdbcClient` SQL and row mapping, not policy.
Spring configuration wires repository/provider implementations into services.

Every slot/booking creation or state change goes through `ConsultationLifecycle`.
Role services cannot call raw core write repositories to bypass it. Tutor note
persistence remains inside `tutor/`; it uses shared locking support without making
`platform/core` import Tutor classes. The lifecycle enforces shared transition rules
under its transaction; role services cannot assume a caller already authorized them.

Role packages can depend on `platform/`, never on another role package. Platform
code must not depend on roles. Keep `platform/core` limited to genuinely shared
domain/storage/lifecycle concerns; private notes belong to Tutor, and optional
agenda/report storage is added only if a reviewed product requirement needs it.

The gateway enforces shared quotas/deadlines/provider failures. Role AI services
select authorized context and validate task-specific output. No LLM call occurs
inside a database transaction. Generation returns a draft; only a separate explicit
Save request may write reviewed content after fresh checks/version validation.

React owns presentation and confirmations, not security policy. Optional targeted
ArchUnit tests may enforce package/layer boundaries after scaffolding; they are not
a prerequisite or a replacement for behavior/security integration tests.

### Current ownership allocation and proposed reviewers

The human corrected the allocation on 20 October 2026: Huanghao owns Tutor plus
`platform/core`; shared AI infrastructure belongs to the Student role. Reviewers
and other assignments below remain proposals requiring teammate agreement.

| Area | Maintainer | Reviewer |
| --- | --- | --- |
| student/ | Student owner | Tutor owner |
| tutor/ | Huanghao | Admin owner |
| admin/ | Admin owner | Student owner |
| platform/core and core schema | Huanghao (Tutor) | Student owner; Admin owner for management rules |
| platform/auth | Admin owner | Student owner |
| platform/ai | Student owner | Admin owner |
| platform/web | Assigned per task | Another affected teammate |
| Deployment/CI | Team to assign | Another teammate |

These reflect the current allocation, not claims that teammates accepted them. Changes to
shared platform code, migrations, or API contracts need another affected teammate's
review. Do not automatically allocate all shared work to Huanghao.

## 3. Authentication and Request Security

SH-AUTH-01: Use stored account roles, not a role supplied at login. Student/Tutor/
Admin provisioning policies beyond login are subject to the responsible owners'
product specifications. No default public admin password or unrestricted setup API.

SH-AUTH-02: Rotate the session identifier on authentication. Production session
cookies are HttpOnly, Secure, SameSite=Lax, path `/`, without a broad Domain.
Proposed idle timeout: 30 minutes, subject to team review. Development may use
non-Secure cookies only on localhost HTTP. Do not store session secrets in browser storage.

SH-AUTH-03: Retain CSRF protection for all unsafe methods, including login/logout.
Use a session-backed token returned by `GET /api/auth/csrf`; React sends its returned
header name/value on mutations. Fetch a new token after login and logout. Never
disable CSRF just to make the SPA work. See the
[Spring Security CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

SH-AUTH-04: Revalidate current active status and role on protected requests, and
again inside mutation transactions. An account deactivated in another session
cannot continue using cached privileges. Invalidate a session whose account has
become inactive; return 403 for that request, then 401 for later unauthenticated requests.

SH-AUTH-05: Production React and `/api` share one origin; local Vite proxies `/api`
to Spring Boot. Broad credentialed CORS is not required. Authenticate API failures
with JSON 401 responses, not HTML redirects. React controls sign-in navigation.

SH-AUTH-06: Proposed login throttle: at most 5 failed attempts per normalized email
per rolling 15 minutes, returning a generic failure/throttle response regardless of
account existence. Counters span sessions, are bounded/expired, and must be tested.
Do not log raw credentials. Account provisioning and password lifecycle remain
shared work requiring a separate reviewed feature specification.

## 4. Data Integrity and Consistency

SH-DATA-01: UUID identifiers, UTC instants (`timestamptz`), and SGT display/filtering.
Interpret an SGT date as the half-open UTC interval from that day's SGT midnight
to the next midnight. Use a controllable server clock in time-sensitive tests.

SH-DATA-02: Use foreign keys, non-null/check/unique constraints, and transactions.
Booking status and slot status must change atomically. Read-after-write against
the primary database reflects a committed mutation. No browser-side authoritative state.
See [PostgreSQL constraints](https://www.postgresql.org/docs/current/ddl-constraints.html).

SH-DATA-03: Use PostgreSQL row locks under READ COMMITTED transactions, following
the operation matrix in the integration spec. Acquire required users first, then
modules, slots, bookings, and feature rows; sort IDs within each group. All paths,
including Admin eligibility mutations, follow the same order. Re-read/check actor,
ownership, status, assignment, and overlap after acquiring the required locks.

Lock the tutor user row before checking/inserting slot intervals, including an empty
schedule. Lock the student user row before checking overlapping ACTIVE bookings,
even across distinct slots. Locking only matching slots/bookings cannot serialize
an empty-result overlap check. Account deactivation, module changes, and assignment
changes coordinate on these same user/module rows. Never introduce a late lock on
an earlier group; discover immutable relationship IDs first, then lock and recheck.
No global consultation coordination row is used in this design. A separate AI
quota row still serializes quota admission only. See
[PostgreSQL row locking](https://www.postgresql.org/docs/current/explicit-locking.html).

SH-DATA-04: Notes use compare-and-set versions; stale saves return a conflict.
Do not add version columns to every mutable record without a stale-write requirement;
slot/booking transitions use locked status preconditions and atomic updates.
Repeated completion/cancellation cannot repeat transitions. Slot creation is not
automatically retried after a lost response; refresh before resubmitting. No
general-purpose idempotency framework is required for this scope.

SH-DATA-05: Flyway owns schema evolution. Test migrations against an empty database.
Do not rewrite an applied shared migration; add a new one. Development fixtures
contain synthetic accounts/data and cannot run in production.

## 5. AI Boundary and Deferred Provider Connection

SH-AI-01: Develop first against a deterministic fake client. The real SoC API link,
model, authentication format, and actual limits can be configured later. Lack of
provider details does not block ordinary Tutor work or fake-backed AI tests.
Production cannot report fake content as a functioning SoC feature.

SH-AI-02: Client interface accepts task instructions, untrusted task input, and a
maximum-output budget; returns text or a typed provider failure. It exposes no
database access, file access, shell, browsing, callbacks, or mutation tools. Select
the provider at deployment, never through a browser request parameter.

SH-AI-03: Only server configuration contains `SOC_LLM_API_KEY`, provider URL, and
model. Development/production use different team-owned keys. Do not put secrets
in `VITE_*` variables, images, committed environment files, responses, or logs.
An authenticated account, not an IP or session alone, is the per-user quota identity.

SH-AI-04: Validate authorization and input before consuming quota. Admission is
atomic across all roles using a separate locked quota-state row plus metadata-only
attempt records. Check per-user rolling windows, provider-wide configured request
and token budgets, and a proposed shared in-flight maximum of one. Do not hold a
database transaction open while waiting for the model. Failed admitted attempts
still consume their reservation; rejected validation/auth requests make no provider call.

SH-AI-05: Tutor proposal: 3 admissions per rolling 5 minutes; one outstanding
generation per account; 30-second end-to-end provider deadline; no automatic retry.
Metadata stores request ID, actor ID, role/task, admission time, reserved budget,
and an in-flight expiry, never note text. Reserve maximum possible token use before
dispatch; do not refund unknown usage. Provider limits/tokenization determine the
reservation calculation and must be verified before enabling the real client.

SH-AI-06: In-flight reservations last 60 seconds, including after client disconnect
or uncertain network failure. Reject conflicting admissions until expiry rather
than starting duplicate calls. The provider must support a bounded request/output
contract compatible with these deadlines; otherwise revise the lease before enabling it.
Rolling counts and reservations survive restart. Quota-store failure denies AI
admission without affecting manual operations; prune expired metadata after it is
no longer needed for rolling accounting.

SH-AI-07: Without valid provider-wide limits or real-client configuration, real
generation is unavailable and makes no call. Fake mode is allowed only in dev/test
and is visibly labeled in the UI. Production can run with AI unavailable until
configuration is supplied, but that does not satisfy the final AI-feature requirement.

SH-AI-08: Bound input bytes, Unicode code points, response bytes, token output,
request duration, and concurrency. Task-level constraints can tighten shared
limits. Tutor values and validation are in its technical specification. Unknown
provider errors become sanitized 502/503/504 responses; never expose upstream bodies.

SH-AI-09: Treat prompts, stored notes, model output, repository issues, and agent
handoffs as untrusted content. Prompt instructions are supplementary controls;
authorization and restricted capabilities are enforced outside the model. Any
future model/tool permission needs an explicit spec change and security review.

## 6. Browser and Error Behavior

SH-WEB-01: JSON contracts and HTTP statuses are defined in the integration spec.
Reject unknown mutation-body fields to catch stale clients and actor spoofing.
Apply validation on the backend even if React already validated.

SH-WEB-02: Render note/model text through ordinary React text rendering; do not use
raw HTML injection or automatically follow suggested URLs. Protected responses use
`Cache-Control: no-store`; do not introduce service-worker caching for private data.

SH-WEB-03: Clear private UI state on sign-out/account change. Ignore late responses
from an earlier actor/selection. On session expiry, keep unsaved text only in the
current page memory and allow reauthentication only as the same actor; discard it
on explicit sign-out or a different actor. Never persist it in local/session storage.

## 7. Environments, Deployment, and Secrets

| Environment | Data and AI | Access |
| --- | --- | --- |
| Local dev | Local PostgreSQL, synthetic fixtures, fake LLM by default; optional dev SoC key later | localhost |
| Automated tests | Disposable PostgreSQL and deterministic fake provider; no real SoC key | Isolated CI/local test process |
| Production | Separate persistent PostgreSQL and production configuration/key | Public HTTPS |

Propose one production backend replica with in-memory HTTP sessions; restart
requires users to sign in again but never loses committed business data. Multi-replica
sessions are outside the initial scope. Durable AI counters live in PostgreSQL.

Use a Docker image that serves the built frontend and API through Spring Boot;
ordinary hosting supplies TLS termination. The hosting choice remains open and
must support Java containers, durable database access, and later SoC connectivity.
GitHub Pages serves product information/docs, not the dynamic backend.

CI checks frontend/backend builds and tests, then builds an immutable image tagged
with the commit. Only trusted default-branch jobs can access deployment secrets;
PR/untrusted agent jobs cannot. Deployment runs migrations once, checks health,
and performs a synthetic-account smoke test. Never make a destructive schema
rollback automatic; use compatible forward migrations and retain a backup before
schema changes. Deployment and restoration instructions belong in the Developer Guide.

No agentic built-in hosting or existing SDD toolkit. Avoid paid hosting where
possible and disclose costs. The working branch is currently `main`; reconcile
the assignment's required grading branch `master` before release, not silently in CI.

## 8. Verification and Evidence

- Domain tests: times, ownership, validation, state transitions, note versions.
- MockMvc tests: session/CSRF, inactive/wrong-role users, cross-owner identifiers,
  HTTP contracts, sanitization, and zero provider calls on denied input.
- PostgreSQL Testcontainers integration tests: migrations, simultaneous writes,
  empty-schedule overlap, one student's bookings on different slots, rollback,
  eligibility-change races, constraints, note conflicts, durable quota admission,
  and restart persistence. CI requires a working Docker-compatible runtime.
- Full MockMvc request-to-PostgreSQL tests with real services/repositories for role,
  active-account, CSRF, ownership, and transition behavior; mocked controllers alone
  cannot establish that layers integrate correctly.
- Real SoC adapter transport tests against a local mock HTTP server for auth/request
  mapping, HTTP errors, malformed/oversized bodies, and bounded timeouts. A fake
  gateway cannot verify the real provider adapter. No real key is needed for these tests.
- React tests: filters, confirmations, errors, drafts, selection races, sign-out.
- Small real-provider evaluation later: synthetic normal/adversarial notes,
  grounding, quotas, and failures. Never use exact model wording as the oracle.
- Deployment check: HTTPS, separate resources/keys, no public secrets, durable data.

Use the team's own analyst/developer/reviewer handoffs. Each task records spec
revision, scoped files, acceptance IDs, commands/results, deviations, and human
verification. No hidden reasoning or raw secrets are required. Detailed task
sequencing is in the [Tutor Plan](tutor/PLAN.md); all tasks remain pending.

## 9. Open Team Decisions

Approve the stack/toolchain, shared mutation protocol, login throttle/session policy,
Tutor proposal limits, privacy policies, and shared ownership. Select hosting and
assign deployment responsibility. Real SoC endpoint/model/quota verification is
explicitly deferred to provider activation and release validation, not a blocker
for drafting, scaffolding, or fake-backed development.

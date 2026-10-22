# Admin technical specification

Status: draft for review, 21 October 2026. Owner Kok Seng; reviewer Student.
[Product](PRODUCT-SPEC.md), [shared technical](../SHARED-TECHNICAL-SPEC.md),
[integration](../INTEGRATION-SPEC.md) at main98ca801 are inputs.

## Structure and inherited conventions

src/backend/src/main/java/unichope/admin: controllers/DTOs, AdminService,
ActivityReportService and StatisticsRepository/Postgres adapter. No SQL/ownership
rules in controllers; services depend on platform/core, auth, ai and web contracts,
never Tutor/Student packages. Core owns physical users/modules/assignments storage;
Admin coordinates management writes via reviewed core interfaces/transactions.
No blanket versions, paginated envelopes, /api/v1 or global write guard.
READ COMMITTED and existing shared user/module/slot/booking lock order apply.
UUID/camelCase/UTC/SGT, unpaginated arrays,64KiB JSON body, safe shared errors and
no-store apply. Dates are single SGT date filters, no new period model in baseline.

## Proposed Admin API additions

Every path below is relative to /api/admin; current active ADMIN required.
All unsafe methods need CSRF. Explicit role,tutorId and active body fields below
are narrow proposed exceptions to shared no-arbitrary-override rule for Admin
management only; they never select the actor. Other fields/queries rejected400.

UserSummary={id,name,email,role,active}; Module={id,code,name,active};
Assignment={tutorId,moduleId}; BookingSummary={id,studentId,studentName,tutorId,
tutorName,moduleId,moduleCode,startTime,endTime,status}.

| Method/path | Request/filter | Response |
| --- | --- | --- |
| GET /users | optional role,active | 200 UserSummary[], sort normalized email then ID |
| POST /users | {role,name,email,password}; Student/Tutor only | 201 UserSummary |
| PUT /users/{id}/active | {active:boolean} | 200 UserSummary |
| GET /modules | optional active | 200 Module[], sort code then ID |
| POST /modules | {code,name} | 201 Module |
| PUT /modules/{id} | {code,name} | 200 Module |
| POST /modules/{id}/deactivate | {} | 200 Module |
| GET /assignments | optional tutorId,moduleId | 200 Assignment[], sort moduleId then tutorId |
| PUT /assignments/{tutorId}/{moduleId} | {} | 204 |
| DELETE /assignments/{tutorId}/{moduleId} | no body | 204 |
| GET /bookings | optional moduleId,tutorId,date,status | 200 BookingSummary[], startTime then ID |
| GET /statistics | none | 200 Statistics |
| POST /activity-report | {} | 200 Report |

Account names/email/password use auth proposal. Module code trim+uppercase with
locale-independent rules,1–20 ASCII letters/digits/hyphen; name trim1–200 Unicode
code points; reject controls. User edits/role changes/password-reset endpoints not
included. Invalid fields400 VALIDATION_FAILED; missing target404 NOT_FOUND; Admin
target mutation403 FORBIDDEN; guards409 INVALID_STATE; unique email/code409
DUPLICATE_RECORD. Stale edit policy: serialized last committed edit wins; no optimistic
versions are promised. Shared lock timeouts/deadlocks503 TEMPORARILY_UNAVAILABLE,
no automatic retries. Lost-response creation requires refresh before resubmission.

## Management transactions

Use integration section7 operation matrix exactly. Lock actor+target user sorted
for account activation; actor then module for module edit/deactivation; actor+tutor
sorted then module then existing assignment for assignment changes. Recheck actor
role/active, target/type/state and blockers after locking. All other role writes
must use the same parent locks so absent child rows cannot bypass eligibility checks.
Proposed blockers from product include ACTIVE bookings regardless of age and
AVAILABLE slots with start strictly after injected Clock.instant. No cascade cancel.
Same-state activation/duplicate assignment/absent unassignment is a no-op. Module
edits preserve identity/active. Every slot/booking mutation remains lifecycle-only.
New accounts and password hash commit atomically through auth/core collaboration.
Never port MP2 credentialless AdminService.addUser as independent user creation.

Selected session revocation extension is in auth technical: deactivation changes
session generation atomically, so reactivation cannot revive earlier sessions.
Coordinate this explicit schema addition with core; it does not replace existing
active403/invalidate then401 semantics or change public actor DTO.

## Statistics and report contracts

Statistics={capturedAt,total,completed,cancelled,completionRate,cancellationRate,
byModule:[{id,label,count}],byTutor:[{id,label,count}]}.
Rates percentages HALF_UP two decimals, zero denominator->0. Group by ID, not label;
sort label then ID. Retain inactive records; missing legacy references count in an
Unknown/null-ID group rather than disappearing. Implement one SQL statement/CTE
under READ COMMITTED for a consistent statement snapshot of all aggregates; do not
independently query totals/groups and claim one snapshot. No notes/credentials joins.
AI snapshot contains totals/rates only; per-group display names/email never sent.

Report={capturedAt,statistics,draft,requiresReview:true,providerMode}. providerMode
fake/soc matches shared conventions; empty data mode none with deterministic text
and zero provider calls is an explicit Admin addition. In nonempty case validate
Admin before quota, read snapshot/close transaction, call gateway, validate output,
then recheck session/current account/role before response. No report persistence,
no LLM inside transaction, no domain writes from generated instructions.

Admin-only model JSON {summary:string,caveats:string[]}, exact keys,
summary1–2000 code points,0–5 caveats each1–400. Reject blank summary/caveat,
wrong types, HTML, extra keys and final formatted text>4000 code points. Prompt
version ADM-REPORT-1 asks for counts/proportions only, no invented causes/trends.
Render plain text, never clickable generated URLs/raw HTML. Structural validation
cannot establish truth: require separate evaluation with test data and human checks for factual accuracy.
Use response<=64KiB and an overall30second deadline for Admin gateway request,
one outstanding request/account and3 requests/rolling5min. These are selected Admin
application limits, not verified SoC provider limits.
Shared global in-flight/budgets/durable60second leases remain authoritative.
Use shared409 AI_BUSY,429 AI_RATE_LIMITED+Retry-After,502 AI_INVALID_OUTPUT,
503 AI_UNAVAILABLE,504 AI_TIMEOUT; no retry/LLM repair. Fake-first dev/test; real
limits/model/key verification gates activation only. Statistics work without AI.

## Verification and reuse

Case U1: atomic credentials/unique races; U2/M1/A1: service rules and full PostgreSQL
races with booking/slot creation; B1: full MockMvc-to-DB safe DTO/filter/SGT cases;
S1: identity/empty/rebooking/statement-snapshot cases; R1: fake call counter, exact
permitted input fields, rejection before an AI request, output failures/deadline/quota and zero domain writes.
Independent security review plus actual SoC synthetic evaluation remain separate
verification gates. Backend source/tests and Gradle commands exist at a9dd650;
this specification refresh does not rerun or accept earlier test/review claims.
Assess MP2 AdminService/AdminQueries/tests as candidates; verify revision and local
diff, record original/destination paths, unchanged/adapted/new line counts with
method, carried tests and reasons. Do not copy broad snapshots, shared mutable authenticated-user state,
Java-only guard, private data or default credentials. Existing Admin code is present; reuse measurements are not established here.



Selected report input: total, completed, cancelled, completionRate and cancellationRate
only. Do not send group labels, identities or private notes. The shared gateway gets
trusted actor identity for quota accounting but the provider prompt does not. Use
{summary,caveats} validation above, plain-text output, no persistence or automatic
actions, and no retry. Empty data makes no provider call. Shared provider budgets
may impose stricter limits; real generation stays unavailable until verified provider
token accounting and output-token cap are configured by the shared AI implementation.
Fake-backed development and tests do not require those provider details.

## Runtime implementation notes — 22 October 2026

Use app-api to activate authenticated Admin APIs. AccountStorage provides guarded
provisioning/activation; PostgresAdminRepository supplies module/assignment mutations
with the existing parent lock order and statistics using a single SQL statement.

Report gateway calls use4 workers/16queue and30-second response deadline. Same-user
in-flight calls return409 AI_BUSY with Retry-After. Uncertain calls remain blocked
for at least60seconds and until actual worker completion, preventing overlap when
a provider ignores interruption. This role-level safety does not replace shared
durable request/token budgets. The default runtime has no real provider; local fake
mode is labelled, limited in memory and never claims production quota durability.
Selected development token cap is2048; real shared provider must verify compatible
request/token accounting before activation. shared mode accepts an externally supplied
AiGateway bean, rejects fake results outside local and retains role safeguards.

# Authentication technical specification

Status: draft, 21 October 2026. Owner Kok Seng; reviewer Student.
References: [Product](PRODUCT-SPEC.md), [shared](../SHARED-TECHNICAL-SPEC.md),
[integration](../INTEGRATION-SPEC.md). Baseline main50c6213.

## Shared compatibility

Keep /api,64KiB JSON body bound, shared error envelope and existing exact actor DTO.
Do not introduce /api/v1, persistent/multi-replica sessions, blanket version columns,
a second user table or independently chosen throttle. Current core User includes
passwordHash; do not move it into a credentials table without reviewed core migration.
platform/auth owns hashing/authentication logic, core owns physical user storage.
Collaborator interface for atomic credential-bearing user creation is agreed with
Huanghao before implementation. Platform must not import Admin role classes.

## HTTP contract

Existing endpoints are normative proposals in Integration section2:
GET /api/auth/csrf ->{headerName,token}; POST /api/auth/login {email,password}
->200{id,name,role}; GET /api/auth/me ->same; POST /api/auth/logout ->204.
Private responses no-store. Missing/invalid CSRF403 CSRF_INVALID; other status/code
semantics remain shared. Filter ordering may affect denial precedence, never access.
Login malformed JSON/missing/wrong-type fields400 VALIDATION_FAILED; syntactically
invalid email/password strings use generic401 INVALID_CREDENTIALS without echo.
Success regenerates session/CSRF; caller must GET fresh CSRF before next mutation.
Logout clears cookie; repeat with expired session and freshly obtained anonymous
CSRF remains204. Unknown mutation/query fields400 per shared convention.

New proposed registration: POST /api/auth/register {name,email,password,confirmation}
->201{message:"Registration completed"}, no session authentication. Duplicate409
ACCOUNT_UNAVAILABLE, invalid400. Browser cannot choose role/active state. Provision
entry is /api/admin/users (Admin spec), delegates shared auth/core collaborator.
No public bootstrap endpoint. No additions to login or /me DTO needed in baseline.

## Components and locking

unichope.platform.auth: AuthController and DTOs, AuthenticationService, current actor
resolver, password-hasher interface/Argon2id adapter and Spring Security config.
No SQL in services/controller; repositories via core interfaces. Trusted actor is
request-scoped ID plus current stored role, not singleton mutable state.
Hash before short transaction; core shared row-lock order remains users -> modules
-> slots -> bookings -> feature rows, IDs sorted within groups, READ COMMITTED.
Register/provision inserts user+hash together with normalized unique-email constraint.
Provision transaction locks Admin actor and rechecks role/active before insert.
Bootstrap uses a reviewed transaction-level advisory lock and persisted bootstrap
marker; do not invent core schema migration without owner review.

Use proposed MP2 Argon2id candidate parameters only after compatibility/runtime
benchmark and team review; not a dependency version claim. Unknown-email login uses
a dummy hash verification to reduce obvious timing differences. Read credential,
verify outside locks, then lock/re-read current account/hash/role before issuing
session principal; reject changed state rather than authenticating stale credentials.
Recheck current user on protected requests and in mutations. Selected core extension: sessionGeneration on User, stored only in internal
principal, not public DTO. Deactivation increments it atomically under target user
lock; reactivation never decrements it. Login records only generation actually
verified under the same user lock. Every protected request compares it with current
stored generation. Inactive detection still returns403 ACCOUNT_INACTIVE and invalidates;
active account with obsolete generation returns401 UNAUTHENTICATED and invalidates.
This prevents session revival even if reactivation precedes the next request, without
persistent HTTP sessions or blanket entity versioning. Coordinate this schema extension with core before dependent implementation. Test concurrent login /
deactivation/reactivation and no-intervening-request reactivation. Backend restart
still discards all HTTP sessions per shared single-replica design.
Throttle store: bounded in-memory keyed counters on the single replica, independent
of sessions and normalized account existence. Serialize admission/failure recording
per normalized-email key; reserve slots for outstanding login checks so concurrent
requests cannot bypass five-failure window. Success clears its reservation, not
other failures; exception releases reservation or records failure deterministically.
At most5 outstanding+failed in window; reservations expire at configured bounded
login deadline.429 AUTH_RATE_LIMITED and integer Retry-After; unknown users identical.
Restart clears counters; this is accepted for the initial single-replica deployment. Configure
trusted proxies; no user-controlled forwarded-header key. No raw emails as log keys.

## Planned tests and reuse

L1/S1/C1/E1 full MockMvc session/CSRF/role tests with real services/PostgreSQL;
registration/provision G1/P1 unique race/rollback/migration tests; I1 bootstrap race;
T1 fixed clock counter admission/expiry/capacity tests. Assert safe DTO/error/log
fields, exact inactive403 then401, cross-role/forced actor rejection. PostgreSQL
Testcontainers for real transaction rules; no mock substitutes for races. Backend
restart signs out but retains committed data. No test command exists before app
scaffold; plan must establish actual commands/results then.
Assess MP2 AuthenticationService/Argon2PasswordHasher and tests without copying
data/default credentials. Verify exact source revision/diff; measure reused/adapted/
new lines, document changed desktop/session policies. No reuse measured or tests run.


## Throttle review corrections

Use a configurable hard cap of 10,000 normalized-email buckets and a 10-second overall
login deadline, including hash verification. These are the selected application defaults. Remove expired buckets only; never evict unexpired failure/reservation history
when requests use many different email addresses. At capacity, deny new-key admission generically with429
AUTH_RATE_LIMITED and Retry-After; existing keys still use their own limits. No
account-existence-specific response. Test requests using many different email addresses cannot erase a blocked key.
A reservation has an unique ID identifying the login request and expiry; after verification, require
still-owned unexpired reservation and remaining deadline immediately before session
issuance. Expired/cancelled worker cannot issue authentication even if hashing finishes
later. Releasing old ownership cannot erase a later reservation. Test slow-hash,
expiry/reuse, and capacity races with fixed clock. Timeout returns503
TEMPORARILY_UNAVAILABLE and no session; never automatically retry. Bound the worker
pool independently of request arrivals so abandoned hashing cannot exhaust resources.



## Selected credential storage

Keep passwordHash on the existing core User record, as defined by the shared data
model; do not create a second credentials/user table. Auth owns validation and
Argon2id hashing; core provides the parameterized storage operation. Registration
and Admin provisioning insert the user and hash in one transaction, with normalized
email uniqueness enforced by PostgreSQL. A failed insert commits neither account
nor credential. Never return the hash in public DTOs. Coordinate migration details
with core without duplicating its storage implementation.

The internal sessionGeneration is a nonnegative bigint defaulting to zero. Increment
only when active changes from true to false, atomically with that change. It is not
an entity edit version. Login stores the generation verified under the user lock;
protected requests compare current generation before granting access. Same-state
requests do not increment it. Reactivation requires fresh login for prior sessions.

The login defaults are five failed attempts per normalized email per rolling15minutes,
30-minute idle session expiry, maximum10,000 counter keys and10-second login deadline.
Use at most four password-verification workers and queue at most16; reject excess
with503 TEMPORARILY_UNAVAILABLE. Queue time counts toward the deadline. Do not evict
active failure histories; full new-key store returns429. Slow/expired workers never
issue a session. These values may be tuned with explicit spec/test updates.

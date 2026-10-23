# Core Authentication Storage Specification

Status: Implemented and independently verified as PR #2 on 21 October 2026;
teammate agreement and final implementation acceptance remain pending.
Date: 21 October 2026
Scope: Core credential/account state and guarded account operations only.
References: [auth technical](../auth/TECHNICAL-SPEC.md),
[auth product](../auth/PRODUCT-SPEC.md), [Admin technical](../admin/TECHNICAL-SPEC.md),
[integration](../INTEGRATION-SPEC.md), [plan](AUTH-STORAGE-PLAN.md).

## 1. Concrete proposal and review decisions

Extend the existing physical users table with password_hash and session_generation;
do not create another users or credentials table. Preserve credential-free public
User projections. Core owns parameterized storage/transactions; auth owns password
validation/hashing, login/session/security policy and throttle. Admin owns its HTTP
management service; it delegates the complete guarded account operation to core.

The auth draft says the current core User includes passwordHash. Actual PR1 Java
User does not, and its ordinary repository explicitly selects five public columns.
Propose interpreting the auth statement as physical account storage and providing
an internal AccountCredentialSnapshot rather than adding a secret component to
every Student/Tutor/Admin value. This interpretation requires auth/core owner review;
it is not recorded as agreed. Public DTOs and ordinary UserRepository remain secret-free.

Other material review decisions: align core email validation with the selected auth
policy; choose Unicode normalization/lookup semantics explicitly; approve the
credential-enabled isolated test migration route below. No implementation or
production migration activation is authorized by preparing this design.

Subsequent implementation authorization: proceed with the proposed physical-row /
internal-snapshot interpretation, auth-aligned dotless email policy, Java canonical
credential lookup with the unchanged (potentially stricter) PostgreSQL uniqueness
index, trusted actor ID/generation context and isolated empty-schema extension.
These are documented implementation assumptions for a focused PR, not teammate
approval. Ordinary email lookup must preserve its existing case-insensitive behavior
while preferring exact canonical matches and never returning a different Java
canonical identity through PostgreSQL's potentially different case folding.

## 2. Storage and compatibility boundary

PR1 commit 1c14c34f73b0792a2c46724d30e8aa7d48dd5e36 is a proposed base, not merged.
Its db/core-review/V1__create_core_tables.sql is credential-free and remains byte-for-byte
unchanged. Existing PR1 fixtures use bare INSERT VALUES and tests assert absence of
credential columns; do not run those fixtures against an extended schema and weaken
their assertions to accommodate this work.

Propose a separate db/auth-review/V2__add_auth_account_state.sql selected explicitly
only by new isolated tests, after the original core-review location, on an EMPTY
core-review schema before explicit-column synthetic seeding. Add:

- password_hash text NOT NULL with a nonblank check and no default. Registration,
  provisioning and new synthetic fixtures supply an encoded hash with the user row.
  Applying this extension to existing credentialless accounts must fail atomically;
  real-account migration/backfill needs a separate reviewed strategy, not fabricated
  hashes, nullable accommodation or a default password.
- session_generation bigint NOT NULL DEFAULT 0 CHECK (session_generation >= 0).
  It is a revocation counter, not an edit version. Never wrap/decrease on overflow.

Use separate disposable schema/database per suite, with explicit column lists in
new fixtures. No default db/migration activation, production schema assumption,
real-account backfill, default password/hash or forced password-change column.
The test V2 name does not reserve eventual shared production numbering. Once the
base and owner decisions are integrated, select a new unused production migration
and deliberate activation as a separate reviewed task; never edit applied V1.

## 3. Internal reads and proposed operation contracts

Names below are proposals; interfaces must not import auth/Admin classes or expose
JDBC. No generic save, unrestricted role editing, password mutation or transaction
callback is introduced.

| Proposed operation | Input/result and boundary |
| --- | --- |
| findCredentialByEmail(normalizedEmail) | Optional internal AccountCredentialSnapshot(User, encodedHash, sessionGeneration); no snapshot for a missing account |
| findAccountState(userId) | Optional credential-free AccountState(User, sessionGeneration), for current request checks |
| registerStudent(name,email,encodedHash) | Core generates ID, inserts active STUDENT plus hash and generation 0 atomically; returns credential-free User |
| provisionAccount(adminContext,role,name,email,encodedHash) | Current active ADMIN; role only STUDENT/TUTOR; inserts account/hash/generation together; returns User |
| setAccountActive(adminContext,targetId,active) | Full guarded Admin target/blocker check and state/counter update in one transaction; returns AccountState |
| confirmVerifiedLogin(userId,observedRole,observedHash,observedGeneration) | Locks user; compares current role/hash/generation and active state; returns credential-free confirmed User/generation or generic rejection |

AdminContext is a trusted internal actor UUID and expected session generation from
auth, never browser-supplied identity. Under actor lock verify stored active ADMIN
and matching generation. If owner chooses an existing trusted-context interface,
reuse it; do not invent another session model. Core confirmation does not verify a
password: only auth may call it after successful outside-lock verification.

Credential snapshots must have redacted toString and never enter ordinary DTOs,
logs, exceptions, handoffs, serialization or public repository projections. Avoid
automatic record toString exposing hash text. No public arbitrary hash endpoint.
Core accepts only already-encoded validated hash material, not raw passwords or
hash-verifier callbacks. Argon2id algorithm/parameters and expensive format/hash
validation remain auth-owned; storage verifies non-null/nonblank contract values.

## 4. Account input policy and normalization question

Auth's selected proposal: stripped name 1–100 Unicode code points; email strip then
Locale.ROOT lowercase, <=254 code points, one @ with nonempty sides and no whitespace
or controls; password validation belongs to auth. Current core Validation.email
requires a domain dot and cannot represent all auth-accepted addresses. A narrower
core constructor would fail valid insert results and later reads.

Recommended owner-reviewed change: align core email acceptance with auth's stated
policy, preserving required identity validation; add meaningful tests for dotless
domains, invalid/control/blank inputs and Unicode normalization. Enforce account-flow
name/email length bounds at the creation boundary; do not gratuitously restrict
unrelated existing model fields. Auth must review any policy refinement.

The existing unique index uses PostgreSQL lower(btrim(email)), while Java uses
strip/Locale.ROOT lowercase; these are not claimed equivalent for Unicode or all
collations. Proposed new flows always persist the Java canonical email and perform
credential lookup by exact canonical email equality. The existing index guarantees
identical canonical emails cannot both persist, but can reject additional distinct
Unicode spellings depending on DB collation; report those as generic account
unavailability. Review whether that extra equivalence is acceptable, or select a
reviewed canonical-key/index change before implementation. Do not silently restrict
email to ASCII or rewrite the PR1 index. Ordinary findByEmail behavior must be checked
against chosen Unicode lookup policy; any necessary adjustment needs explicit tests.

## 5. Atomic operations and lock protocol

Use READ COMMITTED and the existing bounded transaction/lock policy. Each operation
owns one atomic boundary. Do not wrap a core write in REQUIRES_NEW and claim it
commits atomically with unrelated Admin writes. Auth hashes before entering core.
If additional Admin changes must share an account transaction, specify that concrete
combined operation separately rather than exposing a general Supplier callback.

Registration needs no actor row; concurrent normalized-email creation is serialized
by uniqueness. Provisioning locks/rechecks the Admin before inserting. Activation
locks actor and target users sorted, then reads current target type/state/blockers.
Do not acquire another earlier-group user after module/slot/booking locks. Shared
consultation operations already coordinate on the affected user rows. Existing
ACTIVE bookings count regardless of age; time checks use injected Clock after locks.

Admin target accounts cannot be modified by this operation, including same-state
requests. Missing target is NOT_FOUND. For Student/Tutor, after actor/target checks:

| Transition | Guard and result |
| --- | --- |
| active -> inactive STUDENT | Reject any ACTIVE booking; else active=false and generation+1 together |
| active -> inactive TUTOR | Reject affected ACTIVE booking or owned strictly future AVAILABLE slot; else active=false and generation+1 together |
| inactive -> active | No deactivation blocker applied; active=true with unchanged generation |
| same state | No-op, unchanged generation; still authorize actor and reject Admin target |

Retain assignments/history; no cascade cancellation. A failed blocker/counter
overflow/write leaves both active and generation unchanged. Concurrent repeated
deactivation increments once; a later distinct deactivate after reactivation increments
again. Module/assignment management and general account edits remain excluded.

## 6. Login confirmation and revocation handoff

Auth reads a credential snapshot, verifies the raw password outside transactions,
then supplies the observed role, hash and generation. Core locks and freshly re-reads the
same user, rejects missing/inactive/changed role/changed hash/changed generation and
returns current credential-free identity/generation only after committing. The
locked successful confirmation is the authentication-state linearization point;
it does not create or reserve an HTTP session. No expensive verification, network,
password, session callback or throttle callback runs while holding that lock.

Auth must still check its own deadline/reservation ownership before issuance and
perform a fresh state/generation check after confirmation before granting a session.
A deactivation can occur after the core commit or after that final check: returning
a confirmation is not a guarantee that an issued session remains valid. Every
protected request checks current active/generation; obsolete sessions cannot regain
access through reactivation. Auth retains the required inactive-first 403/invalidate,
then401 behavior, and active obsolete-generation401 behavior. This increment proves
storage state/ordering, not HTTP session revocation itself.

## 7. Failure and acceptance contract

Use a small safe typed outcome set: INVALID_INPUT, ACCOUNT_UNAVAILABLE (duplicate
canonical/index email), ACCESS_DENIED (actor/type/generation denial), NOT_FOUND
(missing management target), INVALID_STATE (blockers/overflow), LOGIN_REJECTED
(generic failed confirmation), TEMPORARILY_UNAVAILABLE (bounded contention) and
STORAGE_FAILURE (unexpected storage failure with sensitive SQL detail removed).
No HTTP dependency or hash/SQL identifiers in public messages; auth/Admin maps its
existing public codes, including differing registration/provision duplicate codes.
Unexpected storage failures roll back and propagate as STORAGE_FAILURE without
retaining hash-bearing SQL causes; this preserves failure classification while
keeping database row diagnostics out of caller-visible exception chains.
No automatic retries and no broad exception-to-duplicate mapping.

Proposed provisioning distinction: a valid ADMIN target role is forbidden and yields
ACCESS_DENIED; a null/invalid role value yields INVALID_INPUT. Unknown textual role
values are rejected by the HTTP adapter before the typed core call. Auth/Admin still
classifies missing, inactive or stale-generation actors into its existing safe
401/403 policy; a core ACCESS_DENIED is not permission to map every actor failure to 403.

| ID | Measurable acceptance |
| --- | --- |
| CA-01 | Original review DDL/tests remain unchanged and pass separately; auth extension adds only selected columns to an empty isolated test schema before seeding; application to existing credentialless rows fails atomically without backfill/default hashes |
| CA-02 | Ordinary User/DTOs/read methods exclude hash; internal snapshot diagnostics redact it; nonnegative generation and blank-hash checks tested |
| CA-03 | Student registration role/active/generation fixed internally; malformed input rejected; normalized duplicate races admit exactly one full row |
| CA-04 | Provision only current active matching-generation ADMIN; only Student/Tutor roles; denied/stale actors create no row; user/hash rollback together |
| CA-05 | Deactivation blocker cases include overdue ACTIVE bookings and strict now boundary for available Tutor slots; history/assignments unchanged |
| CA-06 | First deactivate increments once; same-state/reactivation do not increment; repeated cycle increments again; overflow rolls back |
| CA-07 | Controlled booking/slot-creation versus deactivation races share parent locks: either mutation commits and blocks deactivation, or deactivation commits and mutation denies |
| CA-08 | Outside-lock verification can be simulated; confirmation rejects changed role/hash/generation/inactive/missing and succeeds only for matching current snapshot; test-only role changes do not authorize a role-edit feature |
| CA-09 | Confirm/deactivate/reactivate interleavings show generation never revives; postcommit limitation is recorded, not called atomic session issuance |
| CA-10 | Real PostgreSQL failure after a row write/before successful commit rolls back the whole operation; timeout is typed, bounded and leaves DB usable. No artificial split writes or product fault hooks are required |
| CA-11 | Selected email/name behavior is consistent across creation, constructors and credential/current reads; Unicode canonical and DB-equivalence cases have explicit owner-reviewed oracles |
| CA-12 | Normal unit/PostgreSQL/build checks retain reports; no auth HTTP/hash/session implementation, public bootstrap, AI or production activation added |

Use synthetic encoded fixtures, real PostgreSQL and independent connections with
controlled barriers/bounded cleanup. No secrets or real-account data. Independent
test matrix precedes implementation; passing existing tests is not evidence this
new storage contract has been implemented or run.

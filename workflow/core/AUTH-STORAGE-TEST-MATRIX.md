# Core/Auth Storage Test Matrix

Status: Independent design artifact. Implementation was subsequently authorized on
21 October 2026 as a proposal using the assumptions recorded in the specification.
The O1/O2/O3 labels below retain the design-stage review questions; they do not claim
teammate agreement. Execution results are recorded in the [implementation coverage record](evidence/auth-storage-implementation-2026-10-21/developer/case-to-test.md) and [independent review](../handoffs/2026-10-21-review-auth-storage.md).
Date: 21 October 2026. Task: CORE-AUTH-STORAGE-TEST-DESIGN-01.
Independent designer: `/root/lifecycle_test_designer`.
References: [spec](AUTH-STORAGE-SPEC.md), [plan](AUTH-STORAGE-PLAN.md),
[input manifest](auth-storage-test-design-inputs-2026-10-21.json),
[supplemental draft inputs](auth-storage-test-design-supplemental-inputs-2026-10-21.json),
[architect handoff](../handoffs/2026-10-21-architect-auth-storage.md).
All cases below are **designed, not executed**. No runtime, DB or build readiness
was probed. Baseline supplied: original HEAD `508c5bd769e889dea405cb848ddd60429f24ce3b`;
PR1 `1c14c34f73b0792a2c46724d30e8aa7d48dd5e36` is proposed/open, not merged.

## Decisions and conditional oracles

1. **Public User versus physical account:** auth technical wording includes a hash
   on User; the proposed design preserves a credential-free User and adds an internal
   credential snapshot. Recommendation: owners explicitly approve the physical-row
   interpretation and internal type before implementation. AS-04/05 target that
   proposal and are owner-blocked; do not silently alter the shared auth policy.
2. **Email policy:** current dot requirement conflicts with auth-accepted dotless
   addresses. Recommendation: approve auth's strip/Locale.ROOT lowercase and stated
   syntax/length policy consistently at creation, constructors and reads. Separately
   decide whether PostgreSQL lower(btrim(email)) may reject additional distinct
   canonical Unicode values. Either approve that extra equivalence with generic
   ACCOUNT_UNAVAILABLE or review a canonical-key/index change; do not choose ASCII
   restriction, NFC normalization, index rewrite or DB collation equivalence silently.
   AS-07/09/10 remain conditional until the exact owner oracle is recorded.
3. **Migration and trusted context:** approve the separate empty-schema test extension
   route and the auth-owned actor ID/expected-generation handoff. New fixtures must
   not weaken legacy tests or imply production/backfill approval. AS-01/11/12 target
   this proposed boundary and remain owner-review dependent.

The coordinator resolved the provisioning-role ambiguity in draft §7: valid ADMIN
target role => ACCESS_DENIED; null/invalid typed input => INVALID_INPUT. Unknown
textual enums belong to the later HTTP adapter. AS-12 uses that explicit oracle;
it does not establish every actor failure maps to HTTP 403. No HTTP is tested here.

Preparing this matrix approves none of these choices. Synthetic test fixtures
cannot establish that callers are trusted; auth/Admin own their adapters.

## Fixtures, tools and interleavings

Use isolated PostgreSQL review schemas and synthetic users: A active ADMIN, T active
TUTOR, S active STUDENT, inactive variants and an absent UUID. Choose deterministic
UUID order so both actor-before-target and actor-after-target branches are exercised.
Use opaque synthetic encoded-hash fixtures H1/H2 already accepted by the storage
contract; no passwords, Argon2 invocation or real credential material. They are
sentinels for storage/comparison/redaction, not proof of hash algorithm validity.
Generation starts at 0; time is injected fixed N with database-representable precision.

**PG** means normal Gradle PostgreSQL-tagged tests on an explicitly selected disposable
runtime. **UNIT** covers pure validation/redaction/outcome comparison; **REVIEW**
checks source/schema and retained reports. Missing DB configuration fails rather
than silently skips. **D** = designed, needs implementation approval and fixtures;
**O1/O2/O3** additionally needs the corresponding owner/contract decision above.

New auth tests apply unchanged core-review V1, then the separate auth-review V2 to
an EMPTY schema, then seed with explicit columns. Old credential-free suites retain
their original schema/fixtures/assertions separately. No production/default Flyway
location, invented hash backfill, bootstrap or shared migration numbering is implied.
For each denial/failure compare affected rows/counts/hash/generation/history/assignments
from a fresh connection, not merely returned values; compare synthetic hashes only
in assertions without logging them.

Concurrency uses independent physical connections/transactions and bounded latches/
barriers. Establish actual lock waiting using bounded `pg_blocking_pids` inspection
or equivalent controlled database evidence, not arbitrary sleeps. Acquire users
sorted before later groups. Hold/release a parent-row lock to choose which operation
linearizes first; bound joins above configured per-statement limits, propagate worker
exceptions and always roll back/close/drop test fixtures. Finite races supplement
independent lock/transaction review, not a universal deadlock guarantee.

## Cases

| Case / requirements | Fixture and risk | Action / controlled sequence | Expected result and persisted oracle | Layer / readiness |
| --- | --- | --- | --- | --- |
| AS-01 · CA-01/12 | Empty isolated schema; original V1 and credential-free suite inputs | Apply original base then auth extension before any user seed; inspect schema; run old suite separately later | V1 bytes unchanged; exactly selected columns added; hash text NOT NULL/nonblank and no default; generation bigint NOT NULL default 0/check >=0. No default production activation or altered original no-credential assertions | PG + REVIEW · D/O3 |
| AS-02 · CA-01/10 | Base-only schema already contains a credentialless synthetic account | Apply new extension through reviewed migration route | Migration fails atomically; original columns/data and V1 remain usable, no partial auth columns or successful V2 mark. No fake/default hash, nullable accommodation or silent row removal; clean empty-schema route remains separate | PG · D |
| AS-03 · CA-02 | Auth schema with explicit-column seeding | Attempt null/blank/omitted hash; negative/null generation; valid hash with generation omitted | Invalid inserts rejected by DB, zero partial row. Valid hash + omitted generation => 0; constraint checks supplement application contract. Use agreed blank semantics rather than assuming SQL trim handles all Unicode whitespace | PG · D |
| AS-04 · CA-02 | Stored synthetic H1, generation 7, T/S user rows | Ordinary UserRepository reads, account-state reads and operation returns; internal credential read separately | Public User/state fields omit hash; state includes current generation; credential snapshot has exact H1 only on internal path. Ordinary SQL projection remains explicit secret-free; no second users/credentials table | PG + REVIEW · D/O1 |
| AS-05 · CA-02/12 | Internal snapshot carrying distinctive synthetic hash sentinel | Inspect snapshot diagnostics, errors and captured configured log output for success/rejections; inspect DTO/serialization boundaries | toString redacts hash; no hash/raw snapshot in public values/messages/logs, handoffs or serialization. Internal accessor still returns comparison material; no arbitrary public hash endpoint or auth dependency in core | UNIT + REVIEW · D/O1 |
| AS-06 · CA-03 | Empty available canonical email, valid name and H1 | registerStudent; read with fresh connection | One internally generated UUID, active STUDENT, canonical email/name, H1 and generation 0 in same row; returned User secret-free. No caller-controlled role/active/generation; no session issuance | PG · D |
| AS-07 · CA-03/11 | New-flow values at accepted boundary and invalid variants | Name strip length 1/100/101 code points; blank; email 254/255, one/multiple @, blank sides, controls/whitespace, surrounding strip; dotless/Unicode owner cases | Accepted policy values persist and construct/read back consistently; malformed inputs INVALID_INPUT and no row. Code points, not UTF-16 units, determine limits. Do not invent name-control restrictions absent auth policy; dotless/Unicode oracle awaits O2 | UNIT + PG · D/O2 |
| AS-08 · CA-03/04 | H1 null/blank; null required context/IDs; otherwise valid creation fields | Independently register/provision invalid required arguments | INVALID_INPUT for malformed/null material; zero user/credential partial row. Core enforces nonblank encoded material, not raw-password rules or Argon2 parameters; no expensive verifier callback | UNIT + PG · D |
| AS-09 · CA-03/04/11 | No account for chosen email; two create calls with approved-equivalent normalization, same or different names/H values | Barrier-start register/register and register/provision on independent transactions with current A | Exactly one full row succeeds, other ACCOUNT_UNAVAILABLE; winning row's user/H/generation match one request, never mixed; no orphan identity. No precheck-only uniqueness or automatic retry; ordinary infrastructure errors not broadly relabeled duplicate | PG concurrent · D/O2 |
| AS-10 · CA-02/11 | Owner-selected non-ASCII email examples, canonical equivalence and Java/DB-disagreement pair under actual collation | Create then exact canonical credential lookup and ordinary findByEmail; try approved aliases/distinct variants | Java strip/ROOT canonical persisted exactly; lookup/constructor agree. If extra DB equivalence approved: duplicate generic ACCOUNT_UNAVAILABLE, never wrong-account lookup. If rejected: await separately reviewed key/index design. No assumption DB lower/trim equals Java; unknown/none returns Optional.empty | UNIT + PG · D/O2 |
| AS-11 · CA-04 | A active ADMIN/current expected generation; STUDENT and TUTOR target variants | provisionAccount with H1 and valid selected fields | Exactly one new active target with requested allowed role, H1/generation 0, internally generated ID and secret-free return; Admin unchanged, no independent outer/inner transaction claim | PG · D/O3 |
| AS-12 · CA-04 | Missing/inactive/wrong-role A, stale generation after deactivate/reactivate, valid ADMIN target-role input, null role | Provision with each context/role; include actor validity changing while waiting for actor lock | Invalid/stale actor => ACCESS_DENIED; no inserted row; current state/generation rechecked under lock. Valid ADMIN target role => ACCESS_DENIED; null input INVALID_INPUT. No cached session context bypass | PG + controlled wait · D/O3 |
| AS-13 · CA-05/06 | Current A; absent target, Admin target including A itself, same-state Admin request | setAccountActive each target with valid expected Admin generation | Missing => NOT_FOUND; Admin target => ACCESS_DENIED even same-state. No account/generation writes; no self-deactivate loophole | PG · D |
| AS-14 · CA-05 | Active S with ACTIVE booking before/at/after N; CANCELLED/COMPLETED booking controls | Deactivate S in each fresh fixture | Any ACTIVE booking, even overdue, => INVALID_STATE with active/generation unchanged; only historical bookings permits inactive/generation+1; booking/history retained | PG · D |
| AS-15 · CA-05 | Active T with ACTIVE booking; or owned AVAILABLE start >N/=N/<N; CANCELLED/COMPLETED and assignment controls | Deactivate T in each fresh fixture | ACTIVE booking or strictly future AVAILABLE blocks; AVAILABLE at/before N alone does not. Successful change retains assignments/slots/history without cascade; unrelated rows unchanged | PG · D |
| AS-16 · CA-05/06 | Unblocked T/S active g=4; inactive with blockers; generation Long.MAX_VALUE | Deactivate; same-state inactive; reactivate despite deactivation blockers; same-state active; deactivate again; overflow branch | First => inactive g5; no-ops/reactivation keep g5; next distinct deactivation => g6. Overflow => INVALID_STATE, still active/MAX; never wrap/decrease or partially change active. Actor/target checks precede no-op | PG · D |
| AS-17 · CA-06 | Unblocked active target g=0; current A or two current Admins | Two independent repeated deactivations with controlled shared target locking | Both return permitted final state/no-op, target inactive g1 exactly once; after committed reactivation a subsequent distinct deactivate increments to2. No lost increments or repeat increment for same transition | PG concurrent · D |
| AS-18 · CA-07 | S booking eligible slot versus A deactivating S; and T creation versus A deactivating T | Establish shared-parent contention; run both first-commit orders | Book-first => ACTIVE/BOOKED and deactivation INVALID_STATE; deactivate-first => inactive/g+1 and booking ACCESS_DENIED. Create-first future AVAILABLE blocks deactivation; deactivate-first makes create ACCESS_DENIED. No successful mutation using inactive actor or cascade cleanup | PG concurrent · D |
| AS-19 · CA-05/07 | Race fixture: T owns future AVAILABLE slot, S eligible to book. Separate time fixture: same slot but no concurrent booking and no ACTIVE booking | For race, control T parent lock and exercise booking-first versus Admin-check-first order. Separately hold lock while advancing Clock to slot start, then let deactivation recheck | Race: deactivation INVALID_STATE in either order (future AVAILABLE before booking, ACTIVE booking afterward); T stays active with unchanged generation and eventual valid booking may succeed. No compliant deactivation-first success is possible here. Separate time fixture: at start, absent ACTIVE booking, slot no longer future AVAILABLE blocker and deactivation can succeed/generation+1; that does not imply booking remains time-eligible | PG concurrent · D |
| AS-20 · CA-08 | Snapshot from active user (role R,H1,g4), outside-lock verification simulated complete | confirmVerifiedLogin with exact observed values; separately mismatch role/hash/generation, missing/inactive user | Exact active match returns credential-free current identity/generation after commit; every valid-shaped mismatch/missing/inactive => generic LOGIN_REJECTED without identifying reason/hash. Each observed dimension must matter; test-only role edit is not an authorized feature | PG · D |
| AS-21 · CA-08/09 | Auth snapshot R/H1/g4 captured before confirmation | Another compliant test transaction locks/changes role, hash or generation/active; commit before blocked confirmation resumes | Confirmation freshly checks locked current state and LOGIN_REJECTED; old snapshot cannot authenticate. No expensive hash verification/session/throttle callback inside locked transaction; compare only preverified observed values | PG controlled wait + REVIEW · D |
| AS-22 · CA-09 | Active unblocked user at g4; independently controlled confirm and deactivate/reactivate | Run deactivate/reactivate before confirm; then confirm-first followed by deactivate/reactivate before hypothetical issuance | First ordering rejects old g4 even though active again at g5. Confirm-first may validly return g4, but current state becomes active g5 and old confirmation is stale; fresh AccountState exposes mismatch. Core returns no session guarantee. Auth must recheck deadline/reservation before issuance and state/generation per protected request; no HTTP revocation test here | PG concurrent · D |
| AS-23 · CA-04/06/10 | Otherwise-valid register/provision/activation, synthetic DB failure fixture | Test-only deferred constraint trigger raises noncontention failure at commit after row write; separate duplicate versus unrelated DB-failure variants | Entire operation rolls back; no new account/hash or partial active/generation. Failed deactivation does not consume generation; Admin unchanged; fresh connection sees prior state. Unexpected infrastructure failure not ACCOUNT_UNAVAILABLE/success; no product injection hook or separate REQUIRES_NEW side transaction | PG failure injection · D |
| AS-24 · CA-04/08/10 | Holder owns required Admin/target/user row; valid provision/activation/confirmation | Establish actual blocked independent operation beyond configured lock limit; release; explicitly call fresh transaction afterward | Typed bounded TEMPORARILY_UNAVAILABLE with safe message, zero partial writes, no automatic retry; subsequent operation succeeds. If claiming same physical connection recovery, verify deterministic reuse; otherwise claim new-transaction/resource recovery only. Inspect rollback/local timeout settings and unexpected SQL-state classification | PG timeout + REVIEW · D |
| AS-25 · CA-01/02/11/12 | Separate original core and auth-extended isolated suites, chosen owner policies, scoped inputs | Once authorized/implemented, normal unit/PostgreSQL/build checks; independent diff/report and retained-fixture review | Original credential-free regressions pass separately unchanged; auth suite explicit-column seeds/readback valid under selected policy; retain XML/HTML/commands/hashes. No hashing/session/HTTP/bootstrap/Admin HTTP or role-service implementation/AI/production activation; core guarded account operations are included. Actual counts/results required; design rows are not executed test count | Gradle + REVIEW · D/O1/O2/O3 |

## Verification order and limits

Resolve remaining owner choices, approve scope, capture exact
implementation inputs, then schema/redaction/input tests first; guarded operations
and confirmation; real PG races/failure recovery; retained regressions and independent
review. Map parameter variants to executable case IDs, preserve initial failures
and fresh outputs, record deviations rather than weakening oracles for a pass.
No source fixtures or build scripts were inspected for this design run; tools are
proposed verification layers, not executed commands or observed compatibility.

Coordinator review corrected AS-19's initial impossible race oracle. Independent
re-derivation from CA-05 confirms a future AVAILABLE slot already blocks Tutor
deactivation, and booking replaces that blocker with an ACTIVE booking. Both
orders therefore deny deactivation. The separate post-start/no-booking fixture
tests fresh Clock behavior without weakening either blocker or future-booking rules.
This is a resolved draft test-design finding, not an executed-test result.

These cases prove only the scoped storage behavior when actually implemented/run.
They do not verify password correctness, real session expiry/revocation, login
throttles, caller trust, production migrations or any SoC/agent-security claim.

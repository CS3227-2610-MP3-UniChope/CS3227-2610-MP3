Authentication needs credential-bearing account storage and a revocation counter that changes atomically with account deactivation. This adds the core `AccountStorage` contract and PostgreSQL implementation for internal reads, Student registration, Admin provisioning, guarded activation/deactivation and confirmation of a previously verified login snapshot.

This PR is based on #1 and targets `prepare/core-sharing` while that PR is open, so this diff contains only the storage increment. It can be retargeted to `main` after #1 merges.

### Interfaces and behavior

- `AccountStorage`: `findCredentialByEmail`, `findAccountState`, `registerStudent`, `provisionAccount`, `setAccountActive`, `confirmVerifiedLogin`.
- Ordinary `User` values stay credential-free. The internal credential snapshot is not a record/bean and redacts its diagnostic string; callers must keep it out of DTOs/custom serializers.
- Each write owns its short transaction and locks users in PostgreSQL UUID order. Deactivation applies the shared booking/slot blockers and increments the session generation only for an actual active-to-inactive transition. Reactivation does not revive an older generation.
- Login confirmation compares the observed role, hash and generation under a row lock. Auth still owns password verification and session issuance. It must recheck fresh account state/generation plus deadline/reservation ownership before issuance and current state/generation on protected requests; core confirmation does not reserve a valid session.
- Duplicate email classification uses PostgreSQL constraint metadata. Unexpected storage errors return a safe failure without retaining SQL detail that might contain a hash.

### Assumptions for auth/core review

1. Store the hash and generation on the existing physical users row, while keeping existing Java User projections secret-free through a separate internal snapshot.
2. Use the auth spec's dotless-domain acceptance, Unicode code-point bounds and Java strip/ROOT-lowercase canonical email. Credential lookup uses exact canonical equality. Legacy ordinary lookup filters PostgreSQL case-folding aliases against Java canonical identity. The unchanged database index can still reject additional equivalents; these return generic account unavailability.
3. `AdminContext` carries a trusted actor UUID and expected generation from auth. Consumers must not populate it from browser-selected identity fields or assume a surrounding transaction is atomic with this boundary.
4. Only new isolated tests select the unchanged core review schema plus `db/auth-review`. The extension requires an empty users table, has no default password/hash, and fails atomically for credentialless existing rows. Production migrations remain inactive; numbering/backfill/application wiring are separate reviewed work.

### Validation

Developer and independent reviewer each passed 6 unit + 57 PostgreSQL tests, zero failures/errors/skips; `bootJar` succeeded. The independent clean run completed in 37 seconds and found no blocking issues.

Tests cover schema constraints/migration failure, canonical/legacy email lookup, safe diagnostics, unique-email races, stale actors/snapshots, generation cycles/overflow, controlled lifecycle races, deferred commit rollback and timeout recovery. A regression first reproduced PostgreSQL rounding of a clock one nanosecond before a slot start; the guard now floors its SQL boundary to the stored microsecond grid and checks before/exactly/after start.

Existing V1 SQL and baseline tests are unchanged. Verification uses disposable local PostgreSQL 17.6 and synthetic schemas; it does not establish real password/session behavior or deployment readiness. No auth HTTP/hash implementation, AI, workflow records, personal guides/reflections or logs are included.

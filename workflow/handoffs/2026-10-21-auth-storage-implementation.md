# Core/auth storage implementation

Status: Implemented, independently verified, committed and pushed for owner review.
Date: 21 October 2026. Teammate agreement and final human acceptance pending.

[PR #2](https://github.com/CS3227-2610-MP3-UniChope/CS3227-2610-MP3/pull/2)
contains 17 files, commit 3f3ec8b. Branch feat/core-auth-storage in sibling
core-auth-storage checkout targets prepare/core-sharing because PR #1 remains open.
Neither PR was merged. The original dirty main checkout and PR #1 are preserved.

Implemented AccountStorage/PostgresAccountStorage; safe AccountState and internal
redacted credential snapshot; trusted AdminContext; atomic Student registration,
Admin provisioning, guarded activity changes and role/hash/generation confirmation.
Review-only extension adds NOT NULL/nonblank hash without default and nonnegative
generation; it fails atomically on existing credentialless users. Baseline V1 and
all seven protected original tests/fixtures remain unchanged.

Documented proposal assumptions: hash stays on physical users row, ordinary Java
User stays credential-free; auth-aligned dotless/Unicode canonical email with exact
credential lookup and filtered legacy ordinary reads; unchanged DB unique index can
be stricter; trusted actor context supplied by auth; no production activation or
backfill. The existing PostgreSQL artifact is compile-visible for constraint metadata.
Unexpected storage failures redact SQL causes rather than leak failing-row hashes.

Developer /root/auth_storage_developer and independent reviewer
/root/auth_storage_reviewer each ran full unit/PostgreSQL/build checks: 6 unit + 57
PostgreSQL tests, zero failures/errors/skips; bootJar passed. Independent clean run
exit 0 in 37s. All 25 scenario groups map to executable tests/source review. Raw
password/session behavior and caller provenance are not proven by storage tests.

Coordinator identified direct Clock-to-PG rounding at start-minus-1ns. Developer
reproduced a red test, then floored the post-lock SQL comparison boundary to the
stored microsecond grid. Before/exact/after start cases passed independently.
The nullable metadata assertion was corrected to SQL IS NULL without weakening its
no-default oracle. No actionable blocking findings remain after independent review.

Evidence stays local in workflow/core/evidence/auth-storage-implementation-2026-10-21
with input/frozen hashes, red and final logs, independent XML/HTML and case mapping.
Separate [developer](2026-10-21-developer-auth-storage.md) and
[reviewer](2026-10-21-review-auth-storage.md) records retain exact actions/limits.

Controls: instruction-scoped agents inherited workspace writes/restricted network;
reviewed socket escalation used for disposable PostgreSQL/Gradle. Only synthetic
data, existing offline cache and local test schemas were used; no secrets/SoC/global
installs/auth HTTP/deployment. Coordinator performed authorized GitHub commit/push/PR
actions. No workflows, handoffs, logs, guides or reflections were published. Disposable
PostgreSQL was stopped after both verification runs. Await affected teammate review.

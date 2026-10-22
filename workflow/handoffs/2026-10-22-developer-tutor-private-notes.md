# Developer handoff: Tutor private notes

Date: 22 October 2026
Status: Implementation verified locally, independently reviewed, and human-accepted; ready for publication review
Branch: `feat/tutor-private-notes`
Base: `2d298bdd912d5b881a873ea456eb8342d42b91f2` (`origin/prepare/core-sharing`)

## Scope implemented

This work adds manual Tutor notes for owned completed bookings:

- Tutor-owned note record, repository contract, PostgreSQL adapter, and service.
- `GET` and `PUT /api/tutor/bookings/{id}/note` with trusted session actor,
  ownership/completed-state checks, strict request shape, Java `String.strip()`
  and 8,000-code-point validation, and compare-and-set versions.
- The save transaction uses the shared `DataSource` and calls
  `ConsultationLifecycle.lockForPrivateNote` before any note-row access. It does
  not call the AI gateway.
- Safe Tutor API error mapping and isolated test-only DDL at
  `src/backend/src/main/resources/db/tutor-review/V2__create_private_notes.sql`.
  No production migration was added or activated.

## Verification evidence

`./gradlew test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55432/postgres -Dcore.test.user=mp3_test -Dcore.test.password=`
from `src/backend` passed against a temporary PostgreSQL 17.11 cluster bound to
localhost. The unit suite reports 26 tests and the PostgreSQL suite 103 tests;
both report zero failures, errors, or skips. This includes 4 `TutorNoteInputTest`
cases for strict parsing, version range, supplementary Unicode code points,
limits, and Java trimming, plus database-backed note persistence, version races,
authorization, rollback, lock timeout, and schema constraints.

Reports: `src/backend/build/reports/tests/{test,postgresTest}/` and
`src/backend/build/test-results/{test,postgresTest}/`. The temporary server was
stopped and its `/private/tmp` data directory and log removed after the run.
No shared or production database was used.

The current worktree inputs (including untracked implementation sources) are
fingerprinted in `workflow/handoffs/2026-10-22-tutor-notes-inputs.sha256` for
reproducible review. The manifest intentionally excludes its own hash file.

`git diff --check` passed. No commit or push was made.

The first database run exposed a fixture mismatch: Tutor's review-only DDL was
being applied with an unrelated Flyway history. The fixture now applies the
core and Tutor review locations together, with Tutor's script at review-only
version 2. Database execution also caught a test lifecycle wrapper that still
rejected the new note-lock operation; it now delegates to the actual core
lifecycle. The final full PostgreSQL run passed.

## Review focus

1. Confirm that the caller-owned transaction and separate
   `DataSourceTransactionManager` instances join the core seam using the same
   `DataSource` resource in production wiring.
2. Inspect whether load authorization and status checks meet the agreed privacy
   boundary; confirm missing/foreign booking responses are indistinguishable.
3. Run the PostgreSQL tests against a disposable database and verify their cases
   cover the matrix, especially simultaneous first insert/update, deactivation
   during lock wait, rollback, and storage contention.
4. Check additive schema ownership and production migration coordination. The
   review-only Flyway script must not be picked up by production migration setup.
5. Compare the implementation against
   `workflow/tutor/NOTE-INCREMENT-SPEC.md` and
   `workflow/tutor/NOTE-TEST-MATRIX.md`; record findings separately before
   implementation acceptance.

The independent source review initially identified missing PostgreSQL scenarios,
a weak blank-content DDL check, and missing frozen inputs. Those findings were
addressed: the DDL check uses PostgreSQL `chr()` values for Java's
`Character.isWhitespace` set, the additional cases were added, and the worktree
SHA-256 manifest is present and verified. After the final PostgreSQL run, the
reviewer checked the fixtures, matrix coverage, report counts, and manifest and
found no remaining source-level findings. The human owner accepted the
implementation on 22 October 2026; the separate record intentionally omits their
identity and verbatim message. No commit or push has been made.

See [human acceptance record](2026-10-22-tutor-notes-human-acceptance.md).

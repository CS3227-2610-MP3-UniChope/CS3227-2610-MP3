# Core-only test separation

Date: 21 October 2026. Status: Implemented and independently verified; no actionable findings. Human package review pending. No commit or push.

Request: separate Tutor database tests so shared core can be published without role
code; keep preparation workflows local. Original checkout remains on main; prepared
checkout is sibling core-sharing, branch prepare/core-sharing, baseline 508c5bd.

Moved two existing Tutor integration methods from PostgresCoreReadTest into
`src/backend/src/test/java/unichope/tutor/PostgresTutorReadTest.java` in the original
checkout, with independent isolated-database setup. Retained four core methods in
PostgresCoreReadTest and copied that core-only test into the prepared package.
All six method texts are unchanged; production source and SQL are unchanged.
Pre-existing Tutor REST tests and fixtures were not reorganized.

Removed only the copied Tutor files from the prepared checkout; the originals are
preserved. Only 41 required core/build/test/README files are staged. New workflows,
handoffs, reuse inventory and verification records remain local to the original
checkout. The prepared checkout has no unstaged changes or untracked files. Existing
tracked Admin/Auth/shared specifications remain exactly as committed at baseline.

Developer verification: clean core-only test/postgresTest/bootJar exit 0 in 20s;
4 unit + 34 PostgreSQL cases, zero failures/errors/skips. Original focused core and
relocated Tutor cases exit 0 in 8s; 4 + 2 PostgreSQL cases, zero failures/errors/skips.
The six cases removed from the earlier 44-test package are four Tutor unit tests
and two Tutor PostgreSQL tests; all remain in the original checkout. No tests or
expected results were dropped from the overall project.

Scoped input hashes and pre-split file content are archived locally under
`workflow/core/evidence/core-test-separation-2026-10-21/`. Runtime controls unchanged:
workspace writes, restricted network, offline dependency cache /tmp/mp3-gradle-home;
reviewed socket escalation for existing disposable synthetic PostgreSQL and Gradle.
No production data, secrets, installations, external messages or live provider calls.

Independent reviewer reproduced a clean core build (4 unit + 34 PostgreSQL, exit 0
in 18s) and the original focused 4 core + 2 Tutor cases (exit 0 in 7s), all with
zero failures/errors/skips. Independently checked unchanged method/setup/helper
texts, all 41 staged files, no new workflows/role code, and clean whitespace checks.
See [independent review](2026-10-21-review-core-test-separation.md). Disposable
PostgreSQL stopped after verification. Human package review remains pending.

# Independent review: strictly core-only sharing package

Date: 21 October 2026. Reviewer: /root/core_share_review.
Recommendation: Ready for human review of the prepared package. No actionable findings. Human acceptance remains pending. No source edits, staging, commits or pushes performed by reviewer.

## Scope and evidence

Original repository `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/CS3227-2610-MP3`; prepared checkout `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/core-sharing`, branch prepare/core-sharing at baseline 508c5bd769e889dea405cb848ddd60429f24ce3b. Read original agent common/reviewer instructions, existing accepted lifecycle specification, pre-split test `/tmp/mp3-core-test-split-before.java`, manifests `/tmp/mp3-core-test-split-inputs.json` and `/tmp/mp3-core-split-final-inputs.json`, and actual split sources/tests/build/README/stage. Final scoped hashes matched when inspected.

Prepared stage contains exactly 41 files: shared core domain/repositories/lifecycle, review-only DDL, core tests and minimal build/wrappers/readmes/ignore/line-ending attributes. No Tutor, Student/Admin role package, web/security, AI, frontend, newly staged workflow documents, private logs, generated outputs or caches included. Core source/tests have no role/web/security/AI imports. Existing baseline workflow files including Admin/Auth remain unchanged; final `git diff HEAD -- workflow` empty. Prepared checkout has no unstaged changes or untracked files. `git diff --cached --check` and ordinary diff check both exit 0. Unix wrapper staged executable.

Original Tutor product sources remain in original checkout; only the approved read-test separation changes original tests. Four core @Test methods remain in PostgresCoreReadTest; two Tutor methods moved into unichope.tutor.PostgresTutorReadTest. Independently extracted all six old/new methods: same method-name set and all six complete @Test method texts byte-for-byte unchanged. Copied Tutor schema setup, teardown, repository/service helpers, account/slot helpers and UUID helper also byte-for-byte unchanged. Prepared core test equals retained original core test. No oracle/assertion removed or weakened. Main pre-existing Tutor REST test/fixture structure was outside this change and remains excluded from prepared package.

## Independent verification

Reviewed Gradle/settings/wrapper and isolated PostgreSQL fixture before execution. Used existing offline cache `/tmp/mp3-gradle-home` and synthetic local disposable PostgreSQL at 127.0.0.1:55439 (core_test, empty password), never shared dev/prod or a live external resource. Per-test unique schemas created/dropped by fixtures. Actual inherited controls: workspace-write, network restricted; source prohibition is instruction-only. Reviewed escalation allowed necessary local Gradle/database sockets. Reviewer writes only this report and temporary logs/XML/cache/build outputs.

Core checkout command from src/backend:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks clean test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Exit 0, BUILD SUCCESSFUL in 18s, 8 tasks executed. Parsed XML: 4 unit + 34 PostgreSQL = 38 tests, zero failures/errors/skips. `bootJar` succeeds. Log `/tmp/mp3-core-split-independent.log`, preserved XML `/tmp/mp3-core-split-independent-reports/{test,postgresTest}`. Clean plus rerun avoids stale removed-Tutor classes.

Original checkout focused command from src/backend:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks postgresTest --tests unichope.platform.core.postgres.PostgresCoreReadTest --tests unichope.tutor.PostgresTutorReadTest -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Exit 0, 4 retained core + 2 moved Tutor PostgreSQL cases, zero failures/errors/skips. Log `/tmp/mp3-tutor-split-independent.log`, preserved XML `/tmp/mp3-tutor-split-independent-reports`. Original entire REST/auth/AI suite was not rerun because this scope only separates these existing read cases; package full unit/PG/build is verified independently.

## Limits and next step

This supersedes the earlier mixed core/Tutor sharing review. Core source behavior is unchanged. Package is a development foundation, not auth-ready deployment: credential-free review DDL is not production migration, selected passwordHash/sessionGeneration extension is unimplemented, and production wiring/real identity/management writes remain pending. Short backend README states these boundaries and explicitly treats SQL pending-decision header as historical. No new workflow documentation is staged. Coordinator should present this core-only staged diff and verification outcome for human review; commit/push still requires existing user authorization applicable to those actions.

# Independent core-sharing package review

Date: 21 October 2026. Reviewer: /root/core_share_review (independent subagent).
Recommendation: Accept the prepared package scope for human review. No actionable implementation findings. Actual human package acceptance remains pending; no commit/push authorized or performed by this reviewer.

## Scope and controls

Reviewed worktree `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/core-sharing`, branch `prepare/core-sharing`, HEAD `508c5bd769e889dea405cb848ddd60429f24ce3b`. Read original `workflow/agents/{common,reviewer}.md` and original accepted lifecycle specification as governing inputs. Inspected `/tmp/mp3-core-sharing-inputs.json` (61 files), followed by `/tmp/mp3-core-sharing-final-inputs.json` (63 files). All scoped working-file hashes matched on inspection. Later status-only documentation changes require coordinator preservation checks.

Inherited filesystem is workspace-write; reviewer instruction-only write limit allows this report and temporary verification outputs/caches/build files. Reviewer could technically edit source but did not. Network restricted; no dependency fetch, external service, real credentials, production resources, commits or messages to teammates used. Local socket escalation was needed for Gradle and disposable PostgreSQL. Permitted cache `/tmp/mp3-gradle-home`, build outputs under prepared backend, review log/reports under `/tmp`.

## Scope and source checks

- Final staged scope: 57 files containing build/wrapper files, shared core models/read repositories/lifecycle, existing Tutor service/read integration, credential-free review schema, original tests and concise integration/reuse/sharing/verification documentation.
- No REST controllers, web/security/AI/frontend dependencies or implementation, developer/user guides, reflections, private logs or generated reports staged.
- Packaged core/Tutor implementation, SQL and test bytes match original working-tree files. The Boot entrypoint/build intentionally omit HTTP/security/AI. Core has no role/HTTP import dependency.
- Existing Admin/Auth specification files have no baseline differences. Auth now explicitly selects passwordHash and sessionGeneration on existing users; sharing and integration documents correctly say implementation remains pending. No duplicate credentials/users table proposed.
- Review-only DDL is outside default Flyway migration location; app has no production datasource/service wiring. It is a buildable development foundation, not deployable auth-ready integration.
- Generated wrapper JAR matches recorded upstream checksum, distribution ZIP SHA-256 pinned. Unix wrapper staged mode 100755. `.gitattributes` preserves LF shell and CRLF Windows worktree behavior; final `git diff --cached --check` exit 0. Initial check reported CRLF-only Windows-wrapper whitespace until attributes/index normalization, which did not alter working bytes.
- Local Markdown links in root/backend README, sharing guide and reuse inventory resolve. Historical reuse inventory entries describe earlier increments; current sharing guide is authoritative for package readiness.

## Independently reproduced verification

Inspected Gradle/settings/wrapper configuration and test fixture before execution. PostgreSQL fixture uses explicit supplied URL and unique synthetic schemas, creates/drops only those schemas; tagged tests require configuration rather than silently skipping.

Command from prepared `src/backend`:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Initial sandbox invocation failed before build with `java.net.SocketException: Operation not permitted`; reviewed socket escalation rerun exited 0, `BUILD SUCCESSFUL in 19s`, all 7 tasks executed. Independent log `/tmp/mp3-core-sharing-independent.log`; preserved independent JUnit XML `/tmp/mp3-core-sharing-independent-reports/{test,postgresTest}`.

| Task | Tests | Failures | Errors | Skips |
|---|---:|---:|---:|---:|
| test | 8 | 0 | 0 | 0 |
| postgresTest | 36 | 0 | 0 | 0 |

`bootJar` succeeded. Unit counts: DomainTest 3, LifecycleInputTest 1, TutorReadServiceTest 4. PostgreSQL counts: PostgresCoreReadTest 6, PostgresLifecycleTest 19, PostgresLifecycleConcurrencyTest 11. Original accepted lifecycle tests are unchanged, including observed blocking interleavings, both race orders, eligibility/clock rechecks, second-write rollback, lock timeout/connection recovery and microsecond regression coverage. This packaging review adds no feature behavior and no replacement/weakened oracle.

## Remaining limits / next role

Credential model/storage extensions and atomic provisioning/revocation, production datasource/migration activation, real sessions/identity, Admin management writes and deployment remain future integration work. No acceptance or security guarantee for those is implied by 44 passing tests. The preserved SQL header reflects the earlier pending credential decision; sharing guidance should identify that header as historical without changing the accepted test migration checksum. Coordinator may publish concise independent results in verification documentation, then present prepared diff for human review. No source edits or test fixes required by this review.

# Independent Core Read-Persistence Review

Task: CORE-READ-2026-10-20. Date: 20 October 2026 (SGT).
Reviewer: actual independent subagent `/root/core_increment_reviewer`.
Recommendation: accept this scoped read-persistence implementation; human acceptance
of implementation remains pending. Human design approval was already supplied.
No actionable numbered findings. No product/spec source edits, commit or push.

## Inputs and Boundaries

HEAD: `50c6213abdde9ee5063c7f333e448b33ac83d59b` plus dirty/untracked scoped inputs in
[reviewer manifest](../core/reviewer-inputs-2026-10-20.json). Independently verified
all listed hashes against the actual working files; zero mismatches. Also verified
[developer output manifest](../core/implementation-outputs-2026-10-20.json); zero
mismatches. Parent disclosed documentary README/reuse and core-status updates
around dispatch; reviewed current contents, never interpreted those updates as
original human approval. Reviewed architect/developer handoffs and common/reviewer
instructions, core/shared/integration specifications, schema, classes and tests.

Inherited controls: workspace-write, restricted network, auto-review escalation.
Read-only source scope is a behavioral rule, not an enforced read-only sandbox.
Permitted generated paths: backend `build/`, `.gradle/`, temporary Gradle cache;
permitted evidence writes: this record and sanitized review summary. Gradle local
socket access used the tool's `require_escalated` review and was granted. Offline
execution avoided dependency downloads. Actual PostgreSQL was the parent's isolated
17.6 localhost server, synthetic `core_test` account, unique per-test schemas; no
production, SoC, real credentials, global installation or MP2 mutations.

## Independent Checks

- Inspected wrapper/build/settings, Java, SQL, and test setup before execution.
- Parameterized reads return credential-free models. Core has no reverse imports
  from Tutor/Student/Admin/auth/AI, JavaFX or SQLite; no public raw write operations.
- Review-only DDL has UUID/FK/check/index constraints and resides exclusively under
  `db/core-review`. No production V1, password columns, datasource or startup
  Flyway activation was added. Auth boundary remains unresolved as requested.
- Tutor adapter joins actual assignments/modules, scopes slots by owner, and the
  service gates current active Tutor state and applies future/status/SGT/order rules.
- PostgreSQL tests exercise related fixtures, wrong owner, inactive account/module,
  exact date boundaries, ordering, absent records, constraint failures and migration
  history. Four existing Tutor scenarios and five AI foundation tests remain green.
- All 12 adapted domain files and DomainTest equal MP2 after package/import-prefix
  normalization. Reuse figures and replacement-vs-adaptation distinctions are supported.
- MP2 Gradle conventions reproduced: Java 25, Foojay 1.0.0, Gradle 9.1.0 wrapper,
  UTF-8, `-Xlint:all`, and explicit tagged suites; JavaFX application setup excluded.

## Independent Execution

From `src/backend`, inspected then executed exactly:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon --console=plain --rerun-tasks test postgresTest bootJar -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres -Dcore.test.user=core_test -Dcore.test.password=
```

Exit **0**, `BUILD SUCCESSFUL in 8s`, seven tasks executed. Parsed fresh JUnit XML:
**13 unit tests plus 6 PostgreSQL tests; zero failures, errors or skips.** BootJar
packaging succeeded. Existing Student-owned AiGateway.Failure serialVersionUID
warning remains; it is unrelated to this increment. No warning fix was requested.

Fresh generated reports: `src/backend/build/test-results/{test,postgresTest}` and
`src/backend/build/reports/tests/{test,postgresTest}/index.html`. Their independently
read counts and SHA-256 values are retained in the sanitized summary below. Parent
execution reports are separately retained under `workflow/core/evidence/`.

## Limitations and Follow-up

Acceptance recommendation is for adapted domain, review-only PostgreSQL schema,
read repositories and PostgreSQL-backed Tutor read service only. This is not a
production schema/migration acceptance or authenticated web feature. Session/HTTP/
React integration, finalized auth storage, lifecycle writes and race tests, CI,
Testcontainers, deployments and application/SoC AI security remain outside this
increment. Repository objects are constructed explicitly in tests; application
bean/datasource wiring remains future integration. No controls from those future
features are claimed. Parent must stop temporary PostgreSQL after review.

Next: human reviews this slice, confirms pending credential boundary with auth,
then proceeds to the separately specified lifecycle increment. No blocker was found
requiring a developer fix/re-review loop for the current patch.

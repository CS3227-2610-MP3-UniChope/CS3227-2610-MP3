# Core package verification

Date: 21 October 2026. Baseline: `508c5bd769e889dea405cb848ddd60429f24ce3b`.
Scope: shared core plus existing Tutor service/read-repository integration, without
REST, AI or production authentication. Preparation does not imply human acceptance,
commit or push.

From `src/backend`, ran with Java 25, the supplied Gradle 9.1.0 wrapper and a
pre-existing offline dependency cache:

```sh
GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew --offline --no-daemon \
  --console=plain --rerun-tasks test postgresTest bootJar \
  -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55439/postgres \
  -Dcore.test.user=core_test -Dcore.test.password=
```

The database was an existing disposable local PostgreSQL 17.6 runtime, containing
synthetic test users only. Each database test created/dropped its own schema.
It was not Docker or a shared development/production database. Network dependency
resolution was unnecessary. Socket access used the runtime's reviewed escalation.

Developer result: exit 0, BUILD SUCCESSFUL in 19 seconds. Parsed JUnit XML:
8 unit tests + 36 PostgreSQL tests, zero failures/errors/skips. `bootJar` succeeded.
Reports are generated under `src/backend/build/test-results` and `build/reports/tests`;
build outputs and dependency caches are excluded from Git.

Independent reviewer reran the same command: exit 0, BUILD SUCCESSFUL in 19 seconds,
8 unit + 36 PostgreSQL tests with zero failures/errors/skips. Review found no
actionable implementation findings; inspected unchanged test oracles, file scope,
executable wrapper, preserved Admin/Auth specs and stated integration boundaries.
Human package acceptance remains pending. These checks cover
the credential-free test baseline; they do not establish credential-enabled schema,
production wiring, login/session behavior, Admin management writes or deployment.

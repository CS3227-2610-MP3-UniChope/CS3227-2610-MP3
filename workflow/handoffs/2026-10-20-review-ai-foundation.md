# Independent review: isolated AI foundation

Task ID: REV-2026-10-20-01. Actual actor: independent Codex reviewer subagent
`/root/reviewer`; developer was parent `/root`, architect was `/root/architect`.
Recommendation: accept this isolated slice. No blocking implementation findings.
Human verification and acceptance: **pending**.

## Inputs and scope

Input HEAD: `e48694a04d80544ce127fc75cb7bd215dfd713a0`, branch `main`.
Read the custom reviewer instructions, architect readiness and developer handoffs,
AI contract, shared technical specification, relevant Tutor technical/plan and
integration requirements, backend source/build/tests/README, and handoff index.
The working-tree baseline already contained modified `README.md`,
`workflow/PRODUCT-SPEC.md`, `workflow/tutor/PRODUCT-SPEC.md`, plus untracked
`.github/`, `.gitignore`, `docs/`, `logs/`, `src/`, shared/integration/reuse documents,
agent instructions and handoffs. Review attributed the newly implemented backend
to DEV-2026-10-20-01 without treating all pre-existing dirty material as its patch.

Permitted reviewer write: this evidence file only. No source edits, commits,
pushes, deployment, real SoC calls or external messages. Build outputs are ignored.
Requirements assessed: partial P1 and isolated P6; SH-AI-01/02/03/09 foundations.
Eventual SH-AI-04/05/06/07/08, TUT-AI-01/02 and SEC-02/03 are not satisfied by
this slice. Shared dependencies remain trusted actor/auth integration, durable
admission/schema, provider configuration/limits, and role validation/save contracts.

## Reproduced evidence

Backend directory: `src/backend`. Cached executable:
`/Users/huanghao/.gradle/wrapper/dists/gradle-9.1.0-bin/9agqghryom9wkf8r80qlhnts3/gradle-9.1.0/bin/gradle`.
Both Gradle commands used `GRADLE_USER_HOME=/tmp/mp3-gradle-home` and approved
escalated execution for local sockets, with `--offline` preventing dependency access.

| Check | Exit | Observed result |
| --- | --- | --- |
| Cached Gradle `--offline --no-daemon --console=plain test bootJar` | 0 | Successful, five tasks up to date; insufficient fresh-test evidence alone |
| Cached Gradle `--offline --no-daemon --console=plain --rerun-tasks test bootJar` | 0 | Successful; all five actionable tasks executed |
| Python XML and boot JAR inspection | 0 | Fresh JUnit reports: five gateway tests plus one application-context test; zero failures/errors/skips. JAR exists, 9,087,659 bytes, 21 runtime libraries; no web/servlet/JDBC/Flyway/Postgres/Hibernate library names |
| `git diff --check` from repository root, repeated after index correction | 0 | No whitespace errors in tracked diff; does not validate all untracked contents |
| `rg` for network/environment/persistence/bean/role imports in main sources | 1 | No matching lines; corroborated by reading all three production Java files |
| Default JShell supplemental probe | 1 | Execution engine blocked by sandbox local socket restrictions; no assertions executed |
| `jshell --execution local --class-path src/backend/build/classes/java/main` supplemental assertions | 130 after interruption | All assertions executed without assertion errors and printed completion; JShell then raised preferences-store shutdown failure and required interruption. This is supplemental observed behavior, not a clean passing command |

Fresh XML timestamps: `2026-10-20T03:18:35.214Z` (application) and
`2026-10-20T03:18:35.610Z` (gateway). Existing JUnit tests cover deterministic
fixture output across changed instructions/input, request separation and budget
validation, redacted text diagnostics, typed failures and absence of a gateway bean.
Supplemental JShell assertions checked actor/task diagnostic redaction, arbitrary
non-Tutor and malformed fixtures remaining unchanged, and exact sanitized failure
messages with null initial causes. Only synthetic values were used.

Parent reports successful checksum-pinned `./gradlew --no-daemon --console=plain
test bootJar` distribution download/build, exit 0; the reviewer independently used
the cached executable and freshly reran tasks, rather than claiming independent
wrapper-download verification. Wrapper properties pin Gradle 9.1.0 and SHA-256;
build pins Java 25 and Spring Boot 4.0.8. Runtime JDK was 25.0.4.

## Findings and limitations

No actionable implementation defect found within the authorized foundation.
The request preserves trusted actor/task metadata separately from instructions
and untrusted input. It validates a positive declared output budget; the fake
does not enforce token counts. Diagnostics redact actor/task/input/instructions
and output; failure constructors take only a code and provide fixed messages.
The generic fake returns its immutable supplied fixture without parsing, prompt
interpretation, environment lookup, external calls, or persistence. This permits
role-specific malformed-output tests without making Tutor schema a platform rule.

The application has no web dependency, controller or AI bean. Startup test confirms
no `AiGateway` bean exists. SOC is only an enum value; there is no real adapter.
Absence of provider wiring means production fake-selection rejection is deferred,
not implemented or tested. No prompt-injection resilience, factual grounding,
transport safety, authorization, quota/deadline enforcement or persistence claim
can be inferred from deterministic fake tests.

A low-priority documentation inconsistency was reported during review:
`workflow/handoffs/README.md:3` said no handoffs had executed despite dated records.
The developer corrected it to distinguish executed records from pending human
verification; the reviewer reread the correction. No unresolved blocker remains.
Previously recorded ownership-table conflicts and affected-owner agreement remain
coordination items, not newly assigned work or claimed teammate acceptance.

Still unverified: frontend/full P1/CI, Spring Security sessions/CSRF/ownership,
PostgreSQL/Testcontainers and quota races, authorization-before-admission/provider,
deadlines/retries/response bounds, Tutor parser and endpoint/UI, explicit save and
note-version checks, production configuration, real SoC and deployment. Docker
absence was recorded by the architect; no database or transport test was attempted.

## Next task and human verification

Human reviews this slice and provisional shared AI contract, reconciles ownership
with affected role owners, and chooses the next bounded integration task. Before
any endpoint uses this seam, agree trusted actor and durable admission boundaries
and enforce authorization, limits and deadlines outside the provider. Human
acceptance, teammate contract review and later manual feature verification remain
pending. This review does not mark complete P1, P6 or final AI requirements.

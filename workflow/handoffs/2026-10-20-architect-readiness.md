# Architect readiness handoff

Task ID: ARCH-2026-10-20-01. Date: 20 October 2026 (SGT).
Actual actor: Codex architect subagent `/root/architect`, dispatched by the parent
agent under the team's custom `workflow/agents/architect.md` instructions.
Human verification: **pending**. This record proposes a development slice; it
does not record teammate approval or completion of P0, P1, or P6.

## Inputs and permitted changes

Input HEAD: `e48694a04d80544ce127fc75cb7bd215dfd713a0`, branch `main`.
Read the working copies of `workflow/PRODUCT-SPEC.md`,
`workflow/tutor/PRODUCT-SPEC.md`, `workflow/SHARED-TECHNICAL-SPEC.md`,
`workflow/INTEGRATION-SPEC.md`, `workflow/tutor/TECHNICAL-SPEC.md`, and
`workflow/tutor/PLAN.md`, plus repository/source READMEs and agent/handoff instructions.

Initial `git status --short` (exit 0) showed modified `README.md`,
`workflow/PRODUCT-SPEC.md`, `workflow/tutor/PRODUCT-SPEC.md`; untracked `.github/`,
`.gitignore`, `docs/`, `logs/`, `src/`, `workflow/INTEGRATION-SPEC.md`,
`workflow/REUSE-INVENTORY.md`, `workflow/SHARED-TECHNICAL-SPEC.md`,
`workflow/agents/`, `workflow/evaluations/`, and `workflow/handoffs/`.
These pre-existing changes are the actual inspected baseline and must be preserved.
The architect's sole permitted repository write is this file. No product code,
specification edits, installs, commits, pushes, external messages, or live SoC calls
were performed by this architect.

Relevant requirements: P0/P1 and the isolated-contract portion of P6;
SH-AI-01/02/03/07/09; eventual TUT-AI-01/02, SEC-02/03, AI-02.
The proposed first slice supports those requirements but cannot satisfy their
authorization, durable quota, UI, or production acceptance conditions alone.

## Readiness evidence

Read-only probes executed from repository root unless stated otherwise:

| Command/probe | Exit | Observed result |
| --- | --- | --- |
| `git rev-parse HEAD`; `git log -3 --oneline`; `git branch --show-current` | 0 each | HEAD above; recent history is documentation; branch `main` |
| `find src -maxdepth 5 -type f` | 0 | Only `src/backend/README.md` and `src/frontend/README.md` |
| `rg --files` filtered for Gradle, Maven, npm, Java, Kotlin, SQL, TypeScript, Docker, YAML build/source files | 1 | No matching source/build files; no wrapper, manifests, migrations, application, or runnable tests |
| `java -version` | 0 | Zulu OpenJDK 25.0.4, arm64 |
| `javac -version` | 0 | 25.0.4 |
| `/usr/libexec/java_home -V` | 0 | Installed Azul and Amazon Corretto 25.0.4; current default is Azul |
| `node --version`; `npm --version` | 0 each | v22.17.1; 10.9.2 |
| `gradle --version` | 127 | No Gradle executable in PATH |
| `docker --version` | 127 | No Docker executable in PATH; no PostgreSQL Testcontainers readiness established |
| Python pathlib globs under `~/.gradle/wrapper/dists` | 0 | Extracted Gradle 8.14, 9.1.0, and 9.6.1 binaries exist |
| Python pathlib globs under `~/.gradle/caches/modules-2/files-2.1` | 0 | JUnit Jupiter 5.13.4 artifacts and API/engine 5.14.4 directories exist; no `org.springframework.boot/*/*` or Jackson databind matches |
| `du -sh ~/.npm/_cacache` | 0 | 1.4G cache exists; this does not establish any specific frontend dependency is available offline |
| Cached Gradle 9.1.0 executable with `--version` | 1 | Could not initialize native services: failed loading `libnative-platform.dylib` for Mac OS X aarch64 |

The Gradle probe used
`/Users/huanghao/.gradle/wrapper/dists/gradle-9.1.0-bin/9agqghryom9wkf8r80qlhnts3/gradle-9.1.0/bin/gradle --version`.
It is an observed startup failure, not proof that Java 25 is incompatible. A
writable native/cache location or an approved execution context is a reasonable
next diagnostic; neither repair nor a successful build was attempted here.

Parent-agent evidence, not independently reproduced by this architect: a public
Gradle dependency HEAD request using escalated `curl` returned exit 0; default
sandbox DNS had failed. Dependency acquisition is therefore possible through the
approved execution path, but restricted default networking remains a build concern.
The parent also checked official Gradle Java-compatibility guidance for Gradle 9.1
and subsequently reproduced cached Gradle 9.1 `--version` with exit 0 using
`GRADLE_USER_HOME=/tmp/mp3-gradle-home`. That demonstrates a working version probe
with writable cache configuration; the failed architect probe above is retained.
These reports do not establish resolved project dependencies or successful tests.

No application build/test command was run: the manifests and scripts do not yet
exist. No Docker daemon, database, deployment target, real SoC connection, or
frontend build has been verified.

## Findings and contract proposals

1. The source directories are documentation placeholders. P1 is unstarted;
   obtaining a working wrapper/dependency resolution is the first executable check.
   Java and Node are present, so the proposed stack need not be changed merely
   because Gradle and Docker are absent from PATH.
2. The shared technical ownership table lists `platform/ai` under the Student
   owner, contradicting current human direction and the Tutor technical spec/plan.
   Huanghao owns Tutor and shared `unichope.platform.ai`. The same table proposes
   Tutor ownership of core/schema; current authorization does not allocate that
   work. This handoff records the disagreement without editing the table or
   assigning responsibilities to other people.
3. `workflow/tutor/PLAN.md` says specialized-agent instructions are still to be
   created, while scoped architect/developer/reviewer instructions now exist.
   README statements that instructions are absent are likewise stale. Existence
   is not evidence that the complete agent evaluation workflow has passed.
4. Platform code must stay independent of role code. Tutor-specific prompts,
   exact-three-field parsing, formatting, and private notes belong to Tutor.
   Shared AI exposes provider text/failures without taking a booking ID, deciding
   ownership, or writing notes. Generation and explicit version-checked Save remain
   separate operations throughout later integration.

Provisional minimal Java contract for affected role-owner review:

- `unichope.platform.ai.AiGateway` accepts trusted server actor/task metadata,
  immutable instructions, a distinct untrusted-input string, and an explicit
  positive maximum-output-token budget. Actor metadata is for admission/accounting
  and must not enter provider content. It does not prove authorization by itself.
  It returns immutable raw text with provider mode, or a typed sanitized failure.
  No callbacks, tool registry, database handles, browser provider selector, or
  implicit environment/credential lookup belongs in the contract.
- Keep result/failure types small (nested records/enums are sufficient). Initial
  failure categories may represent unavailable, timeout, and invalid response;
  later admission adds busy/rate-limited outcomes with retry metadata. Do not expose
  upstream exception messages/bodies. Do not claim fake failures test transport.
- `FakeProvider` deterministically returns an injected synthetic fixture. Tutor
  tests may supply valid JSON with `summary`, `topicsDiscussed`, and `followUp`
  strings; Student/Admin fixtures need not use Tutor's schema. It reads no
  secrets, performs no HTTP, and never echoes arbitrary input into a success claim.
  Failure variants can be injected test doubles; avoid a runtime scenario framework.
- Future admission receives authenticated account/task metadata from trusted
  backend orchestration, not the browser. Preserve the future ordering: current
  role/active/ownership/completed checks and input validation, atomic durable
  admission, provider call outside the transaction, then authorization recheck.
  Decide the exact admission/provider seam before integrating real role endpoints;
  this first provider contract is not a quota bypass for application requests.
- Expose no gateway bean or HTTP feature in this isolated slice, until trusted
  authorization and durable admission can be wired together. Later fake Spring
  wiring is dev/test only. An explicit production configuration must
  reject fake selection. Production may leave AI unavailable until real settings
  exist. Do not create a placeholder real provider that pretends to succeed.

Shared dependencies still requiring coordination/review: trusted actor/auth API,
core user/booking eligibility reads, note transaction/version contract, AI metadata
migration numbering and durable quota storage, reviewed limits, and final provider
configuration. These block complete Tutor AI integration, not isolated fake tests.
No teammate agreement is asserted by the proposals above.

## Smallest recommended next slice

Task proposal DEV-2026-10-20-01: establish a Java 25 Spring Boot backend scaffold
with a pinned Gradle wrapper and isolated `AiGateway`/`FakeProvider` acceptance tests.
This is partial P1 plus an isolated P6 foundation. Frontend scaffolding can follow
separately; it is unnecessary to verify this first contract.

Suggested file scope: `src/backend/` build/settings/wrapper, minimal `unichope`
application entry point and dev/test configuration, `unichope/platform/ai/` and its
matching tests. Preserve/update its README only to document actual verified commands.
Pin mutually compatible framework/wrapper versions and record dependency evidence.
Do not introduce auth, role endpoints, SQL/migrations, persistence dependencies,
Student/Admin behavior, a real SoC adapter, or deployment in this slice.

Acceptance sequence:

1. Resolve the Gradle startup/cache limitation and establish wrapper `--version`.
   Then run backend tests and `bootJar`; retain exact commands/exit codes. A
   version output alone is insufficient build evidence.
2. Write failing contract tests before production code: deterministic repeated fake
   output; distinct instructions/input; positive output-budget validation; safe typed
   failure behavior through injected doubles; dev/test fake wiring and production
   fake rejection if wiring is included. Test only actual implemented boundaries.
3. Implement the minimal contract and fake. Confirm `platform/ai` has no role
   imports, HTTP clients, persistence writes, secret reads, or external tool hooks.
   Inspect test reports and record test counts, failure details, and limitations.
4. Independently review the resulting diff against this handoff and requirement
   subset. Reproduce reported checks; keep human verification pending until the
   human accepts the evidence.

A working Docker-compatible runtime is still needed before required PostgreSQL
Testcontainers evidence. Do not replace PostgreSQL race tests with SQLite/H2 or
record them as passing because isolated fake tests pass. Actual budgets/model/
endpoint/key/connectivity and a mock-server transport suite are deferred to P8;
real SoC absence must not block this slice. Auth/CSRF/ownership, quota durability,
manual note persistence, reviewed copy/save, browser races, full P1 frontend/CI,
and complete P6 remain explicitly unverified after this proposed slice.

Human verification pending: contract and provisional stack acceptance, ownership
table reconciliation, shared integration agreement, independent review evidence,
and later manual feature/deployment walkthroughs.

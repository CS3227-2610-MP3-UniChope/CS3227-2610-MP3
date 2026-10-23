# Developer handoff: isolated AI foundation

Task: DEV-2026-10-20-01. Actual actor: parent Codex `/root`, acting as developer.
Input: HEAD `e48694a04d80544ce127fc75cb7bd215dfd713a0`, main; dirty baseline and
spec revisions recorded in ARCH-2026-10-20-01. Read `workflow/agents/developer.md`.
Architect handoff: [readiness](2026-10-20-architect-readiness.md), recorded before
production sources were created. Human verification: pending. No commit/push.

Scope: partial P1 plus isolated P6 contract; supports SH-AI-01/02/03/09 and eventual
TUT-AI-01/02, SEC-02/03 without claiming those end-to-end requirements pass.
Permitted implementation: backend build/wrapper, minimal application, platform/ai,
matching tests and backend README. Additional records: custom agent instructions,
contract proposal, handoffs and sanitized logs. No other roles, schema, auth or
product behavior were implemented. No MP2 application source was copied.

Changes: Java25/Spring Boot4.0.8/Gradle9.1 foundation; trusted actor/task metadata,
separated instruction/input, positive output budget, redacted diagnostics,
sanitized typed failures, fixture-driven deterministic FakeProvider. No gateway
bean, web endpoint, production provider activation or note writes.

## Commands and evidence

Commands run from backend unless noted. Gradle used a writable isolated cache
`GRADLE_USER_HOME=/tmp/mp3-gradle-home` and escalated execution for local sockets
and public dependency access. Cached executable:
`/Users/huanghao/.gradle/wrapper/dists/gradle-9.1.0-bin/9agqghryom9wkf8r80qlhnts3/gradle-9.1.0/bin/gradle`.

- Cached Gradle `--version`: exit 0, Gradle9.1.0 and Java25.0.4. Default sandbox
  build initially failed with `SocketException: Operation not permitted` (exit1).
- First scaffold write accidentally nested test paths. Those newly created paths
  were corrected without touching existing files. An early `test` command returned
  exit0 with NO-SOURCE; this was not test evidence and is not counted as passing.
- Initial test compilation with absent production sources: exit1. A preliminary
  test also requested Jackson absent from the minimal starter; that unnecessary
  dependency was removed when following the fixture-driven architect contract.
- Final scoped red run: cached Gradle `--no-daemon --console=plain test`, exit1,
  20 compile errors for missing AiGateway/application types. No production code yet.
- After implementation: same Gradle executable with `--no-daemon --console=plain
  test bootJar wrapper --gradle-version 9.1.0 --distribution-type bin`, exit0.
  JUnit XML: 5 AiGateway tests + 1 application startup test; 0 failures/errors/skips.
  Jar built and wrapper generated. Published distribution checksum fetched with
  `curl` exit0 and pinned in wrapper properties.
- Repository `git diff --check`: exit0 before handoff; final wrapper rerun and
  independent review are recorded separately after this handoff.

Dependencies not satisfied: auth/current actor integration; durable admission,
quota schema and budgets; deadlines and rate-limit failures; role parsers; manual
Tutor notes; React/Vite scaffold, Bootstrap, Vitest/RTL; REST/security/JdbcClient/
Flyway/Postgres/MockMvc/Testcontainers and CI. Docker is absent. Full P1/P6 are not
complete. Real SoC activation remains P8. Fake determinism is not proof of prompt
injection resistance, factual grounding or provider transport correctness.

Next actor: independent reviewer; read custom reviewer instructions, inspect scoped
files and contract, reproduce tests and bootJar, report findings/limitations. Human
acceptance and affected teammate contract review remain pending.

## Wrapper and preservation follow-up

Generated wrapper command: `GRADLE_USER_HOME=/tmp/mp3-gradle-home ./gradlew
--no-daemon --console=plain test bootJar`, exit0. The pinned distribution downloaded
and was checksum verified; all five build tasks were up-to-date. This validates
the wrapper path, while the original six-test execution and independent rerun
supply actual test evidence.

Final preservation comparison: 8 captured existing files unchanged (root README,
shared/Tutor product, shared/Tutor technical, integration, Tutor plan and frontend
README). Backend README intentionally updated; reuse inventory original content
preserved with measured wrapper attribution appended. Agent/handoff indexes updated
to match actual records. `git diff --check` exit0. No commit or push.

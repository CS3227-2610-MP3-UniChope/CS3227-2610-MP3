# Core sharing preparation

Historical package: superseded by the [core-only test separation](2026-10-21-core-test-separation.md).
The current staged selection has 41 files and excludes Tutor code and newly added
workflow documents; its teammate entry point is the prepared backend README.

Status: Prepared and independently reviewed; human package review pending.
Date: 21 October 2026. No commit or push.

Prepared checkout: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/core-sharing`.
Branch: `prepare/core-sharing`; baseline `508c5bd769e889dea405cb848ddd60429f24ce3b`.
57 files are staged there. The original checkout and its index retain the unfinished
Tutor REST/AI work. A commit must be made from the prepared checkout, not by adding
all files in the original repository.

Included: existing shared domain/read repositories/lifecycle and tests, required
Tutor service read integration, Gradle wrapper/build, review-only schema, scoped
sharing/build instructions, core/read reuse inventory and integration clarification.
Excluded: Tutor HTTP/Security, AI, frontend, personal guides/reflections and raw logs.
The build omits MVC/Security dependencies and uses a minimal non-web entrypoint.
Original packaged Java/test/SQL bytes match all 42 corresponding source files.

Developer and independent reviewer each ran normal offline Gradle
`test postgresTest bootJar` against disposable PostgreSQL: 8 unit + 36 PostgreSQL
tests, zero failures/errors/skips; both builds succeeded in 19 seconds. Independent
review found no actionable implementation findings. Generated reports remain local.
The synthetic PostgreSQL server was stopped after verification. Index whitespace
check passes; wrapper executable mode and cross-platform line endings are recorded.

The committed auth spec already selects passwordHash and sessionGeneration on users.
Updated the local integration wording and prepared package accordingly; the prior
claim that no storage selection was recorded was incomplete. No such columns,
credential/domain extension, provisioning/revocation writes, real actor adapter or
production database wiring are implemented here. Admin/Auth specs remain unchanged.
This is a development baseline for role integration, not an auth-ready production core.

Controls: inherited workspace-write/network-restricted environment; permitted build
outputs in prepared checkout, caches/temp under /tmp; reviewed escalations for local
Gradle/PostgreSQL sockets only. The app worktree tool failed because this chat's outer
folder is not the nested Git repository, so native Git created the isolated worktree.
No secrets, external messaging, live providers, installations or deployment used.

Evidence: `workflow/core/evidence/sharing-preparation-2026-10-21/` contains scoped
input hashes, verification logs, independent XML and staged file list. Separate
[independent review](2026-10-21-review-core-sharing.md) records actual reviewer checks.
The shareable checkout's `workflow/core/SHARING.md` is the teammate entry point.

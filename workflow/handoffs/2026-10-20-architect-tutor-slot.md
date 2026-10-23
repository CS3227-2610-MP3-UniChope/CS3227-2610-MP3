# Architect handoff: Tutor upcoming slots and active assigned modules

## Dispatch and scope

- Role: Architect; actual agent identity: `/root/tutor_slot_architect`.
- Task: assess and recommend the smallest implementation slice for Tutor `TUT-SLOT-01` and active assigned-module options.
- MP3 checkout: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/CS3227-2610-MP3`, HEAD `e48694a04d80544ce127fc75cb7bd215dfd713a0`.
- MP3 input manifest: [`tutor-slot-inputs-2026-10-20.json`](tutor-slot-inputs-2026-10-20.json). It identifies scoped MP3 contents, including untracked inputs. This handoff adds hashes for the inspected MP2 source because MP2 is outside that manifest.
- MP2 reference checkout: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP2/CS3227-2610-MP2`, HEAD `d6a9a244f960854b2addb1546b45e506217f4823`. It remains read-only. Its pre-existing untracked `AGENTS.md` and `logs/Huanghao-details.md` were preserved; the log was not read or copied.
- Human verification/acceptance: pending; this handoff is an agent recommendation, not approval or teammate agreement.

## Requirements and semantic differences

MP3 `workflow/tutor/PRODUCT-SPEC.md` TUT-SLOT-01 requires an active authenticated tutor to see only their own AVAILABLE and BOOKED slots whose start instant is strictly after the current time, sorted by start instant, with module and status data. An optional date selects the slot's start date in Asia/Singapore; without it, return all upcoming rows. Empty results remain an empty list. `workflow/INTEGRATION-SPEC.md` specifies `GET /api/tutor/slots?date=` and `GET /api/tutor/modules`, `Slot` and `ModuleOption` response fields, active TUTOR authorization, list ordering (start then ID), and array responses including `[]`.

MP2 `TutorService.findUpcomingSlots` at `src/main/java/tutor/TutorService.java:219-225` retains starts equal to `now` (`!isBefore(now)`). MP3 explicitly requires strictly future, so adapt this to `startTime.isAfter(now)` and add a fixed-clock equality test. MP2 already applies `Asia/Singapore` to the optional start-date comparison and orders by start then ID. Its overloads distinguish a date from no date. MP2's `findActiveAssignedModules` filters inactive assigned modules and sorts by code then ID, matching the module-options ordering, although MP3's `ModuleOption` also requires ID, code, and name.

MP2 directly passes a persistent `actorId` into `TutorService`; do not carry that constructor pattern into an application-scoped MP3 service. MP3 technical requirements instead call for request-scoped authenticated identity, re-reading current account status, and no client-supplied actor ID. The source reference also uses JavaFX-era repositories and SQLite persistence; those are not MP3 integration choices.

## Reuse and adaptation map

Line quantities below are source line counts (`wc -l`); cited line ranges are 1-based and inclusive. No MP2 code or tests were copied during this architect stage.

| MP2 source at `d6a9a244f960854b2addb1546b45e506217f4823` | Quantity | MP3 destination/recommendation | Treatment |
| --- | ---: | --- | --- |
| `src/main/java/tutor/TutorService.java` | 279 lines total; slot query at 219-225 (7 lines), module option query at 98-109 (12 lines), slot projection at 242-246 (5 lines); account checks at 193-199 (7 lines) | New small service under `src/backend/src/main/java/unichope/tutor/`; use the documented shared auth/core seams only when available. | Adapt rules, not class wholesale. Replace SQLite facade/operation logging and persistent actor ID. Make strict-future comparison; keep SGT date and deterministic ordering; map to MP3 DTO fields. |
| `src/main/java/tutor/TutorSlotView.java` | 9 lines total | Tutor-local slot projection/response DTO, shaped to MP3 `Slot` (`id`, `moduleId`, `moduleCode`, `startTime`, `endTime`, `status`). | Adapt: MP2 projection omits module ID and names its key `slotId`; MP3 contract requires `id` and `moduleId`. |
| `src/main/java/model/consultation/ConsultationSlot.java` | 25 lines | Prefer the existing agreed `platform/core` consultation contract once present; otherwise keep a minimal Tutor-local read projection for the first isolated slice. | Domain idea/field mapping only. It contains owner ID (useful for defensive owner filtering) and validates `endTime > startTime`; do not copy its old package unchanged. |
| `src/main/java/model/consultation/SlotStatus.java` | 3 lines | Shared `platform/core` slot status contract when available. | Enum values align: AVAILABLE, BOOKED, CANCELLED, COMPLETED. |
| `src/main/java/model/module/Module.java` | 18 lines | Shared `platform/core` module contract when available; MP3 API returns only `ModuleOption` fields. | Adapt projection; exclude inactive modules and retain stable code/ID ordering. |
| `src/main/java/model/module/TutorModule.java` | 12 lines | Repository query boundary for assigned modules; no need for Tutor service to invent assignment relationships. | Reuse assignment semantics, not old model package. |
| `src/main/java/data/repository/SlotRepository.java` | 10 lines | Narrow Tutor slot reader contract or eventual core repository query. | Its `findByTutorId` is the relevant owner-scoped query shape; repository implementation is not portable. |
| `src/main/java/data/repository/ModuleRepository.java` | 18 lines | Narrow assigned-module reader contract or eventual core repository query. | Its assignment lookup plus module lookup gives the needed eligibility behavior; do not port SQLite repository. |
| `src/test/java/tutor/TutorServiceTest.java` | 650 lines total; upcoming slots tests at 221-284 (64 lines), assigned active-module tests at 610-633 (24 lines) | New focused JUnit tests under `src/backend/src/test/java/unichope/tutor/`, using deterministic fakes, not the MP2 SQLite fixture. | Adapt scenarios. Existing tests cover date, SGT boundary, past rows, list ordering/status, no date, inactive modules and inactive tutor. Add strict equality and authorization-matrix cases absent from this test block. |
| `src/test/java/tutor/TutorFixture.java` | 37 lines | A small MP3 in-memory test fixture in the Tutor test package. | Replace the SQLite, mutable actor construction, and operation log with explicit fake current-actor and repository dependencies. |

MP2 source hashes (SHA-256):

| Path | SHA-256 |
| --- | --- |
| `AGENTS.md` (untracked local input; hash recorded, contents not read or copied) | `51c1cba3940b49878c36c38b472b5f933629e44d58b3947020444022314b1191` |
| `src/main/java/tutor/TutorService.java` | `50a26b1ce919cb5df04d2d08df95e3527454f42450c855f6cb5738a2f2d06e73` |
| `src/main/java/tutor/TutorSlotView.java` | `c05e3c3febf0ee0837c51d178e7abda602deb3af9ce78222f54249befb72e612` |
| `src/test/java/tutor/TutorServiceTest.java` | `6a0af95b2eaa44cebc7461e88619c9224f6f90f35f1f24ac24850b6c84970718` |
| `src/test/java/tutor/TutorFixture.java` | `dcdd5638391fc067e2a074c0ba0b8c934f54c10c38302fbf72fddeed4c0b412c` |
| `src/main/java/data/repository/SlotRepository.java` | `abed5b0c8a837fc22d87860b95aa2ece31565b39e7be5bcac3145c71e9a7c0b7` |
| `src/main/java/data/repository/ModuleRepository.java` | `d9a4e5fb32ee613e3d959a36ba601fcc88115f3e28bd696c76954a61790f0503` |
| `src/main/java/model/consultation/ConsultationSlot.java` | `bfb717f04668a20ea40aa786712d64656c6cd5df74b58969d43f67c4b162dc1b` |
| `src/main/java/model/consultation/SlotStatus.java` | `06c23bb654c33557f4c700b1baf6aeec556bee77fd380b299a81fb6c13573e5f` |
| `src/main/java/model/module/Module.java` | `16836670a33d6ff55c1fc402c6df09970644ea45cdc159b3fd42cb7bbfbcb09d` |
| `src/main/java/model/module/TutorModule.java` | `029c6071fcd2477417d25bb0831ebc4539b12b253b5aa18b8b6e65d07d778afb` |
| `src/main/java/model/user/Tutor.java` | `3de646f50626bfee2060260900056c52a3945b2b0de57baa0deae3e193d8a9c9` |

The MP3 reuse inventory currently says no MP2 application code/tests have been copied. Update it only after actual implementation and record actual changed/copied quantities then.

## Scaffold readiness and missing dependencies

The MP3 backend currently contains the Spring Boot entry point and the `platform/ai` contract/fake/tests. `src/backend/build.gradle` applies Java and Spring Boot 4.0.8, targets Java 25, and declares only `spring-boot-starter` plus Spring Boot's test starter and the JUnit platform launcher. There is no Tutor service/test package yet and no Spring Web MVC, Spring Security/session, JDBC/JdbcClient, Flyway, PostgreSQL driver, or Testcontainers dependency/configuration in the inspected scaffold. The inspected source file list contains no `platform/core`, `platform/auth`, or `platform/web` implementation, account/session provider, consultation/module read repository, schema, migration, controller, or browser client.

Therefore the scaffold is ready only for an isolated deterministic Tutor service slice and fake-based tests. It is not ready to claim HTTP route, session identity, database, migration, browser, or end-to-end readiness. The shared technical/integration specs describe the intended `platform/auth` and `platform/core` boundaries, but those implementations are absent. Keep any current-actor/repository boundaries local and narrow for the isolated slice; do not fabricate session security or assume shared contracts have landed. Reconcile/adapt those seams when actual shared contracts become available through authorized coordination. No ownership assignment or teammate agreement is made here.

## Smallest implementable slice

Implement a pure Tutor application service under `unichope.tutor` with injected `Clock`, a current-actor seam, an account-state reader, and read-only Tutor slot/assignment readers backed only by deterministic in-memory fakes in tests. The service should not accept an actor ID from a request parameter/body. It should resolve the current server-side actor, require an existing active account with role TUTOR, query only that actor's assigned modules and slots, and return the scoped list projections. Keep HTTP/controller and Spring Security integration out until `platform/auth` and `platform/web` exist; do not add SQL or schema in this slice. If the service needs types not yet present in agreed `platform/core` contracts, use narrow Tutor-local read projections and document the later mapping rather than adding unreviewed shared contracts.

## Measurable acceptance checks for the developer

Use fixed clock and synthetic IDs/data. These are behavioral checks, not a claim that they have run:

1. No current actor/session yields a denied result; authenticated non-TUTOR roles (at least STUDENT and ADMIN) are denied; missing account and inactive TUTOR are denied. Test the active-account re-read path independently from the role check.
2. The service API has no caller-supplied tutor ID. A slot belonging to another tutor is absent even if a fake reader accidentally returns it; normal repository calls use the current actor's ID.
3. Include only AVAILABLE and BOOKED rows with `startTime > clock.instant()`. Exclude equality-at-now, past, CANCELLED, and COMPLETED rows.
4. With an optional `LocalDate`, include a slot only when its start date in `Asia/Singapore` equals the requested date. Cover the UTC/SGT date boundary (e.g. 16:30Z maps to the next SGT date) and a neighboring date exclusion. With no date, include all otherwise eligible future rows.
5. Sort slots by `startTime`, then stable ID for ties. Empty eligible input returns an empty list.
6. Module options contain only active modules assigned to the current tutor; exclude inactive and unassigned modules; sort by code then ID; empty options return an empty list. Ensure name/code/ID projection matches the integration spec.
7. Tests use fakes and do not touch SQLite, PostgreSQL, external SoC, or production configuration. No endpoint/session/database claim is made without a later integration check.

## Actual controls, commands and side effects

- Inherited sandbox mode reported by the task environment: `workspace-write`; writable roots include the MP3 checkout and temporary roots. MP2 was treated as read-only. Network access is restricted; `auto_review` governs any requested sandbox escalation. No escalation was requested or granted.
- Role/output limits are also instruction-only: no implementation edits, builds, dependency installs, or MP2 writes were performed. This agent wrote only this assigned handoff and `logs/2026-10-20-architect-tutor-slot.md`.
- Inspected using read-only `pwd`, `git status --short`, `git rev-parse HEAD`, `cat`, `rg --files`, `rg -n`, `nl -ba`, `sed -n`, `wc -l`, and `sha256sum` in the MP3 and MP2 checkouts. No build/version probe ran. These commands produced no deliberate repository cache/build artifacts; there is no build/test result to report.
- MP3 was already dirty at dispatch (`README.md`, product spec and many untracked scaffold/docs/evidence files); no pre-existing file was overwritten. MP2 had untracked `AGENTS.md` and `logs/Huanghao-details.md`; both were left in place, and the private log was not inspected.

## Handoff

Next role: developer, to implement only this slice under the measured checks and actual shared contracts available at implementation time. Do not expand to other Tutor features, HTTP/Postgres/auth-session integration, SoC activation, other role ownership, commits, or pushes. After developer verification, an independent reviewer must inspect the implementation and reproduce the scoped checks; developer fixes must reference numbered findings, followed by independent re-review. Human verification remains pending.

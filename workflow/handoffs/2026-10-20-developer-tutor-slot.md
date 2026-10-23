# Developer handoff: Tutor upcoming slots and active assigned modules

## Dispatch and current state

- Role: Developer; actual agent identity: `/root/tutor_slot_developer`.
- MP3 checkout: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/CS3227-2610-MP3`, HEAD `e48694a04d80544ce127fc75cb7bd215dfd713a0` (`main`). No commit or push.
- Architect source: [`2026-10-20-architect-tutor-slot.md`](2026-10-20-architect-tutor-slot.md); its MP2 source hashes and MP3 scaffold manifest remain the provenance record. Developer input manifest: [`tutor-slot-development-inputs-2026-10-20.json`](tutor-slot-development-inputs-2026-10-20.json), captured with `workflow/agents/capture_inputs.py` after the scoped source and evidence files were written. The output manifest omits its own hash.
- MP2 checkout was read-only. It was not modified; the pre-existing untracked files noted by the architect remain untouched.
- Human verification/acceptance: pending. Independent reviewer is next; no developer self-approval.

## Requirements implemented

Implemented the architect's measurable checks for TUT-SLOT-01 and Tutor module options. The service has no caller-supplied actor ID: it reads an ID from `CurrentActorProvider`, then re-reads authoritative account role and active state through `TutorReadRepository`. Missing actor/account, non-TUTOR roles, inactive tutors, and mismatched account IDs are denied. Slots are owner-filtered again in the service, must start strictly after the injected clock, and must be AVAILABLE or BOOKED. Optional start-date filtering uses `Asia/Singapore`; unfiltered results include all otherwise eligible future slots. Slot ordering is start instant then UUID. Module options use repository rows defined as assigned, filter active status, sort code then UUID, and project ID, code, and name. Empty query results stay empty lists.

No HTTP route, Spring Security/session adapter, shared `platform/core` or `platform/auth` contract, SQL, schema, database, frontend, or SoC connection was added. The server-side actor and read interfaces are Tutor-local seams pending actual shared infrastructure. The repository interface contract requires its assigned-module query to return assigned rows; durable assignment enforcement remains a future repository implementation responsibility.

## Changes

- New production files under `src/backend/src/main/java/unichope/tutor/`: `AccountRole`, `AssignedModule`, `ConsultationSlot`, `CurrentActorProvider`, `ModuleOption`, `SlotStatus`, `SlotView`, `TutorAccessDeniedException`, `TutorAccount`, `TutorReadRepository`, and `TutorReadService`.
- New `src/backend/src/test/java/unichope/tutor/TutorReadServiceTest.java`: four behavioral tests with synthetic UUIDs, fixed clock, and in-memory read fake.
- Updated [`REUSE-INVENTORY.md`](../REUSE-INVENTORY.md) with source-to-destination mapping, adapted source quantities, and an explicit zero-lines-copied statement. No MP2 code or tests were copied unchanged.
- No product specification or other role/shared contract was changed.

## Verification evidence

TDD red: before adding production code, `javac` was run on the new test source with cached JUnit API artifacts in `/Users/huanghao/.gradle/caches/modules-2`. It exited 1 with missing-symbol errors for the not-yet-created Tutor service/types (54 compiler errors total). This was a test-compilation red, not a behavioral JUnit run. The first JUnit execution after implementation found three tests successful and one failing because the test helper populated the account in the supposed missing-account case. The test was corrected to supply an actor with an empty account reader; this was a test setup defect, not a production fix.

Final focused verification ran in `/tmp/mp3-tutor-slot-final`, with copied JUnit 5.14.4 / JUnit Platform 1.14.4 jars from the pre-existing Gradle cache copied to `/tmp/mp3-tutor-slot-gradle-home`; the repository build folder and Gradle cache were not used as output locations. The command compiled the Tutor production files, tests, and a temporary JUnit Platform Launcher runner using `javac -proc:none -Xlint:all -Werror`, then ran `java -cp [temporary classes + copied cached JUnit jars] TutorTestRunner`. Compilation exited 0 with no warnings. JUnit reported 4 tests found, 4 started, 4 successful, 0 failed. Temporary runner and outputs are outside the repository.

The repository Gradle test task remains unverified. The inspected `gradlew` wrapper and `build.gradle` were run only from a disposable backend copy under `/tmp`. First, `./gradlew --offline --no-daemon test --tests unichope.tutor.TutorReadServiceTest` with an empty disposable Gradle home failed before test discovery because the wrapper could not resolve `services.gradle.org` (`UnknownHostException`). No further network attempt was made. After copying the already-cached Gradle 9.1.0 distribution and dependency cache to `/tmp`, the same offline task exited 1 during Gradle startup: `SocketException: Operation not permitted` while creating `FileLockContentionHandler`. This prevented the Gradle task and full existing suite from running. No escalation or installation was requested or granted.

## Controls and side effects

- Inherited enforced sandbox: `workspace-write`; writable roots included the MP3 checkout and temporary roots. Network was restricted; `auto_review` governed escalations. No escalation was requested/granted. MP2 was treated as read-only.
- Human/repository role limits were instruction-only: source edits confined to the permitted Tutor paths; only the inventory, this handoff, and the sanitized log were changed as evidence. The sandbox does not provide read isolation.
- Read-only inspection included `git status --short`, `git rev-parse HEAD`, instruction/spec/handoff reads, source listing, and inspection of `src/backend/build.gradle` and `gradlew` before execution. Build/test writes were confined to `/tmp/mp3-tutor-slot-red`, `/tmp/mp3-tutor-slot-final`, and `/tmp/mp3-tutor-slot-gradle-home`. Gradle attempts could not write to the repository.
- The pre-existing dirty/untracked workspace was preserved. The three pre-existing modified tracked documents remained modified; this task did not edit them. Existing untracked scaffold files were left in place; only the assigned Tutor source/test/evidence files were added or updated.

## Next handoff

Independent reviewer: inspect the scoped diff, reproduce the focused JUnit Launcher run if useful, and review the Gradle limitation as a coverage gap. Do not infer HTTP/session/database readiness from this slice. Number any findings for the developer. Human verification remains pending.

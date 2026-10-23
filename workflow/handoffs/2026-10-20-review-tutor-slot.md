# Independent review: Tutor upcoming slots and active assigned modules

## Dispatch and reviewed inputs

- Role: Reviewer; actual agent identity: `/root/tutor_slot_reviewer`.
- MP3 checkout: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/CS3227-2610-MP3`; HEAD `e48694a04d80544ce127fc75cb7bd215dfd713a0` (`main`).
- Reviewed scope manifest: [`/tmp/mp3-tutor-slot-review-2026-10-20/inputs.json`](/tmp/mp3-tutor-slot-review-2026-10-20/inputs.json), 26 files, captured with `workflow/agents/capture_inputs.py`. The developer input manifest lists 33 files; all 33 recorded SHA-256 values matched the current files at review time.
- Read `workflow/agents/common.md`, `workflow/agents/reviewer.md`, the architect and developer handoffs, the developer sanitized log, the Tutor product/technical specs, integration/shared technical specs, reuse inventory, implementation, and tests. Read-only MP2 line-count checks confirmed the source quantities recorded in the inventory; MP2 was not modified and its private log was not read.
- Pre-existing MP3 modified files remain `README.md`, `workflow/PRODUCT-SPEC.md`, and `workflow/tutor/PRODUCT-SPEC.md`. The added Tutor implementation/evidence paths are untracked because the scaffold is untracked; other pre-existing untracked paths remain in place. No unrelated path was edited by this reviewer.
- Human verification and acceptance remain pending. This is a scoped reviewer recommendation, not a human approval.

## Findings and requirement assessment

No blocking implementation finding was identified for the isolated service slice.

The service obtains an actor only through `CurrentActorProvider`, re-reads account state, and rejects absent actors/accounts, mismatched account IDs, non-TUTOR roles, and inactive accounts. Its public methods accept no tutor ID. It scopes repository calls to the authenticated actor and defensively drops foreign-owner slots returned by a faulty reader. Slot selection uses a single injected-clock instant, strictly excludes equality and earlier starts, permits only AVAILABLE/BOOKED, applies the optional start-date comparison in `Asia/Singapore`, sorts by start then UUID, and returns an empty list when no rows qualify. The response projection contains the integration contract's slot ID, module ID/code, instants, and status.

The module path requests modules through `findAssignedModulesByTutor`, filters inactive returned modules, sorts by code then ID, and projects ID/code/name. Tests assert the actor ID passed to both scoped readers, active filtering, order, projection, and empty module results. The fake models each returned `AssignedModule` as already assigned; therefore this slice tests the active filter and query boundary, not a real assignment join. A future persistence adapter must implement the interface's assigned-to-this-tutor query correctly before the product-level assigned-module behavior can be claimed end to end.

Tests cover missing actor, missing account, STUDENT/ADMIN, inactive tutor, foreign-owner slot defense, strict equality, past/status exclusion, optional and absent date, SGT boundary and adjacent date, tie ordering, empty results, and module active/order/projection. The account-ID-mismatch defensive branch is implemented but not directly exercised by this fake, which filters mismatched account IDs itself; this is a narrow test gap rather than an observed behavior failure. No numbered finding is raised because the branch is straightforward and the required absent/wrong-role/inactive cases are covered.

The reuse inventory's listed MP2 line counts match `wc -l` in the read-only source checkout (279, 9, 25, 3, 18, 12, 10, 18, 650, and 37 lines respectively). Source line ranges and the classification as adapted ideas/rules rather than unchanged copied code are consistent with the inspected MP3 implementation. No SQLite/session/UI repository implementation was carried over. Role-first production and test packages are under `unichope.tutor`; all added contract seams remain local as the architect directed because shared auth/core implementations do not yet exist.

## Independent verification

- Disposable source/test copy: `/tmp/mp3-tutor-slot-review-2026-10-20/` (12 Java source/test files plus a temporary runner); all outputs/classes are outside the repository.
- Cached JUnit 5.14.4 and JUnit Platform 1.14.4 artifacts were read from the existing Gradle cache and used directly; no dependency install or network access occurred.
- Command: `javac -proc:none -Xlint:all -Werror -cp <cached JUnit 5.14.4 / Platform 1.14.4 jars> -d /tmp/mp3-tutor-slot-review-2026-10-20/classes /tmp/mp3-tutor-slot-review-2026-10-20/main/*.java /tmp/mp3-tutor-slot-review-2026-10-20/test/*.java /tmp/mp3-tutor-slot-review-2026-10-20/ReviewRunner.java` — exit 0, no warnings.
- Command: `java -cp /tmp/mp3-tutor-slot-review-2026-10-20/classes:<same cached JUnit jars> ReviewRunner` — exit 0; 4 tests found, started, and successful; 0 failed.
- I did not rerun Gradle. The developer recorded its offline wrapper/network and sandbox socket failures; this review independently confirms only direct Java compilation and the focused JUnit class. The full Gradle suite, application context, endpoint, browser, session integration, and persistence are not verified.
- Developer claims reproduced: focused compile and 4/4 focused JUnit results. Not reproduced: initial red compile, earlier fixture setup failure, or Gradle startup failures. No full-build claim is made.

## Controls, changes, and handoff

- Inherited filesystem mode: `workspace-write`; configured writable roots include the MP3 parent workspace and temporary directories. The MP2 read-only restriction was an instruction-level limit, not an OS-enforced write boundary. No MP2 write was attempted.
- Network was restricted; escalation policy was `auto_review`. No escalation was requested or granted. No installation, external service, SoC, database, production configuration, commit, or push was used.
- Commands inspected before use: Gradle build/wrapper scripts and properties; the repository's `capture_inputs.py` was read before its invocation. Commands were limited to read-only Git/status/file reads and listings, SHA-256/line-count inspection, the input-hash capture, and the scoped `javac`/JUnit run. Repository output permitted to this reviewer was limited to this handoff and the sanitized review log; the probe cache/output was `/tmp/mp3-tutor-slot-review-2026-10-20/`.
- Review recommendation: accept this isolated Tutor service slice for the narrow scope reviewed, with human acceptance pending and the explicit integration limits above. Next: return this review to the parent for human disposition; if any implementation issue is identified later, the parent coordinates a scoped fix and requests an independent re-review.

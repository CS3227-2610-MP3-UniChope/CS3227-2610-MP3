# Test design handoff: core note lock seam

- Task/role: independent test designer for proposed core private-note lock seam.
- Actual agent identity: `/root/tutor_notes_test_designer`.
- Specification/plan: main checkout `workflow/core/NOTE-LOCK-SEAM-SPEC.md` and `NOTE-LOCK-SEAM-PLAN.md`.
- Source/test baseline: `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/core-note-lock-seam`, HEAD `3f3ec8b99215b8b4cba9b88e6572465c88c47a7e`. No scoped hash manifest was supplied; commit ID identifies the requested baseline only.
- Permitted work: scoped spec/plan and test-infrastructure inspection; write only `workflow/core/NOTE-LOCK-SEAM-TEST-MATRIX.md` and this handoff. No tests/probes/source changes.
- Actual controls: inherited workspace-write project/temp roots; restricted network; auto-review approval policy; no escalation granted/requested. No runtime read-only boundary was present. Behavioral no-probe/no-source-edit limits were instructions. No build/test/database/network/install command or probe was run; no build caches, test reports, or temporary artifacts were created.
- Files changed: only the assigned matrix and handoff in the main repository checkout. No source or executable test files changed.
- Inspected commands and results:
  - In source checkout: `pwd && cat workflow/agents/common.md && cat workflow/agents/test-designer.md && cat workflow/core/NOTE-LOCK-SEAM-SPEC.md && cat workflow/core/NOTE-LOCK-SEAM-PLAN.md` stopped at missing `workflow/agents/common.md` (exit nonzero); no requested source checkout specs were at that path.
  - Located workflow documents with `rg --files -g 'NOTE-LOCK-SEAM-SPEC.md' -g 'NOTE-LOCK-SEAM-PLAN.md' -g 'common.md' -g 'test-designer.md' /Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3` (completed); specs/instructions were in sibling `CS3227-2610-MP3` checkout.
  - Read role instructions and specs with `cat` in the main checkout (completed).
  - Inspected candidate infrastructure with `rg --files` and `sed` for `LifecyclePgFixture.java`, `PostgresLifecycleConcurrencyTest.java`, `PostgresLifecycleTest.java`, and `ConsultationLifecycle.java` in the source checkout (completed). This was limited to test fixture practicality and existing lifecycle regression surface; implementation logic was not used as the oracle.
  - Verified source baseline with `git rev-parse HEAD` (output matched requested commit).
  - No test/build/database/network/probe commands were run; no report paths exist.
- Matrix: 10 cases covering fail-before-query, same-resource transaction joining, lock retention until caller completion, actor/ownership/completion checks, ordered locks and fresh rechecks, rollback, bounded contention mapping, API/capability boundary, and existing lifecycle regression. Cases are design-only.
- Open questions: Q1 same-datasource/resource identity and cross-datasource behavior; Q2 isolation and deterministic practical lock-order oracle; Q3 rollback ownership and safe nested exception mapping after PostgreSQL aborts a transaction. See matrix for proposed resolutions. No implementation correctness or test pass is claimed.
- Next role: coordinator reconciles Q1–Q3 with Core/Tutor owners before implementation. Developer then implements approved API and runs real PostgreSQL lock/transaction tests plus existing lifecycle regressions; independent review remains separate.
- Human verification/acceptance: pending; this handoff does not record approval.

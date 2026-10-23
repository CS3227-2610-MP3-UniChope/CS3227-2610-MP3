# Architect handoff: Tutor note lock seam

Date: 21 October 2026. Role: design only. Agent: `/root/tutor_lock_seam_architect`.

## Inputs and checkout distinction

Requirements/spec inputs are from main checkout HEAD `508c5bd769e889dea405cb848ddd60429f24ce3b`, including its dirty/untracked source/spec inputs listed in `workflow/core/note-lock-seam-inputs-2026-10-21.json`. Core code was inspected separately in candidate checkout `/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/core-auth-storage`, HEAD `3f3ec8b99215b8b4cba9b88e6572465c88c47a7e`, clean branch `feat/core-auth-storage`.

Read `workflow/agents/common.md`, `architect.md`, core lifecycle spec/plan, Tutor NOTE-INCREMENT spec/plan and `NOTE-TEST-MATRIX.md`, plus relevant core Java source. Lifecycle owns READ_COMMITTED/REQUIRES_NEW and 5s/10s local DB bounds; locks are private; order is sorted users, module, slot, booking. Lifecycle explicitly rejects unrestricted callbacks. The requested matrix file is `NOTE-TEST-MATRIX.md`.

## Recommendation / decision needed

A narrowly named `withCompletedBookingMutation` callback still grants unrestricted executable work while locks are held; the type cannot prevent network/AI calls. Treat that as a blocker to callback implementation pending explicit owner review. Proposed alternative: Tutor owns a short transaction; a core operation requires an already-active transaction, joins it, locks sorted tutor/student users then module, slot, booking, re-reads actor/ownership/completion, and returns IDs only. Tutor then performs note SQL in the same transaction. Tradeoff: Tutor controls duration and rollback. Details and acceptance criteria are in `workflow/core/NOTE-LOCK-SEAM-SPEC.md`; sequence in `workflow/core/NOTE-LOCK-SEAM-PLAN.md`.

## Controls and actions

Runtime controls: workspace-write, restricted network, auto-review; no escalation. Scope was instruction-only: only the three assigned Markdown documents were written. No source, tests, build, DB, network, probe, install, commit, push, or external message actions occurred. No tests were run. No human or teammate approval is claimed.

## Next role / open items

Core/Tutor owners should decide transaction ownership and exception/rollback semantics; then an independent test designer should derive seam tests before implementation. NOTE-INCREMENT Q1/Q2/Q3 and GET authorization remain out of scope and unresolved. Human acceptance is pending.

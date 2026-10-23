# Tutor REST implementation dispatch

Status: Scope approved on 21 October 2026; implementation and independent review
authorized. Final implementation acceptance pending. No commit/push/deployment.
Task: TUTOR-REST-IMPLEMENT-01. Developer: `/root/core_increment_developer`.

## Scope and boundaries

Implement the approved REST spec and 22-case matrix: modules, upcoming slots,
slot detail, slot creation and cancellation. Retain CurrentActorProvider; synthetic
identities live only in tests. Real login/session integration and production
credential/schema activation remain separate. No lifecycle rule redesign, AI,
other-role features or frontend work. Preserve pre-existing dirty/untracked files.

Allowed changes: focused Tutor HTTP/DTO/access/read adapters and tests, minimal
platform/web and security activation helpers, needed build dependencies, scoped
evidence/coverage/reuse/handoff. Coordinator maintains status/plan.
Inputs: `workflow/tutor/rest-developer-inputs-2026-10-21.json` plus actual role/common
instructions and approved spec/matrix. Hashes identify working copies, not backups.

## Verification and runtime

Tests first, recorded initial failure, then security-enabled MockMvc tests and
real PostgreSQL request integration. Map RT01–22 to executable checks and limits.
Independent reviewer inspects and reproduces after developer archives reports;
numbered findings require developer fixes and independent re-review.

Inherited workspace-write project/temp sandbox, restricted network, auto-review
escalations. Role scopes are behavioral instructions, not enforced read isolation.
Allowed generated outputs: backend build/.gradle, /tmp/mp3-gradle-home, existing
/tmp/mp3-core-pg-runtime, /tmp/mp3-tutor-rest-* synthetic probes and assigned
workflow/tutor/evidence/rest-* outputs. No global installs or real secrets.

Coordinator verified the existing disposable PostgreSQL was stopped, then started
it on loopback port55439 with approved escalation (exit0). Explicit synthetic
JDBC target jdbc:postgresql://127.0.0.1:55439/postgres, user core_test, emptypassword;
per-test unique schemas and unchanged review-only DDL. Stop server after review.

Inspected updated build dependencies and resolved MVC/Security test runtime through
Maven Central into isolated cache (89artifacts); retained resolution logs. No repo
mutation from the small inspected temporary Gradle resolution task. Subsequent
checks use offline Gradle where possible. Inspect commands/scripts before execution;
retain actual exit/report results rather than claiming planned checks passed.

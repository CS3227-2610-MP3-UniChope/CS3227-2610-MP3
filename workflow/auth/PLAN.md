# Authentication implementation plan

Status: backend tasks implemented locally, 22 October 2026. Owner Kok Seng; independent review performed.
Inputs [Product](PRODUCT-SPEC.md),[Technical](TECHNICAL-SPEC.md),
[shared integration](../INTEGRATION-SPEC.md). Use existing /api/auth contracts.

| Task | Dependencies | Work and completion evidence |
| --- | --- | --- |
| AUTH-P0 | none | Use selected credential/session/throttle defaults; coordinate core interfaces and migrations. Specify remaining account/bootstrap details. |
| AUTH-P1 | P0; team backend scaffold/core interfaces | Configure auth/hasher/actor interfaces, synthetic fixtures and migration contracts with core owner; establish actual build/test commands and Docker/PostgreSQL environment. |
| AUTH-P2 | P1 | Tests first for existing login/me/logout/csrf. Implement stored-role actor, rotated cookies, idle expiry, strict safe DTO/errors and current-active checks. Full MockMvc-to-PostgreSQL coverage L1/S1/E1/C1. |
| AUTH-P3 | P2; reviewed core addition | Register/provision atomic user+hash and generation revocation; G1/P1 uniqueness/rollback/race tests. Reject unknown role/actor fields; Admin role exception reviewed explicitly. |
| AUTH-P4 | P2 | T1 bounded per-email failure throttle and concurrent reservations/fixed-clock expiry. Test account-existence non-disclosure, capacity and documented restart limitation. |
| AUTH-P5 | P1; bootstrap contract/deployment owner | I1 offline initial Admin procedure, concurrent bootstrap/rollback/no default secret tests and documented operation. No public endpoint. |
| AUTH-P6 | P3/P4/P5 | Independent reviewer security/integration checks, Student review, fix/retest findings, guides/measured MP2 reuse/verified summaries/reflections, human acceptance. |
| AUTH-P7 | P3; shared core storage; increment spec | Force newly provisioned Tutors to replace their initial password; gate other protected routes; verify atomic hash/flag/session-generation update in PostgreSQL and HTTP tests, plus forced UI behavior. |

Use the decisions recorded in the product and technical specifications.
Review relevant specs before code; tests prove intent, not just implementation
shape. Record actual commands, revisions, failures and remaining limits. In-memory
HTTP sessions are not business persistence; test restart signs users out and retains
committed data. Platform/core migration changes reviewed by Huanghao; auth does not
invent duplicate users tables or port desktop setup UI. Password recovery and Admin
password reset remain excluded. Tutor first-login change is specified in
[FIRST-LOGIN-PASSWORD-CHANGE-SPEC](FIRST-LOGIN-PASSWORD-CHANGE-SPEC.md).


## Current results

Frontend task FE1 is separately tracked at
[stu-fe1-auth-ui](../student/changes/stu-fe1-auth-ui.md), dependent on FE0 harness.
This current user-authorized UI scope does not imply Kok Seng's review approval.
Existing backend results below do not prove frontend integration.
Frontend revision 2 (Phuc, 22 October 2026) changes only how the UI presents existing
Auth responses: logout of an already-ended session is treated as signed out
(FE1-LOGOUT-ENDED) and a Login failing only because an old session ended shows
"Please try again." (FE1-LOGIN-STALE); see
[stu-fe4-followups](../student/changes/stu-fe4-followups.md). No Auth endpoint,
policy or backend change; Kok Seng reviews these with the Auth merge review.

AUTH-P0â€“P5 are implemented using AccountStorage and existing shared contracts.
AUTH-P6 includes independent security review, unit/HTTP/PostgreSQL verification and
guides; final user acceptance remains pending. Cached Java25 and a separate local
PostgreSQL17 instance were used; Docker is not required by the current explicit
PostgreSQL test task. Actual commands are gradlew.bat test postgresTest bootJar with
core.test.jdbcUrl/user/password to a disposable database. Startup instructions are
in src/backend/README.md. All commands/results in the log refer to actual checks.

## Registration frontend — 23 October 2026

Student registration is now included in Kok Seng's assigned frontend scope.
The /register page sends the existing public contract with CSRF, clears password
fields after submission and returns to Login without signing in automatically.
Existing login, logout, session restoration and Student routing are retained.
See [Admin and registration frontend specification](../admin/FRONTEND-SPEC.md).
No password recovery or Admin reset feature is added. Tutor first-login change is tracked by AUTH-P7.

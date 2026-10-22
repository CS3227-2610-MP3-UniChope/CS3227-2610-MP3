# Authentication product specification

Status: backend implemented locally, 22 October 2026. Owner: Kok Seng.
Baseline: main 98ca801. Parents AUTH-01/02 and SH-AUTH-01 through SH-AUTH-06.
[Shared technical](../SHARED-TECHNICAL-SPEC.md) and
[integration](../INTEGRATION-SPEC.md) take precedence for inherited contracts.

## Existing shared requirements

Stored roles; Argon2id and server sessions; rotate ID at login. Secure/HttpOnly/
SameSite=Lax cookie in production, path /, no broad Domain. Selected idle timeout: 30 minutes. Same-origin production and Vite proxy in development. CSRF on all
unsafe methods including login/logout; refresh token after login/logout.
Current active/role checks per protected request and mutation. Newly detected
inactive session returns 403 ACCOUNT_INACTIVE and is invalidated; subsequent use
returns 401 UNAUTHENTICATED. Login invalid/inactive returns generic 401
INVALID_CREDENTIALS. Selected login limit is five failed attempts per
normalized email per rolling 15 minutes. Do not substitute other limits locally.

## Requirements and planned acceptance

| ID | Behavior | Cases |
| --- | --- | --- |
| AUTH-LOGIN-01 | Existing /api/auth/login uses stored role and generic credential failure; rotate session on success. | L1 wrong/unknown/inactive credentials have same code/status/body; valid session ID changes; no hashes/credentials returned. |
| AUTH-SESS-01 | /me returns exactly {id,name,role}; logout invalidates session/cookie. Sessions are in-memory on one replica; backend restart requires sign-in again per shared design. | S1 expired/logout/restart cookie fails; new anonymous CSRF can be obtained; actors cannot share cached privileges. |
| AUTH-ACCESS-01 | Trusted session identity; roles and services re-read current eligibility. Never trust actor IDs from request bodies. | E1 wrong-role 403, inactive first 403 then 401; reactivation does not revive old sessions; transaction rechecks defeat deactivation races. |
| AUTH-CSRF-01 | /csrf anonymous token acquisition, token on every write, no broad credentialed CORS. | C1 missing/mismatch token 403 CSRF_INVALID, no writes; fresh token after login/logout works. |
| AUTH-THROTTLE-01 | Shared baseline five failed logins/normalized email/rolling15min, bounded expiring counters independent of browser session. | T1 first five failures recorded, sixth request denied429 until oldest leaves window; concurrent sessions cannot exceed limit; existence not revealed. |
| AUTH-REG-01 | Public Student registration only, atomic user+hash; no role input and no automatic login. | G1 normalized-email race admits one; rollback leaves no orphan; role spoof400; duplicate409 with generic registration error. |
| AUTH-PROV-01 | Admin provisions Student/Tutor only; accepts supplied permanent initial password through protected API. No forced-change/reset flow in this minimal baseline; add only via shared reviewed contract change. | P1 current active Admin only; permitted roles; credential never returned; failed create commits nothing. |
| AUTH-BOOT-01 | Proposed deployment-operated one-time initial Admin bootstrap, no public setup API/default password. | I1 competing bootstrap creates one; later invocation rejects; credentials absent from logs/repo. |

Kok Seng decides registration and provisioning policies. Initial passwords require
secure delivery by Admin. Password reset and forced password change are excluded.
No recovery email, MFA, role-edit or password-change endpoint is included.

## Proposed validation for new account flows

Name strip,1–100 Unicode code points; email strip+lowercase locale-independently,
max254 code points, exactly one @ and nonempty sides, no whitespace/control chars.
Password8–128 code points, exact (no trimming/normalization), confirmation on public
registration only. Hash only; no raw password storage/logging. Unique normalized
email is database-enforced. These policies are proposals, not facts about all email
syntax or inherited MP2 validation. Generic registration error still exposes that an
address is unavailable; Kok Seng decides this tradeoff.

## Decisions and integration

The authorized frontend-only FE1 increment implements Login, session restoration,
sign-out and stored-role routing under the existing policies above. Canonical UI
acceptance is [Student frontend design](../student/FRONTEND-DESIGN.md), FE1 IDs.
Tutor/Admin destinations are honest placeholders, not new role functionality.
Registration/reset UI and Auth policy changes are excluded. Kok Seng's affected
Auth review remains a merge gate; no teammate approval is claimed here.

Kok Seng decides authentication policies, validation, limits and account flows.
Coordinate shared storage and session interfaces with their consumers. Independent
code/security review does not transfer feature decision ownership.


Credential storage, session revocation and login defaults are selected in the
technical specification. Password reset and forced password change remain excluded.


## Implementation status — 22 October 2026

Authentication endpoints, Argon2id hashing, bounded login processing, registration,
Admin provisioning, session rotation/expiry, fresh account/generation checks and
one-time offline bootstrap are implemented locally. Uses the teammate-provided
AccountStorage boundary, not replacement core storage. Runtime app-api wiring and
fresh-database Flyway migrations are included. No reset/change/forced-change endpoint.
Tests include real PostgreSQL and request-to-database HTTP integration. Final results
are recorded in logs/kokseng.md after verification; implementation is not a deployment.

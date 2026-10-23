# Tutor first-login password change

Status: implemented locally, 23 October 2026. Scope: newly provisioned Tutor accounts.

## Behavior

When an Admin provisions a Tutor, the account is marked as requiring a password
change. V7 also marks all existing Tutor accounts once, so they replace any initial
password on their next authenticated visit. Student accounts and Admin accounts are
unaffected. The system cannot tell whether an existing Tutor already changed the
initial password, so those Tutors will also be asked to choose a new one once.

The normal login verifies the Admin-provided initial password and returns the
existing actor fields plus `mustChangePassword`. `GET /api/auth/me` returns the same
flag from current stored state. A forced Tutor may only load `/api/auth/csrf` and
`/api/auth/me`, sign out, or submit the password change. Every other protected
request returns `403 PASSWORD_CHANGE_REQUIRED`; the frontend shows only the password
change form and does not load Tutor data.

`POST /api/auth/change-password` requires the authenticated server-side session,
CSRF, and exactly `{password,confirmation}`. Passwords must contain 8–128 Unicode
code points and match exactly; values are never trimmed. The endpoint hashes the
new password with the existing Argon2id hasher. Core atomically checks that the
account is an active Tutor, still requires this change, and has the session generation
captured at login; it stores the hash, clears the flag, and increments the generation.
It returns `204`. The old session is thereby revoked and the Tutor signs in again
with the new password. Invalid input is `400 VALIDATION_FAILED`; an account that
does not meet the forced-change state is denied without a write.

## Acceptance cases

See [test matrix](FIRST-LOGIN-PASSWORD-CHANGE-TEST-MATRIX.md). The key intent checks
are that a newly provisioned Tutor cannot access Tutor APIs before changing the
password, the reset is one atomic storage operation, and old sessions cannot remain
usable after success. Tests use synthetic accounts and passwords only.

## Limits and open review

No password recovery or Admin reset workflow is introduced. The change form does
not ask for the initial password again because the authenticated, freshly issued
session proves that the initial credential was verified. The schema change is Flyway
V7 in the normal runtime migration sequence. `app-api` startup runs Flyway
automatically, so deploying this build against an existing database flags every
current Tutor for one password change. Coordinate that user impact before production
startup. Review the shared `AccountStorage` contract and V7 migration with the
core/auth owners before integration.

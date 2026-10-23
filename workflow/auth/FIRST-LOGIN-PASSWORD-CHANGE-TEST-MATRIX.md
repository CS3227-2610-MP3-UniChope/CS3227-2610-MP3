# Tutor first-login password change test matrix

| ID | Scenario | Expected evidence |
| --- | --- | --- |
| PW-01 | Admin provisions Tutor or V7 upgrades existing Tutor | `must_change_password=true`; login and `/me` return the flag; no credential is returned. |
| PW-02 | Forced Tutor requests Tutor data | `403 PASSWORD_CHANGE_REQUIRED`; controller/service data is not reached. |
| PW-03 | Forced Tutor changes password with valid CSRF and matching 8–128 code-point value | `204`; hash changes, flag clears, generation increments once. |
| PW-04 | Old session is used after change | `401 UNAUTHENTICATED`; old generation cannot access `/me` or Tutor data. |
| PW-05 | New password is used to sign in | Login succeeds with `mustChangePassword=false`; normal Tutor APIs work. |
| PW-06 | Mismatched/short/long password or malformed fields | `400 VALIDATION_FAILED`; hash, flag, and generation are unchanged. |
| PW-07 | Student/Admin/existing Tutor attempts the endpoint | No password update; endpoint rejects because forced-change state is absent or role is wrong. |
| PW-08 | Concurrent/replayed change uses stale generation | At most one operation succeeds; stale attempt cannot overwrite the new credential. |
| PW-09 | Missing/invalid CSRF or anonymous request | Rejected before account mutation. |
| PW-10 | Forced Tutor reloads app | `/me` restores the flag; frontend shows the form and issues no Tutor feature requests. |

Backend unit tests verify validation and service delegation. PostgreSQL tests verify
the atomic account operation. MockMvc with PostgreSQL verifies provisioning, route
restriction, CSRF, session revocation, and subsequent sign-in. React tests verify the
forced form and that Tutor data is not requested before completion.

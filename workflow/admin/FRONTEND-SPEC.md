# Admin and registration frontend

Owner: Kok Seng. Baseline: main 8bb8dc3, 23 October 2026.

## Scope and acceptance

The user has assigned the remaining Admin and authentication frontend to Kok Seng.
Keep the existing Student interface and shared login/session behavior.

| ID | Required behavior |
| --- | --- |
| ADM-FE-01 | Active Admin sees overview statistics and links to users, modules, assignments, bookings and activity reports. Student/Tutor cannot enter Admin routes. |
| ADM-FE-02 | List users, provision Student/Tutor with a permanent initial password, and confirm eligible activation/deactivation. Never display stored passwords or edit roles. Clear entered passwords after submission. |
| ADM-FE-03 | List/create/edit/deactivate modules and assign/unassign tutors using the existing guarded APIs. Confirm deactivation and unassignment; show safe rejection messages without automatically repeating writes. |
| ADM-FE-04 | Filter booking activity by module, tutor, SGT date and status; show all-time statistics with the backend's denominator and capture timestamp. No private notes or agendas. |
| ADM-FE-05 | Generate a read-only activity report from an empty request body. Show its exact returned statistics snapshot, capture time, plain-text draft, provider mode and human-review notice. Fake responses must be clearly labelled; unavailable AI must not block management/statistics. No free-text prompt, automatic retries, record changes or report persistence. |
| AUTH-FE-02 | Student registration sends exactly name/email/password/confirmation, uses CSRF and returns to sign-in without automatically logging in. No role selector, reset, password change or forced-change flow. |
| ADM-FE-06 | Loading, empty, permission, expired-session and failure states remain usable in both themes and on small screens. Ignore responses after leaving a page/session. Uncertain writes ask the user to refresh before resubmitting. |

## Implementation

Use the existing React/TypeScript/Vite UI, same-origin API client and server-side
role/session enforcement. Extend the client for PUT/DELETE and declared HTTP
statuses, retaining existing Student request behavior. Keep Admin components under
src/frontend/src/admin. React renders report output as text, never raw HTML.
Routes: /admin and /admin/users, /admin/modules, /admin/assignments,
/admin/bookings, /admin/reports; public /register. API payloads and validation
remain those in the Admin/Auth technical specifications and current controllers.

## Plan and verification

1. Confirm current backend contracts and existing frontend integration.
2. Add API transport/routing and registration; add Admin management/report screens.
3. Run focused mutation, registration, role routing and report privacy/failure tests.
4. Run the existing frontend unit/build/browser checks to detect regressions.
5. Independently review changes and check real local PostgreSQL integration.

Real SoC reports still depend on the shared provider implementation. Local fake
reports only demonstrate UI integration and do not establish model quality.
Human acceptance and production deployment remain pending.

## Verification — 23 October 2026

- Node24: `node node_modules/vitest/vitest.mjs run --maxWorkers=2` — 212 tests passed.
- `node node_modules/typescript/bin/tsc --noEmit` — passed.
- `node node_modules/vite/bin/vite.js build` — passed.
- `node node_modules/@playwright/test/cli.js test --workers=2` — 82 tests passed,
  including Admin navigation at320/768/1440px, confirmation focus, plain-text report
  rendering, explicit CSRF requests and the existing Student/theme checks.
- Real local PostgreSQL: Admin login/statistics, synthetic user creation and
  deactivation, cleared provisioning password and labelled fake report verified.
- Independent review findings were corrected. Human acceptance remains pending.

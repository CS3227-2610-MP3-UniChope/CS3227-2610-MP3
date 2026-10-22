# User guide

## Admin and authentication backend

This increment provides APIs, not a browser interface. A client must keep the session
cookie, obtain a CSRF token and include its returned header/token on every mutation.
Use HTTPS outside localhost. See src/backend/README.md for server/database setup.

1. GET /api/auth/csrf to obtain {headerName,token} and an anonymous cookie.
2. POST /api/auth/login with {email,password}, cookie and CSRF header.
3. Fetch a fresh CSRF token after login. GET /api/auth/me returns id,name,role.
4. Call only the APIs for the stored role. POST /api/auth/logout signs out; fetch
   another CSRF token before a new login. A session expires after30minutes idle.

Public POST /api/auth/register accepts name,email,password,confirmation and creates
a Student without signing in. Passwords are8–128 Unicode code points; confirmation
must match. Password reset and forced changes are not provided. Admin-created
Student/Tutor accounts use the initial password supplied securely by the Admin.

Admin users can list accounts, create Student/Tutor accounts and activate/deactivate
those accounts. Admin accounts cannot be changed through these APIs. Deactivation
is blocked by active bookings and, for Tutors, future available slots. Deactivation
revokes previous sessions; reactivation requires a new login for those sessions.

Admin modules can be created, edited and deactivated. Assignment endpoints assign
or unassign active Tutors/modules. Resolve active bookings and future available
slots before deactivation/unassignment; history is retained. Repeated no-op state
changes do not duplicate records. No history deletion or module reactivation is offered.

GET /api/admin/bookings accepts optional moduleId,tutorId,date,status. Date means
Singapore slot start date. GET /api/admin/statistics returns all-time booking counts
and completion/cancellation percentages; every booking record is in the denominator.
Empty data gives0percent. Lists use stable ordering and are unpaginated.

POST /api/admin/activity-report with {} returns the statistics snapshot and a draft
explanation requiring review. Reports have no side effects and cannot change records.
Empty data returns a no-data message without AI. Local fake mode is labelled fake
and does not call SoC. Real AI is unavailable until the shared gateway is integrated;
manual operations and statistics still work. Never treat an explanation as verified
fact or as permission to perform an operation.

Errors use {code,message,requestId}.401 requires login;403 may mean wrong role,
inactive account or invalid CSRF;409 indicates conflicting current state/duplicate
record;429 means wait for Retry-After;503 indicates temporary unavailability.
Refresh after an uncertain mutation result before resubmitting creation requests.
Exact endpoint/body shapes are in workflow/admin/TECHNICAL-SPEC.md and
workflow/auth/TECHNICAL-SPEC.md. No frontend or deployment is claimed by this guide.

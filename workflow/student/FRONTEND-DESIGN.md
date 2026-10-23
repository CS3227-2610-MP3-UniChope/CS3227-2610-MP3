# UniChope frontend design contract

Status: scoped frontend baseline for FE0/FE1/FE2; no implementation or owner review claim.
Revision 2 (22 October 2026, spec analyst): applies Phuc's owner decisions OD-1..OD-12
recorded in the stu-fe1/stu-fe2 "Owner decision addendum". New acceptance IDs are in
"Revision 2 acceptance" below and are implemented by
[stu-fe3-review-fixes](changes/stu-fe3-review-fixes.md) (before acceptance) and
[stu-fe4-followups](changes/stu-fe4-followups.md) (follow-ups). Until those changes
pass review, the current frontend does not yet meet the revision 2 IDs.
Revision 3 (22 October 2026, spec analyst): adds the light/dark theme switch from
Phuc's owner decisions TH-D1..TH-D3 ("Revision 3 acceptance" below), implemented by
[stu-fe5-theme-switch](changes/stu-fe5-theme-switch.md) after stu-fe3 (and, by
default, stu-fe4). Revision 3 changes no revision 2 scenario for stu-fe3/stu-fe4;
its refinements of FE1-UI-LANDMARKS S2 and of the browser-storage rule apply from
stu-fe5 onward.
Owner: Phuc. Shared Auth owner: Kok Seng; Tutor/core owner: Huanghao.

## Authorization and boundaries

Phuc requested implementation of Login, Tutor/Admin placeholder destinations and
Student booking UI. In the current request Phuc selected **"Weekly timetable with
a filter sidebar and a list view on mobile (Recommended)"** and authorized the
white/orange UniChope workspace described in that choice. This records the direct
request, not approval by other role owners. Keep their backend contracts unchanged.
Affected owners' review remains a merge gate, not a prerequisite for authorized
frontend development. There is no registration, password reset, AI UI, deployment,
new account policy, live NUSMods integration or Tutor/Admin feature work here.

This is several independently testable capabilities: FE0 runnable foundation,
FE1 real shared authentication and role destinations, FE2 Student booking UI.
FE0 precedes execution of FE1 tests; FE1 precedes FE2 integration. FE2 tests may
be authored after FE0 using the specified authenticated transport fixture.

## Visual and interaction contract

UniChope remains the visible product name. Use a white canvas, pale neutral
workspace surfaces, dark readable text, orange primary actions and a compact
sidebar/timetable composition. Use Bootstrap with local semantic CSS tokens,
a 4px spacing scale, restrained radii and borders, and a system sans-serif font.
Orange text/buttons must satisfy contrast; status always has text, not color alone.
Contrast criterion (OD-9, FE-UI-CONTRAST): WCAG 2.1 AA. Text and images of text
reach 4.5:1 (3:1 for large text: at least 24px regular or 18.67px bold) per SC 1.4.3;
control boundaries, selected/current state indicators and the keyboard focus
indicator reach 3:1 against adjacent colours per SC 1.4.11. Disabled controls and
pure decoration are exempt as WCAG allows.
No gradient hero, ornamental dashboard metrics, fake counts, copied logos/assets,
or fabricated account/consultation content. Display actual server strings as React
text. The NUSMods reference expresses density, time navigation and sidebar hierarchy;
the analyst directly viewed public NUSMods timetable and course-browser screenshots
captured through Playwright/installed Edge at `https://nusmods.com/timetable` and
`https://nusmods.com/courses`. They show slim white navigation, orange active
controls, pale neutral header, flat white content, thin borders, compact outlined
controls and a course-filter sidebar. Its timetable uses day rows and horizontal
time columns with alternating neutral stripes. UniChope adapts density and
hierarchy, not reference assets or exact layout. The user's selected direction
determines our orientation and responsive behavior. Include all seven days and
all returned start times rather than copying the reference's initially visible
weekday/time limits. Screenshots were temporary research artifacts, not vendored
product assets. Implementation must visually verify resulting UniChope pages.

Desktop at 1024px and above: compact product header with account name/role and
Sign out; Student navigation for Find a consultation and My bookings. Slots page
has a filter sidebar and a Monday-Sunday weekly timetable. Week controls are
Previous week, This week, Next week with an explicit SGT date range. Each event
exposes module code/name, tutor, SGT date/start/end and a Book action. Events at
the same time remain separately selectable; never hide overlapping choices.
Day columns preserve chronology and distinguish days; the renderer must not
clip early/late/cross-midnight slots (a cross-midnight event may show once on its
SGT start day with full end date/time). The concrete row sizing/positioning and
breakpoint within the intermediate 768-1023px band are builder choices.

Mobile uses a chronological list grouped by SGT date, with the same week controls,
filter controls and event information/actions. Filters precede results and remain
reachable without horizontal scrolling. At 320px neither the page nor dialogs
overflow; long names wrap. Test 320, 768, 1024 and 1440px. Login is a focused
branded form with visible Email/Password labels, password autocomplete, Sign in,
pending/error states and no role selector. Tutor/Admin destinations show their
own role heading, honest "workspace is not ready yet" copy and Sign out only.
Their page header (OD-12) shows only the app name UniChope (optionally a home link)
and Sign out: no navigation landmark, account name/role, caption or other control.
Revision 3 (TH-D3, TH-PLACEMENT) additionally places the theme switch in every page
header, including this one; from stu-fe5 it is the only other permitted control.

Every page has one h1 and a skip link. Every page, including Login, the session
check/failure screens and not-found, has one header (banner) landmark showing the
app name (OD-12, FE1-UI-LANDMARKS). A navigation landmark is required where there
are navigation choices (the Student workspace). Headings follow document order
without skipped levels; on Login the h1 "Sign in" is the first heading, and any
intro text before it is not a heading. Native buttons/inputs,
visible keyboard focus, named regions and announced status/error messages are
required. Confirmations use an accessible modal/dialog: labelled title, all
consultation details, explicit Confirm/Keep actions, Escape dismissal before
submission, trapped focus and focus restored to the trigger or a meaningful next
heading if refreshed results removed it. Pending mutation cannot submit twice;
closing/dismissing cannot imply that an already submitted mutation was cancelled.

## Transport and session contract

Use React + TypeScript + Vite + Bootstrap under `src/frontend/`; npm lockfile,
Vitest/React Testing Library and browser QA via Playwright. Keep tests alongside
source. All `/api` requests use the browser's same-origin cookie/session and
`cache: no-store`; local Vite proxies `/api` to `http://127.0.0.1:8080` with an
optional server-side Vite target setting for a different local backend port. No
browser secrets or backend CORS changes. Production is one origin; deployment
server must supply SPA history fallback without rewriting `/api` errors to HTML.

FE1 restores identity with GET `/api/auth/me` before rendering protected data.
Use `{id,name,role,mustChangePassword}` actor and stored uppercase
STUDENT/TUTOR/ADMIN role. A Tutor with `mustChangePassword=true` sees only the
mandatory password-change form; do not request Tutor workspace data until the
backend confirms a later session with the flag cleared.
Public route `/login`; role homes `/student/slots`, `/tutor`, `/admin`.
Root, authenticated Login and wrong-role deep links resolve to the actor's own
home; unknown routes have an accessible not-found/back-to-home state. Pending
restoration does not flash a prior workspace. Anonymous protected routes go to
Login. Never submit role/actor IDs with login or Student mutations.
Restoration outcomes (OD-3, FE1-RESTORE-UNREACHABLE): `/me` 401 means anonymous
(Login, no expiry message); 403 ACCOUNT_INACTIVE means Login with the inactive
message; 503, network failure, malformed body or any other unexpected status shows
"We can't reach the server right now" with a Try again action on the same URL,
not Login, and no workspace. Try again performs a new `/me` check without a full
page reload and then follows these same outcomes.

Fetch GET `/api/auth/csrf` -> `{headerName,token}` before login/unsafe operations,
using the returned header name and token in memory. Reuse only within the current
session generation. POST login JSON exactly `{email,password}` -> 200 actor;
clear the password after the attempt, then acquire fresh CSRF before enabling
protected mutations. CSRF acquisition failure permits retry but no mutation.
A CSRF acquisition made for a protected Student mutation is part of that protected
request: its 401 or 403 ACCOUNT_INACTIVE follows protected session handling below
(clarifies existing FE1-DENIAL; reviewer finding FE-R1). Any other acquisition
failure before the POST means nothing was sent: show exactly "Nothing was changed.
Please try again." (OD-4, FE2-NOTHING-SENT).
A Login whose CSRF acquisition returns 401 or 403 ACCOUNT_INACTIVE, or whose login
POST returns 403 CSRF_INVALID, failed only because an old browser session ended:
discard the token, show exactly "Please try again.", keep Login usable and never
retry automatically (OD-2, FE1-LOGIN-STALE). This is anonymous Login, not
protected session expiry.
POST logout JSON `{}` -> 204, then acquire anonymous CSRF. A logout attempt
immediately clears private state/aborts or invalidates outstanding requests.
A 204 confirms sign-out even if the follow-up anonymous CSRF fetch fails: show no
warning; Login acquires a token when submitted (OD-5, FE1-LOGOUT-CONFIRMED).
If the logout's CSRF acquisition or logout POST returns 401 or 403
ACCOUNT_INACTIVE, the server session had already ended: treat it as signed out
and show Login with no warning (OD-1, FE1-LOGOUT-ENDED).
Otherwise, if logout transport fails (network, 503, malformed/unexpected response
or 403 CSRF_INVALID), show that server sign-out could not be confirmed and
offer retry from the signed-out screen. Do not automatically restore `/me` after
this failure and silently display the still-live server session.

On protected 401 UNAUTHENTICATED or 403 ACCOUNT_INACTIVE: clear actor/private
state, invalidate requests and require login with a suitable announced message.
Generic 403 FORBIDDEN is access denied without pretending server logout occurred;
clear denied resource content and offer home/sign-out. 403 CSRF_INVALID clears
the token and offers explicit retry after acquiring a fresh one, never automatic
mutation replay. Login 401 INVALID_CREDENTIALS is generic; 429 AUTH_RATE_LIMITED
uses safe Retry-After guidance without account-existence detail; 503 or network
failure is retryable feedback, never an automatic login retry. Malformed/unexpected
HTTP responses fail safely. Never expose stack traces or upstream response text.

Private state, CSRF and credentials stay in memory; no localStorage/sessionStorage,
service-worker cache or persisted private query cache. Revision 3 (TH-D2) exception:
the single non-private theme preference key `unichope.theme` with the literal value
`light` or `dark` may be written to localStorage, only when the user activates the
theme switch (TH-PERSIST). It holds no account, actor, session or consultation data,
is not private data under SH-WEB-03, and is neither read for nor cleared by session
handling. No other browser-storage write is permitted. On logout/account change,
clear all role state and invalidate in-flight work by account/session generation.
Abort plus stale-result checks must prevent an earlier actor/selection response
from rendering or changing the new actor's state, including mutation completions.
No unsaved private text is introduced by this scope.

## Student behavior and exact API mapping

Inherit Student Technical sections 3/3.1/3.2; there are no endpoint additions.
GET `/api/student/modules` has no query; fields `{id,code,name}`. GET `/slots`
accepts only optional `moduleId` and exact SGT `date=YYYY-MM-DD`. Slot fields are
`{id,moduleId,moduleCode,moduleName,tutorName,startTime,endTime}`. Weekly display
uses the unpaginated response filtered to the chosen Monday-Sunday SGT week;
**never send a week/range parameter to the API**. With no date selected fetch
module-filtered slots once and display the chosen week. A selected exact date
is sent to the server and also moves the visible week to contain that date.
Changing week clears an exact-date selection outside the new week. Refresh uses
current filters. Default week contains today's SGT date; choosing week filters
presentation only and is not a claim about server eligibility.

Browser URL state: Slots `moduleId`, `date`, `week` (Monday SGT date); Bookings
`view=upcoming|history`, `date`, `status=CANCELLED|COMPLETED` (history only).
Missing filters use defaults; malformed/unknown UI parameters are removed or
normalized with visible invalid-filter feedback, never passed wholesale to API.
A well-formed `moduleId` that is absent from a successfully loaded `/modules` list
is likewise removed, with the same notice (OD-7, FE2-UNKNOWN-MODULE); while the
module list is unavailable the `moduleId` is kept. The "Invalid filters were
removed" notice hides on the next filter change or page move by the student;
the normalizing URL replacement itself does not hide it (OD-8, FE2-NOTICE-CLEAR).
Back/forward restores filters/results. URLs contain no actor/private text.

POST `/api/student/bookings` exactly `{slotId}` -> 201 StudentBooking. Show
confirmation first; keep no optimistic booking state. On success announce
confirmed booking and refresh slots and any loaded owned-booking data; provide
My bookings navigation. 409 SLOT_UNAVAILABLE or BOOKING_OVERLAP shows distinct
safe actionable messages and refreshes data without automatic replay. Failed or
lost mutation responses can follow a committed write: explain uncertainty, offer
refresh/check My bookings and never automatically resubmit. This uncertainty copy
applies only after the POST was sent; a failure before the POST uses the
"Nothing was changed. Please try again." message (FE2-NOTHING-SENT).

GET `/api/student/bookings?status=ACTIVE` is Upcoming, including already-started
ACTIVE bookings: do not invent a client-time cutoff. History sends repeatable
`status=CANCELLED&status=COMPLETED`, or the single selected history status. Either
supports exact SGT date. Return fields are
`{id,slotId,moduleId,moduleCode,moduleName,tutorName,startTime,endTime,status,cancellable}`.
Preserve server ordering startTime then id; labels use ACTIVE/CANCELLED/COMPLETED
with readable text. Show Cancel only when server `cancellable` is true, never
derive eligibility from the browser clock. After confirmation POST
`/api/student/bookings/{id}/cancel` exactly `{}` -> 200 projection. Success
refreshes owned bookings and slots, announces cancellation; 404 NOT_FOUND or 409
INVALID_STATE gives safe unavailable/conflict feedback and refresh. General
failure/uncertain response follows the same no-replay rule as booking.

Each data region has loading skeleton/progress, empty guidance, validation,
retryable error, success and denial states. A `/modules` failure does not hide
slots: show slots, disable the module filter and show a short notice (OD-11,
FE2-MODULES-DEGRADED). On changed filters remove stale
results or clearly mark them unavailable while loading; late filter responses
cannot overwrite newer results. Retry reads explicitly. Dates/times use
`Intl.DateTimeFormat` with `timeZone: 'Asia/Singapore'`, including dates around
UTC midnight; show SGT visibly. The UI never computes booking policy or modifies
server ordering/DTOs. No agenda button or dead AI route is exposed in FE2.

## Task acceptance IDs (Given / When / Then)

| ID | Given / When / Then |
| --- | --- |
| FE0-RUN | Given a clean supported Node/npm environment / when npm ci, test and build run / then the locked React/TS/Vite/Bootstrap application and discovered meaningful harness smoke test execute; documented dev command starts the shell. |
| FE0-PROXY | Given a local backend / when dev browser calls /api / then Vite forwards to the configured backend; frontend build contains no credentials and production uses relative /api. |
| FE0-QA | Given the harness / when browser checks run / then a reproducible Playwright browser smoke command exists and setup/browser prerequisites are documented; no fake feature red result is claimed for missing setup. |
| FE1-LOGIN | Given anonymous Login / when valid credentials are submitted / then CSRF precedes login, exact body is used, fresh CSRF follows success, and stored role selects own home; generic failures/throttle/network errors remain usable and password is cleared. |
| FE1-RESTORE | Given restored STUDENT/TUTOR/ADMIN or anonymous session / when root, protected deep link, login or wrong-role route opens / then restoration/loading and role routes follow this contract with no protected flash or role selector. |
| FE1-LOGOUT | Given authenticated/pending private work / when sign-out succeeds or transport fails / then private state is cleared and late results discarded; success invalidates server session and refreshes anonymous CSRF, failure explicitly offers server sign-out retry. Revision 2: "success" includes FE1-LOGOUT-CONFIRMED and FE1-LOGOUT-ENDED; "failure" is the remaining unconfirmed cases. |
| FE1-DENIAL | Given protected 401, ACCOUNT_INACTIVE, FORBIDDEN or CSRF_INVALID / when received / then distinct session/denial/token handling above occurs and no mutation automatically replays. |
| FE1-PRIVACY | Given a prior actor and late request / when logout/new actor occurs / then no prior private data/CSRF reappears; no private browser storage writes, HTML injection or client-selected identity occurs. |
| FE1-UI | Given any role/login / when keyboard or 320/768/1024/1440px layout is used / then form/navigation/status are accessible and role placeholders contain honest unfinished copy and sign out only. |
| FE2-BROWSE | Given eligible API data, overlaps and UTC/SGT boundaries / when week/module/date/filter history changes / then correct query and chronological SGT week/list render exact real fields; empty/loading/error and refresh work; no unsupported API params. |
| FE2-BOOK | Given a selected slot / when confirmation is cancelled, confirmed, duplicated or returns success/conflict/uncertain failure / then only explicit confirmation submits once with CSRF, exact body, no optimistic state, correct announcement and refresh/check guidance. |
| FE2-LIST | Given owned ACTIVE/CANCELLED/COMPLETED data / when Upcoming/History/status/date changes / then correct repeatable statuses/date are requested and all returned rows/statuses/cancellable flags are respected without browser-time eligibility. |
| FE2-CANCEL | Given cancellable/noncancellable booking / when cancellation is cancelled, confirmed or becomes stale / then server flag controls action, confirmation submits exact {} once, success/conflict/not-found refresh and failures never imply a completed transition. |
| FE2-RACE | Given outstanding reads/mutations / when filters/actor/session changes / then late results cannot replace newer data or reintroduce private state; expired/inactive/denied/CSRF cases follow FE1. |
| FE2-UI | Given desktop/mobile and long names / when navigating and confirming by keyboard / then timetable/list/sidebar, focus/escape/announcements and responsive contract hold; no clipped slots, hidden overlapping choices or dead agenda actions. |

## Revision 2 acceptance (owner decisions OD-1..OD-12, 22 October 2026)

Authorization: Phuc, 22 October 2026, reply to the 17 listed decisions: "for 6 dont
need to think about for now the rest use A", recorded in the "Owner decision
addendum" of [stu-fe1-auth-ui](changes/stu-fe1-auth-ui.md) and
[stu-fe2-booking-ui](changes/stu-fe2-booking-ui.md). OD-13 orders the work; OD-1 and
OD-2 also need Kok Seng's review with the Auth merge review. These IDs refine the
FE1/FE2 rows above; where a row and a refinement differ, the refinement governs.
"Session denial" means 401 UNAUTHENTICATED or 403 ACCOUNT_INACTIVE. Quoted text is
the exact visible copy unless marked as a pattern.

### Before acceptance: [stu-fe3-review-fixes](changes/stu-fe3-review-fixes.md)

| ID (decision) | Given / When / Then |
| --- | --- |
| FE2-NOTHING-SENT (OD-4; FE-R2, CS-05) | S1: Given a Student with no cached CSRF token and an open Book or Cancel confirmation / when Confirm is chosen and GET `/api/auth/csrf` fails with 503, a network error, a malformed body or any status other than session denial / then zero booking/cancel POSTs are sent, the confirmation closes, an alert reads "Nothing was changed. Please try again.", no "may have completed"/check-My-bookings uncertainty copy appears, and a Retry control reopens the confirmation for the same consultation. S2: Given S1's Retry / when the student confirms again and CSRF now succeeds / then exactly one POST with the fresh token is sent; nothing is ever sent automatically. S3: Given CSRF succeeded and the POST was sent / when its response is lost, 503 or malformed / then the existing FE2-BOOK uncertainty copy is shown, not the S1 message. S4: Given the CSRF acquisition returns session denial / then FE1-DENIAL applies (clear, Login, announce, zero POST). |
| FE1-DENIAL-CACHED (FE-R7b; regression for FE1-DENIAL) | Given a Student who already holds a CSRF token for the current session / when a Book or Cancel POST returns 401 UNAUTHENTICATED or 403 ACCOUNT_INACTIVE / then exactly one POST was sent, actor, slots, bookings and dialog are cleared, Login shows the distinct session-expired or inactive message, no mutation is replayed and `/me` is not re-requested. |
| FE1-LOGOUT-CONFIRMED (OD-5; FE-R3) | S1: Given an authenticated actor of any role / when Sign out is chosen, POST `/api/auth/logout` returns 204 and the following GET `/api/auth/csrf` fails (503, network or malformed) / then Login shows no alert or warning, no Retry sign out control, private state stays cleared and `/me` is not re-requested. S2: Given S1 / when the student then submits Login / then a fresh CSRF token is acquired before exactly one login POST. S3: Given the logout POST itself fails by network, 503, malformed/unexpected response or 403 CSRF_INVALID / then the existing unconfirmed-sign-out alert and Retry sign out remain (FE1-LOGOUT). |
| FE2-NOTICE-CLEAR (OD-8; FE-R5) | S1: Given a Student URL with invalid filters / when it is normalized / then "Invalid filters were removed" is visible after the normalizing URL replacement. S2: Given S1 / when the student changes any filter (module, date, Previous/This/Next week, Reset, history status, Upcoming/History) or moves to the other Student page or another URL by back/forward / then the notice is gone. S3: Given S1 / when only Refresh or a read Retry is used / then the notice stays. S4: Given a later navigation to a URL that is itself invalid / then the notice is shown again. |
| FE1-UI-LANDMARKS (OD-12; FE-R6) | S1: Given each page (Login, session check, session-check failure, not-found, Find a consultation, My bookings, Tutor and Admin placeholders) / when rendered / then exactly one banner landmark containing the text "UniChope" exists, together with one h1 and the skip link. S2: Given a Tutor or Admin placeholder / then its banner contains only the app name (optionally a home link) and a Sign out button: no navigation landmark, account name, role or caption text and no other control. S3: Given Login / then the first heading in document order is the h1 "Sign in" and heading levels do not skip. S4: Given the Student workspace / then its existing header with account name/role, Sign out and the Student navigation landmark are unchanged. |

### Follow-ups: [stu-fe4-followups](changes/stu-fe4-followups.md)

| ID (decision) | Given / When / Then |
| --- | --- |
| FE1-RESTORE-UNREACHABLE (OD-3; CS-02) | S1: Given any app URL / when GET `/api/auth/me` fails with 503, network error, malformed body or an unexpected status (anything except 200 actor, 401 and 403 ACCOUNT_INACTIVE) / then the URL is unchanged, neither Login nor a workspace nor private data is shown, an alert shows "We can't reach the server right now" and a "Try again" button is available. S2: Given S1 / when Try again is chosen / then a new GET `/api/auth/me` is sent without a full page reload; 200 routes per FE1-RESTORE, 401 shows Login with no expiry message, ACCOUNT_INACTIVE shows Login with the inactive message, and another failure keeps S1. S3: Given `/me` returns 401 on first load / then Login appears with no warning; given 403 ACCOUNT_INACTIVE / then Login shows the inactive message. |
| FE2-UNKNOWN-MODULE (OD-7; CS-04) | S1: Given a Find a consultation URL whose well-formed `moduleId` is not in a successfully loaded `/modules` list / when the list loads / then `moduleId` is removed from the URL by replacement (valid `week`/`date` kept), "Invalid filters were removed" is shown (and clears per FE2-NOTICE-CLEAR), the next `/slots` request has no `moduleId`, the Module control shows "All modules", and a response for the removed module is never shown as current results. S2: Given the `moduleId` is in the list / then it is unchanged. S3: Given the module list failed (FE2-MODULES-DEGRADED) / then the `moduleId` is kept and still sent. |
| FE2-MODULES-DEGRADED (OD-11; INFO-1) | S1: Given Find a consultation / when GET `/modules` fails (503, network, malformed or other non-session, non-FORBIDDEN error) and GET `/slots` succeeds / then slots render with Book actions, the labelled Module control is present but disabled, a short non-alert status notice matching the pattern /module filter.*unavailable/i is shown (recommended "Module filter unavailable right now."), and the generic results error/Retry is not shown for this cause. S2: Given S1 / when Refresh is chosen and `/modules` then succeeds / then the control is enabled and the notice is gone. S3: Given a `moduleId` filter is active during S1 / then the disabled control does not display "All modules". S4: Given `/modules` returns session denial or FORBIDDEN, or `/slots` fails / then FE1-DENIAL, the existing denied state or the existing read error applies. |
| FE-UI-CONTRAST (OD-9; CS-06) | Given Login, Find a consultation at 1440px and 320px, My bookings, the confirmation dialog, Tutor and Admin placeholders, session-check failure and not-found / when rendered in default and keyboard-focus states / then every text element meets the WCAG 2.1 AA ratio in the visual contract and control boundaries, selected/current indicators and the focus indicator meet 3:1. Automated: a browser check computing text contrast (axe-core color-contrast rule or computed-style ratios) reports zero failures on those screens. Non-text and focus contrast: computed ratio check or recorded manual check. |
| FE1-LOGIN-STALE (OD-2; CS-01b, FE-R7c) | S1: Given anonymous Login while the browser still holds an ended earlier session / when Sign in is submitted and GET `/api/auth/csrf` returns session denial / then zero login POSTs are sent, the alert reads "Please try again.", no session-expired, inactive, incorrect-credentials or temporarily-unavailable message appears, the password is cleared and Login stays usable without any protected session-clear transition. S2: Given the login POST returns 403 CSRF_INVALID / then exactly one POST was sent, the token is discarded and S1's message and usability apply. S3: Given S1 or S2 / when Sign in is submitted again / then a fresh CSRF token is acquired before exactly one new login POST; no automatic retry ever occurs. |
| FE1-LOGOUT-ENDED (OD-1; CS-01c) | Given an authenticated actor / when Sign out is chosen and the logout's GET `/api/auth/csrf` (no cached token) or POST `/api/auth/logout` returns session denial / then Login is shown with no alert or warning and no Retry sign out control, private state is cleared, `/me` is not re-requested, and a later Sign in acquires a fresh CSRF token first. Other logout failures follow FE1-LOGOUT-CONFIRMED S3. |
| FE2-BROWSE-MIDNIGHT (FE-R7a; regression for FE2-BROWSE) | Given a slot from 23:30 to 00:30 SGT crossing midnight / when the week is shown on desktop and mobile / then the slot appears once on its SGT start day and its visible details include the following day's SGT end date and the 00:30 end time. |

### Human check and deferred items

| ID (decision) | Disposition |
| --- | --- |
| FE-HV-TEXTSIZE (OD-10; INFO-2) | Human visual check, not an automated acceptance criterion: Phuc checks timetable text size on a laptop and a phone (part of the OD-15 journey). If text is hard to read, enlarging it is a new change with its own spec revision. |
| OD-6 (CS-03) | Deferred out of scope by the owner: distinguishable accessible names for repeated Book/Cancel buttons are not an acceptance criterion of FE2-UI or any revision 2 ID. Do not test or implement them under fe3/fe4. |
| FE-R4 (aria-pressed/selected state on Upcoming/History) | No owner decision recorded in OD-1..OD-17; remains a recorded low reviewer follow-up outside fe3/fe4. |

## Revision 3 acceptance: light and dark theme switch (TH-D1..TH-D3, 22 October 2026)

Authorization: Phuc, 22 October 2026, current-session request to add a light and dark
theme switch, with decisions recorded in
[stu-fe5-theme-switch](changes/stu-fe5-theme-switch.md):
- TH-D1: the default theme is always light on a first visit; the device setting
  (`prefers-color-scheme`) is not followed.
- TH-D2: the chosen theme is remembered on the same browser, including on Login.
  Only the theme preference is stored (no personal data); it is not private data
  under SH-WEB-03.
- TH-D3: the switch is in the top bar on every page: Login, Student workspace and
  the Tutor/Admin placeholder pages.

### Theme contract

Themes: `light` (the existing white/orange UniChope contract above) and `dark` (dark
neutral surfaces, light readable text and the same orange brand accent, whose exact
value may differ per theme to meet contrast). The active theme is exposed on the
document root as Bootstrap 5.3's colour-mode attribute `data-bs-theme="light|dark"`,
and the CSS `color-scheme` of the root matches it so native controls (date input,
select, scrollbars) render for that theme. All local colours come from per-theme
semantic tokens; light-only literal colours must not remain in components shown in
dark. Exact palette values are builder choices measured by TH-CONTRAST.

Control: one native `<button type="button">` inside each page's banner. Its
accessible name is exactly "Dark theme" in both states (the name does not change
with state) and `aria-pressed` is `"true"` when dark is active and `"false"` when
light is active, so screen readers announce it as a toggle with its pressed state.
It shows a visible icon and may show visible text; the pressed state is visually
distinguishable by more than colour (for example a sun/moon icon change). It works
for anonymous and signed-in users identically.

Persistence (TH-D2): localStorage key `unichope.theme`, value exactly `light` or
`dark`. It is written only when the user activates the switch, never on load,
render, sign-in, sign-out or session handling, and it is never removed by the app.
On load, exactly `dark` selects dark; anything else (absent, `Dark`, `blue`, empty,
JSON) or a storage access error selects light, without an error message, alert or
uncaught exception. If writing fails, the switch still changes the theme for the
current page lifetime. The preference belongs to the browser, not to an account.
Cross-tab live synchronization is not required (another tab applies it on its next load).

Early application: before the application bundle executes, a same-origin synchronous
script referenced from `index.html` `<head>` (not an inline script, so a future
`script-src 'self'` policy needs no change) applies the stored theme with the same
validation rules. The React application also applies the stored theme itself, so
rendering `<App />` without `index.html` (RTL tests) produces the same result. The
two code paths must share identical rules. Feasibility note: under the Vite dev
server CSS is injected by script, so the automated check verifies the root
attribute before the bundle runs; visual absence of a light flash in a production
build is a recorded manual check.

Placement and OD-12: the switch sits in the single banner landmark required by
FE1-UI-LANDMARKS. Student header: added beside the account area, header otherwise
unchanged. Login header: added at the end of the existing header. Tutor/Admin
placeholder banner: app name (optionally a home link), the switch and Sign out only.
Session check, session-check failure and not-found screens: the banner that stu-fe3
adds also carries the switch (analyst reading of "every page"). While the
confirmation dialog is open the switch is outside the dialog's focus trap; dialog
behaviour is unchanged. At 320px the banner and page do not overflow horizontally.

Session independence: activating the switch sends no network request and changes no
URL, actor, CSRF token, filter, loaded result, open notice, pending request or
focus target other than keeping focus on the switch. Sign-out (every outcome),
session expiry, inactive account, account change and restoration never change the
active theme or the stored value.

### Theme acceptance IDs (Given / When / Then)

| ID (decision) | Given / When / Then |
| --- | --- |
| TH-DEFAULT (TH-D1) | S1: Given a browser with no `unichope.theme` value and the device set to dark (`prefers-color-scheme: dark`, emulated) / when any page (Login, a Student page, a placeholder) first loads / then the root has `data-bs-theme="light"`, the light palette is used and the switch reports `aria-pressed="false"`. S2: Given S1 / when the device scheme changes while the page is open / then the theme stays light. S3: Given the same with the device set to light / then the result is identical. |
| TH-TOGGLE | S1: Given light is active / when the switch is activated / then the root immediately has `data-bs-theme="dark"`, dark colours apply, `aria-pressed="true"`, there is no reload or route change and the page content is unchanged. S2: Given dark is active / when activated again / then light returns and `aria-pressed="false"`. S3: Given any page / then the visual pressed state is not conveyed by colour alone. |
| TH-A11Y | S1: Given any page with the switch / then exactly one control with role button and accessible name exactly "Dark theme" exists in the banner, in both states, with `aria-pressed` reflecting the theme. S2: Given keyboard-only use / when Tab moves through the banner / then the switch receives focus in document order with a visible focus indicator; Enter and Space each toggle once; focus remains on the switch after toggling; there is no keyboard trap. S3: Given 320, 768, 1024 and 1440px / then the switch is visible and operable and neither page nor banner overflows horizontally. S4: Given the confirmation dialog is open / then Tab cycling stays within the dialog (FE2-UI unchanged). |
| TH-PLACEMENT (TH-D3, OD-12) | S1: Given each of Login, session check, session-check failure, not-found, Find a consultation, My bookings, Tutor placeholder and Admin placeholder / when rendered / then exactly one theme switch exists and it is inside the page's single banner landmark (FE1-UI-LANDMARKS S1 still holds). S2: Given a Tutor or Admin placeholder / then its banner contains only the app name (optionally a home link), the theme switch and a Sign out button: no navigation landmark, account name, role or caption and no other control. This refines FE1-UI-LANDMARKS S2 from stu-fe5 on. S3: Given the Student workspace / then its header keeps account name/role, Sign out and the Student navigation landmark, with the switch added. S4: Given Login / then FE1-UI-LANDMARKS S3 heading order is unchanged. |
| TH-PERSIST (TH-D2) | S1: Given dark was chosen on any page / when the page is reloaded or another app URL is opened in the same browser / then dark is active from the start. S2: Given dark was chosen on Login while anonymous / when the user signs in as STUDENT, TUTOR or ADMIN / then the role home is dark; when that user signs out (confirmed, ended or unconfirmed outcome) / then Login is dark; when a different account signs in / then it is dark. S3: Given dark is stored / when light is chosen and the page reloaded / then light is active. S4: Given the switch is activated / then exactly one localStorage write occurs, to key `unichope.theme` with value `dark` or `light`; given no activation (load, restore, sign-in, sign-out, session expiry, booking or cancellation) / then no localStorage or sessionStorage write occurs and the key is never removed. |
| TH-STORAGE-FALLBACK | S1: Given reading localStorage throws (access denied or `getItem` throws) / when any page loads / then light is active, the page renders normally, and no error message, alert or uncaught exception occurs. S2: Given `unichope.theme` holds `Dark`, `blue`, an empty string or `{"theme":"dark"}` / when a page loads / then light is active and `aria-pressed="false"`. S3: Given writing throws (for example quota or blocked storage) / when the switch is activated / then the theme still changes for the current page with the correct `aria-pressed`, no error is shown and no uncaught exception occurs; after reload the stored or default rule of S1/S2 applies. |
| TH-NOFLASH | S1: Given `unichope.theme` is `dark` / when `/login`, `/student/slots` or `/admin` is opened in the browser while the application bundle is held unexecuted / then the root already has `data-bs-theme="dark"`. S2: Given the value is absent, invalid or storage throws / then under the same hold the root has `data-bs-theme="light"`. S3: Given `index.html` / then the early step is a same-origin `<script src>` in `<head>` before the application module, not an inline script. S4 (recorded manual check): Given a production build served locally with dark stored / when pages are reloaded / then no light frame is visibly shown before the dark page. |
| TH-CONTRAST (OD-9) | Given each theme (light and dark) on Login (with an error notice and Retry sign out visible), Find a consultation at 1440px (timetable header, today column, striped rows, consultation events with module code and Book) and at 320px (mobile list and day headings), My bookings (Upcoming and History, status labels, empty state), the open confirmation dialog over its backdrop, success, error and status notices, loading state, Tutor and Admin placeholders, session-check failure and not-found / when rendered in default, selected/current and keyboard-focus states / then every text element (including orange accent text, links, text buttons, primary and Book buttons and the switch) meets FE-UI-CONTRAST's WCAG 2.1 AA text ratio, and control boundaries (inputs, selects, date input, outline and Book buttons, the switch), selected/current indicators (current navigation item, active Upcoming/History, today column) and the focus indicator meet 3:1. Automated: a browser text-contrast check (axe-core color-contrast or computed-style ratios) reports zero failures in both themes; non-text and focus contrast by computed ratio or recorded manual check. The root `color-scheme` matches the theme. |
| TH-SESSION | S1: Given an authenticated Student with loaded results, an active filter and a visible notice / when the switch is activated / then zero network requests are sent and the URL, results, filters, notice and actor display are unchanged. S2: Given a slots or bookings read still pending / when the switch is activated / then the read is not aborted or repeated and its result renders. S3: Given dark is active / when sign-out (any outcome), protected 401/ACCOUNT_INACTIVE session denial or restoration failure occurs / then the theme stays dark and the stored value is unchanged. S4: Given private-state clearing on sign-out / then it does not clear or depend on the theme value, and the theme value never contains actor or session data. |

Non-goals: following or offering a "system" theme, per-account or server-stored
preference, more than two themes, live cross-tab sync, changing Bootstrap or any
dependency version, backend, Auth contract or role-workspace content changes,
timetable text-size changes (FE-HV-TEXTSIZE remains a human check), and OD-6/FE-R4.
Updating the static `theme-color` meta in `index.html` per theme is optional.

Frozen-test interactions for the stu-fe5 test writer (update intent, never weaken):
`auth.test.tsx` "FE1-RESTORE restored %s ..." asserts exactly one button on
Tutor/Admin placeholders; it becomes exactly one Sign out button plus the "Dark
theme" switch and no other button. The stu-fe3 FE1-UI-LANDMARKS S2 test, once
written, gains the same allowance. "FE1-PRIVACY ... no private browser-storage
writes" stays valid unchanged because no write happens without activation. Tests
reset `unichope.theme` between cases so earlier choices cannot leak.

## Verification and completion limits

FE0 setup write boundary: `src/frontend/package.json`, `package-lock.json`,
`index.html`, `tsconfig*.json`, `vite.config.ts`, `vitest.config.ts` if separate,
`playwright.config.ts`, `.gitignore`, `README.md`, `src/vite-env.d.ts`,
`src/main.tsx`, `src/App.tsx`, `src/styles.css`, `src/test/setup.ts`, harness tests
under `src/` and browser smoke under `e2e/`. The App shell may render only
UniChope branding and a truthful setup heading; it implements no auth/booking.
Harness includes jsdom, RTL/user-event and optional MSW/axe tools. Use isolated
deterministic HTTP fixtures that expose `givenActor(actor|null)`, endpoint response
and deferred-response controls conceptually; their actual helper names/structure
are test-writer choices, not new production APIs. Feature test writers may replace
the setup App smoke expectation as part of intentional shell integration, retaining
an executable meaningful smoke. Browser can use installed Edge channel `msedge`
when Chromium is unavailable; document the selected channel and prerequisites.

Behavioral RTL tests must exercise real components and fetch boundary, with
deterministic HTTP fixtures (MSW or fetch mock) and deferred responses for races.
Do not assert CSS class names as acceptance. Browser QA covers layout, keyboard,
dialog focus, responsive viewports and console errors, plus accessible-name/axe
checks where feasible. A request-to-real-backend synthetic login/book/list/cancel
smoke is required before claiming integration; mocks alone prove UI behavior.
No new database races are implemented here; existing PostgreSQL backend suites
remain their evidence. Record unavailable browser/DB prerequisites honestly.
Build, focused tests and independent review precede human keyboard journey review.
No document or automated check substitutes for Phuc's final verification.
Revision 2 IDs need new behavioral tests written before their implementation in
stu-fe3/stu-fe4. A frozen FE1/FE2 test whose oracle a revision 2 refinement
changes (for example, the FE1-PRIVACY case that shows the actor name on the Admin
placeholder, which FE1-UI-LANDMARKS removes) is updated by the test writer to keep
its intent, never deleted or weakened.

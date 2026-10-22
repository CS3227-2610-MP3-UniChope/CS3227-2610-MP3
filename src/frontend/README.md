# UniChope frontend

React, TypeScript, Vite and Bootstrap. The application provides real session
sign-in, role destinations and Student consultation browsing, booking and cancellation.
Tutor and Admin destinations are honest placeholders for their owners' future work.

Use Node 24 or newer (verified with Node 24.21.0 and npm 12.0.2). Dependencies
are pinned in `package.json` and `package-lock.json`.

From `src/frontend` in PowerShell:

```powershell
npm.cmd ci
npm.cmd run dev
```

Open the printed localhost URL. `npm.cmd` avoids the Windows PowerShell policy
that can block `npm.ps1`; changing execution policy is unnecessary.

Vite forwards relative `/api` requests to `http://127.0.0.1:8080`. Start the
backend separately using its README. For a different local backend, set the
server-only setting before starting Vite:

```powershell
$env:API_PROXY_TARGET = 'http://127.0.0.1:8081'
npm.cmd run dev
```

The override is read by the Vite server and is not exposed as a `VITE_*` browser
variable. Keep requests relative to `/api`, with same-origin session cookies;
do not add browser credentials, API keys or CORS changes. Start with `/login` or
`/student/slots`; identity is restored through `/api/auth/me`. Production must serve the application and `/api` on one origin,
provide SPA history fallback for UI routes, and preserve API JSON error responses.
`vite preview` previews build output; it is not a production server or API proxy.

Checks:

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run test:browser
```

`build` runs strict TypeScript checking before bundling. Vitest uses jsdom and
React Testing Library. The test-only harness sentinel verifies real DOM keyboard
events without implementing application behavior.

Playwright starts an isolated Vite server on port 5173 and defaults to the
installed Microsoft Edge channel (`msedge`). Close a conflicting development
server before checking a clean build. On another machine, install Edge or install
Playwright Chromium, then select that browser:

```powershell
npx.cmd playwright install chromium
$env:PLAYWRIGHT_CHANNEL = 'chromium'
npm.cmd run test:browser
```

Browser installation and the initial `npm.cmd ci` may require network access.
Browser tests use isolated contexts and do not use a personal browser profile.
In a restricted Windows sandbox, browser tests may need process-control permission
for Playwright to stop its child Vite server; an observed check completed after
that permission was granted. A printed test result without runner exit is not a
completed check.
The browser suite checks rendering, responsive layouts at 320/768/1024/1440px,
keyboard dialog focus and Login. Browser and unit fixtures live only in tests;
the application always calls the real API and never creates demo accounts/data.
Mock checks do not establish backend integration: use synthetic accounts against
the running `app-api` backend to check sign-in, book, list and cancel separately.

Student filters are stored in the URL. The weekly timetable includes Monday
through Sunday in SGT; smaller screens show a chronological list. Booking and
cancellation require explicit confirmation and wait for the server. After an
uncertain response, refresh/check My bookings rather than automatically retrying.
Private account data, credentials and CSRF tokens remain in memory only.

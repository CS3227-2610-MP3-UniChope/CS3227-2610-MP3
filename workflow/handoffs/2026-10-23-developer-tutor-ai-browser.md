# Developer Handoff: Tutor Note-Draft Browser Flow

Date: 23 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Scope: Browser-level regression for the Tutor UI using a test-only fake API.

## Behavior checked

`src/frontend/e2e/tutor-ai.spec.ts` starts the app through Playwright, exercises
the Tutor sign-in screen against a synthetic auth response, loads a completed
booking, submits rough notes, displays a labeled fake draft, copies the reviewed
text to the note editor, and saves it explicitly. The test records API calls and
asserts that generation and copying send no PUT, the booking-list URL requests
`status=COMPLETED`, and the final explicit save sends exactly one PUT with the
original note version.

The browser test uses a fake API route. It verifies rendered browser behavior and
request sequencing, but not the backend, real session cookies/authentication,
PostgreSQL persistence, SoC provider, or production key/quota wiring.

## Verification

From `src/frontend`:

```text
PLAYWRIGHT_CHANNEL=chrome npm run test:browser -- --grep 'Tutor reviews a fake draft'
Exit 0; 1 Playwright test passed in Chrome.

PLAYWRIGHT_CHANNEL=chrome npm run test:browser
Exit 0; all 78 Playwright tests passed in Chrome.
```

Playwright started Vite on `127.0.0.1:5173`; the sandbox initially denied the
local bind, so the successful run used the approved local-server permission. The
test made no external API calls and used synthetic content only.

Related checks recorded for the same UI increment: `npm run build` passed;
Vitest passed 196 tests across 7 files; `TutorReadServiceTest` passed; and all 34
`TutorRestPgTest` cases passed against a disposable UTF-8 PostgreSQL cluster. The
independent review and its findings are recorded in
[`2026-10-23-review-tutor-ai-ui.md`](2026-10-23-review-tutor-ai-ui.md).
The focused browser-test review is in
[`2026-10-23-review-tutor-ai-browser.md`](2026-10-23-review-tutor-ai-browser.md).

No commit or push was made. Live backend/session browser integration and P6
affected-owner/human acceptance remain open.

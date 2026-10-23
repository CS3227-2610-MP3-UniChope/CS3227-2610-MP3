# Developer Handoff: Restore Tutor Slot Management UI

Date: 23 October 2026
Branch: `feat/tutor-booking-completion`
Base input: current branch worktree; existing backend REST slot contract.
Requirements: TUT-SLOT-01, TUT-SLOT-02, TUT-SLOT-03.

## Finding and change

MP2's `tutor/TutorWorkspace.java` had a Slots tab for listing upcoming slots,
filtering by Singapore date, creating a slot from an assigned module/date/time, and
cancelling a selected slot. MP3's backend already exposes the matching secured
endpoints, but the React Tutor workspace had no slot list or mutation controls.

Added an Availability section to the React Tutor workspace. It loads assigned modules
and owned upcoming slots, filters by Singapore start date, converts selected SGT
date/time to UTC `Z` instants, creates slots through the existing API, and offers
confirmed cancellation only for AVAILABLE rows. It refreshes after successful
mutations and preserves creation fields on failure. No lifecycle or persistence code
changed. Added `/api/tutor/slots` to the frontend client's expected HTTP 201 routes;
the backend already returns 201 for creation.

## Verification

- `npm test`: 11 test files, 232 tests passed.
- `npm run build`: TypeScript check and Vite production build passed.
- `git diff --check`: passed before the final wording-only error mapping update; rerun
  before commit.
- No live database or browser session was used for this increment.

New React tests cover SGT-to-UTC creation, date filtering and all-upcoming reset,
interval validation, preserving values after server rejection, cancellation
confirmation, request body, list refresh, and hiding cancellation for booked slots.

## MP2 reuse

Source consulted: `CS3227-2610-MP2/src/main/java/tutor/TutorWorkspace.java`,
`slotsTab()`. React behavior is an adaptation; JavaFX views and MP2 persistence were
not copied. MP3 uses its existing authenticated Tutor REST/controller/service and
shared lifecycle contracts.

## Limits

The existing layout remains work-in-progress. The SoCLaaS live request is separate
and still requires the user's local development key and disposable database.

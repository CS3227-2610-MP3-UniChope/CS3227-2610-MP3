# Tutor Bookings and History UI Increment

Status: Implemented; React/browser evidence recorded; scoped independent review complete, human acceptance pending. Based on `TUT-BOOK-01`, `TUT-BOOK-02`,
`TUT-HIST-01`, and the shared API contract.
Date: 23 October 2026

## Goal and scope

Add functional booking and slot-history controls to the existing Tutor workspace.
This increment is about request, selection, confirmation, and result state; it does
not attempt to finalize visual design or change the existing note/draft workflow.

## Booking list

Show the authenticated Tutor's bookings using `GET /api/tutor/bookings`, with
independent module, Singapore start-date, and booking-status filters. The list shows
module, the booked student's display name, SGT time, and status. A case-insensitive
student-name search filters loaded rows without changing the module/date/status API
query. For ACTIVE bookings, provide a completion action that
asks for explicit confirmation before sending `{}` to
`POST /api/tutor/bookings/{id}/complete`. On success, refresh the booking list and
announce completion. On failure, retain filter values and show a safe error.

The list's filters and refresh must not change the booking selected by the separate
completed-consultation note selector. The selector continues to load
`GET /api/tutor/bookings?status=COMPLETED`.

## Slot history

Show a separate history view with one slot status (`CANCELLED` or `COMPLETED`), an
optional SGT start-date filter, and a case-insensitive student-name search. Load through
`GET /api/tutor/history/slots?status=...&date=...`. Applying history filters must
not change booking filters or the selected note. Display loading, empty, error, and
retry states. Show the booked student's display name when present. For a completed slot that matches a completed booking already loaded
for the note selector, show an **Open note** action. It selects that booking and
opens Consultation notes, where the existing secured note endpoint loads its saved
content. Cancelled slots have no note action. If the tutor has unsaved note or draft
edits, keep the existing confirmation before changing the selected booking. The
availability list also shows the student name for booked slots. Student names are
returned only through the authenticated Tutor's owner-scoped views; rows without an
ACTIVE or COMPLETED booking show an em dash.

## Verification

The matrix is the acceptance oracle. React tests verify request parameters, distinct
filter state, completion confirmation and refresh, safe errors, and separation from
the note selector. Playwright verifies the browser flow using a deterministic API
fixture. This does not establish a live browser session against PostgreSQL or final
visual-design acceptance.

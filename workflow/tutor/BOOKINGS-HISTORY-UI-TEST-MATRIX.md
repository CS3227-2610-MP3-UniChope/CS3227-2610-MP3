# Tutor Bookings and History UI Test Matrix

Applies to [`BOOKINGS-HISTORY-UI-SPEC.md`](BOOKINGS-HISTORY-UI-SPEC.md).

| ID | Action | Expected result | Evidence |
| --- | --- | --- | --- |
| P4-UI-01 | Load workspace | Completed-note selector request remains `status=COMPLETED`; booking list loads independently | `tutor.test.tsx` |
| P4-UI-02 | Apply module/date/status booking filters | Correct query parameters; displayed results update | React test with API fixture |
| P4-UI-03 | Complete an ACTIVE booking and decline confirmation | No POST; booking remains unchanged | React confirmation test |
| P4-UI-04 | Confirm completion | POST `{}` once; booking list refreshes; success is announced | React mutation test and Tutor Playwright flow |
| P4-UI-05 | Completion fails before commit | Refresh the current list, retain filters, and show a safe error | React failure test |
| P4-UI-06 | Apply CANCELLED/COMPLETED history filters with optional date | Correct separate history query and results | React API test and Tutor Playwright flow |
| P4-UI-07 | Change history filters while note has a selected completed booking | Note selection and note editor content remain unchanged | React state-separation test |
| P4-UI-08 | History load fails or returns empty | Safe retry or empty state appears | React error/empty tests |
| P4-UI-09 | Completion returns an ambiguous server error after it may have committed | Refresh the list so the UI reflects persisted state; show a safe error | React simulated post-commit failure test |
| P4-UI-10 | Older booking or history filter request resolves after a newer request | Ignore stale response and keep results for the latest submitted filters | React deferred-response test |
| P4-UI-11 | Booking filters change while completion is pending | Completion refresh uses the latest applied filters | React deferred-completion test |

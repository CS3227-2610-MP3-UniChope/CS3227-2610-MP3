# Tutor Booking Completion Test Matrix

Applies to [`COMPLETION-INCREMENT-SPEC.md`](COMPLETION-INCREMENT-SPEC.md),
requirement `TUT-BOOK-02`. Test the HTTP adapter against the real shared lifecycle
and PostgreSQL fixture; do not duplicate the lifecycle's SQL/race tests.

| ID | Setup / request | Expected result | Evidence |
| --- | --- | --- | --- |
| P4-C01 | Active Tutor owns an ACTIVE booking with a BOOKED slot; valid session and CSRF; body `{}` | 200 exact Booking shape; booking and slot both persist as COMPLETED | `TutorRestPgTest` request-to-PostgreSQL |
| P4-C02 | Another active Tutor targets the booking; compare with a nonexistent UUID | Both return identical 404 `NOT_FOUND`; neither record changes | `TutorRestPgTest` |
| P4-C03 | Student, Admin, missing actor, and inactive Tutor call endpoint | Wrong roles 403; missing actor 401; inactive account 403 and session invalidated; no state change | Real Security filter/session fixture and PostgreSQL state assertions |
| P4-C04 | Missing or invalid CSRF token on an otherwise valid completion | 403 `CSRF_INVALID`; neither status changes | Security-enabled MockMvc with valid JSON and session |
| P4-C05 | Body is missing, null, scalar, malformed, or contains extra field such as `tutorId` | 400 `VALIDATION_FAILED`; no mutation | `TutorRestPgTest` request validation cases |
| P4-C06 | Booking is already COMPLETED, CANCELLED, or its slot is not BOOKED | 409 `INVALID_STATE`; no further state change | Real lifecycle through endpoint |
| P4-C07 | Future-dated ACTIVE/BOOKED booking; Tutor explicitly calls completion | 200; both statuses become COMPLETED | `TutorRestPgTest`; confirms the specified early-completion rule |
| P4-C08 | Force the slot status update to fail after booking update is attempted | Safe 500; transaction rolls back both statuses | PostgreSQL trigger/fault fixture |
| P4-C09 | Fail while assembling the response after the lifecycle commits | Safe 500; booking and slot remain COMPLETED; client must refresh rather than infer rollback | `TutorRestPgTest` with projection failure fixture |
| P4-C10 | Successful response inspected for exact fields | Contains only `id`, `slotId`, `moduleId`, `moduleCode`, `studentName`, `startTime`, `endTime`, `status`; no email | Exact JSON property assertion |
| P4-C11 | Race completion against Student cancellation | At most one transition wins; final booking and slot statuses agree | Reuse existing `PostgresLifecycleConcurrencyTest`; no duplicated race implementation |

All API errors must use the existing `{ code, message, requestId }` envelope and
`Cache-Control: no-store`. Test data uses synthetic users only. No SoC key/provider
is read or called.

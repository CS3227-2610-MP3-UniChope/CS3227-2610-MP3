# Tutor Bookings, Completion, and History Test Matrix

Status: Independently reviewed; scope approved and implementation human-accepted on 22 October 2026; implementation evidence reverified against current main. No application tests added here.
Specification: [Bookings increment](BOOKING-INCREMENT-SPEC.md).
Requirements: TUT-BOOK-01, TUT-BOOK-02, TUT-HIST-01.

| ID | Requirement | Setup and action | Expected result | Evidence layer |
| --- | --- | --- | --- | --- |
| BK-01 | TUT-BOOK-01 | Tutor has ACTIVE, CANCELLED, and COMPLETED bookings; request without filters | All and only owned bookings; stable start-time then ID order | PostgreSQL repository + MockMvc |
| BK-02 | TUT-BOOK-01 | Request each supported status | Only matching owned status returned | PostgreSQL request integration |
| BK-03 | TUT-BOOK-01 | Filter by owned assigned module | Only matching module bookings returned | PostgreSQL request integration |
| BK-04 | TUT-BOOK-01 | Filter by valid module with no matching bookings | 200 with `[]`; no existence disclosure | PostgreSQL request integration |
| BK-05 | TUT-BOOK-01 | Filter by Singapore date where UTC instant falls on adjacent calendar date | Match uses Singapore start date | Repository boundary test + PostgreSQL request integration |
| BK-06 | TUT-BOOK-01 | Combine module, date, and status filters | AND semantics; only rows satisfying every filter | PostgreSQL request integration |
| BK-07 | TUT-BOOK-01 | No owned bookings or filters with no results | 200 with `[]` | PostgreSQL request integration |
| BK-08 | TUT-BOOK-01, SEC-01 | Two tutors have bookings; T1 lists without or with filters | No T2 row appears, even when filter identifies T2's module/date/status | PostgreSQL request integration |
| BK-09 | TUT-BOOK-01 | Inspect booking JSON | Exact approved fields; no student email/ID, tutor ID, or unrelated profile data | MockMvc JSON contract |
| BK-10 | TUT-BOOK-01 | Invalid UUID, date, status, unknown/repeated query parameter | 400 safe API error; no database mutation | MVC validation tests |
| BK-11 | AUTH-01/SEC-01 | Missing session, wrong role, inactive tutor session, missing/invalid CSRF where applicable | Existing 401/403/CSRF contract; no booking data or mutation | Security-enabled MockMvc fixture |
| BC-01 | TUT-BOOK-02 | Active T1 owns ACTIVE booking paired with BOOKED slot; complete | 200; both rows COMPLETED; IDs/counts/history retained | MockMvc to PostgreSQL |
| BC-02 | TUT-BOOK-02 | Complete before scheduled start using fixed clock | 200 and both rows COMPLETED, consistent with approved early-completion rule | MockMvc to PostgreSQL |
| BC-03 | TUT-BOOK-02 | Complete the same booking again | 409 `INVALID_STATE`; state remains COMPLETED/COMPLETED | MockMvc to PostgreSQL |
| BC-04 | TUT-BOOK-02 | Attempt completion for CANCELLED booking, non-BOOKED slot, or inconsistent test fixture | 409 `INVALID_STATE`; complete fixture snapshot unchanged | Lifecycle tests + request integration |
| BC-05 | TUT-BOOK-02 | T1 completes T2's booking; compare with missing ID | Both return same 404 `NOT_FOUND`; no state changes | MockMvc to PostgreSQL |
| BC-06 | TUT-BOOK-02 | Cancel UI confirmation | No POST; booking and slot remain unchanged | React component test |
| BC-07 | TUT-BOOK-02 | Confirm completion successfully, then refresh | One request; row displays completed status and is not offered for repeat completion | React component + MockMvc integration |
| BC-08 | TUT-BOOK-02 | Completion request fails with 409 or 5xx | UI retains accurate server state, shows safe error, permits refresh | React component test |
| BC-09 | TUT-BOOK-02, SEC-01 | Send malformed completion JSON or include actorId, tutorId, studentId, role, or status | 400 safe API error; PostgreSQL booking/slot snapshot unchanged | MockMvc to PostgreSQL |
| BC-10 | TUT-BOOK-02, AUTH-01/SEC-01 | Attempt completion with missing session, wrong role, inactive tutor, or missing/invalid CSRF | 401/403 per shared contract; PostgreSQL booking/slot snapshot unchanged | Security-enabled MockMvc to PostgreSQL |
| HS-01 | TUT-HIST-01 | Request history with `status=CANCELLED` | Only owned CANCELLED slots returned | PostgreSQL request integration |
| HS-02 | TUT-HIST-01 | Request history with `status=COMPLETED` | Only owned COMPLETED slots returned | PostgreSQL request integration |
| HS-03 | TUT-HIST-01 | Omit/invalid/repeat status or use ACTIVE/BOOKED | 400 safe API error | MVC validation tests |
| HS-04 | TUT-HIST-01 | Filter by Singapore date around UTC/SGT boundary | Match uses slot Singapore start date | Repository boundary test + PostgreSQL request integration |
| HS-05 | TUT-HIST-01, SEC-01 | Two tutors have historical slots; T1 requests each history status | T2 slots excluded; stable start-time then ID order | PostgreSQL request integration |
| HS-06 | TUT-HIST-01 | No matching owned history | 200 with `[]` | PostgreSQL request integration |
| HS-07 | TUT-HIST-01 | Change history status/date filters while a booking is selected | History changes only; selected booking and booking filters do not change | React component test |

## Reuse and non-duplication

- Reuse the existing `PostgresConsultationLifecycle` completion and concurrency
  tests for ordered locking, cancellation/completion races, repeated completion,
  and atomic state transitions. Do not clone those tests into Tutor HTTP tests.
- New PostgreSQL request tests prove the route uses the current actor, scoped
  queries, DTO mapping, and the shared lifecycle against persisted state.
- React tests cover only user-visible confirmation, filtering independence, and
  refresh/error behavior; they do not claim backend authorization.

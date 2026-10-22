# Tutor Slot History Test Matrix

Applies to [`HISTORY-INCREMENT-SPEC.md`](HISTORY-INCREMENT-SPEC.md), requirement
`TUT-HIST-01` and `GET /history/slots` in the shared integration spec.

| ID | Setup / request | Expected result | Evidence |
| --- | --- | --- | --- |
| P4-H01 | Tutor owns COMPLETED and CANCELLED slots; request each status | Only requested status returned, exact `SlotView` fields | `TutorRestPgTest` against PostgreSQL |
| P4-H02 | Include another Tutor's historical slots | No foreign slot returned | PostgreSQL ownership fixture |
| P4-H03 | Optional date on Singapore midnight boundary | Match by `startTime` date in Asia/Singapore | PostgreSQL request test |
| P4-H04 | Multiple matching slots, including equal start times | Ordered by start time, then slot ID | PostgreSQL request test |
| P4-H05 | No matches | 200 with empty array | PostgreSQL request test |
| P4-H06 | Missing, unsupported, repeated status, malformed/repeated date, or unknown query | 400 `VALIDATION_FAILED` | Parser unit and HTTP tests |
| P4-H07 | Anonymous, Student, Admin, or inactive Tutor | Existing security behavior: 401/403; no historical data | Security-enabled MVC test |

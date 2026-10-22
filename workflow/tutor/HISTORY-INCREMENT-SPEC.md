# Tutor Slot History API Increment

Status: Implemented; backend and UI evidence recorded, independent review pending.
Date: 23 October 2026

## Goal and scope

Expose historical slots owned by the authenticated Tutor. This increment implements
`GET /api/tutor/history/slots`; it does not add a UI or change cancellation and
completion behavior.

The request requires exactly one `status` query value, either `CANCELLED` or
`COMPLETED`, and accepts an optional ISO date `date`. The date matches the slot's
start date in `Asia/Singapore`. Unknown, missing, repeated, or invalid query
parameters return the standard 400 validation envelope.

## Access and response

Only an active authenticated Tutor may call the endpoint. Actor identity comes from
the session, and results are limited to slots owned by that Tutor. The response is
an array using the existing `SlotView` shape: `id`, `moduleId`, `moduleCode`,
`startTime`, `endTime`, and `status`. Results include only the requested historical
status and are ordered by `startTime` ascending, then slot ID. No matches return an
empty array. Foreign ownership is never disclosed.

## Verification

The test matrix is the acceptance oracle. Request-to-PostgreSQL tests verify owner
scoping, status/date filtering, Singapore date boundaries, ordering, exact response
shape, empty results, and role/session denial. Unit tests verify strict query
parsing. Tests use synthetic fixtures and no external provider.

Any response or filtering change must update this spec, its matrix, and the shared
integration contract before implementation changes.

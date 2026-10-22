# Core transaction and lock seam for Tutor private notes

Status: Implemented, independently reviewed, and accepted on 22 October 2026 for the scoped core lock-seam increment.
Date: 22 October 2026

## Finding

A generic `withCompletedBookingMutation(..., callback)` conflicts with the lifecycle contract's prohibition on unrestricted callbacks. Renaming it or giving the callback a note-specific functional-interface type does not make it safe: the callback can still invoke AI/network/HTTP or arbitrary slow work while core locks and its transaction remain open. Core cannot enforce the database-only promise through Java's type system. Do not implement that callback without affected-owner review of this tradeoff.

## Smallest alternative for review

Use a no-callback, core-owned `lockForPrivateNote(tutorId, bookingId)` operation that **requires an already-active caller transaction** and joins it. The core adapter and caller must use the same `DataSource` object and Spring transaction resource; a `PROPAGATION_MANDATORY` transaction boundary rejects a missing or differently bound transaction before core lock queries. The caller transaction must use `READ_COMMITTED`; reject another isolation level before acquiring row locks with an integration/configuration `IllegalStateException`. Tutor owns a short transaction, calls core before any note-row access, then performs only short database work on the Tutor-owned note row and commits. Core returns only an immutable identifier context; it exposes no note model, raw connection, repository, or arbitrary SQL.

Core discovers relationship IDs without locking, then locks tutor and booking-student user rows sorted by PostgreSQL UUID order, module, slot, and booking. It re-reads under lock and requires an active TUTOR actor, tutor ownership through the slot, and both booking and slot COMPLETED. It returns booking/tutor/student/module/slot IDs only. The method must fail fast if no transaction is active, so locks cannot be released on return. It applies transaction-local lock timeout 5s and statement timeout 10s, consistent with lifecycle. The operation must be the caller transaction's first row-locking step; SQL text/order and controlled PostgreSQL lock-wait tests verify the protocol.

Failures: null IDs → `INVALID_INPUT`; missing/foreign booking → indistinguishable `NOT_FOUND`; missing/inactive/wrong-role actor → core actor denial; owned non-completed pair → `INVALID_STATE`; lock/deadlock/serialization/statement timeout → `TEMPORARILY_UNAVAILABLE`. The method throws the sanitized core failure; it does not return a response while the caller transaction remains active. The outer caller must let the failure escape its transaction boundary so rollback completes before HTTP/error mapping. Other SQL errors propagate for rollback. The caller must perform only short database work until commit; this remains a review obligation, not an enforceable API property.

This operation leaves the existing lifecycle `REQUIRES_NEW` mutations unchanged and adds a separate `MANDATORY` join path for this locked context. The caller owns transaction completion. GET authorization remains in the Tutor service/repository and must not be presented as covered by this write seam. The user authorized implementation of the lock seam on 22 October; affected teammate review remains pending before merge/use by another role.

## Acceptance checks proposed

- No active caller transaction: fail before lock queries. Active caller transaction: method joins it and locks remain held through caller commit/rollback.
- PostgreSQL tests prove the core's sorted-user → module → slot → booking ordering, caller lock retention, and fresh post-lock actor/ownership/state checks.
- This core increment has no Tutor note table or note writes, so it cannot prove note-row ordering, first-save races, or note-version CAS. The later Tutor increment must call this seam before note-row access and test those behaviors against its own schema.
- Denial/state failures return no lock context; DB timeouts map to temporary unavailability only after the caller's transaction boundary rolls back, and are not retried here.
- Core has no Tutor dependency and exposes no general transaction/callback/raw-connection API.

NOTE-INCREMENT Q1/Q2/Q3 and its GET path are outside this seam and remain unresolved.

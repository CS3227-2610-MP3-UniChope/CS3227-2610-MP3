# Lifecycle executable evidence map

Date: 21 October 2026. Developer: `/root/core_increment_developer`.
Independent design: [28-case matrix](../LIFECYCLE-TEST-MATRIX.md).
No expected business outcomes were changed. Cases are grouped into JUnit methods
with explicit in-method variants rather than treating every variant as a new
requirement or claiming a percentage of coverage.

Base paths below are under `src/backend/src/test/java/unichope/platform/core/postgres/`.

| Matrix | Executable method(s) | Variants/oracles |
| --- | --- | --- |
| TD-01 | `LifecycleInputTest.td01InvalidInputFailsWithoutConnectingToAnyDatabase`, `PostgresLifecycleTest.td01RequiredArgumentsAndNullTimesHaveTypedFailuresWithoutWrites` | Every null actor/resource, null start/end; unit datasource has no URL and cannot connect; PG rows unchanged |
| TD-02 | `PostgresLifecycleTest.td02InvalidActorRejectedForEveryOperationAndBothPresentMissingResources` | Five operations, absent actor, inactive Tutor/Student, active wrong roles, present/absent resources; no-write snapshots |
| TD-03 | `td03CreatePersistsExactGeneratedSlotAndTd28SurvivesReconnection` | Generated ID, actor/module/interval/status, repository read on a new physical connection |
| TD-04 | `td04StrictTimeAndPositiveDurationWithSupportedPrecisionBoundary`, TD-01 | Equal/past start, equal/reversed end, null intervals, one-microsecond valid boundary |
| TD-05 | `td05ModuleEligibilityFailuresAreIndistinguishable` | Missing/inactive/unassigned module; identical public messages |
| TD-06 | `td06OverlapAcrossModulesHalfOpenAdjacencyAndOtherTutorControl` | AVAILABLE/BOOKED × equal/contained/containing/partial sides, another module, both adjacency boundaries, other tutor |
| TD-07 | `td07CancelledAndCompletedHistoryDoNotBlockNewSlots` | Both history states retained while creating intersecting slot |
| TD-08 | `td08CancelOnlyOwnedAvailableAndPreservePastAndFutureHistory` | Past/future available allowed; repeated cancel and three other states rejected |
| TD-09 | `td09ForeignAndAbsentOwnedResourcesHaveIdenticalNotFound` | Slot cancel and booking cancel/complete; identical missing/foreign code/message, no writes |
| TD-10 | `td10BookAtomicallyAndRejectRelatedEligibilityWithoutDisclosure` | Exact returned/persisted booking and status pair; inactive Tutor/module/removed assignment produce same message |
| TD-11 | `td11BookingRejectsInvalidStatesNonfutureAndExistingActiveConflict` | Three invalid slot states, equal/past start, ACTIVE booking on inconsistent AVAILABLE fixture |
| TD-12 | `td12StudentActiveOverlapButNotAdjacencyHistoryOrAnotherStudent` | Three booking states × same/other student, separate tutor slot, adjacent positive control |
| TD-13 | `td13CancellationRetainsHistoryReleasesSlotAndAllowsRebooking` | Cancellation status pair, retained original, different student rebooks released slot |
| TD-14 | `td14CancelRejectsInvalidPairsStatesAndStrictTimeBoundary` | Cancelled/completed booking, three non-BOOKED slot states, equal/past start, one-microsecond future allowed |
| TD-15 | `td15CompletionAllowsEarlyLateAndNonactorIneligibility` | Before start/after end; inactive student/module and removed assignment; retained IDs/count |
| TD-16 | `td16CompletionRejectsInvalidPairsAndHistoryWithoutWrites`, TD-15 repeat | History booking states, inconsistent pair and repeated completed transition |
| TD-17 | `PostgresLifecycleConcurrencyTest.td17EmptyScheduleConcurrentOverlapsSerializeOnTutorParent` | Held Tutor parent lock, two observed blocked independent callers, exactly one SLOT_OVERLAP and one committed slot |
| TD-18 | `td18SameSlotConcurrentBookingsHaveOneWinnerAndConsistentPair` | Distinct students, one slot; one INVALID_STATE, one ACTIVE row and BOOKED slot |
| TD-19 | `td19SameStudentDifferentTutorsSerializeCrossSlotOverlap` | Same student, distinct tutors/slots; one BOOKING_OVERLAP, only one booked slot |
| TD-20 | `td20BookingVersusSlotCancellationBothQueueOrdersRemainConsistent` | Both deterministic queued winner orders checked; valid resulting pair and exact booking count |
| TD-21 | `td21CancellationVersusCompletionBothQueueOrdersRemainConsistent` | Both winner orders; no mixed slot/booking statuses or extra records |
| TD-22 | `td22EveryOperationRechecksActorAfterWaitingForOrderedUserLocks` | All five operations; compliant holder deactivates actual actor only after caller demonstrably blocks |
| TD-23 | `td23CreateAndBookRecheckAssignmentModuleAndTutorEligibilityAfterWait` | Create assignment/module; booking assignment/module/tutor; all generic MODULE_UNAVAILABLE, unchanged rows |
| TD-24 | `td24ClockIsReadAfterLocksForCreateBookAndBookingCancellation` | Clock advances while blocked to exact start for create/book/cancel; expected time/state failure |
| TD-25 | `td25SecondWriteFailureRollsBackBookingInsertCancelAndCompletion` | Slot-update test trigger first verifies intended booking write is visible, then raises P0001; all three methods roll back and propagate infrastructure exception |
| TD-26 | `td26RealLockTimeoutRollsBackAndSamePhysicalConnectionCanRecover` | Real blocking wait for 5-second timeout, public sanitized failure, no writes, explicit retry on same physical connection, timeout settings restored |
| TD-27 | `td27MappedSqlstatesRollbackAndLocalTimeoutsResetAfterCommitOrFailure` | Triggers raise 55P03/40P01/40001/57014 only after confirming local 5s/10s settings; rollback/no diagnostics, same-connection settings reset after failure/success; P0001 nonmapping covered TD-25 |
| TD-28 | Existing unit/read/Tutor/AI suites + `test postgresTest bootJar`, TD-03 | 14 unit + 33 PG tests pass; JAR builds; read succeeds on recreated connections. Independent source/diff review remains separate |

## Controls and limits

`LifecyclePgFixture` creates a unique schema, explicitly applies the unchanged
review-only Flyway DDL, seeds synthetic relational fixtures, and drops that schema
only. No production datasource, applied V1 or credential field is added. Every
snapshot query comes from DriverManagerDataSource, so it observes commit/rollback
from a fresh connection, rather than the mutation's uncommitted session.

Race tests hold known user locks and inspect `pg_blocking_pids` via bounded polling
before releasing the holder. The 5ms polling delay does not determine race order;
observed DB blocking does. Futures, blocking observation and executor shutdown all
have bounded waits. Workers use distinct physical connections. Queue-order tests
start the first waiter and observe it before starting the second.

TD-27 raises real SQLSTATE-coded exceptions inside test triggers, but **does not
manufacture a real deadlock/serialization conflict or real statement timeout**.
It verifies public mapping and rollback for those states. TD-26 separately supplies
a real lock-timeout/recovery result. Neither finite races nor deterministic fixtures
prove universal deadlock freedom. READ COMMITTED, row-lock order and transaction
binding also require independent source review.

The developer run counts are in [summary](lifecycle-developer/summary.json), XML
alongside it and [full build log](lifecycle-green-attempt-1.log). Normal generated
HTML reports remain under backend `build/reports/tests/`. Human acceptance and
independent implementation review remain pending at this developer handoff.

## Independent-review finding R1: timestamp precision

Independent reviewer found that nanosecond Java timestamps were rounded by JDBC/
PostgreSQL, causing returned values to differ from persisted values and allowing
an apparently future +1ns slot to persist at now. This escaped the original
microsecond-representable fixtures. Coordinator clarified the precision policy:
truncate slot timestamps to microseconds **before** interval/future/overlap checks
and writes; compare against actual post-lock now, reject collapsed/nonfuture
intervals; truncate booking createdAt separately.

Added before the fix, under PostgresLifecycleTest:

- `r1SlotNanosecondsAreTruncatedBeforeValidationAndMatchPersistedValues`: exact
  normalized start/end, persisted equality and adjacency at normalized end.
- `r1SubmicroFutureAndCollapsedIntervalsAreTypedInvalidTimeWithoutWrites`: +1ns
  future that collapses to now, positive interval collapsing to zero, nanos clock
  boundary that normalizes below now, and positive microsecond control.
- `r1BookingCreatedAtWithNanosecondClockMatchesPersistedMicroseconds`: nanos clock
  with exact returned/persisted createdAt equality.

All three failed before the fix (actual focused Gradle run, exit 1); retained
[lifecycle-r1-red.log](lifecycle-r1-red.log) and XML in `lifecycle-r1-red/`.
These regressions refine TD-03/04/10/28 rather than weaken any existing oracle.
Fresh full-suite execution exited 0 in 20s: **14 unit + 36 PostgreSQL tests,
zero failures/errors/skips; bootJar succeeds**. See
[fixed counts](lifecycle-r1-developer/summary.json), XML alongside and
[fixed full log](lifecycle-r1-green.log). Independent re-review/human acceptance
remain separate from this developer result.

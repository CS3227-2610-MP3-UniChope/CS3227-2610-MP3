# Shared AI Admission Test Matrix

This matrix covers the PostgreSQL admission increment from
[the shared gateway proposal](SHARED-GATEWAY-PROPOSAL.md). Tests use synthetic
IDs, a moving clock, a temporary PostgreSQL cluster, and a fresh isolated schema
per case. No SoC credentials or provider calls are used.

| ID | Requirement | Evidence |
| --- | --- | --- |
| ADM-01 | A second in-flight call for the same actor and task is rejected as `BUSY` with a retry delay. | `actorWindowAndInFlightLeaseAreAtomicAndExpire` |
| ADM-02 | For tasks that supply an output validator to the admitted gateway, only usable task output counts toward the actor/task rolling limit; provider failure or invalid task output releases the actor quota charge. | `failedAttemptDoesNotConsumeActorSuccessQuota`; `AdmittedAiGatewayTest.taskOutputValidationRunsBeforeActorQuotaIsCharged` |
| ADM-03 | The actor/task window expires at its configured boundary. | `actorWindowAndInFlightLeaseAreAtomicAndExpire` |
| ADM-03a | Pruning an expired rolling window preserves an unexpired actor/provider in-flight lease, even when the lease deadline exceeds the window. | `pruningKeepsAnActiveLeaseWhenItsWindowHasExpired` |
| ADM-04 | Provider-key reserved output tokens aggregate across actors and task names. | `providerKeyTokenReservationIsSharedAcrossActorsAndTasks` |
| ADM-05 | Provider-key request counts aggregate across actors and task names. | `providerKeyRequestBudgetIsSharedAcrossTasks` |
| ADM-06 | Provider-key in-flight limits aggregate across different actors. | `providerKeyInFlightLimitIsSharedAcrossActors` |
| ADM-07 | Concurrent requests serialize in PostgreSQL; only one can obtain a single actor lease. | `serializedAdmissionsAllowOnlyOneConcurrentRequestForActor` |
| ADM-08 | Missing quota state fails closed with a sanitized `UNAVAILABLE`. | `unavailableSchemaFailsClosedWithSanitizedFailure` |
| ADM-09 | A locked singleton quota row cannot hold admission indefinitely; lock timeout fails closed without inserting an attempt. | `quotaLockWaitIsBoundedAndFailsClosed` |
| ADM-10 | Role-key budgets are independent; exhausting the Tutor alias does not consume the Student alias budget. | `roleKeyBudgetsRemainIsolated` |
| ADM-11 | The shared gateway resolves a server-owned route before admission, admits before provider generation, and retains uncertain timed-out leases without retry. | `AdmittedAiGatewayTest` |
| ADM-12 | An unavailable or broken provider route returns sanitized `UNAVAILABLE` before admission; no quota reservation or provider generation occurs. | `unavailableProviderRouteFailsBeforeAdmission`; `unexpectedProviderResolutionFailureIsSanitizedBeforeAdmission` |
| ADM-13 | Missing provider-key budget fails closed before a durable attempt is inserted. | `missingProviderBudgetFailsBeforeCreatingReservation` |
| ADM-14 | Failed attempts remain charged to provider-key request/token budgets, and uncertain failures retain their in-flight lease. | `failedAttemptStillConsumesProviderKeyBudget`; `failedUncertainAttemptKeepsItsInFlightLeaseUntilExpiry` |

## Limits of the evidence

These tests verify application-side output-token reservations, not actual SoC
billable token usage. They do not test an HTTP provider, raw streaming response
limits, multi-process deployments against a remote managed database, real SoC
quota behavior, prompt-injection effectiveness, production migration rollout,
or endpoint authorization/session revalidation. Those remain separate work.

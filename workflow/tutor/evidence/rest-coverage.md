# Tutor REST executable coverage map

Date: 21 October 2026. Developer: `/root/core_increment_developer`.
Design reference: [independent 22-case matrix](../REST-TEST-MATRIX.md).
Human scope approval: approved on 21 October 2026; scope: Tutor REST implementation
and independent review. Final implementation acceptance remains pending.

New tests are grouped with in-method variants, not one JUnit invocation per matrix
row. PG means `TutorRestPgTest` under core/postgres test package solely to reuse the
existing isolated PostgreSQL fixture; it does not add a platform-to-role production
dependency. Unit means `tutor/http/TutorApiInputTest` or `TutorActivationTest`.

| Matrix | Executable method(s) | Observable evidence |
| --- | --- | --- |
| RT-01 | PG `rt01ModulesAreActiveAssignedExactAndEmpty`; existing Tutor unit ordering | Exact three-field module shape, active actual assignment and empty array. Same-code UUID tie fixture is prevented by schema uniqueness; tie-breaker remains existing unit/source review |
| RT-02/03 | PG `rt02Rt03UpcomingOwnerStatusOrderingAndSgtDates` | Two owners, statuses, strict start=now exclusion, real ID ties/order, SGT midnight split, UTC-Z shape, empty filtered array |
| RT-04 | PG `rt04DetailAllowsAllStatusesAndPastSlotsWithExactProjection` | Detail supports all four statuses and past slots, exact six fields and joined module code |
| RT-05/06 | PG `rt05Rt06AllEndpointsUsePrincipalAndFreshStoredRoleInsteadOfAuthorities` | Five endpoints: anonymous, authenticated principal with empty provider actor, deleted identity, Student/Admin with stale Tutor authorities; current Tutor works with stale Student authority |
| RT-07 | PG `rt07InactiveAndDeletedSessionInvalidatedAndCannotBeReused` | Real session-backed SecurityContext invalidated for inactive/deleted actor; same old session reused without authentication reinjection then receives 401 |
| RT-08 | PG `rt08RealCsrfFiltersDenyMissingInvalidAndPermitValidTokensForBothMutations` | Actual Security filter chain: missing/invalid tokens rejected for both POSTs; anonymous unsafe denial and valid header-token persisted mutations; snapshots unchanged for denied requests |
| RT-09 | PG `rt09ForeignAndMissingIdsHaveSameSafeNotFoundAndNoWrites` | Both detail and cancellation compare code/message for absent/foreign IDs while ignoring server-generated requestId; DB unchanged |
| RT-10/11 | PG `rt10Rt11IdentitySpoofsQueriesAndInvalidPathsNeverSelectActor`; unit `rt11QueriesPathsAndDatesAreStrictAndNeverFirstValueWins` | Spoofed fields/query rejected, arbitrary identity header cannot select owner, repeated date invalid, canonical UUID/date parsing and no first-value-wins |
| RT-12 | Unit `rt12StrictObjectStringFieldsNoCoercionUnknownDuplicateOrTrailingTokens`; PG representative validation | Object/required-field/string-only rules, duplicate/unknown fields, scalar/null/array/malformed/trailing values; real request sample denied without writes |
| RT-13 | Unit `rt13UtcZOnlyAndExactlyEmptyCancellationObject`; PG `rt12Rt13RepresentativeValidationMediaAndRealConflictsHaveSafeResponses` | Fractional Z accepted; offsets/no-zone/invalid calendar rejected; exactly empty object cancellation; safe 415; domain past-time rejection |
| RT-14 | Unit `rt14ActualStreamLimitIncludesWhitespaceAndIgnoresDeclaredLength`; PG `rt14RealRequestBodyBoundaryAndMisleadingDeclaredLength` | Exactly 65536 raw bytes accepted for valid cancel, 65537 rejected with 413 and unchanged DB; UTF-8 multibyte overflow and declared-length header cannot bypass bounded stream reading |
| RT-15/16 | PG `rt15Rt16CreateLocationDetailAndCancelHistoryUseRealLifecycle` | 201 Location follows to same exact resource, real create/cancel counts/state, repeat conflict/history retained, past available cancellation, cancelled detail remains accessible |
| RT-17 | PG `rt17Rt18LifecycleMappingsSafeErrorsAndServerRequestIds`, real conflict sample | All lifecycle codes map to safe statuses; post-precheck ACCESS_DENIED rechecks fresh DB active status; real PostgreSQL time/overlap/assignment denials preserve state |
| RT-18 | PG common `error`/`ok` assertions; `rt18FrameworkMethodFailureHasSafeJsonInsteadOfEmptyOrHtmlResponse` | Exact three error fields, all required statuses plus 405, JSON/no-store, server UUID requestId ignoring header marker, no synthetic SQL/resource leaks; real filter denials use same policy |
| RT-19 | PG `rt19PostcommitProjectionFailureIs500ButDoesNotRollbackOrRetry` | Test-only ModuleRepository projection failure after real lifecycle create/cancel commit; safe 500 before response committed, fresh DB shows retained single mutation; no retry |
| RT-20 | Unit `TutorActivationTest` two methods | Actual default Boot servlet context has no Tutor handler/generated UserDetailsService/login form; activated missing dependency context fails rather than supplying fake actor/data |
| RT-21 | PG `rt21InterleavedSessionsNeverStickToSingletonActor`; source review | T1/T2/T1 session requests remain isolated; CurrentActorProvider fixture implementation lives only in src/test and resolves current context each request |
| RT-22 | Normal `test postgresTest bootJar`; independent report/source review | Existing 50 regressions remain in normal tasks; fresh exact counts/results recorded in developer handoff; no HTTP duplication of SQL race suite |

## Development findings and limits

Initial RED was missing new parser/failure symbols, not environment failure. First
full run then exposed a fixture error: Spring Security test `csrf()` defaults to a
request parameter; the strict contract correctly rejected that unknown parameter.
Fixture tokens were changed to headers with `.asHeader()` while retaining strict
query validation and active filters. No expected status or product rule was weakened.
First-run failed XML is retained under `rest-attempt1/`.

A separate framework-405 regression failed on the next scoped run because MVC has
no selected controller for method mismatch, so controller-scoped advice did not
apply. Added a minimal Tutor-prefix-only MVC resolver for 405/415 using shared safe
JSON output; no global other-role advice change. The `error` assertion now explicitly
requires nonnull content type before checking JSON. Scoped failure XML is retained
under `rest-attempt2/`.

Evidence is layered: strict parser unit tests, actual MVC/Security/session-fixture
requests and real PostgreSQL integration. Fixture identities are not real login,
logout, cookie flags, session rotation/expiry or production-auth evidence. MockMvc
body tests are not real proxy/chunked-transport size-limit evidence. Ephemeral
server startup proves default closed wiring, not production deployment. SQL races
remain covered by the unchanged lifecycle regression suite. No SoC/AI evaluation,
React/browser UI or production schema/credential activation is claimed.

## REST-R1 independent-review extension (RT-18/20)

Independent reviewer reproduced missing safe error JSON on framework 406 (valid
Tutor route, Accept:text/html) and unknown-route 404 (JSON or HTML Accept), even
though initial regressions and positive actual Boot wiring passed. Source scope
remains the existing uniform error contract; no new endpoint or login feature.

Added before the fix:

- `rt18IncompatibleAcceptStillGetsSafe406Json`: correct 406, NOT_ACCEPTABLE, exact
  safe JSON/no-store/requestId despite incompatible Accept.
- `rt18UnknownTutorRouteGetsSafe404JsonForJsonAndHtmlAccept`: both Accept variants
  return safe 404 NOT_FOUND without resource path disclosure.
- `rt18Rt20ActualBootActiveProfileWithTestOnlyDependenciesServesPositiveAndFrameworkErrors`:
  actual Boot servlet application activates tutor-api on loopback/random port with
  test-only dependencies, one controller/advice, positive modules request and safe
  framework responses. It reuses the same isolated PostgreSQL fixture and principal
  provider registration as the other request tests. Requests still use MockMvc,
  so actual server startup is not represented as real browser/login evidence.

Focused RED executed four RT-18 methods: existing 405 passed; all three additions
failed on missing safe JSON content type (exit 1 in 7s). Retained
[RED log](rest-r1-red.log) and XML under `rest-r1-red/`.
The existing Tutor-prefix framework resolver now handles negotiation 406 and
no-handler/no-resource 404 with direct ApiErrors output. Success representation
negotiation remains intact; unrelated role paths are untouched.

The first REST-R1 fix passed 20 unit + 54 PostgreSQL tests and bootJar, retained
under `rest-r1-developer/` with `rest-r1-green.log`. Review then confirmed the same
negotiation gap for controller-originated errors: ResponseEntity advice attempted
to negotiate error JSON against text/html and rethrew the original failure.
This remained part of REST-R1 rather than a new feature or changed error policy.

Three further regressions were written before the rendering fix:

- `rt18ControllerErrorsWithHtmlAcceptKeepValidationRoleOwnerAndStateCodes`: preserve
  400 validation, 403 current-role, 404 ownership and 409 state with safe JSON.
- `rt18InactiveHtmlErrorStillInvalidatesOriginalSession`: preserve 403 inactive,
  session invalidation and old-session follow-up 401 under incompatible Accept.
- `rt18PostcommitHtmlErrorKeeps500AndCommittedMutationExactlyOnce`: preserve safe
  500 and committed single slot, without retrying or claiming rollback.

Focused RED matched five HTML methods: both previous checks passed, all three new
ones failed with original exceptions after error-converter negotiation failed
(exit 1 in 6s). Retained `rest-r1-advice-red.log` and XML in `rest-r1-advice-red/`.
Tutor advice now renders the same existing codes/messages/session behavior directly
through ApiErrors with HttpServletResponse, independently of Accept; successful
representation negotiation remains unchanged. If a transport response is already
committed, the original exception propagates rather than promising a replaceable 500.


Fresh final full run: **20 unit + 57 PostgreSQL tests, zero failures/errors/skips;
bootJar succeeds**, exit 0 in 25s. Retained [final log](rest-r1-advice-green.log),
[counts](rest-r1-advice-developer/summary.json) and corresponding XML. Independent
review closure and final human implementation acceptance remain separate decisions.

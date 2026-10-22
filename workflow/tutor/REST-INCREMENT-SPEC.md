# Tutor REST Increment Specification

Status: Implemented and independently verified on 21 October 2026; REST-R1 closed. Human implementation acceptance pending.
Date: 21 October 2026
Scope: Tutor module/slot reads and slot creation/cancellation HTTP adapters.

## 1. Proposed increment

Expose the existing Tutor read behavior and ConsultationLifecycle through the
agreed JSON contracts. Preserve `CurrentActorProvider.currentActorId(): Optional<UUID>`.
Develop HTTP integration using server-injected test identities until the auth owner
supplies the real session adapter. This is simulated authentication evidence, not
working login, logout, session rotation or expiry.

References: [integration contract](../INTEGRATION-SPEC.md),
[shared security rules](../SHARED-TECHNICAL-SPEC.md),
[Tutor requirements](PRODUCT-SPEC.md), [plan](REST-INCREMENT-PLAN.md).
Requirements: TUT-SLOT-01/02/03; shared AUTH/SEC and SH-AUTH-03/04/05.

## 2. Endpoints and representation

All paths below begin `/api/tutor`. Every operation requires a currently active
TUTOR account. Slot JSON contains only `id`, `moduleId`, `moduleCode`, `startTime`,
`endTime`, `status`; module options contain only `id`, `code`, `name`.

| Method/path | Input | Successful result |
| --- | --- | --- |
| GET /modules | No query parameters | 200 active assigned module array, code then ID order |
| GET /slots | Optional single `date=YYYY-MM-DD` | 200 own strictly future AVAILABLE/BOOKED slots, startTime then ID order |
| GET /slots/{id} | UUID path ID, no query parameters | 200 owned slot of any status |
| POST /slots | JSON object with exactly moduleId, startTime, endTime | 201 Slot; Location: /api/tutor/slots/{id} |
| POST /slots/{id}/cancel | Exactly empty JSON object `{}` | 200 cancelled Slot |

Lists return `[]` when empty. Detail is included so creation's Location has a real
owner-scoped target; it is not filtered by future date or current status. Wrong-owner
and missing detail/cancellation IDs return the same 404 NOT_FOUND response.
The upcoming date means the slot's start date in Asia/Singapore. Preserve existing
service ordering/clock behavior and use shared UTC instant serialization.

Reject unknown/repeated query parameters, invalid/blank dates and IDs, missing/null
fields, unexpected mutation fields, scalar/array bodies, malformed JSON, non-string
IDs/timestamps and timestamps without an explicit UTC Z representation with 400
VALIDATION_FAILED. Do not coerce numbers to strings. Mutation content type must be
application/json; unsupported media types return 415 with a safe error body.
Carry forward the shared JSON body limit: at most 65,536 raw request-body bytes
(64 KiB), including JSON whitespace. Exactly that size is not rejected for size;
above it returns 413 PAYLOAD_TOO_LARGE with the normal safe JSON/no-store response
and no mutation. Enforce a bounded actual-stream read, not only Content-Length,
so absent or understated length cannot bypass it. Size-limit tests use an otherwise
authorized request with valid CSRF; earlier security denial may still take precedence.
Cancellation requires `{}`; absent, null or nonempty bodies fail 400. Reject body
actorId/tutorId/role/status rather than ignoring them. No new pagination or filters.

The integration contract specifies UTC strings ending Z. If existing client owners
need offset timestamp input, amend that shared contract explicitly before broadening
this input rule. Configure strict parsing locally to the adapter where possible,
not by changing unrelated AI or future role behavior.

## 3. Identity and authorization

The existing provider contract remains the service seam. Identity is supplied by
trusted server authentication; never by query/body/header impersonation, URL-selected
user, a dev hardcoded account or an anonymous fallback. Test implementations and
synthetic identities live only under src/test. A request-scoped provider resolves
the current security context each request; never store a mutable current actor in
a singleton. The real principal/session interpretation remains auth-owned.

Re-read account state through the existing repository on each operation. Stored
role/active status, not cached authorities, is authoritative. Prefer a small shared
Tutor access helper or focused exception reason extension reused by reads and
mutations; preserve current behavior/tests and avoid a broad service rewrite.

Proposed HTTP policy for test design:

| Condition | Response and state |
| --- | --- |
| Missing authenticated principal/provider actor | 401 UNAUTHENTICATED |
| Authenticated actor ID no longer exists in DB | 401 UNAUTHENTICATED; clear authentication and invalidate fixture session |
| Existing inactive account | 403 ACCOUNT_INACTIVE; clear authentication and invalidate fixture session |
| Active account has wrong current stored role | 403 FORBIDDEN |
| Missing/invalid CSRF on unsafe request | 403 CSRF_INVALID; no mutation |

Following invalidation, a request using the old fixture session must be unauthenticated.
The auth owner must align the later real adapter with this proposed deleted-account
policy. Real login/logout/expiry behavior cannot be claimed from this fixture.
Filter-chain CSRF denial may precede identity denial; neither path returns private
content or writes. Resource lookup occurs after actor checks. Lifecycle mutations
repeat actor/ownership/state checks under their existing locks.

## 4. Components and writes

Use one focused Tutor controller, DTOs and HTTP service methods. Reuse
TutorReadService and TutorReadRepository; add a minimal owner-scoped slot-detail
lookup, implemented with parameterized SQL and safe joined projection. Core remains
independent of Tutor. Mutations call the existing ConsultationLifecycle; controllers
contain no SQL or business rules. No public raw repository writes are introduced.

Creation/cancellation responses are projected from the lifecycle result and module
code without reapplying upcoming filtering. The mutation result determines status;
a concurrent later transition is reflected by a subsequent GET. Do not describe a
post-commit response/read failure as a rolled-back mutation. No automatic retry.

Map lifecycle INVALID_INPUT/INVALID_TIME to 400 VALIDATION_FAILED, NOT_FOUND to 404,
MODULE_UNAVAILABLE/SLOT_OVERLAP/INVALID_STATE to 409 using their stable codes, and
TEMPORARILY_UNAVAILABLE to 503. On ACCESS_DENIED re-evaluate current actor state for
the safe 401/403 category; preserve transaction denial. Unexpected failures are
sanitized 500 INTERNAL_ERROR. No SQL/upstream exception text or identifiers in errors.

Every success and failure uses Cache-Control: no-store. Error JSON is exactly the
shared `{ code, message, requestId }` shape; messages are safe display text, requestId
is server generated, and clients do not parse message wording. Filter/CSRF errors
must use the same JSON/no-store policy as controller errors, not HTML redirects.
Framework errors on Tutor paths also follow that envelope: 405 METHOD_NOT_ALLOWED
(retain Allow), 406 NOT_ACCEPTABLE, and unmapped-route 404 NOT_FOUND. Write safe
error JSON even when an incompatible Accept header caused the failure. This is
a clarification of the uniform error contract from independent review REST-R1,
not a change to successful-response content negotiation or other role routes.

## 5. Safe wiring while auth and production schema are pending

The current Boot application is non-web, without production datasource/service beans.
Adding MVC/Security dependencies must not silently expose unauthenticated controllers,
enable generated form/basic login or supply a fake main-source actor.

Propose one explicit `tutor-api` activation profile for Tutor HTTP bean wiring and
controllers. Tests enable it and supply test-only authentication/provider/data beans.
Default startup does not expose Tutor routes; a deliberate deny-all security chain
prevents default generated login from granting API access. Exclude default generated
user provisioning where required by the selected Boot configuration. Do not create
a second login system. Missing required identity or database beans in activated mode
must fail startup rather than fall back to anonymous/test data.

Production activation is deferred until auth supplies its reviewed session adapter,
matching filter/CSRF policy, explicit datasource and services are wired, and the
credential-compatible migration is finalized. Before then an activated test context
is not a deployable authenticated app. No default Flyway location, production
database/profile secrets or credential schema is changed in this increment.

Keep the profile mechanism minimal: no rollout framework or hierarchy of feature
flags. The developer must verify actual Boot 4 test module dependencies and security
auto-configuration from the existing pinned build before choosing imports.

## 6. Verification and evidence

| ID | Required scenario/oracle |
| --- | --- |
| REST-01 | Exact module/upcoming/detail JSON, empty lists, stable order and SGT boundaries; own cancelled/completed detail permitted |
| REST-02 | Missing/deleted actor 401; wrong stored role 403; inactive 403 plus session invalidation; no content/mutation |
| REST-03 | Real Security filter chain and CSRF: missing/invalid token deny; valid token allows authorized create/cancel; no disabled filters |
| REST-04 | Cross-owner/missing detail and cancel 404 equivalence; spoofed actor/status fields rejected; session actor governs calls |
| REST-05 | Strict JSON/query/path/media validation and 64 KiB body limit, safe errors, server request IDs and no-store on success/controller/filter failures |
| REST-06 | Valid POST creates exactly one slot, correct 201 Location can be followed; cancellation 200 then conflict; state confirmed by real DB |
| REST-07 | Real PostgreSQL-backed MockMvc reads filter synthetic two-tutor fixtures; create/cancel invoke real lifecycle and persist; no passwords needed |
| REST-08 | Default startup cannot expose Tutor routes/generated login; activated wiring without required beans fails closed; test-only actor absent from main |
| REST-09 | Existing regression suites remain green; retain actual normal Gradle reports and scope-preservation checks |

Use security-enabled MockMvc with a test-only security context/session adapter,
Spring Security test authentication and CSRF fixtures. Do not combine an always-set
fake actor with anonymous requests and claim authentication was tested. Store/reuse
fixture security context in session when testing invalidation; injecting a fresh
authenticated principal on the next request would invalidate that oracle.

Layer evidence honestly: isolated controller mocks verify HTTP mapping; a security
fixture verifies simulated principal/session/filter behavior; real PostgreSQL-backed
MockMvc verifies request-to-service-to-database integration. None validates real
login, cookie flags, rotation or expiry until the auth adapter is integrated.
Reuse existing lifecycle SQL/race tests; do not recreate them in every HTTP test.
Run the normal regression suites once after the implemented slice, then repeat only
affected checks for findings/fixes. The previous isolated PostgreSQL server is stopped;
runtime selection/execution is later authorized implementation work, not this draft.

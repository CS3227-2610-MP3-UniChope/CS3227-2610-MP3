# Tutor REST Test Matrix

Status: Independent test-design artifact. Scope approved on 21 October 2026;
subsequent execution evidence is tracked in the [coverage map](evidence/rest-coverage.md)
and [review](../handoffs/2026-10-21-review-tutor-rest.md). The designer did not execute tests.
Date: 21 October 2026. Task: TUTOR-REST-TEST-DESIGN-01.
Designer: `/root/lifecycle_test_designer` (independent of REST implementation).
Inputs: [manifest](rest-test-design-inputs-2026-10-21.json),
[revised spec/plan manifest](rest-test-design-supplemental-inputs-2026-10-21.json),
[REST spec](REST-INCREMENT-SPEC.md), [plan](REST-INCREMENT-PLAN.md),
[architect handoff](../handoffs/2026-10-21-architect-tutor-rest.md).

## Draft clarification incorporated

**Q1 — resolved in the draft:** the coordinator added the shared body policy to
REST §2 and its plan: maximum 65,536 raw UTF-8 body bytes, including whitespace;
exactly the limit is not size-rejected, above it returns JSON 413/no-store without
mutation. Actual-stream enforcement cannot rely on Content-Length alone. Tests
use authorized requests with valid CSRF; earlier security denial may precede size
validation. RT-14 now has an explicit oracle. At the time of the original test-design
draft, this clarification still awaited scope review. Scope approval and subsequent
implementation/review evidence are recorded in the status above and linked coverage
map; this paragraph records design history only.

## Fixtures and evidence layers

Synthetic T1/T2 active tutors, S1 student, A1 admin, I1 inactive tutor, D1 deleted
account represented only in the test session. Modules include active assigned,
inactive, unassigned and same-code ordering ties where schema permits; use UUID
ordering for ties. If code uniqueness prevents a module-code tie, test ordering
with distinct codes and assert the documented ID tie-breaker by review rather than
create an impossible fixture. Slots include both owners, future AVAILABLE/BOOKED,
past/now, CANCELLED and COMPLETED. Clock is fixed; UUIDs/time precision are stable.
Use populated fixtures so empty results cannot hide authorization/filtering bugs.

Paths below are relative to `/api/tutor`. Define five endpoint variants for shared
auth/header assertions: GET `/modules`, GET `/slots`, GET `/slots/{id}`, POST
`/slots`, POST `/slots/{id}/cancel`. Successful mutation bodies and CSRF are valid
unless the case deliberately varies them. Read private fixture rows through a fresh
connection after requests; compare IDs/counts/relationships/statuses and unchanged
unrelated data, not merely HTTP codes.

- **HTTP:** focused MockMvc binding/mapping tests may mock service results/failures.
- **SEC:** real Spring Security filter chain, active CSRF, server-side test session
  context and test-only CurrentActorProvider; never `addFilters=false`. Simulated
  identities/fixtures live exclusively in `src/test`, not request actor headers or
  unconditional main-source fallback providers. Test-only session creation supplies
  initial authentication without inventing a production login endpoint.
- **PG-REQ:** Security-enabled MockMvc plus actual Tutor services/repositories,
  lifecycle and isolated PostgreSQL review schema. These are request-to-DB checks;
  simulated identity does not prove real login, cookies, session rotation or expiry.
- **CTX/REVIEW:** startup/context assertions and independent source/dependency review.

All cases are **Designed**, not executed. **D** needs scope approval, REST source,
test fixtures and relevant dependencies; **PG** additionally needs explicitly
selected disposable PostgreSQL. Missing
PG configuration must fail rather than skip. Existing SQL concurrency/rollback
regressions remain the authority for lock behavior; do not duplicate that race
suite at HTTP level. No browser/React, real auth, AI or production activation claim.

## Cases

| ID / requirements | Fixture and risk | Action / variants | Expected response and state oracle | Layer / readiness |
| --- | --- | --- | --- | --- |
| RT-01 · REST-01/07 | T1 has active assigned, inactive and unassigned modules; T2 controls | GET `/modules` as T1; repeat with no eligible assignments | 200 exact array elements `{id,code,name}`, active assigned only, code then ID ordering; `[]` when empty, no credential/profile fields or writes | PG-REQ · D/PG |
| RT-02 · REST-01/07, TUT-SLOT-01 | Mixed T1/T2 future/status slots, identical start-time ties and start = now | GET `/slots` with no filter | 200 exact `{id,moduleId,moduleCode,startTime,endTime,status}` shapes; only T1 future AVAILABLE/BOOKED; strictly now excluded; startTime/ID order, UTC Z string times; no implicit truncation; `[]` variant | PG-REQ · D/PG |
| RT-03 · REST-01/05, SH-DATA-01 | Two SGT-midnight boundaries represented in UTC, future relative to fixed now | GET `/slots?date=YYYY-MM-DD` around `16:00:00Z` boundaries | Only slots whose start date falls in selected SGT day; lower boundary included, next midnight excluded; end date does not control selection; filtering retains owner/status/future rules | PG-REQ · D/PG |
| RT-04 · REST-01/04/06 | Owned slots in all four statuses, including past AVAILABLE | GET `/slots/{id}` for each | 200 exact Slot shape regardless of status/future; moduleCode projected correctly; no note/student/owner IDs. This detail target must work independently of upcoming list filtering | PG-REQ · D/PG |
| RT-05 · REST-02/03/07 | No principal; authenticated principal whose provider returns no actor; D1 identity deleted from DB | All five endpoints; valid CSRF for unsafe requests isolates identity denial | 401 UNAUTHENTICATED, safe JSON/no-store; no private content/writes. Missing principal must not be rescued by a fixed actor fixture; deleted account clears authentication and invalidates fixture session | SEC + PG-REQ · D/PG |
| RT-06 · REST-02/07, SH-AUTH-04 | Stored S1/A1 with stale TUTOR authority; stored active T1 with stale non-tutor authority | Exercise five endpoints with misleading cached roles; use actual stored roles | S1/A1 => 403 FORBIDDEN, no writes/data; T1 succeeds for valid requests based on current DB role. Authorities must not override DB state; does not require implementing production principal parsing | SEC + PG-REQ · D/PG |
| RT-07 · REST-02, SH-AUTH-04 | Initially authenticated session for I1, or T1 later deactivated; separate deleted D1 variant | First protected request; second request reuses original session/security context without reinjecting fresh principal | Inactive => 403 ACCOUNT_INACTIVE; deleted => 401 UNAUTHENTICATED; both clear authentication/invalidate session. Old-session follow-up => 401; no protected content/writes. Preserve or snapshot old context reference in fixture to avoid accidentally authenticating anew | SEC + PG-REQ · D/PG |
| RT-08 · REST-03/07, SH-AUTH-03 | Active T1 fixture session, valid mutation data | For both POSTs omit token, supply incorrect token, then valid session-backed token; include anonymous missing-token variant | Missing/invalid => 403 CSRF_INVALID, JSON/no-store, zero writes; valid => expected 201/200 and persisted transition. Anonymous unsafe denial may be CSRF before identity; tests must not disable filters/CSRF or claim `/auth/csrf` endpoint exists | SEC + PG-REQ · D/PG |
| RT-09 · REST-04 | T1 session, absent UUID and valid T2-owned slot in matched states | GET detail and POST cancel for absent vs foreign IDs | Same 404 NOT_FOUND code/message/shape; ignore independently generated requestId when comparing. No details of owner, module or slot in error; T2 records unchanged | PG-REQ · D/PG |
| RT-10 · REST-04/05 | T1 context and T2 IDs; populated T1/T2 data | Send actorId/tutorId/studentId/role/status in body; unsupported identity query; arbitrary impersonation header with otherwise valid request | Unknown body/query => 400 VALIDATION_FAILED and zero writes. Header cannot select actor; otherwise valid reads/create remain T1-scoped; no invented header-based principal resolver | SEC + PG-REQ · D/PG |
| RT-11 · REST-05 | Authenticated valid actor/CSRF | Unknown or repeated query keys on every path; repeated date (same/different values); blank/invalid/noncalendar/non-YYYY-MM-DD date; malformed UUID path | 400 VALIDATION_FAILED, safe JSON/no-store, no mutation. No first-value-wins or silently ignored unknown query; valid UUID missing resource separately 404 | HTTP + SEC · D |
| RT-12 · REST-05 | Valid create fixture, fields varied independently | Missing/null/extra fields; numeric/boolean/object/array moduleId or time; scalar/array/null/empty/malformed JSON; invalid UUID; valid JSON followed by extra content | 400 VALIDATION_FAILED, no coercion or DB changes. Exact required three string fields accepted; trailing second JSON value must not be ignored as an accepted object | HTTP + PG-REQ denial sample · D/PG |
| RT-13 · REST-05 | Future valid UTC interval; valid CSRF/session | Z form including valid fractional seconds; explicit +00:00/nonzero offset, no zone, malformed date, null, start <= now, end <= start; cancel absent/null/nonempty/scalar/array body; unsupported content type | Z interval accepted when otherwise valid; non-Z/invalid values and invalid cancel bodies => 400 VALIDATION_FAILED. Unsupported media type => 415 safe JSON. Distinguish syntax parsing from domain time checks; cancellation accepts exactly `{}` | HTTP + PG-REQ · D/PG |
| RT-14 · REST-05, Integration §5 | Authorized request with session CSRF, byte-counted JSON padding and multibyte UTF-8 | Exactly 65,536 versus 65,537 body bytes; omitted/misleading Content-Length variant where test transport supports actual-byte reading | Above limit => 413 PAYLOAD_TOO_LARGE, same safe JSON/no-store and no writes; at limit not size-rejected, other validation applies. Pad a valid cancel `{}` with whitespace to avoid unrelated semantic rejection. MockMvc body evidence is not proof of real proxy/chunked transport limits | HTTP + PG-REQ sample · D/PG |
| RT-15 · REST-06/07, TUT-SLOT-02 | T1 active/assigned, empty eligible interval, unrelated T2 rows | POST valid create with CSRF, follow returned Location using same session; GET upcoming | 201 exact Slot with generated ID/AVAILABLE/T1-owned DB row; Location `/api/tutor/slots/{id}` resolves to 200 same resource; count +1 only. Client ID/actor/status cannot be assigned; shared domain model is not serialized with extra fields | PG-REQ · D/PG |
| RT-16 · REST-06/07, TUT-SLOT-03 | T1 owned AVAILABLE slot; past-time variant allowed; others booked/completed/cancelled | POST cancel `{}`; repeat; independently wrong-state variants | 200 CANCELLED and retained same ID/history; detail still 200, upcoming omits it. Repeat/non-AVAILABLE => 409 INVALID_STATE, unchanged history/count, no booking mutation; no future-only filter on cancellation response | PG-REQ · D/PG |
| RT-17 · REST-05/06/07 | Fresh eligible identity; synthetic domain failures, representative real unavailable-module/overlap fixtures | Map INVALID_INPUT/INVALID_TIME, NOT_FOUND, MODULE_UNAVAILABLE/SLOT_OVERLAP/INVALID_STATE, TEMPORARILY_UNAVAILABLE; simulate ACCESS_DENIED after preliminary auth | Respectively 400 VALIDATION_FAILED, 404 NOT_FOUND, stable-code 409, 503. ACCESS_DENIED re-evaluates DB actor into safe 401/403 while preserving transaction denial; no invented 2xx on denial. Real PG invalid cases leave snapshot unchanged. Existing lifecycle tests supply actual lock/rollback guarantees | HTTP + PG-REQ representative · D/PG |
| RT-18 · REST-02/05/07 | Service failure with sensitive synthetic SQL/UUID marker, plus errors from validation and actual security filters | Exercise 400/401/403/404/409/413/415/500/503; all successful endpoints too | Errors exactly `{code,message,requestId}`, JSON content type, server-generated nonempty requestId distinct from client-supplied marker; no SQL/stack/IDs/credentials/private contents. Every response `Cache-Control: no-store`, no HTML login redirects. Snapshot unchanged for precommit denials; RT-19 handles postcommit distinction | HTTP + SEC + PG-REQ sample · D/PG |
| RT-19 · REST-06/07, §4 response semantics | Real successful lifecycle mutation, test-side failing postcommit projection/serialization collaborator | Fail response after real create/cancel commit; do not add product fault hook | Sanitized 500/no-store if response not committed; fresh DB shows mutation already committed exactly once. Record this response failure as not rolled back; zero automatic retries. A follow-up detail/list reveals committed state. If serialization already committed response, assert retained state and document transport limitation rather than promise replaceable HTTP status | HTTP + PG-REQ · D/PG |
| RT-20 · REST-08 | Default profile vs tutor-api; deliberately absent identity/datasource/services in activated context | Load default context; inspect mappings/security/default login; activate profile missing dependencies | Default exposes no Tutor handler and cannot grant access through generated form/basic login. Activated missing required beans fails startup, no fallback identity/data. Test actual pinned Boot configuration; default route nonexposure can be context evidence when default remains nonweb | CTX + REVIEW · D |
| RT-21 · REST-04/08 | Two independent fixture sessions T1/T2, requests interleaved | T1 request, T2 request, T1 again; inspect src/main/test separation and bean scopes | Each request uses its own server context; never sticky actor leak from singleton. No fake actor/test principal implementation in main source; production adapter remains absent/auth-owned; platform has no role import; no auth/login/AI/React/credential/Flyway activation scope creep | SEC + PG-REQ + REVIEW · D/PG |
| RT-22 · REST-09 | Retained reported 50 pre-REST regressions, new case mappings, scoped dirty inputs | Normal test/postgresTest/bootJar once after implementation; independent reviewer checks reports and diff, then affected reruns for fixes | Retain prior coverage and fresh XML/HTML, commands and case-to-test mapping; no silent PG skips or deleted regressions. Report actual counts, not assumed 50+22 arithmetic. No passing claim until execution; prior SQL races need not be repeated as new HTTP scenarios | Gradle + REVIEW · D/PG |

## Execution and review notes

Review and approve the increment before implementation. Implement security/DTO
failures first, then PG request positives/denials, activation checks and retained
regressions. A test fixture with always-present actor would invalidate RT-05/07;
a fresh authenticated principal on the follow-up would invalidate session clearing.
Controller mocks cannot establish RT-01–04/15–16 real persistence or actor isolation.

Reviewer should map grouped variants explicitly to executable tests and distinguish
unrun/blocked checks from passed ones. Preserve source/test scope, reports and
sanitized evidence. This matrix does not evaluate the development agents, prove
production authentication, or claim complete application/AI security. No new SQL
race harness, agent role, endpoint or framework is required by these cases.

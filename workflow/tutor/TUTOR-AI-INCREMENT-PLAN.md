# Tutor AI Note-Drafting Increment Plan

Status (23 October 2026): Input validation, output parsing, shared admission, and
the fake-backed Tutor draft endpoint/service are implemented. Production
AuthenticationService/account integration is tested. Independent review found
and verified fixes for the production raw-provider bypass, failure mappings, and
stale status claims. The latest full run passed 91 unit tests, 192 PostgreSQL
tests, and 193 frontend tests; `bootJar` and the frontend build succeeded. See
the current verification evidence.
The completed-booking list API and draft UI are implemented; production provider
wiring, active migration, affected-owner acceptance, and final post-UI verification
remain pending. No SoC calls or production AI activation are claimed.
Specification: [Tutor AI increment](TUTOR-AI-INCREMENT-SPEC.md).
Test matrix: [Tutor AI test matrix](TUTOR-AI-TEST-MATRIX.md).
Evidence register: [Tutor AI evidence](TUTOR-AI-EVIDENCE.md).

## Sequence and gates

1. Obtain shared gateway/admission decisions from the Student AI owner,
   including per-role key routing, per-key provider quota
   accounting, HTTP mappings, `Retry-After`, raw response byte cap, production
   fake-provider behavior, task key, and exact positive `maxOutputTokens` value.
   The proposed app limits are 3 accepted admissions per Tutor per rolling 5
   minutes, one in-flight request per Tutor, and a 20-second end-to-end deadline.
2. [x] Capture the request's `SessionPrincipal`; after generation call
   `AuthenticationService.current(capturedPrincipal)` and verify the returned
   role is TUTOR. PostgreSQL app-api integration covers generation-time session
   revocation and account deactivation. Auth-owner review and coordination of the
   shared AI migration version with Core remain pending.
3. [x] Implement and unit-test exact rough-note request parsing and the strict
   Tutor output parser independently of provider calls.
4. [x] Implement Tutor-owned request construction around `AiGateway`; fake-backed
   tests cover task/actor metadata, prompt/data separation, and output validation.
   Deadline and admission behavior belong to the shared wrapper. This class is not
   endpoint or production wiring.
   Do not duplicate the generic provider transport or shared quota store.
   The proposed shared wrapper/API is recorded in
   [SHARED-GATEWAY-PROPOSAL](../ai/SHARED-GATEWAY-PROPOSAL.md); Student/Admin
   review and production migration/wiring remain pending.
5. [x] Implement Tutor authorization/input validation, versioned prompt, strict output
   parser, and draft endpoint. Service tests verify rejected role/session paths make zero gateway calls.
   PostgreSQL-backed MockMvc tests cover CSRF, foreign owner and incomplete booking
   denial before gateway calls. A separate app-api/PostgreSQL integration verifies
   production AuthenticationService/account checks for login, wrong-role denial,
   session-generation revocation, and deactivation during generation.
6. [x] Add the completed-booking selector API and draft editor behavior with async
   state handling to the Tutor page.
   Keep generated draft, rough input, and saved note separate; save only through
   explicit copy, edit, and normal version-checked PUT.
7. [x] Run backend unit/MockMvc tests, PostgreSQL tests, frontend tests, and
   frontend build. Record fake-mode limitations and retain a separate live
   provider evaluation plan; see the final verification evidence.

## Reuse and ownership

Reuse the shared `AiGateway` contract, accepted fake, Tutor completed-booking
authorization, and versioned private-note endpoint. Extend the existing Tutor
note service/page where that keeps the design simple. The Student role owns the
shared quota/admission and provider transport; this branch's shared gateway API
is a proposal for review. Core coordinates any shared AI schema version. Do not
copy provider transport, ship in-memory quotas as production controls, or add an
AI table to the Tutor module.

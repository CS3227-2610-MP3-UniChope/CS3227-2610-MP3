# Tutor SoCLaaS Provider Adapter Specification

Status: Tutor adapter is implemented. Merged PR #10 supplies shared runtime wiring
and the V5 admission migration; V6 adds successful-draft retry accounting. Owner
review, production activation, and live provider limits remain pending. Public
endpoint details are from the SoCLaaS portal; detailed
response/error behavior remains to be verified from the NUS-network documentation.
Date: 23 October 2026

## Goal and boundary

Add a server-side provider adapter that sends an authorized Tutor draft request to
SoCLaaS and returns only model text through the existing `AiGateway` contract. The
adapter does not authorize Tutors, admit/quota requests, save notes, select a model
from browser input, or activate production AI. The shared admitted gateway and its
runtime configuration remain required before the adapter can serve application
requests.

The public [SoCLaaS portal](https://soclaas.comp.nus.edu.sg/) identifies the service
as OpenAI-compatible, documents `POST /v1/chat/completions`, shows Bearer-key
authentication, and directs users to `GET /v1/models` to select a chat-capable model.
It publishes the API base host `https://soclaas-api.comp.nus.edu.sg`. Its detailed
documentation is restricted to the NUS network. Therefore, the adapter uses the
published Chat Completions request contract and a standard OpenAI-compatible success
envelope (`choices[0].message.content`); that response compatibility remains an
explicit live-verification item.

## Fit with the shared AI implementation

The adapter implements the existing `AiGateway` provider interface so the shared
`AiProviderRouter` can return it for the server-owned `AiProviderKey.TUTOR` alias.
The intended call path is:

```text
TutorNoteDraftGateway
  -> AdmittedAiGateway
       -> AiProviderRouter.providerFor(TUTOR)
            -> SoCLaaSChatCompletionsProvider
```

`AdmittedAiGateway` remains responsible for the registered task policy, durable
admission, provider-key budget, overall deadline, and post-response text cap. The
provider adapter only handles SoCLaaS transport and raw response-byte bounding. It
must not be injected directly into Tutor services or replace the shared router.

The router should create/use a Tutor provider instance with the Tutor-specific
server secret and a server-selected model. Student and Admin keys/models remain
separate router entries. The existing `AiProviderKey` aliases select those entries;
no secret or alias comes from a browser request.

## Shared runtime configuration

Merged PR #10 provides `AiConfiguration` under `app-api` when
`unichope.ai.mode=shared`. It selects separate server-side credentials by role:
`SOC_LLM_API_KEY_TUTOR`, `SOC_LLM_API_KEY_STUDENT`, and
`SOC_LLM_API_KEY_ADMIN`. `SOC_LLM_MODEL` selects the server-side model. No browser
request selects a key or model, and a missing role key leaves only that route
unavailable. The endpoint remains fixed in the provider implementation.

The local Tutor trial profile uses this shared configuration and may set
conservative `SOC_LLM_RPM` and `SOC_LLM_TPM` values. The shared runtime currently
sets the per-key in-flight cap in code and applies the Tutor task's per-actor
rolling limit. Verify all limits against current SoCLaaS guidance before widening
them or enabling production. Development and production must use separately
provisioned secrets.

V5 admission schema is in the normal Flyway sequence. The retry accounting change
adds V6, which marks whether an attempt counts toward the actor's
successful-draft limit. It is additive and defaults existing attempts to counted;
Core owner review is still needed before the changed production quota semantics are
used.

## Configuration and request

- Use the fixed HTTPS endpoint `https://soclaas-api.comp.nus.edu.sg/v1/chat/completions`.
  Tests may inject a loopback HTTP URI; production wiring must not accept an arbitrary
  browser-controlled or environment-controlled destination.
- Read the Tutor API key from `SOC_LLM_API_KEY_TUTOR`. Never expose or log it. Do not
  provide a fallback key or model.
- Read the model ID from `SOC_LLM_MODEL` after checking that it supports chat.
  Never accept a model ID from a request.
- POST JSON with `model`, `messages`, `max_tokens`, and `stream: false`. Map fixed
  instructions to one `system` message and only the submitted rough notes to one
  `user` message. `actorId` and task metadata are not prompt content.
- Set `Authorization: Bearer <Tutor key>` and `Content-Type: application/json`.
- Set `Accept: application/json`.
- Pass the request's existing positive output-token cap as `max_tokens`; the adapter
  does not retry or increase it.

## Response and failure handling

- Accept only a successful JSON response containing a non-empty `choices` array whose
  first element has a textual `message.content`. Return that text with provider mode
  `SOC`; the Tutor parser remains responsible for the required three-section draft.
- Bound raw response bytes before JSON parsing. A configured cap must be positive
  and no greater than the shared 65,536-byte policy ceiling. Oversized or malformed
  success responses map to `INVALID_OUTPUT`.
- Map HTTP 429 to `RATE_LIMITED`, preserving a valid positive `Retry-After` value
  when supplied. Map other non-success statuses and transport errors to `UNAVAILABLE`.
  Map provider request timeout to `TIMEOUT`. Never return or log upstream bodies,
  exception messages, prompt text, or credentials.
- Use HTTPS in production, disable redirects, and enforce a bounded request timeout.
  The shared admitted gateway remains responsible for the task deadline and durable
  quota/admission. The adapter must not retry.
- Keep the provider unreachable from the frontend. No key, model selection, endpoint,
  raw provider response, or upstream error body may be returned by the API.

## Local development setup

Use a disposable PostgreSQL database and activate `app-api,local,local-soc`
together. The local profile selects `unichope.ai.mode=shared`,
`SOC_LLM_MODEL=qwen3.6:35b`, a two-request-per-minute Tutor-key cap, a 2,048-token
reserved-token-per-minute cap, and a 1,024 maximum Tutor output-token budget. These
are application trial settings, not verified provider quotas. Shared runtime
migrations V5 and V6 are used; do not create duplicate admission tables.

Set `SOC_LLM_API_KEY_TUTOR` and the disposable PostgreSQL connection in the backend
environment. Do not store the key in the repository or use it in production. With
the database variables set, run:

```zsh
./gradlew bootRun --args='--spring.profiles.active=app-api,local,local-soc'
```

Sign in as a Tutor and POST `{"roughNotes":"..."}` to
`/api/tutor/bookings/{bookingId}/note-draft` for a completed booking. A failed
output can be retried manually without charging the Tutor-success quota, but each
attempt still uses the per-key budget; two attempts in the same minute may be
rate-limited. Never print the key, prompt, or provider response body. This local
configuration does not prove live model quality; retain only sanitized status and
usage evidence.

## Verification and deferred gates

Use a loopback HTTP server with deterministic responses; no test reads a real key,
contacts SoCLaaS, or depends on external network access. Test the exact request fields,
redacted values, response extraction, malformed/oversized responses, status mapping,
`Retry-After`, timeouts, and no retry. Production runtime wiring, active production
migration/quota policy, live confirmation of the selected model and token budget,
key provisioning, and live provider evaluation remain separate gates. Before enabling production calls, verify
this response envelope and all
provider-specific rate, token, and error limits against the internal documentation and
a small controlled test using the team-managed Tutor key.

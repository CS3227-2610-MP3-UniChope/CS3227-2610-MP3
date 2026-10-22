# Tutor SoCLaaS Provider Adapter Specification

Status: Tutor proposal for affected-owner review. Public endpoint details are from
the SoCLaaS portal; detailed response/error behavior and live model limits remain to
be verified from the NUS-network documentation.
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

## Proposed configuration for owner review

Keep the existing global provider mode (`unichope.ai.provider = disabled | fake |
soc`) and add a role-specific key/model pair for each router alias:

| Alias | Key property / secret source | Model property |
| --- | --- | --- |
| `TUTOR` | `unichope.ai.providers.tutor.api-key` / `SOCLAAS_TUTOR_API_KEY` | `unichope.ai.providers.tutor.model` |
| `STUDENT` | `unichope.ai.providers.student.api-key` / `SOCLAAS_STUDENT_API_KEY` | `unichope.ai.providers.student.model` |
| `ADMIN` | `unichope.ai.providers.admin.api-key` / `SOCLAAS_ADMIN_API_KEY` | `unichope.ai.providers.admin.model` |

The variable names are the same across deployment code, but each environment must
store its own value: development keys stay in development secret storage and
production keys in production secret storage. Never copy a key between environments
or roles. Model IDs are server configuration, not secrets and not request fields.
Keep the SoCLaaS endpoint fixed in the provider class; do not accept a URL from the
browser or deployment environment.

When mode is `soc`, a role route is ready only when its key, model, provider budget,
and task policy are configured. A missing Tutor key/model or unavailable route
resolves to sanitized `UNAVAILABLE` before `AiAdmissionService.admit` is called; it
creates no reservation and makes no outbound provider call. An unknown task policy
also fails before admission. A missing provider-key budget is rejected by the
admission service before its transaction, so it creates no durable reservation or
provider call. None of these cases borrows Student/Admin configuration or prevents
unrelated role routes from starting. `AdmittedAiGateway` now resolves its configured
provider before admission. Router resolution must remain a local configuration
lookup, without network work.
`disabled` has no ready routes. `fake` is limited to local/test and production
startup rejects that mode. Provider request and token budgets are configured
independently for each alias in `AiProviderBudget`. A real-provider route remains
unavailable until its budgets are verified against SoC limits and configured.

This is a proposed extension to the Student spec's current single-key provider
configuration. Affected owners must accept the role-specific property shape,
partial-configuration behavior, and fixed-endpoint decision before shared Spring
configuration is added.

## Configuration and request

- Use the fixed HTTPS endpoint `https://soclaas-api.comp.nus.edu.sg/v1/chat/completions`.
  Tests may inject a loopback HTTP URI; production wiring must not accept an arbitrary
  browser-controlled or environment-controlled destination.
- Read the Tutor API key from `unichope.ai.providers.tutor.api-key`, populated from
  the server-side `SOCLAAS_TUTOR_API_KEY` secret. Never expose or log it. Do not
  provide a fallback key or model.
- Read the model ID from server configuration after a team member checks the model
  catalogue using `GET /v1/models` and selects one that supports chat; the proposed
  property is `unichope.ai.providers.tutor.model`. Never accept a model ID from a
  request.
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

## Verification and deferred gates

Use a loopback HTTP server with deterministic responses; no test reads a real key,
contacts SoCLaaS, or depends on external network access. Test the exact request fields,
redacted values, response extraction, malformed/oversized responses, status mapping,
`Retry-After`, timeouts, and no retry. Runtime wiring, active migration/quota policy,
model ID, selected token budget, key provisioning, and live provider evaluation remain
separate gates. Before enabling real calls, verify this response envelope and all
provider-specific rate, token, and error limits against the internal documentation and
a small controlled test using the team-managed Tutor key.

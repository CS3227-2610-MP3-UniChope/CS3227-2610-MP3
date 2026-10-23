# Tutor SoCLaaS Provider Adapter Test Matrix

Applies to [`SOCLAAS-PROVIDER-SPEC.md`](SOCLAAS-PROVIDER-SPEC.md).

| ID | Case | Expected result | Evidence |
| --- | --- | --- | --- |
| SOC-T-01 | Valid Tutor gateway request | Sends `POST /v1/chat/completions`, Bearer key, JSON content/accept headers, fixed model, system instruction, rough notes as user input, positive `max_tokens`, and `stream:false`; no actor/task metadata in prompt | Local HTTP contract test |
| SOC-T-02 | Successful compatible response | Extracts only `choices[0].message.content`; returns `ProviderMode.SOC` | Local HTTP contract test |
| SOC-T-03 | Key or prompt appears in object rendering/log-safe representation | Secret and prompt remain redacted | `toString`/captured-call assertions |
| SOC-T-04 | Missing choices, message, or content; non-text content; invalid JSON/UTF-8; trailing JSON value | `INVALID_OUTPUT`; no raw body in failure | Local HTTP response tests |
| SOC-T-05 | Raw response at and above configured byte cap | Exact-cap valid response is accepted; one byte over is `INVALID_OUTPUT` | Local HTTP byte-boundary tests; bounded streaming/cancellation is verified by implementation review |
| SOC-T-06 | HTTP 429 with valid `Retry-After` | `RATE_LIMITED` with parsed positive delay | Local HTTP status test |
| SOC-T-07 | HTTP 429 with absent, malformed, zero, or past-date `Retry-After` | `RATE_LIMITED` without retry delay | Local HTTP status tests |
| SOC-T-08 | HTTP 400 and 503 responses | `UNAVAILABLE`, with no upstream response details exposed | Local HTTP status tests |
| SOC-T-09 | Request timeout | `TIMEOUT`; no automatic retry | Local delayed-response test |
| SOC-T-10 | Redirect response | Production-configured HTTP client does not follow redirect; safe failure | Local redirect test using the production client factory |
| SOC-T-11 | Tutor route lacks its role key or task policy | Returns `UNAVAILABLE` with no durable quota reservation, outbound call, or fallback to another role's credential/model | Wrapper route-preflight test; runtime configuration test still needed |
| SOC-T-12 | Real SoCLaaS smoke test | Verify chosen model, success envelope, request/token limits and safe error behavior; record sanitized usage | Deferred; requires internal docs and approved team key |
| SOC-T-13 | Each configured `AiProviderKey` is routed with role-specific configuration | Shared runtime resolves each alias only to its matching server key; browser data cannot select alias, key, or model | PR #10 `AiConfiguration`; dedicated configuration unit coverage remains to be added |
| SOC-T-14 | Provider mode is `fake` in production or `unavailable` | Production rejects fake mode; unavailable mode returns unavailable before admission and makes zero provider calls | Runtime profile test with admission/provider counters |

Tests SOC-T-01 through SOC-T-10 use a local loopback server only. SOC-T-11,
SOC-T-12, production-wide SOC-T-13, and production SOC-T-14 remain deferred. Local
runtime tests do not prove live provider compatibility, model quality, production
quota enforcement, or secret provisioning.

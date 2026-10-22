# Live Local Tutor AI Integration Run

Date: 23 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Environment: disposable local PostgreSQL 17.11, `app-api,local`, Vite, headless Chrome  
Provider: deterministic local fake; no SoC request or key was used

## Result

The real local application path passed for an authenticated Tutor: login and CSRF,
completed-booking list, saved-note load, fake draft generation, explicit copy/save,
and reload of the persisted note after an unsaved edit. The fake response was visibly
labeled `FAKE RESULT`. Generation did not itself write a note. The browser reported no
page errors.

Authorization checks against the same synthetic completed booking returned:

| Caller | Request | Result |
| --- | --- | --- |
| Anonymous, with a valid anonymous CSRF token | `POST /api/tutor/bookings/{id}/note-draft` | 401 `UNAUTHENTICATED` |
| Authenticated Student | Same draft request | 403 `FORBIDDEN` |
| Different authenticated Tutor | Same draft request for the first Tutor's booking | 404 `NOT_FOUND` |
| Owning Tutor | Same draft request | 200, fake-labeled response with `requiresReview: true` |

The Chrome run saved a reviewed note as version 2, then changed the editor without
saving. “Reload saved note” restored the version-2 persisted text. Its observed API
sequence included login, completed bookings, note GET, draft POST, note PUT, and note
GET; the PUT occurred only after the explicit Save action.

## Reproduction setup

The run used a disposable PostgreSQL cluster under `/private/tmp`, on port 55444, and
the application on 127.0.0.1:18080. It used the `app-api,local` profiles with
`UNICHOPE_AI_MODE=fake`, a local output limit of 256 tokens, and an empty disposable
database. Flyway V1–V4 ran only against that disposable database. The frontend Vite
proxy targeted port 18080 and was served on 127.0.0.1:15173.

Synthetic accounts and booking data were created through the local application API.
There is no Tutor booking-completion HTTP endpoint in this checkout, so the test
fixture's booking and slot were marked completed directly in the disposable database
after the real Student booking API created them. This run therefore does not verify
booking completion through the Tutor HTTP API. All fixture credentials and data were
local-only; no real user data or secrets were used.

The disposable cluster was stopped and removed after the run. The browser scenario was
executed as a one-off local script; it was not
added as a normal CI test because it depends on an explicitly seeded PostgreSQL
environment and local test credentials.

## Limits

This establishes the local session, CSRF, role/ownership, database, fake-gateway, and
browser note workflow for the fixture. It does not establish SoC connectivity, model
quality or prompt-injection resistance, provider quotas, production key isolation,
production migration activation, or deployment security. Production calls remain
disabled pending the shared AI admission/runtime and provider activation decisions.

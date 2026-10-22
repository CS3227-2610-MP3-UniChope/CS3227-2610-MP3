# Scoped Independent Review: Tutor P4 and P7

Date: 23 October 2026
Branch: `feat/tutor-booking-completion`
Review status: Scoped review complete; human acceptance pending.

## Scope

The reviewer compared the current Tutor booking completion/history UI and the P7
approval-text security check with their specs, tests, and evidence. The review was
read-only. It did not assess final visual design, live SoC behavior, production
provider configuration, or the separate development-agent evaluation.

## Findings and resolution

| Finding | Resolution | Evidence |
| --- | --- | --- |
| A completion error after the lifecycle may have committed could leave a stale ACTIVE row. | Refresh the current booking list after a non-stale completion error. | P4-UI-09 simulates a 500 response and a subsequent list read showing coCOMPLETED. |
| Older booking/history filter responses could replace results for newer filters. | Keep a request sequence per panel and ignore superseded responses and failures. | P4-UI-10 resolves deferred requests out of order for both panels. |
| Completion finishing after a filter change could refresh with the filters captured when completion started. | Read the latest applied filters from a ref when refreshing after completion. | P4-UI-11 defers completion, applies a new date filter, then verifies the refresh uses it. |

The reviewer rechecked each fix and found no remaining blocker or obvious regression.
The error-path interleaving is not separately exercised by P4-UI-11; both completion
paths use the same latest-filter reference, and the separate P4-UI-09 test covers the
ambiguous-error refresh.

For P7, the reviewer found the approval-looking output test and its evidence mapping
consistent with the stated application boundary. This does not establish live-model
resistance, provider safety, or completion of the broader AI security review checklist.

## Verification

After the review fixes, from `src/frontend`:

```text
npm test -- --reporter=dot
Test Files  11 passed (11)
Tests       228 passed (228)

npm run build
TypeScript check and Vite production build passed.
```

`git diff --check` passed. The 83-test Playwright suite passed earlier in this work
branch, before the final race-fix tests; it was not rerun for those test-only changes.
No commit or push was made.

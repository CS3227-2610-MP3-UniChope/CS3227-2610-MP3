# Tutor AI P6: Team Review Checklist

Status: Prepared locally for review. No teammate has been contacted, and no
approval is implied.

## What is ready to review

The Tutor flow lists the signed-in Tutor's completed bookings, accepts rough
notes, requests a draft, keeps the generated text separate for review, and saves
only after an explicit version-checked action. Fake output is visibly labeled.
The endpoint rechecks the captured session after generation. The fake-API
Playwright test covers the rendered sign-in, booking selection, draft review,
and one explicit save.

## Decisions requested from affected owners

| Reviewer | Please check | Evidence / decision needed |
| --- | --- | --- |
| Admin owner (Tutor reviewer) | Tutor API ownership/state checks, private-note boundary, UI save flow, and test coverage | Findings or approval for the Tutor-owned implementation |
| Auth owner | Tutor's post-generation `AuthenticationService.current(SessionPrincipal)` recheck and session invalidation behavior | Confirm this matches the shared session contract or identify required changes |
| Student owner | Shared AI admission and gateway contract used by `tutor.note-draft.v1`: task registration, Tutor key alias, typed failures, quota behavior, and fake-provider seam | Confirm integration contract; later choose provider limits/output budget after checking SoC limits |
| Core owner | Whether shared AI admission metadata needs a production migration and, if so, its version and activation plan | Keep the review migration isolated until this decision is made |

## What remains gated

The code contains no real SoC adapter or configured key. Provider routing, raw
response byte cap, verified token budget, production key separation, and active
admission migration remain unverified. Do not use the local fake result as SoC
evidence. Live backend/session browser integration is also not covered by the
Playwright test; its API is synthetic.

Human implementation acceptance remains separate from these technical owner
reviews. The reviewer should try the flow and explicitly accept or return
findings; none of the recorded agent reviews can accept it on the user's behalf.

## Evidence for review

- [Tutor AI increment specification](../tutor/TUTOR-AI-INCREMENT-SPEC.md)
- [Tutor AI test matrix](../tutor/TUTOR-AI-TEST-MATRIX.md)
- [Tutor AI evidence register](../tutor/TUTOR-AI-EVIDENCE.md)
- [Full verification and focused follow-ups](../tutor/evidence/p6-final-verification-2026-10-23.txt)
- [Independent backend review](2026-10-23-review-tutor-ai-independent.md)
- [Independent booking-list/UI review](2026-10-23-review-tutor-ai-ui.md)
- [Independent browser-test review](2026-10-23-review-tutor-ai-browser.md)
- [Browser-flow run record](2026-10-23-developer-tutor-ai-browser.md)

Latest relevant runs: 91 unit tests and 192 PostgreSQL tests in the full backend
run; follow-ups passed the Tutor read-service unit test, all 34 `TutorRestPgTest`
cases, 196 frontend tests, and all 78 Playwright browser tests in Chrome. The
PostgreSQL route follow-up used a disposable UTF-8 database. No real provider
calls or private consultation data were used.

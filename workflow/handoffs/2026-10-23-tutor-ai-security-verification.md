# P7 Handoff: Tutor AI Security Verification

Date: 23 October 2026
Branch: `feat/tutor-booking-completion`
Status: Focused checks passed; scoped independent review complete; human acceptance pending.

## Scope

This verifies the application boundary around Tutor note drafting. It does not repeat
the existing development-agent security evaluation, activate a provider, or test
live-model resistance. The Tutor prompt has no retrieval, file, database, or tool
channel, so an indirect retrieval-injection test has no applicable input path today.
A future retrieval/tool feature must revisit this boundary before implementation.
The prior agent-evaluation record is in the separate main checkout at
`/Users/huanghao/Desktop/hao/Y3S1/CS3227/MP3/CS3227-2610-MP3/workflow/evaluations/agent-security/RESULTS.md`;
it is not included or changed in this feature worktree.

The complete requirements, existing evidence, and the one newly added case are
mapped in [`AI-SECURITY-VERIFICATION-SPEC.md`](../tutor/AI-SECURITY-VERIFICATION-SPEC.md)
and [`AI-SECURITY-VERIFICATION-MATRIX.md`](../tutor/AI-SECURITY-VERIFICATION-MATRIX.md).

## New test

`TAI security: approval-looking model text cannot save a note or grant app access`
returns synthetic model text that claims approval and directs the app to save and
grant admin access. The test verifies that the draft remains text, the saved-note
editor remains unchanged, and no note PUT, completion POST, or Admin request occurs.
The Tutor has to make a separate explicit UI action to copy/save a draft; the backend
still checks Tutor identity, booking ownership/state, CSRF, and note version.

Existing evidence reused rather than duplicated includes the direct-injection
canary test, model-output parser tests, pre- and post-generation session checks,
foreign-owner and wrong-role zero-call checks, CSRF denial, durable admission
failure behavior, and the PostgreSQL endpoint no-write snapshot.

## Verification

From `src/frontend`:

```text
npm test -- --reporter=dot
Test Files  11 passed (11)
Tests       228 passed (228)
```

Static inspection of `TutorNoteDraftGateway`, `TutorNoteDraftService`, the Tutor
controller, and their tests confirmed that the request contains only the fixed
instruction and rough notes, with actor/task/token budget in separate metadata. A
search for logging calls in those Tutor AI production classes returned no matches.
The full 83-test Playwright suite and production build passed earlier in this same
working branch; neither uses a live SoC provider. `git diff --check` passed for code.

## Limitations and review

Passing fake-backed tests cannot establish live model resistance, output truthfulness,
or production key/quota handling. The P4 completion/history changes remain in this
uncommitted worktree and await human acceptance. No commit or push was made.

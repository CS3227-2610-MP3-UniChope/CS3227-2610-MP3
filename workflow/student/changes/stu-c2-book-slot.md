# stu-c2-book-slot: Student books a slot

Status: SPEC_DRAFT (becomes READY_FOR_TESTS when Phuc confirms A1 and A4 below)
Scope classification and reason: standard path; consultation state mutation and a
concurrency-sensitive transition.
Parent request / related changes / dependencies: same request as
[stu-c1-browse-slots](stu-c1-browse-slots.md). Depends on c1's foundation (C1-T1, C1-T2)
and its read repository. [stu-c3-my-bookings](stu-c3-my-bookings.md) supplies the
`StudentBooking` projection and `GET /bookings/{id}` used by this response and its
`Location`; implement c3-T1 (projection) before or with this change.
Human owner / affected owners: Phuc (Student). Core lifecycle (Huanghao) is used unchanged.
Current agent role / actual model or tool: Spec analyst / Claude Code (Opus 5.5).

## Handoff analyst-1
Producer role / actual tool and model: Spec analyst / Claude Code (Opus 5.5)
Recipient role: Test writer, fresh session
Change ID / task / exact requested next action: stu-c2-book-slot / C2-T1 / write failing
tests for the acceptance IDs below and record the red result.
Stage status / supersedes: SPEC_DRAFT, first revision.
Goal and non-goals: Goal: `POST /api/student/bookings` books an eligible slot for the
session student through `ConsultationLifecycle.bookAvailableSlot`. Non-goals: waiting
lists, booking for others, frontend, changing core lifecycle behavior.
Acceptance IDs: S-BOOK-1 to S-BOOK-5 (product spec §7); C2-IN-1 to C2-IN-3,
C2-ERR-1 to C2-ERR-3, C2-ACC-1 (below).

### Inputs and outputs
Same input artifacts and hashes as [stu-c1 Handoff analyst-1](stu-c1-browse-slots.md#inputs-and-outputs);
relevant sections: PRODUCT-SPEC §3 STU-BOOK-01, §7; TECHNICAL-SPEC §3 (POST `/bookings`),
§3.1-§3.3, §4 (`bookAvailableSlot`). Core reference:
`src/backend/src/main/java/unichope/platform/core/postgres/PostgresConsultationLifecycle.java`
`bookAvailableSlot` at commit 502d09fea8a7b4eb3a24c5cf996b09ad2557fb3d.
Output: Student tests for C2 (to be recorded).

Base commit / result commit / complete diff or patch reference: base 502d09f; none yet.
Uncommitted files and their hashes: as stu-c1.
Allowed write scope: as stu-c1 (tests only for the test writer).
Prerequisites / dependency state and evidence: stu-c1 tests and implementation first.
Recorded decisions and authorization references: D1-D4 in stu-c1.
Open questions / proposals / unverified facts:
- A1 (product spec §10 item 1): no enrolment model. Unconfirmed.
- A4 (§10 item 4): nonexistent and no-longer-bookable slots share 409 `SLOT_UNAVAILABLE`. Unconfirmed.
Verification commands, directories, results and output references: baseline as stu-c1.
Readiness or review verdict, scope and supporting evidence: NOT READY until A1 and A4
are confirmed and stu-c1 is at least READY_FOR_IMPLEMENTATION.

### Receiver receipt
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

## Acceptance scenarios

Fixtures as stu-c1. All POSTs use a valid CSRF header unless stated.

| ID | Given / When / Then |
| --- | --- |
| S-BOOK-1 | Given an eligible future AVAILABLE slot / when S1 POSTs `{ "slotId": ... }` / then 201, `Location: /api/student/bookings/{id}` that S1 can GET, body is a `StudentBooking` with status ACTIVE and `cancellable: true`; the DB has one ACTIVE booking for S1 and the slot is BOOKED. |
| S-BOOK-2 | Given a slot that is BOOKED, CANCELLED, COMPLETED, started, nonexistent, has an inactive tutor, an inactive module, or a removed assignment / when booking / then 409 `SLOT_UNAVAILABLE` with the same message in every case, and no row changes. |
| S-BOOK-3 | Given S1 holds an ACTIVE 10:00-11:00 booking / when booking 10:30-11:30 (another tutor) / then 409 `BOOKING_OVERLAP`, no change; booking 11:00-12:00 succeeds (half-open). |
| S-BOOK-4 | Given two students and one slot booked concurrently (use the fixture's row-lock `race` helper) / then exactly one 201 and one 409 `SLOT_UNAVAILABLE`; one ACTIVE booking exists. Given one student and two overlapping slots booked concurrently / then exactly one 201 and one 409 `BOOKING_OVERLAP`. |
| S-BOOK-5 | Given S1 booked then the booking was CANCELLED and the slot is AVAILABLE again / when S1 books it again / then 201 with a new booking ID; the CANCELLED booking still exists. |
| C2-IN-1 | Bodies `null`, `[]`, `{}`, `{"slotId":123}`, `{"slotId":"not-a-uuid"}`, `{"slotId":"<uuid>","studentId":"<S2>"}` (also `status`, `role`), duplicate `slotId` keys, and trailing tokens → 400 `VALIDATION_FAILED`, no change. Any query parameter → 400. |
| C2-IN-2 | `Content-Type: text/plain` → 415 `UNSUPPORTED_MEDIA_TYPE`. A body over 65,536 bytes with a misleading `Content-Length: 2` → 413 `PAYLOAD_TOO_LARGE`; exactly 65,536 bytes is not rejected for size. |
| C2-IN-3 | Header `X-Actor-Id: <S2>` is ignored: the booking belongs to the session student S1. |
| C2-ACC-1 | Missing/invalid CSRF → 403 `CSRF_INVALID`, no change. No principal → 401; tutor/admin → 403 `FORBIDDEN`; inactive student → 403 `ACCOUNT_INACTIVE` with session invalidated; none changes rows. |
| C2-ERR-1 | Lifecycle double throwing `INVALID_INPUT` → 400 `VALIDATION_FAILED`; `TEMPORARILY_UNAVAILABLE` → 503; a unique-index violation (`DataIntegrityViolationException` on `bookings_one_active_per_slot`) → 409 `SLOT_UNAVAILABLE`; an unexpected exception whose message contains SQL text → 500 `INTERNAL_ERROR` without that text. |
| C2-ERR-2 | Lifecycle throws `ACCESS_DENIED` after the account was deactivated mid-request → 403 `ACCOUNT_INACTIVE`. |
| C2-ERR-3 | Projection after a successful lifecycle call fails → 500, the booking stays committed exactly once, and a later GET shows it. |

## Tasks and acceptance-to-test mapping
| Task | Acceptance ID | Test file/case or manual check | State |
| --- | --- | --- | --- |
| C2-T1 `POST /bookings`: input, lifecycle call, error mapping, projection | All above | | pending |

## Verification evidence
Command, working directory, revision, exit code, result and relevant output: pending.
Pre-implementation expected failure (or existing passing baseline): pending.
Final focused tests / integration tests / build: pending.
Manual and real-provider checks: not applicable (no AI).

## Independent review
Reviewer role / actual model / reviewed revision: pending.
Verdict and evidence: pending.
Findings: pending.
Fixes and re-review results: pending.

## Human verification
Person, date, checked journey, outcome: pending.
Remaining limitations and follow-ups: simulated session identity until auth integration.
Next agent, exact task and required inputs: Test writer (fresh session) after stu-c1 and the A1/A4 confirmation.


## Owner decision addendum - 22 October 2026
Recorded by Codex / GPT-6, coordination role; this is a human decision record,
not a new Claude readiness assessment.
Authorization: current user replies "all" and "Approve all four proposed rules".
Phuc requests all four changes with separate fresh GPT test/implementation sessions.
A1-A4 are approved: no enrolment restriction; overlapping slots remain listed;
cancellation before start with no notice period; unavailable/missing slots share
409 SLOT_UNAVAILABLE. Canonical source: PRODUCT-SPEC.md section 10, SHA-256
ebc3d5090f1fd87abb1bb93e164ffb743c8a5849443a62f2016b5d915d1a0957.
This supersedes only the analyst packet's unconfirmed A1-A4 observations and old
product hash; no acceptance behavior changed. Technical/plan hashes are unchanged.
Dependency order: c1 foundation/browsing, c3 booking views/projection, c2 creation,
c4 cancellation. Earlier analyst conditional readiness and all other entry checks
must be verified by each receiver; this addendum does not waive missing evidence.
AI, frontend, production activation and teammate approval remain outside this scope.

## Workflow decision addendum - 22 October 2026
Recorded by the spec analyst session (Claude Code, Opus 5.5) as a human decision record.
Authorization: Phuc, current user request on 22 October 2026: "remove all the claude
and gpt u do everything each in a fresh session".
D5: one agent tool runs every role (spec analyst, test writer, implementer, reviewer),
each in its own fresh session or fresh subagent with no inherited conversation. This
supersedes D1 and every earlier role-to-model assignment in this record ("Claude
analyst", "GPT test writer", "GPT implementer", "Claude reviewer"). Read those labels
as the role name only. "Actual tool and model" fields and earlier receipts remain
unchanged because they record who actually performed each stage.
No acceptance behavior, readiness verdict or test evidence changed. Workflow files
revised for D5 (role wording only): PLAN.md 66e77b89e0c235cea3fada90316c0c7580f6ea7ce2816b9e35c5bf6f523e4a68,
NOTE.md 5e8ba76fcd3a52a08925ba744dca8e4adda572403b2ac800ec58dbef2b5d62e4, HANDOFF.md
1ee65fd926dff0a710597508226cf109cb723678b760111e7d6120adfe5b5596; they supersede the earlier PLAN.md hash.

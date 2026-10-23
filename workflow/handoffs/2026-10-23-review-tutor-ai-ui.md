# Independent Review: Tutor Booking List and Note-Draft UI

Date: 23 October 2026  
Branch: `feat/tutor-ai-note-draft`  
Review scope: `GET /api/tutor/bookings`, completed-booking selection, Tutor note/draft state, linked specification, tests, and evidence.

## Independent findings

The separate Tutor UI test-design reviewer initially found:

| ID | Finding | Severity | Disposition |
| --- | --- | --- | --- |
| TUI-REV-01 | Copying a draft only prompted when the saved-note editor contained nonempty text. Clearing an existing note left a dirty empty editor, which could be overwritten without confirmation. | P2 | Fixed: confirmation is based on the editor differing from the persisted note. A frontend test verifies cancel preserves the cleared editor and sends no PUT. |
| TUI-REV-02 | The frontend had no `NOTE_CONFLICT` case proving the original note version and user buffers were preserved without an automatic reload. | P2 | Fixed: the component test verifies the original `expectedVersion`, retained rough notes/draft/editor, and exactly one initial note GET after the conflict. |
| TUI-REV-03 | The PostgreSQL list test used one module and dates that did not distinguish UTC from Singapore time. | P2 | Fixed: a second-module booking occurs at 2026-10-20 16:30 UTC (2026-10-21 in Singapore); assertions distinguish both module results and SGT date results. |
| TUI-REV-04 | The evidence register said the React UI was unverified although it also recorded passing UI tests, and did not yet record the updated route-test run. | P2 | Fixed: the register now distinguishes mocked UI verification from browser/session integration and links this follow-up evidence. |
| TUI-REV-05 | The list-component test did not assert that it requested `status=COMPLETED`; the API fixture matches by pathname. | P3 | Fixed: the first Tutor workspace test inspects the recorded request URL and asserts the exact status query. |

The reviewer’s follow-up found no new implementation defect in the reviewed areas and confirmed TUI-REV-01 through TUI-REV-03 resolved. The reviewer requested that the updated test results be recorded and the completed-status query be asserted; both are now recorded/implemented above. The reviewer did not independently rerun PostgreSQL tests.

## Verification after fixes

From `src/frontend`:

```text
npm run build
Exit 0; TypeScript check and Vite production build succeeded.

npm run test -- --reporter=dot
Exit 0; 196 tests passed across 7 files.
```

From `src/backend`:

```text
./gradlew test --tests 'unichope.tutor.TutorReadServiceTest' --console=plain
Exit 0; BUILD SUCCESSFUL.

./gradlew postgresTest --tests 'unichope.platform.core.postgres.TutorRestPgTest' \
  -Dcore.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55436/postgres \
  -Dcore.test.user=huanghao -Dcore.test.password= --console=plain
Exit 0; BUILD SUCCESSFUL; 34 TutorRestPgTest cases passed.
```

`git diff --check` passed. The PostgreSQL run used a disposable UTF-8 local
PostgreSQL cluster with trust authentication and synthetic fixtures. The first
attempt used SQL_ASCII and failed existing Unicode note cases; it was discarded,
recreated as UTF-8, and the full focused class then passed. The final cluster was
stopped and its `/private/tmp/cs3227-tutor-ui-review-pg` directory removed.

## Review boundary

The PostgreSQL MockMvc tests exercise real SQL and Tutor session authorization
with synthetic data. The UI tests use a deterministic fake API. Browser
end-to-end login/session behavior, production SoC provider/key routing, active
admission migration, live-model behavior, and affected-owner/human acceptance
remain unverified. No provider key or real consultation notes were used.

The separate reviewer reproduced the frontend build/tests and Tutor read-service
unit test. The 34-test PostgreSQL run above was reproduced by the primary agent,
not by the reviewer. The follow-up run added 1 UI test after the reviewer’s last
test execution; the reviewer inspected the fix but did not rerun that newest
assertion. No commit or push was made.

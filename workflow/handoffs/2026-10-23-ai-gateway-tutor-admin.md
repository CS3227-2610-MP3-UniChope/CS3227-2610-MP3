# Handoff: shared SoCLaaS AI gateway for Tutor and Admin

## Handoff gateway-1
Producer role / actual tool and model: Student owner (Phuc) with Claude Code (Opus 5.5) as implementer
Recipient role: Tutor owner (TUT-AI-01/02 implementer) and Admin owner (activity report, model setting UI)
Change ID / task / exact requested next action: `ai-gateway`. Tutor: wire Tutor note drafts onto the
gateway with task `TUT-NOTE-1`. Admin: review the already wired report path and the new model
setting endpoint, and add the UI for it. Both: review the shared code before it is merged.
Stage status / supersedes: implementation done, **not independently reviewed**. The owner skipped the
spec/test/review workflow for this change. Nothing is committed yet.
Goal and non-goals: one server-side gateway that sends each role's AI requests with that role's own
SoCLaaS key and enforces per-key limits. Non-goals: no Tutor AI feature, no Admin UI, no Student AI
feature, no change to Tutor or Admin business logic.
Acceptance IDs: SH-AI-01..09 (partly, see open questions), TUT-AI-01/02 (not started), Admin report AI.

### Inputs and outputs
| Artifact path + section/case | Input or output | Commit or SHA-256 | Purpose |
| --- | --- | --- | --- |
| `workflow/SHARED-TECHNICAL-SPEC.md` SH-AI-01..09 | Input | `2d01bb4` | Shared AI contract |
| `workflow/tutor/PRODUCT-SPEC.md` TUT-AI-01, TUT-AI-02 | Input | `2d01bb4` | Tutor draft requirements |
| `workflow/admin/TECHNICAL-SPEC.md` report AI section | Input | `2d01bb4` | Admin report contract |
| Files in the hash table below | Output | SHA-256 below | Gateway code, migration, tests |

Base commit / result commit / complete diff or patch reference: base `2d01bb4` on `branch_student`;
no result commit. The diff is `git diff 2d01bb4` plus the untracked files listed below.
Uncommitted files and their hashes: see [File hashes](#file-hashes).
Allowed write scope for receivers: Tutor may add Tutor wiring, Tutor exception mapping and Tutor tests.
Admin may add UI and Admin tests. Changes to `platform/ai/`, `AiConfiguration` or `V5__ai_usage.sql`
go back to the Student owner as a finding, since they affect all three roles.

## How the gateway works

```
role service ──AiGateway.Request(actorId, task, instructions, input, maxOutputTokens)──▶ RoutedAiGateway
   1. task id → key slot          TUT-NOTE-1 → TUTOR, ADM-REPORT-1 → ADMIN, STU-AGENDA-1 → STUDENT
   2. actor's stored role must equal the slot's role, and the account must be active
   3. that slot's key must be configured
   4. read the Admin-selected model and its price
   5. durable admission in PostgreSQL (limits below), reserves a worst-case cost
   6. POST {SOC_LLM_API_URL}/chat/completions with that slot's key
   7. charge the actual cost from the token usage, release the in-flight lease
```

- Steps 1–3 fail with `AiGateway.Failure(UNAVAILABLE)` and make **no** admission and no provider call.
- A key is never borrowed from another role. If the Tutor key is missing or exhausted, only Tutor AI
  stops working.
- The returned text is untrusted. The gateway strips a leading `<think>` block and nothing else.
  Callers must validate it, and generation must never save anything by itself.
- The gateway checks the role only. **It does not check booking ownership or any business rule.**
  The role service must authorize and validate input before calling `generate`, per SH-AI-04.
- Only metadata is stored (actor, task, times, reserved and charged cost). No prompt, input or output.

### Limits (per key, durable across restarts)
| Limit | Default | Override env var | Rejection |
| --- | --- | --- | --- |
| Requests per minute | 80 (SoCLaaS allows 90) | `SOC_LLM_RPM` | 429 `AI_RATE_LIMITED` |
| Rolling 24 h budget | $70 (SoCLaaS allows $80/day) | `SOC_LLM_DAILY_BUDGET` (microdollars) | 503 `AI_UNAVAILABLE` |
| Rolling 31 day budget | $700 (SoCLaaS allows $800/month) | `SOC_LLM_MONTHLY_BUDGET` (microdollars) | 503 `AI_UNAVAILABLE` |
| Per actor | 3 per rolling 5 min, failures count | none | 429 `AI_RATE_LIMITED` |
| Per actor in flight | 1, lease 60 s | none | 409 `AI_BUSY` |

Our windows are rolling, while SoCLaaS resets at UTC midnight and month start. The margin keeps us under
the SoCLaaS limits, but SoCLaaS can still return 429, which also maps to `AI_RATE_LIMITED`.

### Failures a caller can receive
| Thrown | Meaning | Suggested HTTP mapping |
| --- | --- | --- |
| `ApiFailure(409, "AI_BUSY")` | Actor already has a request in flight | 409 + `Retry-After: 60` |
| `ApiFailure(429, "AI_RATE_LIMITED")` | Per-actor, per-key or SoCLaaS rate limit | 429 + `Retry-After` |
| `ApiFailure(503, "AI_UNAVAILABLE")` | Budget used up | 503 |
| `AiGateway.Failure(UNAVAILABLE)` | No URL or key, role mismatch, unknown task, admission store down, provider error | 503 `AI_UNAVAILABLE` |
| `AiGateway.Failure(TIMEOUT)` | Provider took over 25 s, or returned 408/504 | 504 `AI_TIMEOUT` |
| `AiGateway.Failure(INVALID_OUTPUT)` | Empty, malformed or over 64 KiB reply | 502 `AI_INVALID_OUTPUT` |

`ApiFailure` does not carry a `Retry-After` value. `AdminExceptionHandler` adds one (60 s for busy, 300 s
for 429). Tutor needs to do the same in its own handler.

### Model
- An Admin selects the model for all three roles: `GET`/`PUT /api/admin/ai/model`, body exactly
  `{"model":"<id>"}`. The default is `qwen3.5:9b`. Each request reads the current setting.
- Selectable models and prices live in `AiModelCatalog`: llama3.1:8b, qwen3.5:9b, gemma4:26b,
  qwen3-coder-next, qwen3.6:35b, qwen3.8:27b, ornith1.5:35b. An unknown id gets 400 `VALIDATION_FAILED`.
- Every request sends `reasoning_effort: "none"`. Without it, the Qwen models on SoCLaaS spend the
  whole output budget on hidden reasoning and return empty content. Set `maxOutputTokens` for the
  answer only.

## Configuration
Real values never go in git. Copy `src/backend/.env.example` to `src/backend/.env` and export the values
into the backend's environment. The backend does not load `.env` by itself.

```
UNICHOPE_AI_MODE=shared            # unavailable (default) | fake (local profile only) | shared
SOC_LLM_API_URL=<SoCLaaS base URL including /v1>
SOC_LLM_API_KEY_STUDENT=clsk_...
SOC_LLM_API_KEY_TUTOR=clsk_...
SOC_LLM_API_KEY_ADMIN=clsk_...
```

Each role owner keeps their own key and shares it only through a private channel, never in a commit,
issue, log or chat transcript. Automated tests use fakes and need no key.

## For the Tutor owner
Tutor has no gateway wiring yet. The NOTE increment (TN-12, TN-22) correctly requires zero gateway
calls, and that stays true for the note endpoints. TUT-AI-01/02 needs its own spec and tests first.

1. **Get the gateway** the same way `AdminConfiguration.activityReportService` does: inject
   `ObjectProvider<AiGateway>` and pick by `unichope.ai.mode`. With `shared`, use
   `getIfAvailable()` and fail at startup if it is missing. With `unavailable`, use a gateway that
   always throws `Failure(UNAVAILABLE)`. Use `fake` only with the `local` profile.
2. **Call it after authorization**: the tutor owns the completed booking, and the rough notes are not
   blank and within limits. Then call:
   ```java
   gateway.generate(new AiGateway.Request(tutor.userId(), AiTask.TUTOR_NOTE_DRAFT.id(),
       instructions, roughNotes, maxOutputTokens));
   ```
   `actorId` must be the tutor's user id, because the gateway checks that user's stored role is TUTOR.
3. **Validate the output** (for example, parse the JSON shape and enforce length limits) and return it
   as an editable draft. Never write it to the note table. Saving stays the existing `PUT` with
   its conflict check.
4. **Map failures** in `TutorExceptionHandler` using the table above, including `Retry-After`. Manual
   note load and save must keep working while AI is unavailable (TUT-AI-02).
5. **Tests**: use a counting fake `AiGateway` for service and HTTP tests. `RoutedAiGatewayTest` shows
   the routing rules if you want to test against the real router with a fake provider.

## For the Admin owner
The activity report already uses the gateway. `ActivityReportService` sends `ADM-REPORT-1` with the
Admin's id, and `AdminConfiguration` chooses the shared gateway when `unichope.ai.mode=shared`. What
changed for Admin:

1. **New endpoint** `AdminAiModelController`: `GET /api/admin/ai/model` returns
   `{model, defaultModel, updatedAt, models[{id, contextLength, inputCostMicrosPerMillionTokens,
   outputCostMicrosPerMillionTokens, recommendedFor}]}`. `PUT` takes `{"model":"<id>"}`, requires an
   authenticated Admin, and rejects extra fields. Read the exact field names from the controller
   before building UI against it.
2. **`AdminExceptionHandler`** advice now also covers `AdminAiModelController`. No other behaviour
   changed.
3. **Two layers of timeout and busy checks** now exist. The report keeps its own 30 s bound and
   `ReportBusyFailure` (409 `AI_BUSY`), and the gateway adds 25 s and its own per-actor lease. Both
   return the same codes, so the client sees no difference. Please confirm this is acceptable.
4. **UI to add**: a model selector on an Admin page that loads the list from `GET` and saves with `PUT`.

## Verification evidence
Working directory `src/backend`. Disposable database from `compose.test.yaml` (127.0.0.1:55432).

| Command | Result | When |
| --- | --- | --- |
| `./gradlew test` and `./gradlew postgresTest -Dcore.test.jdbcUrl=...` | 71 unit, 188 PostgreSQL, 0 failures | After the model setting, **before** the `reasoning_effort` change |
| `./gradlew test --tests 'unichope.platform.ai.*'` | exit 0 | After the `reasoning_effort` change |
| Manual end-to-end against a local stub provider | Admin report used the Admin key; a model change applied to the next report; 400 for an unknown model or an extra field; 403 for a Student | Before the `reasoning_effort` change |
| Manual calls to real SoCLaaS with the **Student** key | Listed 18 models; qwen3.5:9b, llama3.1:8b, gemma4:26b and qwen3.8:27b answered with `reasoning_effort: "none"` | 23 Oct 2026 |

**Not verified:**
- The Tutor and Admin keys against SoCLaaS.
- A full backend run in `shared` mode against real SoCLaaS.
- The PostgreSQL suite after the `reasoning_effort` change.
- Any Tutor AI path, since none exists.
- Load or concurrency beyond `PostgresAiAdmissionPgTest`.

## Open questions and spec gaps
1. **Spec gap:** SH-AI-03 names a single `SOC_LLM_API_KEY`. This change uses one key per role. The
   shared spec needs updating, with the decision recorded by its owners.
2. **Spec gap:** SH-AI-05 lists the per-actor limit as a Tutor proposal. The gateway applies it to every
   role. Confirm or change it.
3. **Spec gap:** the Admin technical spec does not describe Admin model selection, which affects
   Tutor and Student cost as well. Admin and Tutor owners should agree on it.
4. The `ornith1.5:35b` price is assumed equal to the published `ornith:35b` price.
5. The Admin can pick a model that a role's key cannot access. There is no per-key model check yet.

## File hashes
SHA-256 at handoff time:

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/java/unichope/platform/ai/AiTask.java` | `edb2fa38effe32227b2791af4cd1fa95f0703ae86512a0fdde36fd627135e67b` |
| `src/backend/src/main/java/unichope/platform/ai/AiKeySlot.java` | `2dc209be10509ac780931e56cf3a613f9a6de0992fcb49bdf1a232ad8cc8ce5c` |
| `src/backend/src/main/java/unichope/platform/ai/AiKeys.java` | `7b25bb81b3818465a89a44f9b4503a4eeea8ce8a1392f82fa6fe6092db774fb5` |
| `src/backend/src/main/java/unichope/platform/ai/AiModel.java` | `c93167511b04b3631512bc5932c8c93abce4dd84024d7b2eae9d84788a31c17d` |
| `src/backend/src/main/java/unichope/platform/ai/AiModelCatalog.java` | `ed5ee8eaf8efa48d840a7f92b68bd829baab9dcbe9dcbd351d2691d8af56bcbd` |
| `src/backend/src/main/java/unichope/platform/ai/AiModelSettings.java` | `e0e42cc992ae198a5e9f9ade8101a5529cee15792c9f94874a44c60daf22c652` |
| `src/backend/src/main/java/unichope/platform/ai/PostgresAiModelSettings.java` | `af42c6268d32c9210ff72374d20d8cbc8f4fd9f153e1bfdadc970fa4d0b5e7cb` |
| `src/backend/src/main/java/unichope/platform/ai/AiProvider.java` | `98f03b79ac456e8bb798f4cdc3ff8c485be778ea536599b8d3c739eef2f3e9f3` |
| `src/backend/src/main/java/unichope/platform/ai/SocLlmProvider.java` | `abde967c7853f9c377fecef770282fd276397affa01082b09acf216a2b40017c` |
| `src/backend/src/main/java/unichope/platform/ai/AiAdmission.java` | `80c4fa693b25d904a583c500ac95dd68ecd4a64abe40ef80e27f2322b060264e` |
| `src/backend/src/main/java/unichope/platform/ai/PostgresAiAdmission.java` | `93bf595d72b2eb063220b5c151baa6fdde3b7c69a842e52cbb08043fa35e7f99` |
| `src/backend/src/main/java/unichope/platform/ai/AiQuotaPolicy.java` | `3d5b9d92c82203c6d60022d0ab60c46b5274aad94fdf6d383b8f46b8d42f0bcf` |
| `src/backend/src/main/java/unichope/platform/ai/RoutedAiGateway.java` | `f1c06f9837a23a5ce46d69ab854b5e06731c3c818d6685fb1b70080b24c41b67` |
| `src/backend/src/main/java/unichope/platform/runtime/AiConfiguration.java` | `cf35cb43173984a7e3f4bae95bda358357f6527c3436632e57927e04b68282eb` |
| `src/backend/src/main/java/unichope/admin/AdminAiModelController.java` | `dbc159c3b72ea2f9d320c0fc605ef3f22f8f27191ad1ec27331cbeeda6b5f51f` |
| `src/backend/src/main/java/unichope/admin/AdminExceptionHandler.java` | `4321f6b3caad6a493679888af23085132a217f3803c7a2957633526f2a8bd041` |
| `src/backend/src/main/resources/db/migration/V5__ai_usage.sql` | `075c9a1f2ff9aa81d97abd40f2ceba8b40746a337955af3981f6b5da4daaaf49` |
| `src/backend/src/test/java/unichope/platform/ai/PostgresAiAdmissionPgTest.java` | `4ee3241b363d42632a7712f0787578a063d7a27337c11c515ea9a35419732791` |
| `src/backend/src/test/java/unichope/platform/ai/PostgresAiModelSettingsPgTest.java` | `77f5525045c4e55b417ba5488755ca4359804317acbdb47b48cec109f5245a38` |
| `src/backend/src/test/java/unichope/platform/ai/RoutedAiGatewayTest.java` | `6d5f07bab51a6b89de441fdd042f598c7fe7a5ca9a51ea5896840d153a7e64bd` |
| `src/backend/src/test/java/unichope/platform/ai/SocLlmProviderTest.java` | `d6dceeca95243d72c77e06c69a37374080d98f2f4d59451994a7f28447657b8f` |
| `src/backend/src/test/java/unichope/platform/runtime/StudentAuthIntegrationPgTest.java` | `370a03965c8c5de5a01e01f2f524cc7b04204bb467fe314dcbdf1e6d1abe112f` |
| `src/backend/.env.example` | `d7dce4549c73213a37d7640d286704f1da79eed5425a811c8829aabe1d10a222` |
### Receiver receipt (Tutor)
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

### Receiver receipt (Admin)
Receiver role / actual tool and model:
Checked task, artifact revisions and required evidence:
Receipt: ACCEPTED | RETURNED
Reason / exact missing or conflicting artifact:

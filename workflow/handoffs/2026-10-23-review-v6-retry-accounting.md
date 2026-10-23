# Independent Core Review: V6 and Tutor Retry Accounting

**Status:** Review findings addressed in the local working tree. This record does
not imply human owner acceptance or production activation.

**Scope:** V6 compatibility with merged V5, actor/provider quota accounting,
uncertain-call leases, retry behavior, test evidence, and Tutor deadline agreement.

## Findings and resolutions

| Finding | Resolution |
| --- | --- |
| Successful-output accounting was described as universal, but only the Tutor caller passed task validation into the admitted gateway. | Scoped the guarantee to validator-aware tasks and documented that legacy one-argument calls treat a bounded provider result as accepted. Tutor passes its parser as the validator. |
| `AiAdmissionService.failed` had a default that only cleared the lease and silently left the actor quota charged. | Made `failed` mandatory so each implementation must explicitly implement failure accounting. The PostgreSQL implementation already clears the actor charge and retains provider-key usage. |
| Fresh-schema tests did not establish that existing V5 attempts remain counted when V6 is applied. | Added a migration-upgrade test that migrates to V5, inserts an attempt, applies V6, and verifies the prior attempt still blocks the actor limit. |
| Tutor spec specified a 20-second deadline while shared runtime configured 25 seconds. | Added a Tutor-specific 20-second policy; other role tasks retain the shared 25-second deadline. |

## Re-review and verification

- Core reviewer rechecked the fixes and found no regressions. The remaining
  deadline-policy test gap was then closed with `AiConfigurationTest`.

- Independent read-only Core review found no V6 schema blocker and raised the
  findings above.
- `./gradlew test`: 109 tests across 27 suites; no failures, errors, or skips.
- `./gradlew postgresTest` against disposable UTF-8 PostgreSQL 17: 246 tests
  across 21 suites; no failures, errors, or skips.
- `AiConfigurationTest` verifies the constructed Tutor task policy uses the
  specified 20-second deadline and the other roles retain 25 seconds.
- `git diff --check` passed.
- No live SoCLaaS request was made. The runtime test uses a synthetic key.

**Remaining gates:** Live provider behavior and quota accuracy, Core/role-owner
acceptance, and production activation remain unverified. No commit or push was
made for this implementation.

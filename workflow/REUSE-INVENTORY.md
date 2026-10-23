# MP2 Reuse Inventory

Status: Active inventory; actual adapted MP2 domain code and domain tests are recorded below.

MP2 reference revision: `d6a9a244f960854b2addb1546b45e506217f4823`.
Existing specifications draw on MP2's consultation setting, business rules, and
design. The Tutor product specification lists candidate code/test paths; candidates
are not evidence of code reuse.

Record actual reuse as it happens:

| Source project/revision/path | MP3 destination | Kind: code/test/idea/documentation | Unchanged/adapted/replaced | Quantity and measurement method | Changes and acknowledgement |
| --- | --- | --- | --- | --- | --- |

For source/tests, report file and line counts using a stated method, separate
unchanged from adapted material, and explain any exclusions. Do not claim an
unsupported percentage. Record non-code sources and their attribution as well.
Summarize the final inventory in the Developer Guide acknowledgements.

## 20 October 2026: backend build foundation

No MP2 application code/tests were copied in this slice. The Spring Boot entry
point, AI contract, fake and tests were newly written in MP3.

| Source project/revision/path | MP3 destination | Kind | Treatment | Quantity/method | Changes/acknowledgement |
| --- | --- | --- | --- | --- | --- |
| Gradle 9.1.0 wrapper task output | `src/backend/gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | Build scripts/binary | Generated unchanged | 2 scripts, 341 lines (`wc -l`: 248 + 93); one JAR, 45457 bytes (`len(read_bytes())`) | Upstream Gradle; scripts carry Apache-2.0 notice; wrapper properties additionally pin published distribution SHA-256 |

Framework/library dependencies are declared in `src/backend/build.gradle`; they
are dependencies rather than copied MP2 source. Wrapper JAR SHA-256:
`76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3`.
This entry does not establish MP2 reuse percentages or future role-porting counts.

## 20 October 2026: Tutor upcoming-slot read service

No MP2 application source or test lines were copied unchanged. The Tutor
application classes and tests listed below are new MP3 files. Their behavior was
adapted from the MP2 rules/scenarios identified in the architect handoff; no
SQLite, UI, session, or repository implementation was ported. Source quantities
are from the pinned MP2 revision, measured with `wc -l`; adapted line ranges are
inclusive.

| MP2 source at `d6a9a244f960854b2addb1546b45e506217f4823` | MP3 destination | Kind | Treatment | Quantity/method | Changes/acknowledgement |
| --- | --- | --- | --- | --- | --- |
| `src/main/java/tutor/TutorService.java` | `src/backend/src/main/java/unichope/tutor/TutorReadService.java` | Business logic | Adapted; no lines copied | 7 lines (`219-225`) for slot eligibility/date/order, 12 (`98-109`) for assigned active modules, 5 (`242-246`) for slot projection, and 7 (`193-199`) for account gating; original file 279 lines | MP3 uses injected actor/read/clock seams, strictly-after-now semantics, the MP3 response fields, and no SQLite/session/operation-log code. |
| `src/main/java/tutor/TutorSlotView.java` | `SlotView.java` and `ModuleOption.java` | DTO shape | Adapted; no lines copied | 9 source lines | MP3 includes `moduleId` and uses `id` per integration contract. |
| `src/main/java/model/consultation/ConsultationSlot.java`, `SlotStatus.java`, `model/module/Module.java`, `TutorModule.java` | Tutor-local `ConsultationSlot`, `SlotStatus`, `AssignedModule`, `ModuleOption` | Domain ideas | Adapted; no lines copied | 25 + 3 + 18 + 12 = 58 source lines (`wc -l`) | Replaced old packages with narrow Tutor-local projections pending real shared contracts. |
| `src/main/java/data/repository/SlotRepository.java`, `ModuleRepository.java` | `TutorReadRepository.java` | Read-boundary idea | Adapted; no lines copied | 10 + 18 = 28 source lines (`wc -l`) | No database implementation or SQL was ported. |
| `src/test/java/tutor/TutorServiceTest.java` | `src/backend/src/test/java/unichope/tutor/TutorReadServiceTest.java` | Test scenarios | Adapted; no lines copied | 64 slot-test lines (`221-284`) and 24 active-module-test lines (`610-633`); original file 650 lines | MP3 uses synthetic IDs and in-memory fakes; adds actor/role/inactive, foreign-owner, strict-equality, status, SGT-boundary, and empty-list assertions. |
| `src/test/java/tutor/TutorFixture.java` | Test-local `FakeReads` in `TutorReadServiceTest.java` | Test fixture idea | Replaced; no lines copied | 37 source lines (`wc -l`) | No SQLite, persistent actor selection, or operation logs. |

All other implementation lines in the listed MP3 files were newly written for
the MP3 service boundary and tests. There is no claim of a percentage of MP2
code reuse. The source attribution is the MP2 checkout at the revision above.

## 20 October 2026: core read-persistence increment

MP2 source revision remains `d6a9a244f960854b2addb1546b45e506217f4823`.
Core adaptation reuses the logical MP2 domain; it does not import SQLite files,
passwords, auth implementation, notes, JavaFX, or real data. Quantities below count
physical lines with Python `len(read_text().splitlines())`, including blanks and
comments, once per source file; they are not estimated changed-code percentages.

| MP2 source | MP3 destination | Kind | Treatment | Quantity/method | Changes/acknowledgement |
| --- | --- | --- | --- | --- | --- |
| `src/main/java/model/Validation.java`, `model/user/{User,Student,Tutor,Admin,Role}.java`, `model/module/{Module,TutorModule}.java`, `model/consultation/{ConsultationSlot,SlotStatus,Booking,BookingStatus}.java` | `src/backend/src/main/java/unichope/platform/core/model/` with matching subpackages | Code | Adapted copy | 12 files; 181 source lines and 181 destination lines | Only package/import prefixes changed; validation and immutable role-fixed domain behavior retained. These replace temporary Tutor role/status/slot values; Tutor keeps a `TutorSlot` projection containing the shared slot plus module code. |
| `src/test/java/model/DomainTest.java` | `src/backend/src/test/java/unichope/platform/core/model/DomainTest.java` | Tests | Adapted copy | 1 file; 42 source lines, 42 destination lines | Package/import prefixes changed; 3 MP2 behavioral tests retained. |
| `src/main/java/data/repository/{User,Module,Slot,Booking}Repository.java` | `platform/core/repository/{User,Module,Slot,Booking}Repository.java` | Contract design | Adapted; new declarations | 4 source interfaces: 13 + 18 + 10 + 22 = 63 source lines; inherited generic Repository contract also informed findById/findAll | Read methods retained; explicit read-only interfaces exclude save/delete, auth, note, and assignment mutations; case-insensitive code lookup added. No SQLite implementation copied. |
| Core CREATE TABLE design in `src/main/java/data/sqlite/SqliteDatabase.java` | `src/backend/src/main/resources/db/core-review/V1__create_core_tables.sql` | Schema design | Replaced implementation | Five logical core tables; no copied source-line count claimed | New PostgreSQL UUID/timestamptz/boolean types, declared FKs, checks, indexes and active-booking partial uniqueness. Credential decision pending; this is review-only test DDL, not an active app migration. |

`platform/core/postgres/Postgres*Repository`, Tutor PostgreSQL read adapter,
PostgreSQL acceptance tests, and SQL are new MP3 implementation. Their behavior
is based on the cited domain/repository and logical schema design. Previous Tutor
read-slice inventory entries describe that earlier state; local role/status/slot
models are now retired rather than maintained as duplicate core models.

This increment does not port or claim implementation of ConsultationLifecycle,
authentication, production migration activation, HTTP/session wiring, or React UI.

### MP2 Gradle conventions

At the user's request, backend Gradle settings also follow MP2's build conventions:
the 4-line `JavaCompile` UTF-8/`-Xlint:all` configuration and 3-line Foojay resolver
plugin block are adapted from MP2 `build.gradle`/`settings.gradle` at the revision
above (physical lines, inclusive of block braces). Java 25 and Gradle 9.1.0 were
already selected. Boot packaging and its dependency BOM replace desktop-specific
JavaFX/application/native-access configuration; PostgreSQL checks use a separate
tagged task, analogous to MP2's separate UI test task. This is configuration reuse,
not new product functionality or reuse of JavaFX tests.

## 21 October 2026: consultation lifecycle increment

MP2 reference remains `d6a9a244f960854b2addb1546b45e506217f4823`.
This increment is a new PostgreSQL implementation of adapted MP2 business rules;
it does not copy SQLite transaction plumbing or retain unrestricted callbacks.
Source quantities count entire referenced files, not lines copied or reuse percentages,
using Python `len(read_text().splitlines())` including comments/blanks.

| MP2 source | MP3 destination | Kind | Treatment | Quantity/method | Changes/acknowledgement |
| --- | --- | --- | --- | --- | --- |
| `src/main/java/data/repository/ConsultationLifecycle.java` | `src/backend/src/main/java/unichope/platform/core/ConsultationLifecycle.java` | Contract design | Adapted; new declarations | 33 source lines, one source interface | Explicit create/cancel slot methods; core-owned clock rather than caller now; no default unsupported operations or generic withExclusiveAccess callback. |
| `src/main/java/data/sqlite/SqliteConsultationLifecycle.java` | `platform/core/postgres/PostgresConsultationLifecycle.java` | Business/atomicity rules | Replaced implementation | 120 source lines, one reference file; no unchanged-line claim | Retains book/cancel/complete status-pair intent, active eligibility and half-open student overlaps. New Spring READ COMMITTED transactions, sorted PostgreSQL user/module/slot/booking locks, fresh actor/time checks, safe typed failures and bounded SQL waits replace SQLite serialization. |
| `src/main/java/tutor/TutorService.java` | Explicit slot methods in `PostgresConsultationLifecycle` | Slot business rules | Adapted ideas; no source copied | 279-line reference file; only create/cancel rules used, not whole-file reuse | Active Tutor, assigned active module, overlap/status and owner rules; strict start-after-now from MP3 spec replaces MP2's equality-permitting check. Slot creation/cancellation now guarded atomically in core. |
| `src/test/java/data/sqlite/SqliteConsultationLifecycleContractTest.java` | `PostgresLifecycleTest`, `PostgresLifecycleConcurrencyTest`, `LifecyclePgFixture` | Test scenarios | Replaced; new PostgreSQL fixtures/tests | 158-line reference file; no test-line copy claim | Retains paired completion, invalid state/owner/missing and competing booking scenarios. New independently designed 28-case matrix adds observed row-lock interleavings, eligibility/clock rechecks, second-write failure rollback, bounded contention and connection recovery. Real FKs require valid synthetic users/modules/assignments. |

`LifecycleFailure` and new null-input test are new MP3 code. Scope excludes auth,
UI, AI, notes and production migration activation. Reused shared models from the
20 October increment are not counted again. These entries attribute rules/design
without claiming the entire referenced MP2 files were ported.

## 21 October 2026: Tutor REST adapters

No additional MP2 source or test files were copied in this increment. New Tutor
HTTP parsing/controller/access/error/profile/security adapters and request tests are
MP3 implementation based on the shared REST contract and independently designed
22-case matrix. Existing TutorReadService, projections and PostgreSQL adapters were
extended narrowly for owner-scoped detail and reasoned account denial; their prior
MP2 attribution remains above and is not counted a second time. Existing core
ConsultationLifecycle supplies mutations unchanged. No JavaFX presentation, desktop
session logic, credentials or SQLite code was brought into the HTTP layer.

Spring Boot MVC/Security and matching testing starters are declared dependencies,
not copied project code. Actual pinned Boot 4.0.8/Security 7.0.7/Jackson 3.1.5 artifact
inspection and dependency-resolution evidence are recorded in the REST handoff.

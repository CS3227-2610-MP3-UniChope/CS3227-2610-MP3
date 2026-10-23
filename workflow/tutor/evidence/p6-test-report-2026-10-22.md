# P6 Parser Test Run

Date: 22 October 2026 (Asia/Singapore)
Checkout: `feat/tutor-ai-note-draft`
Base revision: `1b4ad07fe6e57be9125c8a62be114361352ddeef`

## Results

- Focused: `./gradlew test --tests unichope.tutor.TutorNoteInputTest --tests unichope.tutor.TutorNoteDraftParserTest` — 10 tests passed, 0 failures, 0 errors, 0 skipped. Raw Gradle output: [focused run](p6-parser-focused-2026-10-22.txt).
- Full backend unit suite: `./gradlew test` — 65 tests across 19 suites passed, 0 failures, 0 errors, 0 skipped. Raw Gradle output: [full run](p6-backend-full-2026-10-22.txt).

The counts were read from Gradle's generated JUnit XML reports immediately after the successful full run. These are unit tests only. No endpoint, authorization-before-gateway, PostgreSQL admission, live SoC, prompt-injection behavior, or independent-review result is established by these runs.

## Source and spec snapshot hashes

SHA-256 values identify the tested code/test files and P6 spec revision used for the run. The matrix hash identifies the current matrix revision; its status and limits were clarified after the run. No source or test changed after execution.

| File | SHA-256 |
| --- | --- |
| `src/backend/src/main/java/unichope/tutor/http/TutorApiInput.java` | `54449a9bd4e5ef505a605be885e5f656ab8b0f3274677ea78b90dfd33c995001` |
| `src/backend/src/main/java/unichope/tutor/TutorNoteDraftParser.java` | `e2d8a4770c30552341c142b2e81db4bee57796868022727d0c17ad77d6402e30` |
| `src/backend/src/test/java/unichope/tutor/TutorNoteInputTest.java` | `8c1e181c26efbd7cc659843969d79aca313f012aeac53e5098fed66b3e41039d` |
| `src/backend/src/test/java/unichope/tutor/TutorNoteDraftParserTest.java` | `89de237de7702d8658586d61e7b590ef38a397b45c32ccd7fd7726381e727d2d` |
| `workflow/tutor/TUTOR-AI-INCREMENT-SPEC.md` | `c2c81d072f4f65e2f9a5d4218387d52c157f8312d386b4bdb6475eb1fac9878b` |
| `workflow/tutor/TUTOR-AI-TEST-MATRIX.md` | `a1c73c2dbe61a2ba44e7bd38137bd5fde4b6fb8f96b67a5e713c321b69ad5824` |

The evidence-register and matrix status edits were made after these test runs; they change documentation only. Re-run tests after any source or test changes.

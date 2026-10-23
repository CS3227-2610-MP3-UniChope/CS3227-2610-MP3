# Core-only test separation — 21 October 2026

Separated 4 core repository cases from 2 Tutor integration cases without changing
their test bodies. Tutor cases remain in original checkout under tutor/; prepared
core-sharing branch contains no Tutor source/tests. No production behavior changed.
Prepared stage trimmed to 41 source/build/test/README files; no new workflow docs.
Developer clean core build: 4 unit +34 PostgreSQL cases pass. Original focused split:
all 6 PostgreSQL cases pass. Independent reviewer reproduced both results; no actionable findings. Disposable PostgreSQL stopped. No commits or pushes.

# Core/auth storage design — 21 October 2026

Prepared local AUTH-STORAGE-SPEC, AUTH-STORAGE-PLAN and independent 25-case matrix
covering CA-01–12. Architect: /root/core_architect_review; independent test designer:
/root/lifecycle_test_designer. Original HEAD 508c5bd; PR1 proposed base 1c14c34 remains
open, not merged. Scoped inputs and supplemental clarification are hashed under
workflow/core/auth-storage-*.json; hashes identify content, not recovery snapshots.

Design uses the existing users table, private credential snapshots, atomic account
creation/activation and locked login confirmation after auth-owned verification.
Selected auth fields remain unimplemented. Owner review is pending for public User
versus physical row interpretation, email/Unicode equivalence, isolated migration
route and trusted actor/transaction contract. No approval inferred from documents.

Coordinator clarified valid ADMIN provisioning target versus malformed role errors,
NOT NULL hash/no backfill on empty-schema extension, observed-role confirmation and
failure-after-write/before-commit atomicity. Independent designer corrected an
impossible Tutor deactivation-first race: existing future AVAILABLE slot already
blocks it; separate clock boundary has no concurrent/ACTIVE booking.

Verified local links, 25 cases covering all 12 requirements, unchanged 35 initial
scoped inputs and clean PR1 checkout. No runtime tests were run; cases are designed,
not passed. Only local Markdown/JSON design records changed. No app/build/test/SQL
edits, DB/network access, probes, installs, secrets, external messages, stage, commit,
push or merge. Inherited workspace-write/restricted-network controls apply; role
file limits are behavioral, not OS read isolation. No escalation used for design.

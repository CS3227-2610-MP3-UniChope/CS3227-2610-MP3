# Admin implementation plan

Status: backend tasks implemented locally, 22 October 2026. Owner Kok Seng; independent review performed.
Inputs [Product](PRODUCT-SPEC.md),[Technical](TECHNICAL-SPEC.md),
[shared integration](../INTEGRATION-SPEC.md). Main98ca801 reviewed before implementation.

| Task | Dependencies | Work and completion evidence |
| --- | --- | --- |
| ADM-P0 | none | Review U1/U2/M1/A1/B1/S1/R1; review management policy, core write/session contracts and report utility with affected owners. Record spec revision/decisions. |
| ADM-P1 | P0; shared scaffold/core; AUTH-P2 | Test then implement authenticated read-only user and module endpoints with array DTOs; wrong-role/inactive/CSRF integration verified. |
| ADM-P2 | P1; AUTH-P3 | Test then account provisioning/activation, atomic credentials and deactivation session generation; PostgreSQL eligibility races. |
| ADM-P3 | P1; core mutation support | Module create/edit/deactivate and assignments; uniqueness, affected-pair guards, rollback and concurrent Tutor/Student mutation tests. |
| ADM-P4 | P1; core booking fixtures | Booking filters and one-statement aggregate statistics; all-time denominators, empty and SGT cases. Can run independently of P2/P3. |
| ADM-P5 | P4; Student gateway/fake contract | Prompt/payload/parser and fake-backed report, denial/no-writes and failure tests. No real provider required. |
| ADM-P6 | P5; real shared provider configured | Separate small synthetic SoC evaluation for factual accuracy and resistance to malicious input, no expectation of exact model wording. Document limits, never claim fake proves SoC quality. |
| ADM-P7 | P2/P3/P6 | Independent reviewer security regression and affected-owner review; API integration with UI/deployment owners, guides/reuse/logs/reflections and human acceptance. |

Use the decisions recorded in the product and technical specifications.
Spec -> acceptance/security tests -> scoped implementation -> independent review ->
human verification. Update spec/tests/code together when approved behavior changes.
Core/AI/frontend/deployment implementation not assigned to Admin; coordinate interface
changes rather than duplicating scaffolds. Separate assigned reviews: Tutor, core
management rules and shared AI. Backend Gradle test/postgresTest/bootJar commands and source/tests now exist.
Record command+exit+revision in artifact handoffs; this refresh neither reruns tests
nor establishes earlier independent review/approval claims.


## Current results and remaining dependency

ADM-P0â€“P5 are implemented locally, with management/read/AI validation tests and
application wiring. ADM-P6 real SoC evaluation remains blocked by the shared real
provider/quota implementation. ADM-P7 backend independent review/guides/checks are
performed locally; frontend/deployment remain outside this ownership and user
acceptance is pending. Existing core account operations were reused. Admin-specific
guarded module/assignment writes were implemented in Admin without changing shared
ConsultationLifecycle. Tests include both orderings of module/assignment conflicts
with Tutor slot creation. Results are recorded in logs/kokseng.md.

## Frontend increment — 23 October 2026

The user has now assigned Admin and remaining authentication frontend work to
Kok Seng. The earlier statements leaving frontend outside this scope describe the
previous allocation. See [frontend specification](FRONTEND-SPEC.md) for the current
scope, acceptance cases and plan. Real SoC integration remains a shared-provider
dependency; frontend implementation does not resolve it.

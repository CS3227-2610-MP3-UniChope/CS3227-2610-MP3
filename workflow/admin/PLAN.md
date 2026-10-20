# Admin implementation plan

Status: all pending, draft21October2026. Owner Kok Seng; Student reviews.
Inputs [Product](PRODUCT-SPEC.md),[Technical](TECHNICAL-SPEC.md),
[shared integration](../INTEGRATION-SPEC.md). Main50c6213 read before drafting.

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
management rules and shared AI. Actual build/test commands await app scaffold; record
command+exit+revision then. No implementation/tests/approval claimed by this plan.

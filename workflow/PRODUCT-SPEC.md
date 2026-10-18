# UniChope MP3 Product Specification

Status: Draft for team review; not an implementation or release claim.
Date: 18 October 2026
Reference: MP3 assignment brief and team planning discussion.

## 1. Product Overview

### What is this product?

UniChope is a web application for managing university consultation bookings.
Students book consultations, tutors offer slots and maintain consultation notes,
and administrators manage users, modules, and tutor assignments. Each role has
a separate interface and at least one focused SoC LLM feature.

MP3 selectively adapts MP2's consultation domain and business logic. It replaces
the JavaFX interface with a web interface and adds AI features, explicit AI
security controls, online deployment, and an evidenced development process.

### For whom is this product?

| Role | User need | Ownership |
| --- | --- | --- |
| Student | Find and manage consultations and prepare questions | Teammate to confirm |
| Tutor | Manage availability, consultations, and notes | Huanghao |
| Admin | Manage consultation setup and understand activity | Teammate to confirm |

Three roles are proposed because MP2 has three roles. Team size and final role
allocation remain unconfirmed. If there are two teammates, revise this scope
before using it as an implementation baseline.

## 2. Functional Requirements (Product-Level)

| ID | Requirement | Scope status |
| --- | --- | --- |
| AUTH-01 | Authenticate users and open the workspace for their stored role; provide sign-out. | Proposed shared baseline |
| AUTH-02 | Deny access to protected features for unauthenticated or inactive accounts, including previously signed-in accounts. | Proposed MP3 policy |
| STU-01 | Students browse future available consultations, book a slot, and view or cancel their own eligible bookings. | Team review |
| STU-AI-01 | Students generate a consultation preparation agenda from their own questions and authorized context. | AI feature proposal; team review |
| TUT-01 | Tutors manage their own slots, bookings, completed consultation notes, and AI note drafts. | Detailed in [Tutor Product Spec](tutor/PRODUCT-SPEC.md) |
| ADM-01 | Admins manage accounts, modules, assignments, and view booking activity. | Team review |
| ADM-AI-01 | Admins generate an activity explanation from computed aggregate booking statistics without private consultation notes. | AI feature proposal; team review |

Student and Admin requirements need their owners' detailed acceptance criteria
before implementation. This document does not authorize assumptions about
unlisted account-management or booking behavior.

## 3. Shared Business Rules

- Slots have AVAILABLE, BOOKED, CANCELLED, or COMPLETED status. Bookings have
  ACTIVE, CANCELLED, or COMPLETED status.
- Successful booking changes an available slot to BOOKED and creates an ACTIVE
  booking together. Competing requests for one slot cannot both succeed.
- A student cannot hold overlapping ACTIVE bookings. Cancellation of their own
  future ACTIVE booking changes it to CANCELLED and releases its slot together.
- Tutor completion changes the owned ACTIVE booking and BOOKED slot to COMPLETED
  together. History remains available after cancellation or completion.
- Users cannot gain another role's permissions by changing a URL or request.
- Tutors can access only consultations belonging to their slots. Students cannot
  read tutor-private notes. Admin access to tutor-private notes is excluded from
  this draft; expanding visibility requires explicit team agreement.
- Dates, date filters, and displayed times use Asia/Singapore (SGT).

## 4. Non-Functional Requirements

| ID | Acceptance condition |
| --- | --- |
| SEC-01 | The server checks role, active status, and record ownership before protected reads, writes, or AI calls. |
| SEC-02 | AI receives only the minimum authorized context; credentials and unrelated records are excluded. Generated content is treated as untrusted. |
| SEC-03 | AI generates suggestions; it cannot independently book, cancel, complete consultations, change accounts, or save notes. |
| REL-01 | Successful changes survive an application restart. Rejected operations leave related records consistent. |
| AI-01 | LLM-backed product features use the team's SoC LLM credentials through trusted server-side code. |
| AI-02 | Per-user usage controls and a shared provider budget bound calls. Provider failures preserve ordinary booking and manual note functionality. |
| DEP-01 | Peer testers can access the released application online over HTTPS. |
| DEP-02 | Development and production use separate data, credentials, configuration, and distinct team-owned SoC keys. Automated tests do not use production resources. |
| UX-01 | Each role has a separate workspace with loading, empty, validation, success, and failure states. |

## 5. UI Description and Main Use Cases

- Student: find a consultation, book it, view/cancel own bookings, prepare an agenda.
- Tutor: publish availability, review bookings, complete a consultation, write or
  generate and review a note. See the detailed Tutor specification.
- Admin: manage consultation setup, inspect statistics, request an AI explanation.
- Shared: sign in, enter the authorized workspace, sign out. Expired sessions
  require sign-in again; users must not be shown another account's cached data.

## 6. Out of Scope

Payments, NUS SSO, live NUSMods integration, file uploads, autonomous AI actions,
general-purpose chat, and automatic consultation rescheduling are excluded.
Do not introduce them without updating the reviewed specifications.

## 7. Technical Direction and Related Artifacts

Proposed stack from the planning discussion: React, TypeScript, Vite, Bootstrap;
Java and Spring Boot REST endpoints; Spring Security sessions; PostgreSQL,
Spring JDBC, and Flyway; backend-only SoC integration; GitHub Actions deployment
to ordinary hosting, with Docker where appropriate; GitHub Pages product website.
Hosting and provider access still require verification.

Technical requirements, shared security/reliability constraints, API/data contracts,
and execution tasks belong in separate technical, integration, and plan documents.
Those documents have not yet been written. This product spec
does not establish endpoint names, schema definitions, or implementation steps.

## 8. Development and Submission Constraints

- Build the team's own SDD and specialized-agent workflow; do not adopt an existing
  SDD toolkit. Keep specs current with approved requirement changes and tests.
- Record agent responsibilities, handoffs, independent review, security evaluations,
  and human verification. An agent's unsupported success statement is not evidence.
- Keep development workflow artifacts under `workflow/`, source under `src/`, guides
  and reflections under `docs/`, and verified interaction summaries under `logs/`.
- Quantify and acknowledge reused code, tests, ideas, and documentation. Keep secrets
  and private data out of the public repository and public logs.
- Publish the required GitHub Pages product website and keep the public team
  organization's `CS3227-2610-MP3` repository's `master` branch current for submission.
- Assignment deadline: 23 October 2026, 2pm SGT. Hosting should avoid costs where
  possible; disclose incurred costs. Agentic coding tools' built-in hosting is excluded.

## 9. Review Decisions Before Implementation

1. Confirm team size, role ownership, and Student/Admin AI scope.
2. Review the proposed active-account and tutor-private-note policies together.
3. Agree shared technical and integration contracts before role implementations.
4. Confirm SoC limits and deployment connectivity; approve concrete AI budgets.

# UniChope

UniChope is a role-based consultation booking web app for Students, Tutors, and
Admins. It uses a React/TypeScript frontend, a Spring Boot Java backend, and
PostgreSQL. The browser talks to the backend over HTTP; authentication, role
checks, booking rules, persistence, and AI provider access stay server-side.

## Features

- Students can register, browse and filter available consultations, book slots,
  and manage their own bookings.
- Tutors can manage availability, review and complete bookings, keep private
  consultation notes, and request an AI note draft for review.
- Admins can manage accounts, modules, and Tutor assignments, inspect booking
  activity and statistics, and request an AI explanation of aggregate activity.
- AI output is advisory. Tutors review and explicitly save note drafts; generated
  text does not change bookings or saved notes by itself.

AI uses server-side provider configuration and shared admission controls. Fake
provider mode is for local tests. Live SoCLaaS behavior and production AI
configuration must be verified separately; see the evidence register below.

## Run locally

See the [Developer Guide](docs/DeveloperGuide.md) and [User Guide](docs/UserGuide.md)
for setup and peer-testing instructions. Backend sources and tests are in
`src/backend`; the React application is in `src/frontend`.

## Project and verification documents

- [Product specifications](workflow/PRODUCT-SPEC.md) and [integration contract](workflow/INTEGRATION-SPEC.md)
- [Developer Guide](docs/DeveloperGuide.md), [User Guide](docs/UserGuide.md), and [Reflections](docs/Reflections.md)
- [MP2 reuse inventory](workflow/REUSE-INVENTORY.md)
- [AI evidence](workflow/ai/EVIDENCE.md) and [Tutor AI evidence](workflow/tutor/TUTOR-AI-EVIDENCE.md)
- [Agent workflow](workflow/agents/README.md), [agent evaluation](workflow/evaluations/agent-security/RESULTS.md), and [handoffs](workflow/handoffs/README.md)
- [Development interaction summaries](logs/README.md)
- [Product website source](docs/index.html)

The team-owned GitHub Actions workflows run backend and frontend checks. The
product website is published separately through GitHub Pages; hosting and live
application availability depend on the team's configured environments.

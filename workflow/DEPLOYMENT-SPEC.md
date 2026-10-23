# Render deployment specification

Baseline: main dbe3bfc, 23 October 2026. Scope: package the existing React and
Spring Boot application as one container without changing role/API contracts.

| ID | Acceptance |
| --- | --- |
| DEP-01 | Multi-stage Docker build uses Node24 and Java25; frontend output is packaged under Spring Boot static resources. Final image runs Java only as a non-root user. |
| DEP-02 | app-api permits GET/HEAD for the frontend and its public assets. Existing API authorization, CSRF checks and closed default profile remain unchanged. |
| DEP-03 | Current frontend deep links forward to index.html. API paths never receive SPA HTML, and missing assets are not replaced with index.html. |
| DEP-04 | Image listens on 0.0.0.0 and Render PORT (default10000), using app-api and secure production cookies; local profiles and keys are not baked into the image. |
| DEP-05 | Docker context excludes environment files, credentials, caches, local databases, editor settings, logs, test evidence and dependency/build directories. |

Implementation: root Dockerfile and .dockerignore; explicit frontend controller
and narrow public read matchers in existing web security. The application starts
against externally configured PostgreSQL and runs existing Flyway migrations.
Reuse the existing offline bootstrapAdmin task for initial Admin provisioning.
No public setup endpoint, automatic sample seeding or embedded database is added.

Verification: Java web/security regressions, frontend build, inspect executable JAR
for bundled assets, and real HTTP checks for frontend/asset/deep-link requests and
protected API errors. Build the Docker image if a local Docker engine is available;
otherwise record that limitation and check the build stages locally. Independent
review covers frontend/API separation, container secrets and production settings.

## Verification results

- Frontend TypeScript check and Vite production build passed.
- Five new servlet/security tests passed: public UI GET/HEAD, assets, closed default
  profile, API JSON errors, CSRF, and stale sessions.
- Six existing PostgreSQL-backed application integration tests passed, covering
  role gates, login/logout, CSRF, provisioning and deactivation/session revocation.
- bootJar passed. Inspected its ZIP entries for index.html, theme.js and hashed
  JavaScript/CSS assets under BOOT-INF/classes/static.
- Real packaged-JAR HTTP checks: login, Admin/Student deep links and agenda route
  returned200HTML; bundled assets returned200; unauthenticated API requests stayed
  JSON401 and missing assets did not receive SPA HTML.
- Independent review found no blocking issues. Docker image construction was not
  run: the local Docker engine remained unavailable. Container CI is added to
  build the complete image without secrets once pushed.

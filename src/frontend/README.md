# Frontend

The Tutor P4 view uses React, TypeScript, Vite, Bootstrap, Vitest, and Testing
Library. From this directory, run `npm ci`, `npm test`, and `npm run build`. Use
`npm run dev` to start Vite; `/api` requests proxy to the Spring Boot server on
`localhost:8080`.

The view expects the Tutor API contracts in the [integration specification](../../workflow/INTEGRATION-SPEC.md).
It does not implement login. In particular, completion expects the shared
`/api/auth/csrf` endpoint to be provided by the authentication increment. Never
put SoC keys or backend/database credentials in frontend code or Vite variables.

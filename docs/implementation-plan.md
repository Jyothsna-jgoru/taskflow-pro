# TaskFlow Pro implementation plan

## Delivery principles

- Keep every runtime dependency open source and usable locally for $0.
- Treat workspace membership as the authorization boundary; roles are workspace-scoped.
- Use short-lived access JWTs without persisted refresh tokens. Logout is stateless and removes the client token.
- Persist schema changes with Flyway and expose only DTOs from the API.
- Use Redis cache-aside reads with bounded TTLs and mutation-driven invalidation.
- Reject stale task updates with an optimistic `version` field.
- Validate the same commands used by CI before publishing.

## Phases

1. Create the Maven/Spring Boot backend, PostgreSQL schema, JWT security, workspace RBAC, and consistent API errors.
2. Implement workspace, project, task, comment, activity, dashboard, filtering, pagination, and caching APIs.
3. Add unit, MVC, and Testcontainers integration coverage.
4. Build the React/TypeScript application with authenticated routing, query caching, forms, charts, boards, settings, and responsive states.
5. Add Dockerfiles, Compose health checks, CI, local seed data, API examples, architecture notes, and performance tooling.
6. Run tests/builds, start the full stack, capture real UI screenshots, run a controlled load test, and record only measured results.
7. Review security and source quality, commit with conventional messages, create the public GitHub repository, and push.

## Acceptance checks

- `./mvnw test`
- `npm run lint && npm run test && npm run build`
- `docker compose config`
- `docker compose up --build` reaches healthy state
- Authenticated API smoke flow and cross-workspace denial
- Controlled local load test with raw output retained in `docs/performance-results.json`


# Contributing to TaskFlow Pro

Thank you for improving TaskFlow Pro. Keep changes focused, tested, and free of credentials or personal data.

## Local workflow

1. Fork the repository and create a branch such as `feat/bulk-task-update`.
2. Copy `.env.example` to `.env` and change the local-only secrets.
3. Start dependencies with `docker compose up postgres redis -d`.
4. Run the backend with `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.
5. Run the frontend from `frontend/` with `npm ci && npm run dev`.
6. Before opening a pull request, run the checks below.

```bash
./mvnw test
cd frontend
npm run lint
npm run test
npm run build
```

Set `RUN_INTEGRATION_TESTS=true` before the Maven command to run the PostgreSQL and Redis Testcontainers suite. Docker must be available.

## Standards

- Use conventional commits such as `feat(tasks): add due-date filtering` or `fix(auth): reject expired tokens`.
- Format backend changes with `./mvnw spotless:apply`; CI verifies formatting during `verify`.
- Keep controllers thin, authorization in service boundaries, and API contracts in DTOs.
- Add a Flyway migration for every schema change. Never rewrite an applied migration.
- Add tests for behavior changes and authorization boundaries.
- Do not commit `.env`, tokens, database dumps, generated build output, or fabricated metrics.
- Run `npm run format` before committing and verify it with `npm run format:check`.

Pull requests should explain the user impact, test evidence, schema or security implications, and any follow-up work.

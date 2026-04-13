# Repository Guidelines

## Project Structure & Module Organization
- `backend/`: Spring Boot service. Main code in `backend/src/main/java`, tests in `backend/src/test/java`, resources in `backend/src/main/resources` (profile configs in `application-*.yml`, Flyway scripts in `db/migration`, Elasticsearch config in `elasticsearch`, generated REST Docs in `static/docs`).
- `frontend/`: React + TypeScript app. Source in `frontend/src`, static assets in `frontend/public`, build configs in `webpack*.js`.
- `docs/`: project-level documentation and references.

## Build, Test, and Development Commands
- Backend (from `backend/`):
  - `./gradlew bootRun` runs the API locally (Java 21 toolchain).
  - `./gradlew test` runs JUnit 5/RestAssured/Testcontainers and produces REST Docs snippets.
  - `./gradlew build` builds the jar and Asciidoctor docs.
  - `docker compose -f compose.dev.yaml up` brings up local infra if needed.
- Frontend (from `frontend/`):
  - `pnpm install` installs dependencies.
  - `pnpm dev` starts the webpack dev server.
  - `pnpm build` produces a production bundle.

## Coding Style & Naming Conventions
- Frontend formatting/linting uses Biome: 2-space indentation and double quotes. Run `pnpm biome check .` from `frontend/`.
- Flyway migrations live in `backend/src/main/resources/db/migration` and follow `V##_YYYYMMDD-HH-MM.sql` naming.
- Keep changes scoped to `backend/` or `frontend/` and match existing file organization.

## Testing Guidelines
- Backend tests live under `backend/src/test/java` and use JUnit 5; name tests with `*Test`.
- Frontend currently has no configured test runner (`pnpm test` exits with an error). Add scripts/tests when introducing FE tests.

## Commit & Pull Request Guidelines
- Commit messages follow `type(scope): summary`, e.g. `feat(FE): 이미지 최적화`, `fix(BE): ...`, `chore(COMMON): ...`, optionally with `(#issue)`.
- PRs should include a brief summary, linked issue/ticket, and screenshots for UI changes. Note any API or migration changes explicitly.

## Configuration & Safety
- Keep secrets out of `application-*.yml`; use environment variables for credentials.
- If modifying migrations, update related docs or seed data as needed.

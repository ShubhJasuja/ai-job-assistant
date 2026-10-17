# AI Job & Internship Application Assistant

Multi-user platform for job/internship management with deterministic matching, AI-powered analysis, resume tailoring, and application tracking with audit events.

## Problem Statement

Candidates manually track applications against unstructured descriptions; matching is subjective. This system provides deterministic scoring (skills Jaccard / experience range / location alias / education / remote / role keywords), AI job analysis, resume tailoring against a target job, and structured application-status tracking with audit trails.

## Verified Features

- **Personalized Job Matching** (Phase 4.1 — `4f262cd`): deterministic weight-based engine (skills 40%, experience 20%, location 15%, education 10%, remote 10%, role keywords 5%). Duplicate prevention via `source + sourceJobId`. Public `GET /api/v1/jobs` only.
- **AI-Powered Job Analysis** (Phase 4.2 — `233e8bc`): `AIProvider` interface; `NoOpAIProvider` default (disabled); `OpenAIProvider` via `RestTemplate` with Bearer auth from env `AI_API_KEY`. Structured JSON output with parse fallback; `sanitize()` defense; disabled by default (`ai.enabled=false`). Endpoint: `POST /api/v1/ai/analyze/{jobId}`.
- **AI-Powered Resume Tailoring** (Phase 4.3 — `394e5e0`): `POST /api/v1/ai/tailor/{resumeId}/{jobId}`. Uses existing `Resume` entity (`content` field — actual resume text, not metadata only). Current-user isolation via `SecurityContext`; AI disabled default; structured JSON + sanitize.
- **Application Tracking Status Automation** (Phase 4.4 — `bba1721`): `PUT /api/v1/applications/{appId}/status`. Explicit transition rules enforced in `ApplicationService` (see rules below); audit events (`ApplicationEvent`) recorded. User ownership enforced (`app.getUser().getId()` vs `SecurityContext`).

## Technology Stack

- Java 21 / Spring Boot 3.3.4
- Spring Data JPA (Hibernate 6.5) / Flyway / PostgreSQL 17
- Spring Data Redis / Spring Security / JWT (`jjwt` 0.12.6) / Lombok
- Maven 3 / Docker Compose / nginx
- OpenAPI (springdoc-openapi-starter-webmvc-ui 2.6.0)

## Repository Structure

```
src/main/java/com/jobassistant/
  controller/     AuthController, JobController, MatchController, AIAnalysisController,
                  ResumeTailoringController, ApplicationController, ProfileController
  service/        JobService, MatchService, AIAnalysisService, ResumeTailoringService,
                  ApplicationService, CurrentUserService, MatchingEngine
  entity/         Job, Resume, User, Application, CandidateProfile, Education, Experience,
                  Project, Skill, Preferences, ApplicationEvent
  enums/          ApplicationStatus
  ai/             AIProvider, OpenAIProvider, NoOpAIProvider, JobAnalysisResult,
                  ResumeTailoringResult, CoverLetterResult
  repository/     JobRepository, ResumeRepository, ApplicationRepository, etc.
  dto/            UpdateStatusRequest
```

## Prerequisites

- Java 21
- PostgreSQL (or Docker Compose)
- Redis (optional; default localhost)
- Maven 3
- PowerShell / bash for commands

## Setup Instructions

Copy environment template (no secrets exposed):

```powershell
cp .env.example .env
# or on Windows PowerShell:
Copy-Item .env.example .env
```

Edit `.env` to set placeholders (defaults already present in `application.yml`):

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jobassistant_db
DB_USER=jobadmin
DB_PASSWORD=<set-your-password>
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=change-me-at-least-32-characters-long
# AI optional and disabled by default:
AI_ENABLED=false
AI_API_KEY=
```

Run with Maven (tests use embedded DB / test profile):

```powershell
mvn clean test
```

Docker Compose (env-driven, no hardcoded credentials):

```powershell
docker-compose up -d
```

Run application:

```powershell
mvn spring-boot:run
# or after packaging:
mvn clean package
java -jar target/ai-job-assistant-0.0.1-SNAPSHOT.jar
```

## API Endpoints

Public (no auth):
- `GET /api/v1/jobs` — list jobs; `GET /api/v1/jobs/{id}` — single job; `GET /api/v1/jobs/duplicate-check`

Auth required (JWT via `Authorization: Bearer <token>`):
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/applications` — create (user derived from SecurityContext; never from request body)
- `PUT /api/v1/applications/{appId}/status` — update with transition rules (Phase 4.4)
- `GET /api/v1/applications/{appId}/events` — audit trail
- `GET /api/v1/applications/stats`
- `GET /api/v1/match` / `POST /api/v1/match/recalc` / `POST /api/v1/match/recalc/me`
- `POST /api/v1/ai/analyze/{jobId}` — AI job analysis (disabled by default)
- `POST /api/v1/ai/tailor/{resumeId}/{jobId}` — resume tailoring (disabled by default)
- `GET /api/v1/profiles` / `PUT /api/v1/profiles`

## Status Transition Rules (Phase 4.4 — verified in service layer)

Enforced by `ApplicationService.isValidTransition()`:

- DISCOVERED → SHORTLISTED / WITHDRAWN
- SHORTLISTED → REVIEWING / WITHDRAWN
- REVIEWING → READY_TO_APPLY / WITHDRAWN
- READY_TO_APPLY → APPLIED / WITHDRAWN
- APPLIED → OA / INTERVIEW / REJECTED / WITHDRAWN
- OA → INTERVIEW / REJECTED / WITHDRAWN
- INTERVIEW → OFFER / REJECTED / WITHDRAWN
- OFFER → WITHDRAWN (terminal exit only)
- REJECTED / WITHDRAWN → terminal (no outbound transitions)

Rejection from any active stage is supported (e.g., APPLIED → REJECTED). Withdrawal allowed from any non-terminal state.

## Authentication

Stateless JWT (`jjwt` 0.12.6); `SecurityConfig` allows CORS with credentials; `HttpMethod` import fixed; `SecurityContext` holds principal (email string for JWT). User derived exclusively from `SecurityContext` — never from request parameters (`userId` never accepted by controllers).

## AI Configuration

Disabled by default (`ai.enabled=false`; `AIProvider` is `@ConditionalOnProperty`). To enable with real OpenAI:

```bash
AI_ENABLED=true
AI_API_KEY=<your-key-only-if-enabled>
```

Live OpenAI integration with real API key has not yet been verified in this environment. `OpenAIProvider` uses `RestTemplate` with `SimpleClientHttpRequestFactory` (connect 5000ms / read 15000ms), structured JSON output, and `sanitize()` for untrusted input defense. API key passed via `headers.setBearerAuth()` and never logged.

## Tests

Verified: `mvn clean test` → **31 tests pass**, 0 failures, `BUILD SUCCESS` (last run 2026-10-17).

Mocked tests for Phase 4.2 (`AIAnalysisTest`) and Phase 4.3 (`ResumeTailoringTest`) and Phase 4.4 (`ApplicationStatusTransitionTest`) use `@MockBean`; no external API calls.

## Security Considerations

- User isolation via `SecurityContext`; no client `userId` params accepted.
- `sanitize()` filters embedded instruction strings; structured JSON with parse fallback; not guaranteed injection-proof (defense-in-depth only).
- No hardcoded secrets in repository (`docker-compose.yml` uses env vars; `.env.example` template provided).
- Public `GET /api/v1/jobs` only; all mutating endpoints require auth.
- `ApplicationService` enforces ownership (`app.getUser().getId()` matching `currentUserId`); missing / wrong-owner / inactive resources return `RuntimeException` (existing error convention).

## Known Limitations

- Live OpenAI API verification with real `AI_API_KEY` not performed.
- Job entity has no `user/owner` field (only `isActive`); user isolation not possible for jobs.
- `ApplicationStatusTransitionTest` uses generic assertions (full transition matrix already enforced by service layer; mock tests cover valid/invalid/rejection paths).
- Resume uses `content` string for tailoring (no invented fields); no external scraping / cover-letter generation implemented.
- Frontend `frontend/package.json` exists but minimal integration; not part of completed phases.
- `MiniRedis-Sandbox` is preserved untouched.

## Completed Phases

| Phase | Commit | Scope |
|---|---|---|
| 4.1 | `4f262cd` | Personalized Job Matching |
| 4.2 | `233e8bc` | AI-Powered Job Analysis |
| 4.3 | `394e5e0` | AI-Powered Resume Tailoring |
| 4.4 | `bba1721` | Application Tracking Status Automation |

## Future Roadmap (not completed)

- Live OpenAI integration verification (optional, separate task)
- Interview preparation features
- Frontend integration (React / static build)
- Phase 4.4 mocked test expansion (explicit missing-app / withdrawal paths if desired)
- Coverage for all transition matrix paths via mocked tests (current service layer already enforces rules)

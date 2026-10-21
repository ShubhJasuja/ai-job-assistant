# Pathwise / AI Job Assistant — Claude Code Handover

## Product and engineering role

Pathwise helps candidates research and compare jobs, prepare applications, and track their search. It is a human-directed assistant, **not** an apply-everywhere bot. Work as a senior software engineer: inspect the existing implementation before changing it, verify claims against code and tests, identify risks and gaps, make small complete changes, and report what was and was not verified.

## Stack and repository map

- Backend: Java 21, Spring Boot (current Maven build: 3.5.x), Spring MVC/Security, JPA, PostgreSQL, Flyway, Redis; root `pom.xml`.
- Frontend: React 18, TypeScript, Vite; `frontend/src/`. Main screen: `App.tsx`; API clients: `frontend/src/services/`; design: `frontend/src/styles.css`.
- Backend packages: `src/main/java/com/jobassistant/` (`controller`, `service`, `entity`, `repository`, `dto`, `ai`, `security`).
- Schema changes: add a new sequential Flyway migration under `src/main/resources/db/migration/`. Never rewrite a migration that may have been applied.
- Local ports used by this workspace: frontend 5173, backend 8080, PostgreSQL 5433, Redis 6380. Confirm local configuration before assuming these values.

## Verified implementation snapshot (2026-10-21)

### Job discovery / MVP 1 — working foundation

- Public job listing and filtering through `GET /api/v1/jobs`; currently configured Greenhouse boards: Stripe, Anthropic, Datadog, Cloudflare, MongoDB.
- Greenhouse ingestion, opt-in Lever/RSS connectors, scheduled and authenticated manual refresh, normalized fields, update-in-place source deduplication, refresh summaries, and source update timestamps.
- Catalog had 2,678 listings at the last live check; this count can change as feeds refresh.
- Deterministic match scoring and match explanations exist in the backend. Discovery has per-user interested / not-interested / applied states.
- Search covers roles, companies, locations, descriptions and skills; developer/engineer terms are related. Keyword and location autocomplete use `GET /api/v1/jobs/suggestions`.
- Bounded search APIs `GET /api/v1/jobs/search` and authenticated `GET /api/v1/discovery/page` return `items`, `totalElements` and `hasNext` (`offset` + `limit`, max 100). The legacy array endpoints remain available. Job skills use Hibernate batch fetching to avoid an N+1 query during searches.
- Frontend includes job cards, filters, keyboard-accessible role/technology/location suggestions, match explanation, shortlist, add-job flow, refresh action for signed-in users, and an application list. Search fetches 24 jobs per page; mobile navigation moves to a bottom tab bar.
- Not yet complete: persisted saved searches/alerts, rich ranking controls, comprehensive source health/quality UI, job freshness moderation, or coverage of many providers. Current provider set is intentionally small; do not invent unverified board identifiers.

### Other MVPs — partial foundations, not complete product flows

- **MVP 2, resume tailoring:** backend AI analysis/tailoring abstractions and endpoints exist; AI defaults to the safe `NoOpAIProvider`. Frontend review/version history, side-by-side edits, accept/reject, job-specific saved versions, and export need implementation/verification.
- **MVP 3, application workflow:** application creation, status transitions, user ownership checks, event/audit data and statistics exist. Frontend dashboard/notes, follow-up dates, reminders/calendar, recruiter history, tags and full audit timeline remain incomplete or need verification.
- **MVP 4, assisted applications:** not a complete feature. Any future browser assistance must preserve site restrictions, URL validation, audit trails, explicit per-action user approval, and final human approval before submission. Never bypass these safeguards or claim universal support.
- **MVP 5, alerts:** not implemented as a production notification system. Saved-search alerts, email/webhooks, match-change notifications and reminder delivery remain.
- Interview-preparation and cover-letter backend areas also exist; verify their tests, persistence, approval UX and integration before representing them as production-ready.

## Security and behavior invariants

- Derive the current user from Spring Security's `SecurityContext`; never trust a client-supplied user ID. Keep private profiles, resumes, drafts and applications user-scoped.
- Public job reads are intentional; mutations and personal data operations require authentication.
- Keep AI disabled by default; do not enable paid providers or use real credentials without explicit user configuration.
- Never submit an application without explicit user review and approval.
- `.env`, secrets, database dumps, `node_modules`, and generated build output must not be committed. Do not print or copy local secret values into reports/context.
- Preserve existing data. Do not reset the database, delete volumes, or alter applied migrations.

## Setup and verification

- Copy `.env.example` to `.env`, provide local config, then use `docker compose up -d` for dependencies. Do not expose `.env` values.
- Backend: `mvn spring-boot:run`; tests: `mvn test` (run the focused tests first, then the full suite when appropriate).
- Frontend: `cd frontend`, `npm ci`, `npm run dev`; production/type build: `npm run build`.
- Frontend API default: `http://localhost:8080`; override with `VITE_API_BASE_URL`.
- Latest full verification: `mvn clean test` passed (77 tests, 0 failures/errors); `frontend` `npm run build` passed. Live local checks returned 24 of 2,678 jobs and Java suggestions; after ORM batching, first-letter suggestions and keyword search each completed in about 2 seconds. Earlier target artifacts compiled for Java 25 caused incremental Java 21 test failures; a clean Java 21 build resolved that stale-output issue.
- Live checks previously confirmed backend health and 2,678 jobs; the app process may have stopped since, so check health and ports before claiming it is running.

## Production-readiness work still required

Treat this as an audit and prioritized roadmap, not a claim that every item is definitely a defect. Inspect code/config and report evidence, severity, file/line and a concrete recommendation before changing high-risk behavior.

1. **Release and operations:** establish reproducible clean builds; full backend/frontend test gates; CI; supported deployment environments; safe DB migration/backup/restore; health/readiness checks; structured logs, metrics, tracing, alerting, source-feed failure visibility and recovery runbooks.
2. **Security/privacy:** audit auth, authorization and tenant isolation across every endpoint; validate CORS/CSRF/JWT expiry and secret rotation; rate-limit auth and expensive operations; validate/sanitize inputs and external job descriptions; prevent XSS/SSRF; secure file upload/storage and retention; review dependency/container CVEs and privacy/consent.
3. **Job data quality:** validate provider terms and source access; robust timeouts/retries/backoff/rate limits; move filtering/pagination into database queries (current bounded endpoints still filter in memory); malformed/duplicate/stale/closed listings; location/remote normalization; observability and source-specific health; indexes and bounded query work.
4. **MVP completion:** implement user-reviewed resume versioning/comparison/export, application notes/reminders/history, persisted saved searches and notifications; make assist-apply explicitly approval-gated and adapter-limited; document unsupported sites. Do not automate submissions in this handover task.
5. **Frontend quality:** finish user journeys for profile/resume/AI review/application details; accessible responsive navigation/forms/modals; loading/empty/error states; validate API failures and auth expiry; protect unsaved work; add component/interaction/E2E tests and realistic browser checks.
6. **Release assurance:** define acceptance criteria, supported browser/runtime matrix, accessibility target, privacy policy/data deletion behavior, threat model, operational ownership, and honest product limitations. Do not label the product production-ready until these are evidenced.

## Claude Code working agreement

First inspect the full repository (including Git status/history, manifests, controllers/services/entities/migrations, tests and frontend) without exposing secrets. Produce an evidence-based architecture/feature inventory, prioritized findings, and a gap-to-production roadmap. Then implement in small reviewable batches, beginning with correctness/security blockers and the user-facing frontend; add tests, run relevant builds/tests, and summarize residual risks. Preserve unrelated worktree changes, existing data and human-approval safeguards. Do not reset/clean the database, rewrite applied migrations, enable paid AI, commit, or push unless explicitly asked.

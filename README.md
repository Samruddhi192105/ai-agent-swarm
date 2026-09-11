# AI Agent Swarm — Autonomous Software Engineer

A multi-agent system that takes a one-line project requirement and autonomously
plans, codes, tests, and reviews a working software project — iterating up to
3 times before handing back a downloadable ZIP.

```
User prompt → Next.js Dashboard → Spring Boot API → Planner → Coder
  → Docker Sandbox → Tester → Reviewer → PASS → ZIP
                                   ↓ RETRY (max 3)
                                 Coder
```

## Stack
- **Backend**: Spring Boot 3.3 (Java 21), PostgreSQL, Docker Java SDK
- **Frontend**: Next.js 14
- **LLM**: Gemini (via `LLMProvider` abstraction — swap in Groq/others later)
- **Sandbox**: every generated project builds & tests inside a disposable,
  network-isolated, resource-limited container — never on the host

## Run it locally

### 1. Prereqs
Java 21, Maven, Node 20+, Docker Desktop, PostgreSQL (or just use docker-compose for it).

### 2. Set your Gemini key
```bash
export GEMINI_API_KEY=your_key_here
```

### 3. Everything at once
```bash
docker compose up --build
```
- Frontend: http://localhost:3000
- Backend:  http://localhost:8080

### 4. Or run pieces individually (useful while developing)
```bash
# Postgres only
docker compose up postgres

# Backend
cd backend
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

## API
| Method | Path | Description |
|---|---|---|
| POST | `/api/projects` | `{ "prompt": "..." }` → runs the full Planner→Coder→Tester→Reviewer loop, returns the final `Project` |
| GET | `/api/projects/{id}` | Fetch a project's current state |
| GET | `/api/projects/{id}/download` | Download the generated project as a ZIP |

## What's real vs. what's a starting point
- **Real, wired end-to-end**: the orchestration loop, the Planner/Coder/Reviewer
  JSON contracts, the Docker sandbox execution (build + `mvn test`, resource
  limits, network isolation, forced cleanup), persistence of every agent run,
  ZIP packaging, CORS, CI.
- **Worth hardening before a demo**: the `POST /api/projects` call is currently
  synchronous, so the dashboard blocks until PASS/FAILED instead of showing
  live per-agent progress — swap it for `@Async` + polling or a
  WebSocket/SSE channel (Phase 12 in the original plan) if you want the
  animated real-time view. The Tester currently only targets Maven/Spring
  Boot generated projects; extend `TesterAgent`/sandbox image selection if
  Planner should be able to target other stacks.

## Branching model
```
main
 └── develop
      ├── feature/project-setup
      ├── feature/backend-foundation
      ├── feature/gemini-integration
      ├── feature/planner-agent
      ├── feature/coder-agent
      ├── feature/docker-sandbox
      ├── feature/tester-agent
      ├── feature/reviewer-agent
      ├── feature/retry-loop
      ├── feature/frontend-dashboard
      ├── feature/ci-cd
      └── feature/deployment
```

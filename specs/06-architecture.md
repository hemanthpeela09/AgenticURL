# Captured Text from Screenshots

```md
06-architecture.md > Spec 06 — Architecture & Tech Stack

# Spec 06 — Architecture & Tech Stack

## 1. Component model

```
Spring Boot application

+-----------------------------+
| product: url-shortener      |   | orchestrator (agentic) |
|-----------------------------|   |-------------------------|
| ShortenController           |   | Orchestrator (DAG engine) |
| RedirectController          |   | Node / Gate / Blackboard  |
| StatsController             |   | PolicyEngine (guardrails) |
| LinkService                 |   | Metrics / Lineage         |
| LinkStore (in-mem/H2)       |   | Agents (roles) + LLM seam |
| RateLimiter, ShortCode      |   | ScenarioRunner (S1/S2/S3) |
+-----------------------------+   +-------------------------+

no dependency between the two modules
OrchestratorController (exposes runs)
```

**Strict module separation** (NFR-2): `product` knows nothing about the orchestrator; `orchestrator` knows nothing about URL shortening — it treats “build a shortener” as a *workload*. A thin `OrchestratorController` exposes runs.

## 2. Java package layout

```md
com.example.shortener        # product
├─ api        (controllers, DTOs mirroring 02-openapi.yaml)
├─ domain     (Link, Click, ShortCode)
├─ service    (LinkService, RateLimiter)
└─ store      (LinkStore interface, InMemoryLinkStore)

com.example.orchestrator     # differentiator
├─ core       (Orchestrator, Node, Gate, Blackboard, Artifact, NodeStatus, Autonomy)
├─ policy     (PolicyEngine, Guardrail, PolicyResult)
├─ metrics    (Metrics)
├─ agents     (Analyst, Architect, Implementer, Tester, DocWriter, Governor)
├─ llm        (LLM interface, MockLLM, OpenAILLM)
└─ scenario   (SdlcGraph, ScenarioRunner, Greenfield/Brownfield/Ambiguous)

com.example.app              # Spring Boot entrypoint + OrchestratorController + demo CLI
```

## 2b. React front-end layout

```md
ui/
├─ package.json              # React (JS) single-page app
├─ vite.config.js            (Vite + React)
└─ src/                      (dev proxy -> http://localhost:8080)
   ├─ App.jsx                (tabs: Shortener | Orchestrator)
   ├─ api.js                 (fetch wrappers for the REST endpoints)
   └─ components/
      ├─ ShortenForm.jsx     (create link, show shortUrl + stats)
      ├─ StatsPanel.jsx      (clicks-by-day, referrers)
      ├─ ScenarioRunner.jsx  (pick greenfield/brownfield/ambiguous, run)
      ├─ DagView.jsx         (node statuses, gates, approvals, live)
      └─ MetricsPanel.jsx    (success rate, retries, rollbacks, MTTR, latency)
```

## 3. Tech stack & rationale (speed + defensibility)

| Concern | Choice | Why |
|---|---|---|
| Language/runtime | Java 17+ | LTS; records/sealed types make typed artifacts + gates clean. |
| Web | Spring Boot 3 (spring-boot-starter-web) | Fast REST, DI, testing support. |
| API docs | springdoc-openapi | Serves Swagger UI; validated against `02-openapi.yaml`. |
| Orchestration | Hand-rolled DAG engine | Transparent, fully controls gates/retry/rollback/replan/safe-stop; easier to explain and defend than bending a workflow framework. Trade-off vs. Spring State Machine noted below. |
| Graph utils | topological sort + cycle check (self-contained; JGraphT optional) | No heavy dependency needed. |
| Persistence | In-memory default; LinkStore interface allows H2/Postgres later | Durable option documented. |
| Tests | JUnit + Spring MockMvc + AssertJ | Unit/contract/integration layering (Spec 05). |
| Logging/observability | SLF4J + Logback JSON + metrics | Run lineage, audit-grade traceability (OR-7). |
| LLM | pluggable LLM seam; Mock default | Runs without API keys; optional real-LLM opt-in via env. |
| Front-end | React (JS) + Vite | SPA to drive the shortener and visualize the DAG/gates/metrics live. |
| Build | Gradle + Gradle wrapper (`./gradlew`) | Builds/tests with only a JDK; Node/Vite Gradle task bundles the React bundle into the jar. |
| Run/demo | Spring Boot app + ScenarioRunner CLI profile + React UI + optional Docker | Runnable end-to-end deliverable. |

**Rejected alternative:** Spring State Machine / a BPM engine — capable, but the rubric’s exact features (entry/exit gates, decision lineage, bounded rollback, dynamic replan, reliability metrics) are cheaper and clearer to implement and explain in a few hundred lines of owned code than to map onto a framework’s model.

## 4. Configuration (env)

| Var | Default | Meaning |
|---|---|---|
| SERVER_PORT | 8080 | HTTP port |
| BASE_URL | http://localhost:8080 | shortUrl prefix |
| RATE_LIMIT_MAX | 10 / 60s | rate limiter |
| AGENT_LLM | mock | mock or openai |
| OPENAI_API_KEY | - | only if `AGENT_LLM=openai` |
| APPROVAL_MODE | auto | auto / interactive / deny |
| VITE_API_BASE | http://localhost:8080 | React dev-server target for API calls |

## 5. How the deliverables map to modules
- Working prototype -> Spring Boot app (`./gradlew bootRun`) + React UI + `scenario-greenfield|brownfield|ambiguous` CLI.
- Architecture overview -> this file.
- 3 scenarios -> `orchestrator.scenario` + integration tests.
- Setup instructions -> `README.md`.
- Testing/limitations/trade-offs -> Spec 05 + Spec 04 + README.

## 6. Build/run plan (once specs are approved)
1. `build.gradle` (+ settings) + Gradle wrapper.
2. Product module to satisfy `02-openapi.yaml` + AC-1..AC-7.
3. Orchestrator core (engine, gates, blackboard, metrics, policy) + AC-8..AC-15.
4. Role agents + LLM seam.
5. SdlcGraph + three ScenarioRunners + `OrchestratorController`.
6. React (Vite) UI in `ui/` wired to the REST endpoints; Gradle task bundles it into `static/`.
7. Test suite (unit/contract/integration/governance).
8. README + Dockerfile + zip.

## Run commands
- Backend + bundled UI: `./gradlew bootRun` -> `http://localhost:8080`
- UI dev mode (hot reload): `cd ui && npm install && npm run dev` -> `http://localhost:5173` (proxies API to :8080)
- Scenario CLI: `./gradlew bootRun --args='--scenario=greenfield'`
- Tests: `./gradlew test`
```

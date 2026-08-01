# Spec 00 — Overview & Spec-Driven Development Approach > 00 Deliverables (from the brief) and where each is satisfied

# What we are building
Two things, kept deliberately separate:

1. **The product** — a **URL shortener** REST service (core APIs, analytics, reliability features). This is the “engineering outcome”.
2. **The orchestrator** — an **agentic SDLC orchestration layer** (Java) that drives the full software lifecycle (requirements + design + implementation + testing + documentation + release readiness) with **controlled autonomy**, an explicit dependency graph, governance gates, and audit-grade observability. This is the graded **critical differentiator**.

The URL shortener is intentionally small so the bulk of the engineering effort and evaluation weight land on the orchestration/governance layer.

## Spec-Driven Development (SDD) — how this project is run
SDD means **the specification is the source of truth and is authored before code**. Concretely:

| Order | Artifact | Drives |
|---|---|---|
| 1 | `01-requirements-spec.md` | Scope, functional/non-functional reqs, ambiguities, normalized problem |
| 2 | `02-openapi.yaml` | The API contract → controllers, DTOs, contract tests |
| 3 | `03-orchestration-spec.md` | The SDLC dependency graph ↔ orchestrator engine ↔ nodes |
| 4 | `04-governance-policy-spec.md` | Guardrails, autonomy levels, approvals, reliability metrics |
| 5 | `05-acceptance-criteria.md` | Gherkin acceptance tests + 3 scenarios + JUnit tests |
| 6 | `06-architecture.md` | Component model + Java module layout + tech stack |

## SDD workflow (traceability loop):
```
spec (this folder)  →  tests derived from spec  →  implementation to satisfy tests
       ▲                                              │
       └──────────── spec updated if reality diverges ─┘
```
Every functional requirement carries an ID (e.g. `FR-3`), every acceptance criterion references those IDs, and every test/class will reference the criterion it satisfies. This gives requirement ↔ test ↔ code traceability, which is itself one of the graded qualities.

Interestingly, the orchestrator we build *also practices SDD internally*: its first two SDLC stages (Requirements, Design) emit machine-checkable spec artifacts onto the blackboard, and downstream stages (Implementation, Testing) are gated on those specs — so the tool eats its own dog food.

## Tech stack (summary; full rationale in `06-architecture.md`)
- **Java 17 + Spring Boot 3** (Web) for the shortener API, **springdoc** to serve the OpenAPI contract.
- **Hand-rolled DAG engine** (nodes/edges/gates) for the orchestrator — more defensible and transparent than bending a workflow framework to the rubric.
- **JGraphT** for graph validation (cycle detection, topological order) — optional; can be hand-rolled.
- **In-memory store** by default (H2/Postgres drop-in) for links + audit lineage.
- **React (JS) + Vite** single-page UI to drive the shortener and visualize the orchestrator’s DAG, gates, approvals, and reliability metrics live.
- **JUnit 5 + Spring MockMvc** for unit/contract/integration tests.
- **Slf4j/Logback** JSON logs + a metrics registry for reliability metrics.
- **Pluggable LLM seam** with a deterministic **mock agent** default + runs locally with **no API keys**; optional real LLM via env config.
- **Gradle** build with the **Gradle wrapper** (`./gradlew`) so the zip builds with only a JDK installed; a Node/Vite Gradle task bundles the React UI into the Spring Boot jar.

## Deliverables (from the brief) and where each is satisfied
- Working prototype (runnable end-to-end) + Spring Boot app.
- Architecture overview → `06-architecture.md`
- Three scenarios (greenfield / brownfield / ambiguous) → `05-acceptance-criteria.md` + scenario runners.
- Setup instructions → `README.md` (written at implementation time).
- Testing approach, limitations, trade-offs → `05-acceptance-criteria.md` + `06-architecture.md` + final engineering summary.
# Spec 01 — Requirement Specification

## 1. Requirement understanding (intent)
Deliver an **agentic execution model** that turns a requirement into a reviewable engineering outcome, demonstrating requirement understanding, task decomposition, multi-step execution, and validated output generation — with governed, controlled autonomy across the SDLC. The concrete workload used to demonstrate this is a **URL shortener service**.

## 2. Ambiguities identified & normalization decisions
Surfacing ambiguity (and resolving it explicitly) is a graded behaviour. Decisions below are assumptions the implementation adopts; each is revisitable.

| # | Ambiguity | Decision (normalized) |
|---|---|---|
| A1 | How “real” must orchestrated execution be? | Real generation/validation for design/code/tests/docs; **release stage is simulated behind a human-approved safe-stop** (no real deploy). |
| A2 | Single agent with roles vs. many agents? | **Role-specialized agents** (Analyst, Architect, Implementer, Tester, DocWriter, Governor) over one pluggable LLM seam. |
| A3 | LLM required to run? | **No.** Default Mock agent is deterministic; real LLM is opt-in via env. Keeps the prototype runnable offline. |
| A4 | Shortener scale/SLA? | Single-node, in-memory default; horizontal-scale path noted, not built. |
| A5 | Auth / multi-tenant for shortener? | Out of scope; single tenant. Rate limiting is included as the reliability feature. |
| A6 | Custom alias collision & reserved words? | Aliases validated `[A-Za-z0-9_-]{3,}`; reserved words blocked; duplicate alias = 409. |
| A7 | Link expiry semantics? | Optional `TTL`; expired link -> HTTP 410 Gone. |

## 3. Functional requirements — URL shortener (the product)
| ID | Requirement | Acceptance ref |
|---|---|---|
| FR-1 | Create a short code for a valid long URL via `POST /api/shorten`. | AC-1 |
| FR-2 | Support optional **custom alias**; reject invalid/reserved/duplicate aliases. | AC-2 |
| FR-3 | Support optional **TTL**; expired links return 410. | AC-3 |
| FR-4 | `GET /{code}` issues a **302 redirect** to the long URL and records a click. | AC-4 |
| FR-5 | `GET /api/stats/{code}` returns analytics: total clicks, clicks-by-day, referrers, last access. | AC-5 |
| FR-6 | De-duplicate: same URL with no alias/TTL reuses its existing code. | AC-6 |
| FR-7 | Unknown code -> 404; expired -> 410. | AC-4 |
| FR-8 | `GET /health` returns status and link count. | AC-7 |

## 4. Functional requirements — orchestrator (the differentiator)
| ID | Requirement | Acceptance ref |
|---|---|---|
| OR-1 | Execute an **explicit dependency graph** of SDLC stages with per-node **entry/exit gates**. | AC-8 |
| OR-2 | Support **sequential and parallel** paths with **synchronization/join**. | AC-9 |
| OR-3 | Preserve **cross-stage context** and lineage (**append-only audit log**) on a shared blackboard. | AC-10 |
| OR-4 | Enforce **human approval checkpoints** (design changes, release). | AC-11 |
| OR-5 | Provide **bounded retries, fallback, rollback, and safe-stop**. | AC-12 |
| OR-6 | Embed **policy guardrails** (security / compliance / change-control). | AC-13 |
| OR-7 | Emit **audit-grade observability** and reliability metrics (success rate, retry/rollback freq, MTTR, e2e latency). | AC-14 |
| OR-8 | **Dynamically re-plan** downstream when an upstream output changes. | AC-15 |
| OR-9 | Enforce **controlled autonomy** — per-node autonomy level (auto / needs-approval / blocked). | AC-11 |

## 5. Non-functional requirements
| ID | Requirement |
|---|---|
| NFR-1 | **Runnable end-to-end** with only a JDK (Maven wrapper bundled); no API keys required. |
| NFR-2 | **Modular & testable**: orchestrator has zero dependency on the shortener domain; product has zero dependency on the orchestrator. |
| NFR-3 | **Deterministic** default path so tests and governance are reproducible. |
| NFR-4 | **Secure**: no hardcoded secrets; guardrail scans generated artifacts for secrets. |
| NFR-5 | **Observable**: structured JSON logs + a run trace with a unique run id. |
| NFR-6 | **Traceable**: requirement ID ↔ acceptance criterion ↔ test ↔ code. |

## 6. Out of scope (explicit limitations)
Real cloud deploy; auth/RBAC; multi-region/HA; distributed rate limiting; a production LLM prompt-engineering suite; persistent DB migrations (H2/Postgres wiring is stubbed but in-memory is default).

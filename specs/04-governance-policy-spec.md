# Spec 04 — Governance, Policy & Reliability Specification

Defines the guardrails, autonomy boundaries, approval model, and reliability metrics that make the orchestration *governed* rather than free-running.

## 1. Policy guardrails
Guardrails run as **pre-hooks** (before a node acts) and **post-hooks** (after a node produces an artifact). A guardrail returns `{allowed, reason, requireApproval, severity}`.

| ID | Guardrail | Category | Effect |
|----|-----------|----------|--------|
| G-1 | **No hardcoded secrets** — scan produced artifacts for API keys/passwords/tokens/private keys. | Security | `allowed=false` (severity high) + **safe-stop**. |
| G-2 | **High-impact needs approval** — nodes `design`, `release` are change-controlled. | Change control | `requireApproval=true` |
| G-3 | **Tests must pass before release** — `release` blocked unless a `test_report` exists and `passed=true`. | Compliance | `allowed=false` + **safe-stop**. |

Guardrails are pluggable; the default set is `[G-1, G-2, G-3]`. Every evaluation is written to the lineage (`kind=policy`).

## 2. Autonomy & human approval model (controlled autonomy)
- Each node declares an autonomy level: `AUTO`, `NEEDS_APPROVAL`, or `BLOCKED`.
- `NEEDS_APPROVAL` / `BLOCKED` nodes invoke an **ApprovalFn(node, blackboard) -> bool** checkpoint before the agent runs. A rejection triggers a controlled **safe-stop**.
- Approval strategies:
  - `autoApprove` (demo/CI default) — approves and records the decision, keeping runs non-interactive but still logged/audited.
  - `interactiveApprove` — prompts a human (CLI/UI) for high-impact gates.
  - `denyAll` — used in tests to prove the safe-stop path.
- Principle: **agents execute within defined autonomy boundaries; humans own oversight, approvals, and final quality control.**

## 3. Reliability metrics (OR-7)
Collected per run and emitted at the end (and queryable live):

| Metric | Definition |
|--------|------------|
| `successRate` | node successes / (successes + failures) |
| `nodeAttempts` | total node execution attempts (incl. retries) |
| `retries` | retryFrequency | retry count / retries per attempt |
| `rollbacks` | rollbackFrequency | rollback count / rollbacks per attempt |
| `safeStops` | controlled aborts |
| `approvalsRequested` | human checkpoints hit |
| `replans` | dynamic re-planning events |
| `mttrSeconds` | mean time between a node failure and its subsequent success |
| `e2eLatencySeconds` | wall-clock time of the whole run |

## 4. Failure taxonomy & controls (validation / risk)
| Failure mode | Detection | Control |
|--------------|-----------|---------|
| Agent produces invalid/empty output | exit gate fails | bounded retry + fallback + rollback + safe-stop |
| Upstream requirement changes mid-flight | output-hash change / explicit replan | dynamic re-plan of downstream subgraph |
| Insecure artifact (secret leak) | Guardrail G-1 | safe-stop, artifact rejected |
| Premature release | guardrails G-2/G-3 | approval + tests-pass gate |
| Non-terminating retries | `maxRetries` cap | bounded, then safe-stop |
| Partial state on failure | pre-node snapshot | rollback restores clean state |
| Cyclic/invalid graph | topological validation at load | run refused before side effects |

## 5. Risks & trade-offs (for the engineering summary)
- **LLM nondeterminism** mitigated by structured artifacts + exit gates + deterministic Mock default; real-LLM runs are opt-in and still gated.
- **Cost/latency of real agents** + output caching via node hashes + parallel path; only invalidated nodes re-run on replan.
- **Over-trusting autonomy** — high-impact gates always require approval; `denyAll` test proves the stop path.
- **In-memory persistence** — fine for the prototype; interface allows H2/Postgres for durability. Documented as a limitation.
- **Simulated release** + deliberate safe-stop instead of real deploy, matching a controlled-autonomy posture.

## 6. Traceability
G-1↔NFR-4, G-2/G-3↔OR-4/OR-6, G2↔OR-9/OR-4, G3↔OR-7, G4↔OR-5/OR-8, G5↔Deliverable `risks/trade-offs/validation`.

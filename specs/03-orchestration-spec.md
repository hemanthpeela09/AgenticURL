# Spec 03 — Agentic Orchestration Specification

This spec defines the orchestration engine and the SDLC dependency graph it executes. It is the graded **critical differentiator**; **non-linear, stateful, governed** execution — not a linear task chain.

## 1. Core abstractions
| Concept | Definition |
|---|---|
| **Blackboard** | Shared, persisted run state: artifacts, node statuses, output hashes, open questions, approvals, and an append-only **lineage** log. Single source of cross-stage context. |
| **Artifact** | A typed output (`requirements`, `design`, `code`) tests | docs | report`) produced by a node, versioned and content-hashed. |
| **Node** | An SDLC stage: `requirements`, `design`, `implementation`, `testing`, `documentation`, `release`, fallback, parallelGroup, critical }. |
| **Gate** | A predicate over the blackboard. **Entry gate** = precondition to run; **exit gate** = postcondition validating output. |
| **Agent** | A role that reads the blackboard and emits artifacts. Backed by a pluggable LLM seam (Mock default). |
| **Autonomy** | `AUTO` (no sign-off) / `NEEDS_APPROVAL` (human checkpoint) / `BLOCKED` (never auto-runs). |
| **Governor** | Cross-cutting policy engine invoked pre- and post-node (see Spec 04). |

## 2. The SDLC dependency graph (DAG)
```
requirements ➜ design ➜ ┌ implementation ┐
(Analyst)   (Architect)  │ (Implementer) │ ➜ synchronize ➜ testing ➜ documentation ➜ release
                         └ test_authoring ┘   (join)        (Tester)   (DocWriter)   (safe-stop)
                           (Tester)
```
**Nodes:** `requirements`, `design`, `implementation`, `test_authoring`, `testing`, `documentation`, `release`.
**Parallel path:** `implementation` and `test_authoring` share a `parallelGroup` and both depend on `design`; they run concurrently and **synchronize** before `testing` (proves OR-2 / AC-9).
**Edges (dependsOn):** design↤requirements; implementation↤design; test_authoring↤design; testing↤(implementation, test_authoring); documentation↤testing; release↤(testing, documentation).

## 3. Per-node contract
| Node | Autonomy | Entry gate | Exit gate | Produces |
|---|---|---|---|---|
| requirements | AUTO | always | `requirements_spec` exists AND no open questions | `requirements_spec` |
| design | **NEEDS_APPROVAL** | requires `requirements_spec` | `design_spec` exists | `design_spec` |
| implementation | AUTO | requires `design_spec` | `code_manifest` exists | `code_manifest` |
| test_authoring | AUTO | requires `design_spec` | `test_plan` exists | `test_plan` |
| testing | AUTO | requires `code_manifest`, `test_plan` | `test_report` exists AND passed | `test_report` |
| documentation | AUTO | requires `test_report` | `docs` exist | `docs` |
| release | **NEEDS_APPROVAL** | requires `test_report`, `docs` | `release_record` exists | `release_record` |

## 4. Execution semantics
1. Validate the graph: unknown deps and **cycles** are rejected before running (topological sort).
2. Execute in **waves**; a node runs once all its `dependsOn` are complete. Nodes in the same `parallelGroup` in a wave run concurrently, then **join** (synchronization barrier).
3. For each node: **entry gate** + pre-policy + (approval if gated) + agent retry/rollback + exit gate + post-policy + record output hash + lineage + metrics**.
4. **Bounded retry:** on failure, roll back blackboard to the pre-node snapshot and retry up to `maxRetries`; the final attempt may switch to a `fallback` agent.
5. **Rollback:** every retry and every terminal failure restores the pre-node snapshot (no partial state leaks downstream).
6. **Safe-stop:** a failed **critical** node, a blocking policy result, or a rejected approval aborts the run in a controlled way with full lineage intact.

## 5. Dynamic re-planning (OR-8 / AC-15)
`replanFrom(changedNode)`:
- Compute the transitive **descendants** of `changedNode`.
- Reset their status to `PENDING` and clear their cached output hashes.
- Increment the `replans` metric and log a `replan` lineage event.
- A subsequent `run()` re-executes exactly the invalidated subgraph, preserving governance.

Trigger example (ambiguous scenario): the Analyst revises `requirements_spec` after a clarification is answered ➜ design, implementation, test_authoring, testing, documentation, release are invalidated and re-run.

## 6. Observability contract
- Every state transition appends a `LineageEvent {ts, node, kind, detail, data}`; `kind ∈ {entry_gate, agent, artifact, exit_gate, retry, rollback, approval, policy, replan, parallel, synchronize, metrics, safe_stop, error}`.
- An **event stream** callback (`onEvent(kind, data)`) lets a UI/log tail follow the run live.
- The final blackboard snapshot, event stream, and approvals log are serializable to JSON for replay/debugging.

## 7. Traceability to requirements

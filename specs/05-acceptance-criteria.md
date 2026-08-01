# Spec 05 — Acceptance Criteria & Scenarios

Gherkin-style criteria. Each becomes a JUnit test referencing the requirement IDs from Spec 01. This is the executable definition of "done".

## Product acceptance criteria (URL shortener)
### AC-1 (FR-1) Create short link
```

Given a valid long URL
When I POST /api/shorten with that url
Then I get 201 with a code, a shortUrl, and createdAt
And GET /{code} redirects (302) to the long URL
```
### AC-2 (FR-2) Custom alias
```

Given a valid custom alias "my-link"
When I POST /api/shorten with that alias
Then the created code equals "my-link"
And a second POST with the same alias returns 409
And an alias "ab" (too short) or "api" (reserved) returns 400
```
### AC-3 (FR-3) TTL expiry
```

Given a link created with ttlSeconds in the past (or elapsed)
When I GET /{code}
Then I get 410 Gone
```
### AC-4 (FR-4, FR-7) Redirect + not found
```

When I GET /{code} for a known, live code
Then I get 302 to the long URL and a click is recorded
When I GET /{unknown}
Then I get 404
```
### AC-5 (FR-5) Analytics
```

Given a code that has been visited 3 times
When I GET /api/stats/{code}
Then totalClicks == 3, clicksByDay and referrers are populated, lastAccessed is set
```
### AC-6 (FR-6) De-duplication
```

Given a URL already shortened with no alias/ttl
When I POST /api/shorten with the same URL
Then the same code is returned
```
### AC-7 (FR-8) Health
```

When I GET /health
Then status == "ok" and links == current link count
```
## Orchestrator acceptance criteria
### AC-8 (OR-1) Graph with gates runs to completion
```

Given the SDLC graph and a greenfield requirement
When I run the orchestrator with autoApprove
Then all nodes reach SUCCEEDED, each entry+exit gate is logged, and a release_record exists
```
### AC-9 (OR-2) Parallel + synchronize
```

When the run reaches the design→(implementation||test_authoring) wave
Then implementation and test_authoring execute in the same parallel group
And a synchronize lineage event is recorded before testing runs
```
### AC-10 (OR-3) Lineage / cross-stage context
```

When a run completes
Then the blackboard contains ordered lineage events with kinds including
  entry_gate, agent, artifact, exit_gate, policy, metrics
And later stages can read artifacts produced by earlier stages
```
### AC-11 (OR-4, OR-9) Approval / controlled autonomy
```

Given the design node is NEEDS_APPROVAL and approvalFn = denyAll
When I run the orchestrator
Then the run safe-stops at design, no downstream node runs, and the rejection is logged
```
### AC-12 (OR-5) Retry, rollback, fallback, safe-stop
```

Given an implementation agent that fails its first attempt then succeeds
When I run the orchestrator
Then a retry and a rollback are recorded and the node ultimately SUCCEEDS
Given an agent that always fails on a critical node
Then retries are bounded by maxRetries and the run safe-stops
```
### AC-13 (OR-6) Policy guardrail blocks
```

Given an agent that emits an artifact containing "api_key = 'secret123456'"
When the post-policy runs
Then guardrail G-1 blocks it and the run safe-stops
Given no test_report
When release is attempted
Then guardrail G-3 blocks release
```
### AC-14 (OR-7) Reliability metrics
```

When a run completes
Then metrics expose successRate, retries, rollbacks, mttrSeconds, e2eLatencySeconds,
  approvalsRequested and replans
```
### AC-15 (OR-8) Dynamic re-planning
```

Given a completed run
When requirements change and I call replanFrom("requirements")
Then all downstream nodes are reset to PENDING, replans increments,
And a second run re-executes only the invalidated subgraph
```
## The three required scenarios
### S1 — Greenfield: "Build a URL shortener with analytics"
Full DAG from scratch. Demonstrates decomposition (requirements→design→impl→tests→test→docs→release), the parallel path, approval at design & release, and a passing test_report → release_record. (Covers AC-8..AC-14.)

### S2 — Brownfield: "Add link expiry + analytics to the existing shortener"
Requirement references existing modules. The Analyst/Architect emit an **impact analysis** artifact listing affected components (`storage`, `redirect`, `stats`, DTOs) and data flows before implementation. Demonstrates codebase reasoning + change-controlled design approval on an existing system.

### S3 — Ambiguous: "Make the links safer"
Analyst detects ambiguity + records **open questions** (malicious-URL scanning? rate limits? expiry? auth?) + the `requirements` exit gate (no open questions) forces a **human clarification checkpoint**. Once answered, the `requirements_spec` is revised and **dynamic re-planning** re-runs the downstream subgraph. Demonstrates ambiguity handling + replan + governance. (Covers AC-11, AC-15.)

## Test layering (testing approach)
- **Unit**: gates, policy guardrails, metrics math, short-code encoding, storage.
- **Contract**: controllers vs. `02-openapi.yaml` (MockMvc status/shape checks).
- **Integration**: full orchestrator runs per scenario asserting statuses, lineage kinds, and metrics.
- **Negative/governance**: denyAll safe-stop, secret-leak block, release-without-tests block, bounded-retry exhaustion.
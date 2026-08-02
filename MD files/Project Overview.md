# Agentic URL Shortener - Detailed Architecture

> **Version:** 1.0
> **Date:** August 3, 2026
> **Project:** Governed Agentic SDLC Orchestrator with URL Shortener Workload

---

## Table of Contents

1. [Overview](#1-overview)
2. [UI Architecture](#2-ui-architecture)
3. [Orchestration Engine](#3-orchestration-engine)
4. [LLM Integration](#4-llm-integration)
5. [Blue-Green Resiliency](#5-blue-green-resiliency)
6. [Component Interaction](#6-component-interaction)
7. [Deployment Architecture](#7-deployment-architecture)
8. [Summary](#summary)

---

## 1. Overview

This project implements a **governed agentic SDLC orchestrator** that transforms requirements into reviewable engineering outcomes. The architecture follows a clean separation between:

- **Product Layer (URL Shortener):** The workload being built
- **Orchestration Layer:** The differentiator - DAG-based SDLC automation with LLM agents
- **UI Layer:** React SPA for visualization and interaction
- **Bluefield and Greenfield:** Scenarios

---
---

## 2. UI Architecture

### 2.1 Technology Stack

| Component | Technology | Purpose |
| :--- | :--- | :--- |
| Framework | **React (JavaScript)** | Single-page application |
| Build Tool | **Vite** | Fast dev server & bundling |
| UI Library | **Material UI** | Component library (State Street Photon direction) |
| i18n | **react-i18next** | Multi-language support (EN, HI, ZH, IT) |
| State | React Hooks | Local component state management |

### 2.2 API Integration Layer (`api.js`)

```javascript
// Key API endpoints used by the UI
POST /api/shorten                   -> Create short link
GET  /api/stats/{code}              -> Click analytics
POST /api/orchestrator/run          -> Execute SDLC scenario
GET  /api/orchestrator/scenarios    -> List available scenarios
GET  /api/health/blue-green         -> Blue-Green deployment metrics
POST /api/llm/chain/sdlc            -> Execute LLM chain
POST /api/llm/agent/sdlc            -> Execute ReAct agent
```
---
---
## 3. Orchestration Engine

### 3.1 Core Abstractions

The orchestration engine is a hand-rolled DAG execution framework with full governance support.

| Concept | Description |
| :--- | :--- |
|Blackboard	| Shared run state: artifacts, statuses, lineage, approvals |
|Node	| SDLC stage with agent, gates, autonomy, retry config |
|Gate	| Entry (precondition) and Exit (postcondition) predicates |
|Agent	| Role-based executor backed by pluggable LLM |
|Artifact	| Typed, versioned, content-hashed output |
|Autonomy	| AUTO / NEEDS_APPROVAL / BLOCKED |
|PolicyEngine	| Guardrails for security, compliance, change control |

### 3.2 Package Structure
com.example.orchestrator/
+-- core/
|   +-- Orchestrator.java            # DAG execution engine
|   +-- Node.java                    # SDLC stage definition
|   +-- Blackboard.java              # Shared state container
|   +-- Artifact.java                # Versioned output
|   +-- Gate.java & Gates.java       # Entry/exit predicates
|   +-- Autonomy.java                # Approval levels
|   +-- ApprovalStrategy.java        # Auto/Interactive/Deny
|   +-- LineageEvent.java            # Audit trail events
|   +-- NodeStatus.java              # Execution states
|   +-- SafeStopException.java       # Controlled abort
|   +-- GreenfieldHealthService.java # Blue-Green health
|
+-- agents/
|   +-- Analyst.java                 # Requirements analysis
|   +-- Architect.java               # System design
|   +-- Implementer.java             # Code generation
|   +-- Tester.java                  # Test planning/execution
|   +-- DocWriter.java               # Documentation
|   +-- Governor.java                # Policy enforcement
|
+-- policy/
|   +-- PolicyEngine.java            # Guardrail evaluator
|   +-- Guardrail.java               # Individual policy rule
|
+-- metrics/
|   +-- Metrics.java                 # Reliability tracking
|
+-- llm/
|   +-- Llm.java                     # LLM interface
|   +-- MockLlm.java                 # Offline deterministic
|   +-- SpringAiLlm.java             # OpenAI via Spring AI
|   +-- LlmChain.java                # Sequential pipeline
|   +-- PromptTemplate.java          # Template system
|   +-- AgentExecutor.java           # ReAct agent
|   +-- ConversationMemory.java      # Context management
|
+-- scenario/
|   +-- Scenario.java                # GREENFIELD/BROWNFIELD/AMBIGUOUS
|   +-- ScenarioRunner.java          # Scenario executor
|   +-- SdlcGraph.java               # DAG definition
|   +-- RunResult.java               # Execution result

### 3.3 Scenario Behaviors

| Scenario | Requirement | Outcome |
| :--- | :--- | :--- |
| Greenfield | Build a URL shortener service with core APIs and click analytics | Full graph executes -> COMPLETED |
| Brownfield | Add link expiry (TTL) and analytics to the existing service | G-4 blocks design (breaking change) -> SAFE_STOPPED |
| Ambiguous | Make the links safer | Open questions recorded -> safe-stop -> clarify -> replan -> COMPLETED |

---
---

## 4 LLM Integration

### 4.1 LangChain-Style Architecture
+-------------------------------------------------------------------+
|                       LLM ABSTRACTION LAYER                       |
|                                                                   |
|   +-----------------------------------------------------------+   |
|   |                        Llm Interface                      |   |
|   |           complete(system, prompt) -> String              |   |
|   +-----------------------------------------------------------+   |
|         |                   |                  |            |     |
|         v                   v                  v            v     |
|   +-----------+       +-----------+       +---------+  +--------+ |
|   |  MockLlm  |       | SpringAi  |       |  Azure  |  |Ollama  | |
|   | (offline) |       | (OpenAI)  |       | (Azure) |  |(local) | |
|   +-----------+       +-----------+       +---------+  +--------+ |
+-------------------------------------------------------------------+

### 4.2 Execution flow
Requirement -> [Step 1: Analysis] -> [Step 2: Design] -> [Step 3: Conditional] -> Result
|                     |                     |
requirement_spec        design_spec          security_spec

---
---
## 5.Blue-Green Resiliency

### 5.1 Overview
The "Greenfield" node (Blue) represents the primary execution environment, while "Brownfield" (Green) serves as the current/fallback environment.

+-------------------------------------------------------------------+
|                     BLUE-GREEN DEPLOYMENT MODEL                   |
|                                                                   |
|      +------------------------+      +---------------------+      |
|      |       GREENFIELD       |      |     BROWNFIELD      |      |
|      |      (Blue - New)      |      |   (Green - Current) |      |
|      |                        |      |                     |      |
|      |     localhost:8080     |      |    localhost:8081    |      |
|      |                        |      |                     |      |
|      |     Status:   UP       |      |    Status:   UP     |      |
|      +------------------------+      +---------------------+      |
|                   |                             |                 |
|                   +--------------+--------------+                 |
|                                  |                                |
|                                  v                                |
|                     +------------------------+                    |
|                     | GreenfieldHealthService|                    |
|                     | - Health monitoring    |                    |
|                     | - Retry with backoff   |                    |
|                     | - Request queuing      |                    |
|                     | - Metrics collection   |                    |
|                     +------------------------+                    |
+-------------------------------------------------------------------+

### 5.2 Retry Strategy Flow
+-------------------------------------------------------------------+
|                        RETRY STRATEGY FLOW                        |
|                                                                   |
| Request Received                                                  |
|        |                                                          |
|        v                                                          |
|  +-----------+    healthy                                         |
|  |Check Health|--------------> Execute Operation ---> Success     |
|  |  /health  |                                                    |
|  +-----------+                                                    |
|        |                                                          |
|     unhealthy                                                     |
|        v                                                          |
|  +---------------+                                                |
|  | Queue Request |<--------------------------------------------+  |
|  |  (increment)  |                                             |  |
|  +---------------+                                             |  |
|        |                                                       |  |
|        v                                                       |  |
|  +-------------------------------------------------------+     |  |
|  |               EXPONENTIAL BACKOFF LOOP                |     |  |
|  |                                                       |     |  |
|  | for (retry = 1; retry <= maxRetries; retry++):        |     |  |
|  |     sleep(currentDelay)                               |     |  |
|  |     if (elapsed >= maxWaitMs):                        |     |  |
|  |         return TIMEOUT                                |     |  |
|  |     if (isHealthy()):                                 |     |  |
|  |         return SUCCESS -> Execute                     |     |  |
|  |     currentDelay = min(currentDelay * 2, 10000ms)    |-----+ retry
|  +-------------------------------------------------------+        |
|        |                                                          |
|        v                                                          |
|  +-------------------+                                            |
|  |    Max Retries    |                                            |
|  |     Exceeded      |-----> Return TIMEOUT with metrics          |
|  +-------------------+                                            |
+-------------------------------------------------------------------+

### 5.3 Metrics Dashboard
The UI displays comprehensive Blue-Green metrics

### 5.4 Safe-Stop Mechanism
+-------------------------------------------------------------------+
|                        SAFE-STOP TRIGGERS                         |
|                                                                   |
| 1. CRITICAL NODE FAILURE                                          |
|    +-> Node marked as critical fails -> Immediate safe-stop        |
|                                                                   |
| 2. POLICY BLOCK                                                   |
|    +-> Guardrail returns allowed=false -> Safe-stop with reason    |
|                                                                   |
| 3. APPROVAL REJECTION                                             |
|    +-> Human denies approval request -> Safe-stop                 |
|                                                                   |
| 4. BROWNFIELD CHANGE CONTROL (G-4)                                |
|    +-> Breaking change without migration approval -> Safe-stop    |
|                                                                   |
| 5. USER DENIAL (UI)                                               |
|    +-> Pending request denied -> Safe-stop with user notification |
|                                                                   |
| SAFE-STOP GUARANTEES:                                             |
| / Blackboard state preserved (no partial artifacts)               |
| / Full lineage recorded (audit trail intact)                      |
| / Metrics updated (safeStops counter)                             |
| / Reason surfaced to UI/CLI (safeStopReason field)                |
+-------------------------------------------------------------------+

---
---

## 6. Component Interaction

### 6.1 Full Request Flow
+-------------------------------------------------------------------+
|                       COMPLETE REQUEST FLOW                       |
|                                                                   |
| User (Browser)                                                    |
|    |                                                              |
|    | 1. Select scenario + Submit                                  |
|    v                                                              |
| +------------+  POST /api/orchestrator/run?scenario=greenfield   |
| | React SPA  |---------------------------------------------+      |
| |(Vite :5173)|                                             |      |
| +------------+                                             |      |
|                                                            v      |
|                                              +------------------+ |
|                                              |OrchestratorControl|
|                                              |(Spring Boot :8080)|
|                                              +------------------+ |
|                                                            |      |
|                  2. Validate request                       |      |
|                  3. Check Blue-Green                       v      |
|                                              +------------------+ |
|                                              | GreenfieldHealth | |
|                                              |     Service      | |
|                                              +------------------+ |
|                                                            |      |
|                  4. If healthy, execute                    v      |
|                  5. If down, queue+retry     +------------------+ |
|                                              |  ScenarioRunner  | |
|                                              +------------------+ |
|                                                            |      |
|                  6. Build graph                            v      |
|                  7. Wire agents              +------------------+ |
|                                              |   Orchestrator   | |
|                                              |   (DAG Engine)   | |
|                                              +------------------+ |
|                                                            |      |
|                  8. For each node:                         v      |
|                     - Entry gate             +------------------+ |
|                     - Policy check           |   Agent + LLM    | |
|                     - Agent execute          |  (Mock/OpenAI)   | |
|                     - Exit gate              +------------------+ |
|                     - Record lineage                       |      |
|                                                            v      |
|                  9. Collect metrics          +------------------+ |
|                 10. Build RunResult          |    RunResult     | |
|                                              |   {nodeStatus,   | |
|                                              |    artifacts,    | |
|                                              |     lineage,     | |
|                                              | metrics, completed}|
|                                              +------------------+ |
|                                                            |      |
| +------------+  render -> DagView + MetricsPanel + Lineage |      |
| | React SPA  |<--------------------------------------------+      |
| +------------+                                                    |
+-------------------------------------------------------------------+

---
---
## 7. Deployment Architecture

### 7.1 Development MODE
+-------------------------------------------------------------------+
|                       DEVELOPMENT DEPLOYMENT                      |
|                                                                   |
| +-------------------+    proxy    +-----------------------------+ |
| |     Vite Dev      |------------>|         Spring Boot         | |
| |    Server :5173   |             |         Server :8080        | |
| |    (React HMR)    |             |        (API + Swagger)      | |
| +-------------------+             +-----------------------------+ |
|                                                                   |
| Commands:                                                         |
| |-- Backend: ./gradlew bootRun                                    |
| |-- Frontend: cd ui && npm run dev                                |
+-------------------------------------------------------------------+

### 7.2 Production MODE
+-------------------------------------------------------------------+
|                       PRODUCTION DEPLOYMENT                       |
|                                                                   |
| +---------------------------------------------------------------+ |
| |                    Spring Boot JAR (:8080)                    | |
| |                                                               | |
| | +-----------------------------------------------------------+ | |
| | |                     Static Resources                      | | |
| | | |-- /static/index.html (React bundle)                     | | |
| | | |-- /static/assets/*.js                                   | | |
| | +-----------------------------------------------------------+ | |
| |                                                               | |
| | +-----------------------------------------------------------+ | |
| | |                         REST API                          | | |
| | | |-- /api/shorten, /api/stats, /api/orchestrator/*         | | |
| | +-----------------------------------------------------------+ | |
| +---------------------------------------------------------------+ |
|                                                                   |
| Build Command: ./gradlew bootJar                                  |
| Run Command: java -jar build/libs/agentic-url-shortener-1.0.0.jar  |
+-------------------------------------------------------------------+

---
---
## 8. Summary
This architecture delivers:
UI: Modern React SPA with Material UI, i18n support, real-time DAG visualization, and Blue-Green metrics dashboard.  
Orchestration: Hand-rolled DAG engine with gates, policies, bounded retry, rollback, and safe-stop for governed SDLC automation.  
LLM: Pluggable LangChain-style abstraction supporting Mock (offline), OpenAI, Azure, and Ollama backends with chains, agents, and memory.  
Blue-Green Resiliency: Health monitoring, exponential backoff retries, request queuing, and comprehensive metrics for production-grade reliability.  
---
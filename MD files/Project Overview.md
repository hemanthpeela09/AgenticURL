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

graph TD
    %% Subgraph styling
    subgraph Layer ["LLM ABSTRACTION LAYER"]
        direction TB
        
        %% Interface Node
        Interface["<b>Llm Interface</b><br>complete(system, prompt) -> String"]
        
        %% Concrete Implementations
        Mock["<b>MockLlm</b><br>(offline)"]
        Spring["<b>SpringAi</b><br>(OpenAI)"]
        Azure["<b>Azure</b><br>(Azure)"]
        Ollama["<b>Ollama</b><br>(local)"]
        
        %% Connections
        Interface --> Mock
        Interface --> Spring
        Interface --> Azure
        Interface --> Ollama
    end

    %% Style Classes
    style Layer fill:#f9f9f9,stroke:#333,stroke-width:2px,color:#333
    style Interface fill:#fff,stroke:#333,stroke-width:1px
    style Mock fill:#fff,stroke:#333,stroke-width:1px
    style Spring fill:#fff,stroke:#333,stroke-width:1px
    style Azure fill:#fff,stroke:#333,stroke-width:1px
    style Ollama fill:#fff,stroke:#333,stroke-width:1px


### 4.2 Execution flow
Requirement -> [Step 1: Analysis] -> [Step 2: Design] -> [Step 3: Conditional] -> Result
|                     |                     |
requirement_spec        design_spec          security_spec

---
---
## 5.Blue-Green Resiliency

### 5.1 Overview
The "Greenfield" node (Blue) represents the primary execution environment, while "Brownfield" (Green) serves as the current/fallback environment.

graph TD
    %% Main Wrapper
    subgraph Model ["BLUE-GREEN DEPLOYMENT MODEL"]
        direction TB

        %% Services Row
        subgraph Services [" "]
            direction LR
            Greenfield["<b>GREENFIELD</b><br>(Blue - New)<br><br>localhost:8080<br><br>Status: UP"]
            Brownfield["<b>BROWNFIELD</b><br>(Green - Current)<br><br>localhost:8081<br><br>Status: UP"]
        end

        %% Health Service Node
        HealthService["<b>GreenfieldHealthService</b><br>• Health monitoring<br>• Retry with backoff<br>• Request queuing<br>• Metrics collection"]

        %% Connections
        Greenfield --> HealthService
        Brownfield --> HealthService
    end

    %% Visual Styling
    style Model fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Services fill:none,stroke:none
    style Greenfield fill:#fff,stroke:#333,stroke-width:1px
    style Brownfield fill:#fff,stroke:#333,stroke-width:1px
    style HealthService fill:#fff,stroke:#333,stroke-width:1px


### 5.2 Retry Strategy Flow

flowchart TD
    %% Main Border
    subgraph Flow ["RETRY STRATEGY FLOW"]
        direction TB

        %% Entry Node
        Start([Request Received])

        %% Execution Nodes
        CheckHealth{"Check Health<br><b>/health</b>"}
        Execute[Execute Operation]
        Success([Success])
        Queue["Queue Request<br><i>(increment)</i>"]

        %% Loop Subgraph
        subgraph Loop ["EXPONENTIAL BACKOFF LOOP"]
            direction TB
            LoopStart["<b>for (retry = 1; retry &lt;= maxRetries; retry++)</b>"]
            Sleep["sleep(currentDelay)"]
            
            CheckTimeout{"if (elapsed &gt;= maxWaitMs)"}
            RetTimeout([return TIMEOUT])
            
            CheckLoopHealth{"if (isHealthy())"}
            RetSuccess([return SUCCESS -> Execute])
            
            CalcDelay["currentDelay = min(currentDelay * 2, 10000ms)"]

            %% Internal Loop Flow
            LoopStart --> Sleep
            Sleep --> CheckTimeout
            CheckTimeout -- Yes --> RetTimeout
            CheckTimeout -- No --> CheckLoopHealth
            CheckLoopHealth -- Yes --> RetSuccess
            CheckLoopHealth -- No --> CalcDelay
        end

        %% Failure Nodes
        MaxExceeded["Max Retries<br>Exceeded"]
        FinalTimeout([Return TIMEOUT with metrics])

        %% Main Flow Connections
        Start --> CheckHealth
        CheckHealth -- healthy --> Execute --> Success
        CheckHealth -- unhealthy --> Queue
        Queue --> LoopStart
        
        %% Loop Exits and Iteration
        CalcDelay -- retry --> Queue
        LoopStart -- loop exhausted --> MaxExceeded
        MaxExceeded --> FinalTimeout
    end

    %% Visual Styling
    style Flow fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Loop fill:#f0f0f0,stroke:#666,stroke-width:1px,stroke-dasharray: 5 5
    style CheckHealth fill:#fff,stroke:#333,stroke-width:1px
    style CheckTimeout fill:#fff,stroke:#333,stroke-width:1px
    style CheckLoopHealth fill:#fff,stroke:#333,stroke-width:1px
    style Execute fill:#fff,stroke:#333,stroke-width:1px
    style Queue fill:#fff,stroke:#333,stroke-width:1px
    style Sleep fill:#fff,stroke:#333,stroke-width:1px
    style CalcDelay fill:#fff,stroke:#333,stroke-width:1px
    style MaxExceeded fill:#fff,stroke:#333,stroke-width:1px


### 5.3 Metrics Dashboard
The UI displays comprehensive Blue-Green metrics

### 5.4 Safe-Stop Mechanism

flowchart TD
    %% Main Container
    subgraph Main ["SAFE-STOP TRIGGERS"]
        direction TB

        %% Triggers Block
        subgraph Triggers ["TRIGGERS"]
            direction TB
            T1["<b>1. CRITICAL NODE FAILURE</b><br>Node marked as critical fails"]
            T2["<b>2. POLICY BLOCK</b><br>Guardrail returns allowed=false"]
            T3["<b>3. APPROVAL REJECTION</b><br>Human denies approval request"]
            T4["<b>4. BROWNFIELD CHANGE CONTROL (G-4)</b><br>Breaking change without migration approval"]
            T5["<b>5. USER DENIAL (UI)</b><br>Pending request denied"]
        end

        %% Central Action
        SafeStop[["Immediate Safe-Stop"]]

        %% Guarantees Block
        subgraph Guarantees ["SAFE-STOP GUARANTEES"]
            direction TB
            G1["✓ Blackboard state preserved <i>(no partial artifacts)</i>"]
            G2["✓ Full lineage recorded <i>(audit trail intact)</i>"]
            G3["✓ Metrics updated <i>(safeStops counter)</i>"]
            G4["✓ Reason surfaced to UI/CLI <i>(safeStopReason field)</i>"]
        end

        %% Connections
        T1 --> SafeStop
        T2 --> SafeStop
        T3 --> SafeStop
        T4 --> SafeStop
        T5 --> SafeStop
        
        SafeStop --> Guarantees
    end

    %% Visual Styling
    style Main fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Triggers fill:none,stroke:none
    style Guarantees fill:#fff,stroke:#333,stroke-width:1px
    style SafeStop fill:#ffcccc,stroke:#cc0000,stroke-width:2px,color:#990000
    style T1 fill:#fff,stroke:#666,stroke-width:1px
    style T2 fill:#fff,stroke:#666,stroke-width:1px
    style T3 fill:#fff,stroke:#666,stroke-width:1px
    style T4 fill:#fff,stroke:#666,stroke-width:1px
    style T5 fill:#fff,stroke:#666,stroke-width:1px


---
---

## 6. Component Interaction

### 6.1 Full Request Flow
sequenceDiagram
    autonumber
    actor User as User (Browser)
    participant SPA as React SPA<br/>(Vite :5173)
    participant Ctrl as OrchestratorController<br/>(Spring Boot :8080)
    participant Health as GreenfieldHealthService
    participant Runner as ScenarioRunner
    participant DAG as Orchestrator<br/>(DAG Engine)
    participant Agent as Agent + LLM<br/>(Mock/OpenAI)
    participant Result as RunResult

    User->>SPA: Select scenario + Submit
    SPA->>Ctrl: POST /api/orchestrator/run?scenario=greenfield
    
    Note over Ctrl,Health: Validate request & Check Blue-Green
    Ctrl->>Health: Check status
    
    alt Status Healthy
        Health->>Runner: Execute immediately
    else Status Down
        Health->>Health: Queue + Exponential Retry Loop
        Health->>Runner: Execute once healthy
    end

    Note over Runner,DAG: Build graph & Wire agents
    Runner->>DAG: Initialize Execution Plan
    
    loop For each node in DAG
        DAG->>Agent: 1. Entry gate check
        DAG->>Agent: 2. Policy check
        Agent->>Agent: 3. Agent execute (LLM call)
        DAG->>Agent: 4. Exit gate check
        DAG->>DAG: 5. Record lineage
    end

    Note over DAG,Result: Collect metrics & Build RunResult
    DAG->>Result: Construct payload<br/>{nodeStatus, artifacts, lineage, metrics, completed}
    Result-->>SPA: Return HTTP 200 JSON
    SPA->>User: Render DagView + MetricsPanel + Lineage


---
---
## 7. Deployment Architecture

### 7.1 Development MODE
graph TD
    %% Main Border
    subgraph Main ["DEVELOPMENT DEPLOYMENT"]
        direction TB

        %% Architecture Row
        subgraph Architecture [" "]
            direction LR
            Vite["<b>Vite Dev Server</b><br>Port :5173<br>(React HMR)"]
            Spring["<b>Spring Boot Server</b><br>Port :8080<br>(API + Swagger)"]
            
            Vite -- proxy --> Spring
        end

        %% Commands Block
        subgraph Commands ["Commands"]
            direction TB
            C1["• <b>Backend:</b> ./gradlew bootRun"]
            C2["• <b>Frontend:</b> cd ui && npm run dev"]
        end
    end

    %% Visual Styling
    style Main fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Architecture fill:none,stroke:none
    style Commands fill:#fff,stroke:#333,stroke-width:1px
    style Vite fill:#fff,stroke:#666,stroke-width:1px
    style Spring fill:#fff,stroke:#666,stroke-width:1px


### 7.2 Production MODE
graph TD
    %% Main Container
    subgraph Main ["PRODUCTION DEPLOYMENT"]
        direction TB

        %% Jar Architecture
        subgraph Jar ["Spring Boot JAR (:8080)"]
            direction TB
            
            subgraph Static ["Static Resources"]
                direction TB
                S1["• /static/index.html (React bundle)"]
                S2["• /static/assets/*.js"]
             style Static fill:#fff,stroke:#666,stroke-width:1px
            end

            subgraph API ["REST API"]
                direction TB
                A1["• /api/shorten, /api/stats, /api/orchestrator/*"]
             style API fill:#fff,stroke:#666,stroke-width:1px
            end
        end

        %% Commands Block
        subgraph Commands ["Deployment Commands"]
            direction TB
            C1["<b>Build Command:</b> ./gradlew bootJar"]
            C2["<b>Run Command:</b> java -jar build/libs/agentic-url-shortener-1.0.0.jar"]
        end
    end

    %% Visual Styling
    style Main fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Jar fill:#f0f0f0,stroke:#333,stroke-width:1px
    style Commands fill:#fff,stroke:#333,stroke-width:1px


---
---
## 8. Summary
This architecture delivers:
UI: Modern React SPA with Material UI, i18n support, real-time DAG visualization, and Blue-Green metrics dashboard.  
Orchestration: Hand-rolled DAG engine with gates, policies, bounded retry, rollback, and safe-stop for governed SDLC automation.  
LLM: Pluggable LangChain-style abstraction supporting Mock (offline), OpenAI, Azure, and Ollama backends with chains, agents, and memory.  
Blue-Green Resiliency: Health monitoring, exponential backoff retries, request queuing, and comprehensive metrics for production-grade reliability.  
---
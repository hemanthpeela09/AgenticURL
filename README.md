# AgenticURL

A spec-driven starter template for a URL shortener service plus an agentic SDLC orchestration layer.

## Status

This architecture delivers:
UI: Modern React SPA with Material UI, i18n support, real-time DAG visualization, and Blue-Green metrics dashboard.  
Orchestration: Hand-rolled DAG engine with gates, policies, bounded retry, rollback, and safe-stop for governed SDLC automation.  
LLM: Pluggable LangChain-style abstraction supporting Mock (offline), OpenAI, Azure, and Ollama backends with chains, agents, and memory.  
Blue-Green Resiliency: Health monitoring, exponential backoff retries, request queuing, and comprehensive metrics for production-grade reliability.

## Repository layout

- [specs/](specs/) — product and orchestration requirements, API contract, governance policy, acceptance criteria, and architecture notes.
- [app/](app/) — Spring Boot entrypoint and integration boundary.
- [orchestrator/](orchestrator/) — orchestrator engine, policy, metrics, and agent scaffolding.
- [shortener/](shortener/) — product-side domain, API, service, and persistence scaffolding.
- [ui/](ui/) — React/Vite frontend placeholder.

## Demo
- [AgenticURL Demo.md](MD%20files/AgenticURL%20Demo.md)
- [Project Overview.md](MD%20files/Project%20Overview.md)

## Build command

- [Setup Document.md](MD%20files/Setup%20Document.md)

## Documents

- [Documents](specs/Documents)


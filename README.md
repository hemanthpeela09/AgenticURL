# AgenticURL

A spec-driven starter template for a URL shortener service plus an agentic SDLC orchestration layer.

## Status

This repository currently contains the project scaffold and the specification documents from the attached design. The implementation work is intentionally left for a later pass.

## Repository layout

- [specs/](specs/) — product and orchestration requirements, API contract, governance policy, acceptance criteria, and architecture notes.
- [product/](product/) — product-side domain, API, service, and persistence scaffolding.
- [orchestrator/](orchestrator/) — orchestrator engine, policy, metrics, and agent scaffolding.
- [app/](app/) — Spring Boot entrypoint and integration boundary.
- [ui/](ui/) — React/Vite frontend placeholder.

## Next steps

1. Review the specs in [specs/](specs/).
2. Implement the product API and domain behavior.
3. Implement the orchestrator DAG, gates, and policy engine.
4. Wire the UI to the backend endpoints.

## Build command

The scaffold is ready for Gradle-based development:

```bash
./gradlew test
```

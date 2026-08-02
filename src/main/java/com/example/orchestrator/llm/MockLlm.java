package com.example.orchestrator.llm;

/**
 * Deterministic mock LLM that returns structured, meaningful responses.
 * Recognizes SDLC chain steps and ReAct agent patterns to produce realistic output
 * without requiring any external API or model.
 */
public class MockLlm implements Llm {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String complete(String system, String prompt) {
        // Detect SDLC chain steps by system prompt keywords
        String sysLower = system.toLowerCase();
        String promptLower = prompt.toLowerCase();

        // --- SDLC Chain Steps ---
        if (sysLower.contains("requirements analyst") || sysLower.contains("analyst")) {
            return generateRequirementSpec(prompt);
        }
        if (sysLower.contains("architect") || sysLower.contains("design")) {
            return generateDesignSpec(prompt);
        }
        if (sysLower.contains("implementer") || sysLower.contains("implementation") || sysLower.contains("developer")) {
            return generateCodeManifest(prompt);
        }
        if (sysLower.contains("test") || sysLower.contains("qa")) {
            return generateTestPlan(prompt);
        }
        if (sysLower.contains("security")) {
            return "Security Review:\n- Input validation required for all endpoints\n- Rate limiting applied\n- No sensitive data exposure\n- HTTPS enforced\n- OWASP Top 10 considered";
        }
        if (sysLower.contains("quality") || sysLower.contains("review")) {
            return "Quality Review:\n- Code follows SOLID principles\n- Unit test coverage target: 80%\n- Documentation complete\n- No critical issues found";
        }
        if (sysLower.contains("summary") || sysLower.contains("release")) {
            return "Release Summary:\n- All SDLC stages passed\n- Requirements verified\n- Design reviewed\n- Implementation complete\n- Tests passing\n- Ready for deployment";
        }

        // --- ReAct Agent Pattern ---
        if (sysLower.contains("tools") || sysLower.contains("agent")) {
            return generateAgentResponse(prompt, sysLower);
        }

        // --- URL Shortener fallback ---
        if (promptLower.contains("shorten") || promptLower.contains("url")) {
            return "Final Answer: I will help shorten the URL. The URL has been extracted and processed.";
        }

        // Default structured response
        return generateDefaultResponse(system, prompt);
    }

    private String generateRequirementSpec(String prompt) {
        String topic = extractTopic(prompt);
        return "## Requirements Specification\n\n"
                + "### Functional Requirements\n"
                + "1. The system shall " + topic + "\n"
                + "2. The system shall validate all inputs\n"
                + "3. The system shall return appropriate HTTP status codes\n"
                + "4. The system shall handle concurrent requests\n\n"
                + "### Non-Functional Requirements\n"
                + "- Performance: Response time < 200ms (p95)\n"
                + "- Availability: 99.9% uptime\n"
                + "- Scalability: Handle 1000 req/sec\n"
                + "- Security: Input sanitization, rate limiting\n\n"
                + "### Acceptance Criteria\n"
                + "- All endpoints respond within SLA\n"
                + "- Error responses follow RFC 7807\n"
                + "- API documented via OpenAPI 3.0";
    }

    private String generateDesignSpec(String prompt) {
        return "## Design Specification\n\n"
                + "### Architecture\n"
                + "- Pattern: Layered Architecture (Controller -> Service -> Repository)\n"
                + "- Style: RESTful API with Spring Boot\n"
                + "- Data: In-memory store with interface abstraction\n\n"
                + "### Components\n"
                + "1. **API Layer**: REST controllers with validation\n"
                + "2. **Service Layer**: Business logic, orchestration\n"
                + "3. **Domain Layer**: Entities, value objects\n"
                + "4. **Store Layer**: Persistence abstraction\n\n"
                + "### API Design\n"
                + "- POST /api/resource - Create\n"
                + "- GET /api/resource/{id} - Read\n"
                + "- GET /api/resource - List all\n"
                + "- DELETE /api/resource/{id} - Delete\n\n"
                + "### Error Handling\n"
                + "- Global exception handler\n"
                + "- Structured error responses\n"
                + "- Appropriate HTTP status codes";
    }

    private String generateCodeManifest(String prompt) {
        return "## Implementation Manifest\n\n"
                + "### Files to Create/Modify\n"
                + "```\n"
                + "src/main/java/com/example/\n"
                + "├── controller/ResourceController.java\n"
                + "├── service/ResourceService.java\n"
                + "├── domain/Resource.java\n"
                + "├── store/ResourceStore.java\n"
                + "└── store/InMemoryResourceStore.java\n"
                + "```\n\n"
                + "### Key Implementation Details\n"
                + "- Use Spring Boot @RestController\n"
                + "- Input validation with @Valid and Jakarta annotations\n"
                + "- Service layer handles business rules\n"
                + "- ConcurrentHashMap for thread-safe in-memory storage\n"
                + "- Builder pattern for domain objects\n\n"
                + "### Dependencies\n"
                + "- spring-boot-starter-web\n"
                + "- spring-boot-starter-validation\n"
                + "- springdoc-openapi (API docs)";
    }

    private String generateTestPlan(String prompt) {
        return "## Test Plan\n\n"
                + "### Unit Tests\n"
                + "1. Service layer: test business logic in isolation\n"
                + "2. Domain: test validation rules, equality, hashCode\n"
                + "3. Store: test CRUD operations, concurrency\n\n"
                + "### Integration Tests\n"
                + "1. API tests with MockMvc\n"
                + "2. Happy path: create, retrieve, list, delete\n"
                + "3. Error cases: invalid input, not found, conflicts\n"
                + "4. Rate limiting verification\n\n"
                + "### Test Coverage Targets\n"
                + "- Line coverage: 80%\n"
                + "- Branch coverage: 70%\n"
                + "- Mutation score: 60%\n\n"
                + "### Testing Tools\n"
                + "- JUnit 5, Mockito, AssertJ\n"
                + "- Spring Boot Test, MockMvc\n"
                + "- ArchUnit for architecture tests";
    }

    private String generateAgentResponse(String prompt, String systemLower) {
        // Count iterations by checking for "Observation:" in the prompt
        int observationCount = countOccurrences(prompt, "Observation:");

        if (observationCount == 0) {
            // First iteration - use analyze_requirement tool
            return "Thought: I need to analyze the requirements first.\n"
                    + "Action: analyze_requirement\n"
                    + "Action Input: " + extractTopic(prompt);
        } else if (observationCount == 1) {
            // Second iteration - use design_component tool
            return "Thought: Now I should design the component based on the analysis.\n"
                    + "Action: design_component\n"
                    + "Action Input: Design based on the analyzed requirements";
        } else if (observationCount == 2) {
            // Third iteration - generate tests
            return "Thought: I should generate tests for the designed component.\n"
                    + "Action: generate_tests\n"
                    + "Action Input: Generate tests for the designed component";
        } else {
            // Final answer after tools
            return "Final Answer: Based on my analysis, here is the solution:\n\n"
                    + "1. Requirements have been analyzed and validated\n"
                    + "2. Architecture follows best practices (layered, RESTful)\n"
                    + "3. Implementation plan includes proper error handling\n"
                    + "4. Test coverage strategy defined\n"
                    + "The SDLC pipeline has been completed successfully with all artifacts generated.";
        }
    }

    private String generateDefaultResponse(String system, String prompt) {
        String topic = extractTopic(prompt);
        return "Analysis of '" + topic + "':\n\n"
                + "Based on the input, here is a structured response:\n"
                + "- The requirement has been understood\n"
                + "- Key components identified\n"
                + "- Implementation approach defined\n"
                + "- Ready for next phase";
    }

    private String extractTopic(String prompt) {
        // Try to get meaningful content from prompt
        String cleaned = prompt.replaceAll("\\[mock:[^\\]]*\\]\\s*", ""); // Remove mock prefixes
        if (cleaned.length() > 100) {
            return cleaned.substring(0, 100) + "...";
        }
        return cleaned.isEmpty() ? "the given requirement" : cleaned;
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
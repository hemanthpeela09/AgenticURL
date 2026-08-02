package com.example.orchestrator.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Selects the LLM backend from configuration. Default is the offline MockLlm.
 *
 * In Spring context, use the injected Llm bean directly.
 * For non-Spring context (tests, standalone), use the static fromEnv() method.
 *
 * Supported backends (set via AGENT_LLM environment variable or app.llm.backend property):
 * - mock: Offline deterministic mock (default, no API keys needed)
 * - openai: OpenAI ChatGPT (requires OPENAI_API_KEY)
 * - azure: Azure OpenAI (requires AZURE_OPENAI_API_KEY)
 * - ollama: Local Ollama (requires running Ollama server)
 * - anthropic: Anthropic Claude (requires ANTHROPIC_API_KEY)
 */
@Component
public final class LlmFactory {

    private static Llm springManagedLlm;

    @Autowired(required = false)
    public void setLlm(Llm llm) {
        springManagedLlm = llm;
    }

    /**
     * Get LLM from environment. Uses Spring-managed bean if available,
     * otherwise falls back to mock.
     */
    public static Llm fromEnv() {
        // If running in Spring context, use the injected bean
        if (springManagedLlm != null) {
            return springManagedLlm;
        }

        // Fallback for non-Spring context (tests, standalone)
        String backend = System.getenv().getOrDefault("AGENT_LLM", "mock").toLowerCase();

        return switch (backend) {
            case "mock" -> new MockLlm();
            // For real backends in non-Spring context, return mock with a warning
            default -> {
                System.err.println("Warning: Non-mock LLM backend '" + backend +
                        "' requested outside Spring context. Using MockLlm.");
                yield new MockLlm();
            }
        };
    }

    /**
     * Create a mock LLM (always available, no API keys needed).
     */
    public static Llm mock() {
        return new MockLlm();
    }
}
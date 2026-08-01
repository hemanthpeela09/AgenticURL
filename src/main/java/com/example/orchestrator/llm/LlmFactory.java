package com.example.orchestrator.llm;

/**
 * Selectes the LLM backend from configuration. Defualt is the offline MockLLM
 * A real backend (e.g. Open AI) can be added here and enabled via AGENT_LLM
 * without changing any agent code.
 */
public class LlmFactory {
    public LlmFactory() {
    }

    public static Llm fromEnv() {
        String backend = System.getenv().getOrDefault("AGENT_LLM", "mock").toLowerCase();
        // Here you would implement the logic to create an Llm instance based on environment variables
        // For demonstration, we just return a new instance of Llm
        return new MockLlm();
    }
    
}

package com.example.orchestrator.llm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Enhanced LLM interface with additional capabilities for LangChain-style operations.
 * Extends the base Llm interface with streaming, structured output, and chain operations.
 */
public interface EnhancedLlm extends Llm {

    /**
     * Stream completion responses token by token.
     *
     * @param system System prompt defining the AI's role
     * @param prompt User prompt
     * @param tokenConsumer Consumer that receives each token as it's generated
     */
    default void streamComplete(String system, String prompt, Consumer<String> tokenConsumer) {
        // Default implementation: non-streaming fallback
        String result = complete(system, prompt);
        tokenConsumer.accept(result);
    }

    /**
     * Complete with structured output (JSON).
     *
     * @param system System prompt
     * @param prompt User prompt
     * @param outputSchema Expected JSON schema for structured output
     * @return Parsed map from JSON response
     */
    default Map<String, Object> completeStructured(String system, String prompt, Map<String, Object> outputSchema) {
        String result = complete(system, prompt + "\nRespond with JSON matching this schema: " + outputSchema);
        // Basic JSON parsing - in production, use a proper JSON parser
        return Map.of("raw", result);
    }

    /**
     * Complete with chat history (multi-turn conversation).
     *
     * @param system System prompt
     * @param history List of previous messages as role:content pairs
     * @param prompt Current user prompt
     * @return AI response
     */
    default String completeWithHistory(String system, List<Map<String, String>> history, String prompt) {
        StringBuilder context = new StringBuilder();
        for (Map<String, String> msg : history) {
            context.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        context.append("user: ").append(prompt);
        return complete(system, context.toString());
    }

    /**
     * Execute a chain of prompts, where each step can use the output of the previous.
     * Similar to LangChain's SequentialChain.
     *
     * @param system System prompt
     * @param chainSteps List of prompt templates (use {previous} to reference prior output)
     * @return Final output after all chain steps
     */
    default String executeChain(String system, List<String> chainSteps) {
        String previous = "";
        for (String step : chainSteps) {
            String prompt = step.replace("{previous}", previous);
            previous = complete(system, prompt);
        }
        return previous;
    }

    /**
     * Check if this LLM supports streaming.
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * Get the maximum context window size.
     */
    default int maxContextSize() {
        return 4096;
    }
}
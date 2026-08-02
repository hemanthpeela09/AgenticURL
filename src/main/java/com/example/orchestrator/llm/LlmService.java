package com.example.orchestrator.llm;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Spring service wrapper for LLM operations.
 * Provides a convenient injectable service for using LLM capabilities throughout the application.
 */
@Service
public class LlmService {

    private final Llm llm;

    public LlmService(Llm llm) {
        this.llm = llm;
    }

    /**
     * Get the underlying LLM instance.
     */
    public Llm getLlm() {
        return llm;
    }

    /**
     * Get the LLM backend name.
     */
    public String getBackendName() {
        return llm.name();
    }

    /**
     * Simple completion with system and user prompts.
     */
    public String complete(String systemPrompt, String userPrompt) {
        return llm.complete(systemPrompt, userPrompt);
    }

    /**
     * Create a new chain builder.
     */
    public LlmChain newChain() {
        return new LlmChain(llm);
    }

    /**
     * Create an SDLC chain for a given requirement.
     */
    public LlmChain sdlcChain(String requirement) {
        return LlmChain.sdlcChain(llm, requirement);
    }

    /**
     * Create an agent executor with tool-use capabilities.
     */
    public AgentExecutor newAgent() {
        return new AgentExecutor(llm);
    }

    /**
     * Create an SDLC agent with pre-configured tools.
     */
    public AgentExecutor sdlcAgent() {
        return AgentExecutor.sdlcAgent(llm);
    }

    /**
     * Create a new conversation memory.
     */
    public ConversationMemory newMemory() {
        return ConversationMemory.buffer();
    }

    /**
     * Create a sliding window memory.
     */
    public ConversationMemory newSlidingWindowMemory(int windowSize) {
        return ConversationMemory.slidingWindow(windowSize);
    }

    /**
     * Create a summary memory (requires LLM for summarization).
     */
    public ConversationMemory newSummaryMemory(int windowSize) {
        return ConversationMemory.summary(windowSize, llm);
    }

    /**
     * Format a prompt template with variables.
     */
    public String formatTemplate(PromptTemplate template, Map<String, Object> variables) {
        return template.format(variables);
    }

    /**
     * Execute a single prompt using a template.
     */
    public String executeTemplate(String systemPrompt, PromptTemplate template, Map<String, Object> variables) {
        String formattedPrompt = template.format(variables);
        return llm.complete(systemPrompt, formattedPrompt);
    }

    /**
     * Check if streaming is supported.
     */
    public boolean supportsStreaming() {
        return llm instanceof EnhancedLlm enhanced && enhanced.supportsStreaming();
    }

    /**
     * Stream a completion if supported.
     */
    public void streamComplete(String systemPrompt, String userPrompt, java.util.function.Consumer<String> tokenConsumer) {
        if (llm instanceof EnhancedLlm enhanced) {
            enhanced.streamComplete(systemPrompt, userPrompt, tokenConsumer);
        } else {
            // Fallback: send entire response at once
            tokenConsumer.accept(llm.complete(systemPrompt, userPrompt));
        }
    }
}
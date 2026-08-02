package com.example.orchestrator.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * LangChain-style chain implementation for composing LLM calls.
 * Supports sequential chains, parallel chains, and conditional routing.
 */
public class LlmChain {

    private final Llm llm;
    private final List<ChainStep> steps;
    private final Map<String, Object> context;

    public LlmChain(Llm llm) {
        this.llm = llm;
        this.steps = new ArrayList<>();
        this.context = new HashMap<>();
    }

    /**
     * Add a step to the chain.
     *
     * @param name Step name (used as key for output)
     * @param systemPrompt System prompt for this step
     * @param template Prompt template with {variable} placeholders
     * @return this chain for fluent building
     */
    public LlmChain addStep(String name, String systemPrompt, PromptTemplate template) {
        steps.add(new ChainStep(name, systemPrompt, template, null));
        return this;
    }

    /**
     * Add a conditional step that only runs if condition is met.
     *
     * @param name Step name
     * @param systemPrompt System prompt
     * @param template Prompt template
     * @param condition Function that receives context and returns true to execute
     * @return this chain for fluent building
     */
    public LlmChain addConditionalStep(String name, String systemPrompt, PromptTemplate template,
                                       Function<Map<String, Object>, Boolean> condition) {
        steps.add(new ChainStep(name, systemPrompt, template, condition));
        return this;
    }

    /**
     * Set initial context variables.
     */
    public LlmChain withContext(Map<String, Object> initialContext) {
        this.context.putAll(initialContext);
        return this;
    }

    /**
     * Execute the chain and return the accumulated context.
     */
    public Map<String, Object> execute() {
        for (ChainStep step : steps) {
            // Check condition if present
            if (step.condition != null && !step.condition.apply(context)) {
                continue;
            }

            // Format the prompt with current context
            String prompt = step.template.format(context);

            // Execute LLM call
            String result = llm.complete(step.systemPrompt, prompt);

            // Store result in context for next steps
            context.put(step.name, result);
        }

        return new HashMap<>(context);
    }

    /**
     * Execute with streaming output for the final step.
     */
    public void executeWithStreaming(java.util.function.Consumer<String> tokenConsumer) {
        if (steps.isEmpty()) {
            return;
        }

        // Execute all but last step normally
        for (int i = 0; i < steps.size() - 1; i++) {
            ChainStep step = steps.get(i);
            if (step.condition != null && !step.condition.apply(context)) {
                continue;
            }
            String prompt = step.template.format(context);
            String result = llm.complete(step.systemPrompt, prompt);
            context.put(step.name, result);
        }

        // Stream the last step
        ChainStep lastStep = steps.get(steps.size() - 1);
        if (lastStep.condition == null || lastStep.condition.apply(context)) {
            String prompt = lastStep.template.format(context);
            if (llm instanceof EnhancedLlm enhanced) {
                enhanced.streamComplete(lastStep.systemPrompt, prompt, tokenConsumer);
            } else {
                tokenConsumer.accept(llm.complete(lastStep.systemPrompt, prompt));
            }
        }
    }

    /**
     * Internal step representation.
     */
    private record ChainStep(
            String name,
            String systemPrompt,
            PromptTemplate template,
            Function<Map<String, Object>, Boolean> condition
    ) {}

    // ---- Factory methods for common SDLC chains ----

    /**
     * Create a full SDLC chain from requirements to release.
     */
    public static LlmChain sdlcChain(Llm llm, String requirement) {
        return new LlmChain(llm)
                .withContext(Map.of("requirement", requirement))
                .addStep("requirement_spec",
                        "You are a requirements analyst.",
                        PromptTemplate.ANALYST)
                .addStep("design_spec",
                        "You are a software architect.",
                        PromptTemplate.ARCHITECT)
                .addStep("code_manifest",
                        "You are a senior software engineer.",
                        PromptTemplate.IMPLEMENTER)
                .addStep("test_plan",
                        "You are a QA engineer.",
                        PromptTemplate.TEST_AUTHOR);
    }

    /**
     * Create a review chain for code review.
     */
    public static LlmChain reviewChain(Llm llm, String code) {
        return new LlmChain(llm)
                .withContext(Map.of("code", code))
                .addStep("security_review",
                        "You are a security expert.",
                        new PromptTemplate("Review this code for security issues:\n\n{code}"))
                .addStep("quality_review",
                        "You are a code quality expert.",
                        new PromptTemplate("Review this code for quality and best practices:\n\n{code}\n\nSecurity findings: {security_review}"))
                .addStep("summary",
                        "You are a technical lead.",
                        new PromptTemplate("Summarize these code review findings:\n\nSecurity: {security_review}\n\nQuality: {quality_review}"));
    }
}
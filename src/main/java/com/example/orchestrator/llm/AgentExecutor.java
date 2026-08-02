package com.example.orchestrator.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LangChain-style Agent Executor with tool/function calling capabilities.
 * Supports ReAct-style reasoning and tool use.
 */
public class AgentExecutor {

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "Action:\\s*(\\w+)\\s*Action Input:\\s*(.+?)(?=\\nObservation:|$)",
            Pattern.DOTALL
    );

    private final Llm llm;
    private final Map<String, Tool> tools;
    private final int maxIterations;
    private final ConversationMemory memory;

    public AgentExecutor(Llm llm) {
        this(llm, 10);
    }

    public AgentExecutor(Llm llm, int maxIterations) {
        this.llm = llm;
        this.tools = new HashMap<>();
        this.maxIterations = maxIterations;
        this.memory = ConversationMemory.slidingWindow(20);
    }

    /**
     * Register a tool that the agent can use.
     *
     * @param name Tool name
     * @param description Description of what the tool does
     * @param function The actual function to execute
     * @return this executor for fluent building
     */
    public AgentExecutor addTool(String name, String description, Function<String, String> function) {
        tools.put(name, new Tool(name, description, function));
        return this;
    }

    /**
     * Run the agent with a given input.
     * The agent will use ReAct-style reasoning to decide which tools to use.
     *
     * @param input User input/question
     * @return Final answer
     */
    public AgentResult run(String input) {
        List<AgentStep> steps = new ArrayList<>();
        String systemPrompt = buildSystemPrompt();

        memory.addUserMessage(input);
        String currentInput = input;

        for (int i = 0; i < maxIterations; i++) {
            String thought = llm.complete(systemPrompt, buildPrompt(currentInput, steps));

            // Check if we have a final answer
            if (thought.contains("Final Answer:")) {
                String answer = extractFinalAnswer(thought);
                memory.addAssistantMessage(answer);
                return new AgentResult(answer, steps, true);
            }

            // Try to extract tool call
            Matcher matcher = TOOL_CALL_PATTERN.matcher(thought);
            if (matcher.find()) {
                String toolName = matcher.group(1).trim();
                String toolInput = matcher.group(2).trim();

                Tool tool = tools.get(toolName);
                String observation;
                if (tool != null) {
                    try {
                        observation = tool.function.apply(toolInput);
                    } catch (Exception e) {
                        observation = "Error: " + e.getMessage();
                    }
                } else {
                    observation = "Error: Tool '" + toolName + "' not found. Available tools: " + tools.keySet();
                }

                steps.add(new AgentStep(thought, toolName, toolInput, observation));
                currentInput = input; // Reset to original input, context is in steps
            } else {
                // No tool call and no final answer - treat the response as the answer
                memory.addAssistantMessage(thought);
                return new AgentResult(thought, steps, true);
            }
        }

        // Max iterations reached
        String partialAnswer = "I was unable to complete the task within " + maxIterations + " steps.";
        return new AgentResult(partialAnswer, steps, false);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful AI assistant that can use tools to answer questions.\n\n");
        sb.append("You have access to the following tools:\n\n");

        for (Tool tool : tools.values()) {
            sb.append("- ").append(tool.name).append(": ").append(tool.description).append("\n");
        }

        sb.append("\nTo use a tool, respond with:\n");
        sb.append("Thought: [your reasoning about what to do]\n");
        sb.append("Action: [tool name]\n");
        sb.append("Action Input: [input to the tool]\n");
        sb.append("After receiving an observation, continue reasoning.\n");
        sb.append("When you have enough information, respond with:\n");
        sb.append("Thought: [final reasoning]\n");
        sb.append("Final Answer: [your complete answer]\n");

        return sb.toString();
    }

    private String buildPrompt(String input, List<AgentStep> steps) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(input).append("\n\n");

        for (AgentStep step : steps) {
            sb.append(step.thought).append("\n");
            sb.append("Observation: ").append(step.observation).append("\n\n");
        }

        sb.append("Thought:");
        return sb.toString();
    }

    private String extractFinalAnswer(String thought) {
        int idx = thought.indexOf("Final Answer:");
        if (idx >= 0) {
            return thought.substring(idx + 13).trim();
        }
        return thought;
    }

    /**
     * Get the conversation memory.
     */
    public ConversationMemory getMemory() {
        return memory;
    }

    /**
     * Clear the conversation memory.
     */
    public void clearMemory() {
        memory.clear();
    }

    // ---- Inner classes ----

    private record Tool(String name, String description, Function<String, String> function) {}

    public record AgentStep(String thought, String action, String actionInput, String observation) {}

    public record AgentResult(String output, List<AgentStep> steps, boolean success) {}

    // ---- Factory methods for common agents ----

    /**
     * Create an SDLC assistant agent with common development tools.
     */
    public static AgentExecutor sdlcAgent(Llm llm) {
        return new AgentExecutor(llm)
                .addTool("analyze_requirement",
                        "Analyze a requirement and identify ambiguities",
                        input -> "Analyzed: " + input + "\nKey points: functional needs, NFRs, ambiguities")
                .addTool("design_component",
                        "Design a software component based on requirements",
                        input -> "Designed component for: " + input + "\nIncludes: API, data model, dependencies")
                .addTool("generate_tests",
                        "Generate test cases for a component",
                        input -> "Generated tests for: " + input + "\nIncludes: unit, integration, edge cases")
                .addTool("review_code",
                        "Review code for quality and security issues",
                        input -> "Reviewed: " + input + "\nFindings: style, security, performance");
    }
}
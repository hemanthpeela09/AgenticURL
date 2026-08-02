package com.example.orchestrator.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Enhanced Spring AI LLM with streaming and chain support.
 * Provides LangChain-style capabilities using Spring AI abstractions.
 */
public class EnhancedSpringAiLlm implements EnhancedLlm {

    private final ChatClient chatClient;
    private final String modelName;

    public EnhancedSpringAiLlm(ChatClient chatClient, String modelName) {
        this.chatClient = chatClient;
        this.modelName = modelName;
    }

    @Override
    public String name() {
        return "enhanced-spring-ai:" + modelName;
    }

    @Override
    public String complete(String system, String prompt) {
        try {
            Prompt chatPrompt = new Prompt(List.of(
                    new SystemMessage(system),
                    new UserMessage(prompt)
            ));

            return chatClient.prompt(chatPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            return "[error:" + modelName + "] " + e.getMessage();
        }
    }

    @Override
    public void streamComplete(String system, String prompt, Consumer<String> tokenConsumer) {
        try {
            Prompt chatPrompt = new Prompt(List.of(
                    new SystemMessage(system),
                    new UserMessage(prompt)
            ));

            chatClient.prompt(chatPrompt)
                    .stream()
                    .content()
                    .subscribe(tokenConsumer::accept);
        } catch (Exception e) {
            tokenConsumer.accept("[error:" + modelName + "] " + e.getMessage());
        }
    }

    @Override
    public String completeWithHistory(String system, List<Map<String, String>> history, String prompt) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(system));

            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equalsIgnoreCase(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equalsIgnoreCase(role)) {
                    messages.add(new AssistantMessage(content));
                }
            }
            messages.add(new UserMessage(prompt));

            Prompt chatPrompt = new Prompt(messages);
            return chatClient.prompt(chatPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            return "[error:" + modelName + "] " + e.getMessage();
        }
    }

    @Override
    public Map<String, Object> completeStructured(String system, String prompt, Map<String, Object> outputSchema) {
        try {
            String enhancedPrompt = prompt +
                    "\n\nRespond ONLY with valid JSON matching this structure: " + outputSchema +
                    "\nDo not include any explanation or markdown formatting.";

            String result = complete(system, enhancedPrompt);

            // In production, use Jackson or Gson for proper JSON parsing
            // For now, return raw result wrapped in a map
            return Map.of("response", result, "model", modelName);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public String executeChain(String system, List<String> chainSteps) {
        String previous = "";
        StringBuilder chainLog = new StringBuilder();

        for (int i = 0; i < chainSteps.size(); i++) {
            String step = chainSteps.get(i).replace("{previous}", previous);
            chainLog.append("Step ").append(i + 1).append(": ");

            previous = complete(system, step);
            chainLog.append(previous.substring(0, Math.min(50, previous.length()))).append("...\n");
        }

        return previous;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public int maxContextSize() {
        // GPT-4 has 128k context, but we use a conservative default
        return 128000;
    }
}
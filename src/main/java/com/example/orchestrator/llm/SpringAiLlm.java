package com.example.orchestrator.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Spring AI-backed LLM implementation.
 * Supports multiple providers (OpenAI, Azure OpenAI, Ollama, Anthropic) through Spring AI abstraction.
 * The actual provider is determined by Spring Boot auto-configuration based on dependencies and properties.
 */
public class SpringAiLlm implements Llm {

    private final ChatClient chatClient;
    private final String modelName;

    public SpringAiLlm(ChatClient chatClient, String modelName) {
        this.chatClient = chatClient;
        this.modelName = modelName;
    }

    @Override
    public String name() {
        return "spring-ai:" + modelName;
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
            // Log and return error message - don't fail silently
            return "[error:" + modelName + "] " + e.getMessage();
        }
    }
}
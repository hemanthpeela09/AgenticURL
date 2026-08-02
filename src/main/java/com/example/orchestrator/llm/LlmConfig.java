package com.example.orchestrator.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI LLM Configuration.
 * Configures the appropriate LLM backend based on the app.llm.backend property.
 *
 * Supported backends:
 * - mock: Offline deterministic mock (default, no API keys needed)
 * - openai: OpenAI ChatGPT (requires OPENAI_API_KEY)
 * - azure: Azure OpenAI (requires AZURE_OPENAI_API_KEY and AZURE_OPENAI_ENDPOINT)
 * - ollama: Local Ollama (requires running Ollama server)
 * - anthropic: Anthropic Claude (requires ANTHROPIC_API_KEY)
 */
@Configuration
public class LlmConfig {

    @Value("${app.llm.backend:mock}")
    private String llmBackend;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String openaiModel;

    /**
     * Creates a ChatClient from the auto-configured ChatModel.
     * Spring AI auto-configures the appropriate ChatModel based on dependencies.
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Mock LLM - used when app.llm.backend=mock (default).
     * No API keys required, runs entirely offline.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.llm.backend", havingValue = "mock", matchIfMissing = true)
    public Llm mockLlm() {
        return new MockLlm();
    }

    /**
     * Spring AI LLM - used when app.llm.backend is set to a real provider.
     * Supports: openai, azure, ollama, anthropic
     */
    @Bean
    @ConditionalOnProperty(name = "app.llm.backend", havingValue = "openai")
    public Llm openaiLlm(ChatClient chatClient) {
        return new SpringAiLlm(chatClient, openaiModel);
    }

    /**
     * Azure OpenAI LLM
     */
    @Bean
    @ConditionalOnProperty(name = "app.llm.backend", havingValue = "azure")
    public Llm azureLlm(ChatClient chatClient) {
        return new SpringAiLlm(chatClient, "azure-openai");
    }

    /**
     * Ollama (local) LLM
     */
    @Bean
    @ConditionalOnProperty(name = "app.llm.backend", havingValue = "ollama")
    public Llm ollamaLlm(ChatModel chatModel) {
        ChatClient client = ChatClient.builder(chatModel).build();
        return new SpringAiLlm(client, "ollama");
    }

    /**
     * Anthropic Claude LLM
     */
    @Bean
    @ConditionalOnProperty(name = "app.llm.backend", havingValue = "anthropic")
    public Llm anthropicLlm(ChatClient chatClient) {
        return new SpringAiLlm(chatClient, "anthropic");
    }
}
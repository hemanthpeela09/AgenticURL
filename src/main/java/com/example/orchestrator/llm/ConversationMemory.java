package com.example.orchestrator.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * LangChain-style conversation memory for maintaining chat history.
 * Supports different memory strategies: buffer, summary, and sliding window.
 */
public class ConversationMemory {

    public enum MemoryType {
        BUFFER,         // Keep all messages
        SLIDING_WINDOW, // Keep last N messages
        SUMMARY         // Summarize old messages
    }

    private final MemoryType type;
    private final int windowSize;
    private final LinkedList<Message> messages;
    private final Llm llm;
    private String summary;

    public ConversationMemory() {
        this(MemoryType.BUFFER, 10, null);
    }

    public ConversationMemory(MemoryType type, int windowSize, Llm llm) {
        this.type = type;
        this.windowSize = windowSize;
        this.llm = llm;
        this.messages = new LinkedList<>();
        this.summary = "";
    }

    /**
     * Add a user message to memory.
     */
    public void addUserMessage(String content) {
        addMessage("user", content);
    }

    /**
     * Add an assistant message to memory.
     */
    public void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }

    /**
     * Add a system message to memory.
     */
    public void addSystemMessage(String content) {
        addMessage("system", content);
    }

    private void addMessage(String role, String content) {
        messages.add(new Message(role, content, System.currentTimeMillis()));

        if (type == MemoryType.SLIDING_WINDOW && messages.size() > windowSize) {
            messages.removeFirst();
        } else if (type == MemoryType.SUMMARY && messages.size() > windowSize && llm != null) {
            summarizeOldMessages();
        }
    }

    /**
     * Get messages formatted for LLM context.
     */
    public List<Map<String, String>> getMessages() {
        List<Map<String, String>> result = new ArrayList<>();

        // Add summary if using summary memory
        if (type == MemoryType.SUMMARY && !summary.isEmpty()) {
            result.add(Map.of("role", "system", "content", "Previous conversation summary: " + summary));
        }

        for (Message msg : messages) {
            result.add(Map.of("role", msg.role, "content", msg.content));
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Get the full conversation as a formatted string.
     */
    public String getConversationString() {
        StringBuilder sb = new StringBuilder();

        if (!summary.isEmpty()) {
            sb.append("[Previous context: ").append(summary).append("]\n\n");
        }

        for (Message msg : messages) {
            sb.append(msg.role).append(": ").append(msg.content).append("\n");
        }

        return sb.toString();
    }

    /**
     * Clear all messages.
     */
    public void clear() {
        messages.clear();
        summary = "";
    }

    /**
     * Get the number of messages in memory.
     */
    public int size() {
        return messages.size();
    }

    /**
     * Estimate token count (rough approximation: ~4 chars per token).
     */
    public int estimateTokens() {
        int chars = summary.length();
        for (Message msg : messages) {
            chars += msg.content.length() + msg.role.length() + 4; // role: content\n
        }
        return chars / 4;
    }

    private void summarizeOldMessages() {
        if (messages.size() <= windowSize / 2) {
            return;
        }

        // Take oldest half of messages to summarize
        int toSummarize = messages.size() - windowSize / 2;
        StringBuilder toSummarizeText = new StringBuilder();

        for (int i = 0; i < toSummarize; i++) {
            Message msg = messages.removeFirst();
            toSummarizeText.append(msg.role).append(": ").append(msg.content).append("\n");
        }

        // Generate summary
        String previousSummary = summary.isEmpty() ? "" : "Previous summary: " + summary + "\n\n";
        summary = llm.complete(
                "You are a conversation summarizer. Create a brief summary of the key points.",
                previousSummary + "Conversation to summarize:\n" + toSummarizeText
        );
    }

    /**
     * Create a buffer memory (keeps all messages).
     */
    public static ConversationMemory buffer() {
        return new ConversationMemory(MemoryType.BUFFER, Integer.MAX_VALUE, null);
    }

    /**
     * Create a sliding window memory (keeps last N messages).
     */
    public static ConversationMemory slidingWindow(int windowSize) {
        return new ConversationMemory(MemoryType.SLIDING_WINDOW, windowSize, null);
    }

    /**
     * Create a summary memory (summarizes old messages).
     */
    public static ConversationMemory summary(int windowSize, Llm llm) {
        return new ConversationMemory(MemoryType.SUMMARY, windowSize, llm);
    }

    private record Message(String role, String content, long timestamp) {}
}
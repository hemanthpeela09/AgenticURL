package com.example.orchestrator.llm;

public class MockLlm implements Llm {
    
    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String complete(String system, String prompt) {
        // Mock implementation of the generate method
        int h = (system + "|" + prompt).hashCode() & 0xffff;
        String snippet = prompt.length() > 64 ? prompt.substring(0, 64) : prompt;
        System.out.println("MockLlm: Generating response for prompt: " + snippet + " (hash: " + h + ")");
        return "[Mock response for prompt: " + snippet + " (hash: " + h + ")]";
    }

}
